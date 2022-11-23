package ksl.utilities.dbutil

import ksl.examples.book.chapter6.DriveThroughPharmacy
import ksl.modeling.variable.Counter
import ksl.modeling.variable.Response
import ksl.modeling.variable.TWResponse
import ksl.simulation.Model
import ksl.simulation.ModelElement
import ksl.utilities.io.KSL
import ksl.utilities.random.rvariable.ExponentialRV
import ksl.utilities.statistic.BatchStatisticIfc
import ksl.utilities.statistic.StatisticIfc
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.ktorm.database.asIterable
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.logging.Slf4jLoggerAdapter
import org.ktorm.schema.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZonedDateTime

class KSLDatabase(private val db: Database, clearDataOption: Boolean = false) {

    private val kDb =
        org.ktorm.database.Database.connect(db.dataSource, logger = Slf4jLoggerAdapter(DatabaseIfc.logger))

    internal var simulationRun: SimulationRun? = null
    private val simulationRuns get() = kDb.sequenceOf(SimulationRuns, withReferences = false)
    private val dbModelElements get() = kDb.sequenceOf(DbModelElements, withReferences = false)
    private val withinRepStats get() = kDb.sequenceOf(WithRepStats, withReferences = false)
    private val acrossRepStats get() = kDb.sequenceOf(AcrossRepStats, withReferences = false)
    private val withinRepCounterStats get() = kDb.sequenceOf(WithinRepCounterStats, withReferences = false)
    private val batchStats get() = kDb.sequenceOf(BatchStats, withReferences = false)

    val label = db.label

    val acrossReplicationStatistics: DataFrame<AcrossRepStat>
        get() {
            var df = acrossRepStats.toList().toDataFrame()
            df = df.move("simRunIdFk").to(0)
                .move("id").to(1)
                .move("statName").to(2)
                .move("statCount").to(3)
                .move("average").to(4)
                .move("halfWidth").to(5)
                .move("stdDev").to(6)
                .move("stdError").to(7)
                .move("minimum").to(8)
                .move("maximum").to(9)
                .move("confLevel").to(10)
            df = df.remove("entityClass", "properties", "elementIdFk")
            return df
        }

    val withinReplicationStatistics: DataFrame<WithinRepStat>
        get() {
            var df = withinRepStats.toList().toDataFrame()
            df = df.move("simRunIdFk").to(0)
                .move("id").to(1)
                .move("statName").to(2)
                .move("repNum").to(3)
                .move("statCount").to(4)
                .move("average").to(5)
                .move("minimum").to(6)
                .move("maximum").to(7)
                .remove("entityClass", "properties", "elementIdFk")
            return df
        }

    val withinReplicationCounterStatistics: DataFrame<WithinRepCounterStat>
        get() {
            var df = withinRepCounterStats.toList().toDataFrame()
            df = df.move("simRunIdFk").to(0)
                .move("id").to(1)
                .move("statName").to(2)
                .move("repNum").to(3)
                .move("lastValue").to(4)
                .remove("entityClass", "properties", "elementIdFk")
            return df
        }

    val batchingStatistics: DataFrame<BatchStat>
        get() {
            var df = batchStats.toList().toDataFrame()
            df = df.move("simRunIdFk").to(0)
                .move("id").to(1)
                .move("statName").to(2)
                .move("repNum").to(3)
                .move("statCount").to(4)
                .move("average").to(5)
                .move("halfWidth").to(6)
                .move("stdDev").to(7)
                .move("stdError").to(8)
                .move("minimum").to(9)
                .move("maximum").to(10)
                .move("confLevel").to(11)
            df = df.remove("entityClass", "properties", "elementIdFk")
            return df
        }

    val tables = listOf(
        SimulationRuns, DbModelElements, WithRepStats,
        AcrossRepStats, WithinRepCounterStats, BatchStats
    )

    init {
        val check = checkTableNames()
        if (!check) {
            DatabaseIfc.logger.error { "The database does not have the required tables for a KSLDatabase" }
        }
        if (clearDataOption) {
            clearAllData()
        }
    }

    private fun checkTableNames(): Boolean {
        //check if supplied database is configured as KSL database
        // by checking if the names of the tables match with the KSL table names
        // an admittedly poor test, but it is something
        val tableNames = if (db.defaultSchemaName != null) {
            db.tableNames(db.defaultSchemaName!!)
        } else {
            db.userDefinedTables
        }
        for (name in TableNames) {
            if (!containsTableName(name, tableNames)) {
                return false
            }
        }
        return true
    }

    private fun containsTableName(name: String, list: List<String>): Boolean {
        for (tn in list) {
            if (tn.equals(name, true)) {
                return true
            }
        }
        return false
    }

    fun clearAllData() {
        // remove all data from user tables
//        kDb.deleteAll(BatchStats)
//        kDb.deleteAll(WithinRepCounterStats)
//        kDb.deleteAll(AcrossRepStats)
//        kDb.deleteAll(WithRepStats)
//        kDb.deleteAll(DbModelElements)
//        kDb.deleteAll(SimulationRuns)
        for (table in tables.asReversed()) {
            kDb.deleteAll(table)
        }
        DatabaseIfc.logger.info { "Cleared data for KSLDatabase ${db.label}" }
    }

    fun acrossReplicationRecords(): QueryRowSet {
        val query: Query = kDb.from(AcrossRepStats).select()
        return kDb.from(AcrossRepStats).select().rowSet
    }


    internal fun beforeExperiment(model: Model) {
        // start simulation run record
        insertSimulationRun(model)
        // insert the model elements into the database
        val modelElements: List<ModelElement> = model.getModelElements()
        insertModelElements(modelElements)
    }

    private fun insertModelElements(elements: List<ModelElement>) {
        // it would be nice to know how to make a batch insert rather each individually
        for (element in elements) {
            val dbModelElement = createDbModelElement(element, simulationRun!!.id)
            dbModelElements.add(dbModelElement)
        }
        DatabaseIfc.logger.trace { "Inserted model element records into ${db.label} for simulation ${simulationRun?.modelName}" }
    }

    private fun createDbModelElement(element: ModelElement, id: Int): DbModelElement {
        val dbm = DbModelElement()
        dbm.simRunIDFk = id
        dbm.elementName = element.name
        dbm.elementId = element.id
        dbm.elementClassName = element::class.simpleName!!
        if (element.myParentModelElement != null) {
            dbm.parentIDFk = element.myParentModelElement!!.id
            dbm.parentName = element.myParentModelElement!!.name
        }
        dbm.leftCount = element.leftTraversalCount
        dbm.rightCount = element.rightTraversalCount
        return dbm
    }

    private fun insertSimulationRun(model: Model) {
        val record = SimulationRun()
        record.simName = model.simulationName
        record.expName = model.experimentName
        record.modelName = model.name
        record.expStartTimeStamp = ZonedDateTime.now().toInstant()
        record.numReps = model.numberOfReplications
        if (!model.lengthOfReplication.isNaN() && model.lengthOfReplication.isFinite()) {
            record.lengthOfRep = model.lengthOfReplication
        }
        record.lengthOfWarmUp = model.lengthOfReplicationWarmUp
        record.repAllowedExecTime = model.maximumAllowedExecutionTime.inWholeMilliseconds
        record.repInitOption = model.replicationInitializationOption
        record.repResetStartStreamOption = model.resetStartStreamOption
        record.antitheticOption = model.antitheticOption
        record.advNextSubStreamOption = model.advanceNextSubStreamOption
        record.numStreamAdvances = model.numberOfStreamAdvancesPriorToRunning
        simulationRuns.add(record)
        simulationRun = record
    }

    private fun finalizeCurrentSimulationRun(model: Model) {
        simulationRun?.lastRep = model.numberReplicationsCompleted
        simulationRun?.hasMoreReps = model.hasMoreReplications()
        simulationRun?.expEndTimeStamp = ZonedDateTime.now().toInstant()
        simulationRun?.flushChanges()
        DatabaseIfc.logger.trace { "Finalized SimulationRun record for model: ${model.name}" }
    }

    internal fun afterReplication(model: Model) {
        // insert the within replication statistics
        insertWithinRepResponses(model.responses)
        // insert the within replication counters
        insertWithinRepCounters(model.counters)
        // insert the batch statistics if available
        if (model.batchingElement != null) {
            val rMap = model.batchingElement!!.allResponseBatchStatisticsAsMap
            val twMap = model.batchingElement!!.allTimeWeightedBatchStatisticsAsMap
            insertResponseVariableBatchStatistics(rMap)
            insertTimeWeightedBatchStatistics(twMap)
        }
    }

    private fun insertWithinRepResponses(responses: List<Response>) {
        for (response in responses) {
            val withinRepStatRecord = createWithinRepStatRecord(response, simulationRun!!.id)
            withinRepStats.add(withinRepStatRecord)
        }
        DatabaseIfc.logger.trace { "Inserted within replication responses into ${db.label} for simulation ${simulationRun?.modelName}" }
    }

    private fun createWithinRepStatRecord(response: Response, simId: Int): WithinRepStat {
        val r = WithinRepStat()
        r.elementIdFk = response.id
        r.simRunIdFk = simId
        r.repNum = response.model.currentReplicationNumber
        val s = response.withinReplicationStatistic
        r.statName = s.name
        if (!s.count.isNaN() && s.count.isFinite()) {
            r.statCount = s.count
        }
        if (!s.weightedAverage.isNaN() && s.weightedAverage.isFinite()) {
            r.average = s.weightedAverage
        }
        if (!s.min.isNaN() && s.min.isFinite()) {
            r.minimum = s.min
        }
        if (!s.max.isNaN() && s.max.isFinite()) {
            r.maximum = s.max
        }
        if (!s.weightedSum.isNaN() && s.weightedSum.isFinite()) {
            r.weightedSum = s.weightedSum
        }
        if (!s.sumOfWeights.isNaN() && s.sumOfWeights.isFinite()) {
            r.sumOfWeights = s.sumOfWeights
        }
        if (!s.weightedSumOfSquares.isNaN() && s.weightedSumOfSquares.isFinite()) {
            r.weightedSSQ = s.weightedSumOfSquares
        }
        if (!s.lastValue.isNaN() && s.lastValue.isFinite()) {
            r.lastValue = s.lastValue
        }
        if (!s.lastWeight.isNaN() && s.lastWeight.isFinite()) {
            r.lastWeight = s.lastWeight
        }
        return r
    }

    private fun insertWithinRepCounters(counters: List<Counter>) {
        for (counter in counters) {
            val withinRepCounterRecord = createWithinRepCounterRecord(counter, simulationRun!!.id)
            withinRepCounterStats.add(withinRepCounterRecord)
        }
        DatabaseIfc.logger.trace { "Inserted within replication counters into ${db.label} for simulation ${simulationRun?.modelName}" }
    }

    private fun createWithinRepCounterRecord(counter: Counter, simId: Int): WithinRepCounterStat {
        val r = WithinRepCounterStat()
        r.elementIdFk = counter.id
        r.simRunIdFk = simId
        r.repNum = counter.model.currentReplicationNumber
        r.statName = counter.name
        if (!counter.value.isNaN() && counter.value.isFinite()) {
            r.lastValue = counter.value
        }
        return r
    }

    private fun insertTimeWeightedBatchStatistics(twMap: Map<TWResponse, BatchStatisticIfc>) {
        for (entry in twMap.entries.iterator()) {
            val tw = entry.key
            val bs = entry.value
            val batchStatRecord = createBatchStatRecord(tw, simulationRun!!.id, bs)
            batchStats.add(batchStatRecord)
        }
        DatabaseIfc.logger.trace { "Inserted within time-weighted batch statistics into ${db.label} for simulation ${simulationRun?.modelName}" }
    }

    private fun createBatchStatRecord(response: Response, simId: Int, s: BatchStatisticIfc): BatchStat {
        val r = BatchStat()
        r.elementIdFk = response.id
        r.simRunIdFk = simId
        r.repNum = response.model.currentReplicationNumber
        r.statName = s.name
        if (!s.count.isNaN() && s.count.isFinite()) {
            r.statCount = s.count
        }
        if (!s.average.isNaN() && s.average.isFinite()) {
            r.average = s.average
        }
        if (!s.standardDeviation.isNaN() && s.standardDeviation.isFinite()) {
            r.stdDev = s.standardDeviation
        }
        if (!s.standardError.isNaN() && s.standardError.isFinite()) {
            r.stdError = s.standardError
        }
        if (!s.halfWidth.isNaN() && s.halfWidth.isFinite()) {
            r.halfWidth = s.halfWidth
        }
        if (!s.confidenceLevel.isNaN() && s.confidenceLevel.isFinite()) {
            r.confLevel = s.confidenceLevel
        }
        if (!s.min.isNaN() && s.min.isFinite()) {
            r.minimum = s.min
        }
        if (!s.max.isNaN() && s.max.isFinite()) {
            r.maximum = s.max
        }
        if (!s.sum.isNaN() && s.sum.isFinite()) {
            r.sumOfObs = s.sum
        }
        if (!s.deviationSumOfSquares.isNaN() && s.deviationSumOfSquares.isFinite()) {
            r.devSSQ = s.deviationSumOfSquares
        }
        if (!s.lastValue.isNaN() && s.lastValue.isFinite()) {
            r.lastValue = s.lastValue
        }
        if (!s.kurtosis.isNaN() && s.kurtosis.isFinite()) {
            r.kurtosis = s.kurtosis
        }
        if (!s.skewness.isNaN() && s.skewness.isFinite()) {
            r.skewness = s.skewness
        }
        if (!s.lag1Covariance.isNaN() && s.lag1Covariance.isFinite()) {
            r.lag1Cov = s.lag1Covariance
        }
        if (!s.lag1Correlation.isNaN() && s.lag1Correlation.isFinite()) {
            r.lag1Corr = s.lag1Correlation
        }
        if (!s.vonNeumannLag1TestStatistic.isNaN() && s.vonNeumannLag1TestStatistic.isFinite()) {
            r.vonNeumanLag1Stat = s.vonNeumannLag1TestStatistic
        }
        if (!s.numberMissing.isNaN() && s.numberMissing.isFinite()) {
            r.numMissingObs = s.numberMissing
        }
        r.minBatchSize = s.minBatchSize.toDouble()
        r.minNumBatches = s.minNumBatches.toDouble()
        r.maxNumBatchesMultiple = s.minNumBatchesMultiple.toDouble()
        r.maxNumBatches = s.maxNumBatches.toDouble()
        r.numRebatches = s.numRebatches.toDouble()
        r.currentBatchSize = s.currentBatchSize.toDouble()
        if (!s.amountLeftUnbatched.isNaN() && s.amountLeftUnbatched.isFinite()) {
            r.amtUnbatched = s.amountLeftUnbatched
        }
        if (!s.totalNumberOfObservations.isNaN() && s.totalNumberOfObservations.isFinite()) {
            r.totalNumObs = s.totalNumberOfObservations
        }
        return r
    }

    private fun insertResponseVariableBatchStatistics(rMap: Map<Response, BatchStatisticIfc>) {
        for (entry in rMap.entries.iterator()) {
            val r = entry.key
            val bs = entry.value
            val batchStatRecord = createBatchStatRecord(r, simulationRun!!.id, bs)
            batchStats.add(batchStatRecord)
        }
        DatabaseIfc.logger.trace { "Inserted within response batch statistics into ${db.label} for simulation ${simulationRun?.modelName}" }
    }

    internal fun afterExperiment(model: Model) {
        println("KSLDatabase: in after Experiment")
        // finalize current simulation run record
        finalizeCurrentSimulationRun(model)
        // insert across replication response statistics
        insertAcrossRepResponses(model.responses)
        // insert across replication counter statistics
        insertAcrossRepResponsesForCounters(model.counters)
    }

    private fun insertAcrossRepResponses(responses: List<Response>) {
        for (response in responses) {
            val s = response.acrossReplicationStatistic
            val acrossRepStatRecord = createAcrossRepStatRecord(response, simulationRun!!.id, s)
            acrossRepStats.add(acrossRepStatRecord)
        }
        DatabaseIfc.logger.trace { "Inserted within across replication statistics into ${db.label} for simulation ${simulationRun?.modelName}" }
    }

    private fun createAcrossRepStatRecord(response: ModelElement, simId: Int, s: StatisticIfc): AcrossRepStat {
        val r = AcrossRepStat()
        r.elementIdFk = response.id
        r.simRunIdFk = simId
        r.statName = s.name
        if (!s.count.isNaN() && s.count.isFinite()) {
            r.statCount = s.count
        }
        if (!s.average.isNaN() && s.average.isFinite()) {
            r.average = s.average
        }
        if (!s.standardDeviation.isNaN() && s.standardDeviation.isFinite()) {
            r.stdDev = s.standardDeviation
        }
        if (!s.standardError.isNaN() && s.standardError.isFinite()) {
            r.stdError = s.standardError
        }
        if (!s.halfWidth.isNaN() && s.halfWidth.isFinite()) {
            r.halfWidth = s.halfWidth
        }
        if (!s.confidenceLevel.isNaN() && s.confidenceLevel.isFinite()) {
            r.confLevel = s.confidenceLevel
        }
        if (!s.min.isNaN() && s.min.isFinite()) {
            r.minimum = s.min
        }
        if (!s.max.isNaN() && s.max.isFinite()) {
            r.maximum = s.max
        }
        if (!s.sum.isNaN() && s.sum.isFinite()) {
            r.sumOfObs = s.sum
        }
        if (!s.deviationSumOfSquares.isNaN() && s.deviationSumOfSquares.isFinite()) {
            r.devSSQ = s.deviationSumOfSquares
        }
        if (!s.lastValue.isNaN() && s.lastValue.isFinite()) {
            r.lastValue = s.lastValue
        }
        if (!s.kurtosis.isNaN() && s.kurtosis.isFinite()) {
            r.kurtosis = s.kurtosis
        }
        if (!s.skewness.isNaN() && s.skewness.isFinite()) {
            r.skewness = s.skewness
        }
        if (!s.lag1Covariance.isNaN() && s.lag1Covariance.isFinite()) {
            r.lag1Cov = s.lag1Covariance
        }
        if (!s.lag1Correlation.isNaN() && s.lag1Correlation.isFinite()) {
            r.lag1Corr = s.lag1Correlation
        }
        if (!s.vonNeumannLag1TestStatistic.isNaN() && s.vonNeumannLag1TestStatistic.isFinite()) {
            r.vonNeumanLag1Stat = s.vonNeumannLag1TestStatistic
        }
        if (!s.numberMissing.isNaN() && s.numberMissing.isFinite()) {
            r.numMissingObs = s.numberMissing
        }
        return r
    }

    private fun insertAcrossRepResponsesForCounters(counters: List<Counter>) {
        for (counter in counters) {
            val s = counter.acrossReplicationStatistic
            val acrossRepCounterRecord = createAcrossRepStatRecord(counter, simulationRun!!.id, s)
            acrossRepStats.add(acrossRepCounterRecord)
        }
        DatabaseIfc.logger.trace { "Inserted within across replication counter statistics into ${db.label} for simulation ${simulationRun?.modelName}" }
    }

    /**
     * Deletes all simulation data associated with the supplied model. In other
     * words, the simulation run data associated with a simulation with the
     * name and the experiment with the name.
     *
     * @param model the model to clear data from
     */
    fun clearSimulationData(model: Model) {
        val simName = model.simulationName
        val expName = model.experimentName
        // find the record and delete it. This should cascade all related records
        deleteSimulationRunRecord(simName, expName)
    }

    /**
     * The combination of simName and expName should be unique within the database. Many
     * experiments can be run with different names for the same simulation. This method
     * deletes the simulation run record with the provided names AND all related data
     * associated with that simulation run.  If a SIMULATION_RUN record does not
     * exist with the simName and expName combination, nothing occurs.
     *
     * @param simName the name of the simulation
     * @param expName the experiment name for the simulation
     * @return true if the record was deleted, false if it was not
     */
    fun deleteSimulationRunRecord(simName: String, expName: String): Boolean {
        val sr: SimulationRun? = simulationRuns.find { (it.simName like simName) and (it.expName like expName) }
        if (sr != null) {
            val result = sr.delete()
            DatabaseIfc.logger.trace { "Deleted SimulationRun for simulation $simName in experiment $expName" }
            return result == 1
        }
        return false
    }

    fun doesSimulationRunRecordExist(simName: String, expName: String): Boolean {
        val sr: SimulationRun? = simulationRuns.find { (it.simName like simName) and (it.expName like expName) }
        return sr != null
    }

    object SimulationRuns : Table<SimulationRun>("SIMULATION_RUN") {
        var id = int("ID").primaryKey().bindTo { it.id }
        var simName = varchar("SIM_NAME").bindTo { it.simName }.isNotNull()
        var modelName = varchar("MODEL_NAME").bindTo { it.modelName }.isNotNull()
        var expName = varchar("EXP_NAME").bindTo { it.expName }.isNotNull()
        var expStartTimeStamp = timestamp("EXP_START_TIME_STAMP").bindTo { it.expStartTimeStamp }
        var expEndTimeStamp = timestamp("EXP_END_TIME_STAMP").bindTo { it.expEndTimeStamp }
        var numReps = int("NUM_REPS").bindTo { it.numReps }.isNotNull()
        var lastRep = int("LAST_REP").bindTo { it.lastRep }
        var lengthOfRep = double("LENGTH_OF_REP").bindTo { it.lengthOfRep }
        var lengthOfWarmUp = double("LENGTH_OF_WARM_UP").bindTo { it.lengthOfWarmUp }
        var hasMoreReps = boolean("HAS_MORE_REPS").bindTo { it.hasMoreReps }
        var repAllowedExecTime = long("REP_ALLOWED_EXEC_TIME").bindTo { it.repAllowedExecTime }
        var repInitOption = boolean("REP_INIT_OPTION").bindTo { it.repInitOption }
        var repResetStartStreamOption = boolean("RESET_START_STREAM_OPTION").bindTo { it.repResetStartStreamOption }
        var antitheticOption = boolean("ANTITHETIC_OPTION").bindTo { it.antitheticOption }
        var advNextSubStreamOption = boolean("ADV_NEXT_SUB_STREAM_OPTION").bindTo { it.advNextSubStreamOption }
        var numStreamAdvances = int("NUM_STREAM_ADVANCES").bindTo { it.numStreamAdvances }
    }

    object DbModelElements : Table<DbModelElement>("MODEL_ELEMENT") {
        var simRunIDFk = int("SIM_RUN_ID_FK").primaryKey().bindTo { it.simRunIDFk } //not sure how to do references
        var elementId = int("ELEMENT_ID").primaryKey().bindTo { it.elementId }
        var elementName = varchar("ELEMENT_NAME").bindTo { it.elementName }.isNotNull()
        var elementClassName = varchar("CLASS_NAME").bindTo { it.elementClassName }.isNotNull()
        var parentIDFk = int("PARENT_ID_FK").bindTo { it.parentIDFk }
        var parentName = varchar("PARENT_NAME").bindTo { it.parentName }
        var leftCount = int("LEFT_COUNT").bindTo { it.leftCount }
        var rightCount = int("RIGHT_COUNT").bindTo { it.rightCount }
    }

    object WithRepStats : Table<WithinRepStat>("WITHIN_REP_STAT") {
        var id = int("ID").primaryKey().bindTo { it.id }
        var elementIdFk = int("ELEMENT_ID_FK").bindTo { it.elementIdFk } //not sure how to do references
        var simRunIdFk = int("SIM_RUN_ID_FK").bindTo { it.simRunIdFk } //not sure how to do references
        var repNum = int("REP_NUM").bindTo { it.repNum }
        var statName = varchar("STAT_NAME").bindTo { it.statName }
        var statCount = double("STAT_COUNT").bindTo { it.statCount }
        var average = double("AVERAGE").bindTo { it.average }
        var minimum = double("MINIMUM").bindTo { it.minimum }
        var maximum = double("MAXIMUM").bindTo { it.maximum }
        var weightedSum = double("WEIGHTED_SUM").bindTo { it.weightedSum }
        var sumOfWeights = double("SUM_OF_WEIGHTS").bindTo { it.sumOfWeights }
        var weightedSSQ = double("WEIGHTED_SSQ").bindTo { it.weightedSSQ }
        var lastValue = double("LAST_VALUE").bindTo { it.lastValue }
        var lastWeight = double("LAST_WEIGHT").bindTo { it.lastWeight }
    }

    object AcrossRepStats : Table<AcrossRepStat>("ACROSS_REP_STAT") {
        var id = int("ID").primaryKey().bindTo { it.id }
        var elementIdFk = int("ELEMENT_ID_FK").bindTo { it.elementIdFk } //not sure how to do references
        var simRunIdFk = int("SIM_RUN_ID_FK").bindTo { it.simRunIdFk } //not sure how to do references
        var statName = varchar("STAT_NAME").bindTo { it.statName }
        var statCount = double("STAT_COUNT").bindTo { it.statCount }
        var average = double("AVERAGE").bindTo { it.average }
        var stdDev = double("STD_DEV").bindTo { it.stdDev }
        var stdError = double("STD_ERR").bindTo { it.stdError }
        var halfWidth = double("HALF_WIDTH").bindTo { it.halfWidth }
        var confLevel = double("CONF_LEVEL").bindTo { it.confLevel }
        var minimum = double("MINIMUM").bindTo { it.minimum }
        var maximum = double("MAXIMUM").bindTo { it.maximum }
        var sumOfObs = double("SUM_OF_OBS").bindTo { it.sumOfObs }
        var devSSQ = double("DEV_SSQ").bindTo { it.devSSQ }
        var lastValue = double("LAST_VALUE").bindTo { it.lastValue }
        var kurtosis = double("KURTOSIS").bindTo { it.kurtosis }
        var skewness = double("SKEWNESS").bindTo { it.skewness }
        var lag1Cov = double("LAG1_COV").bindTo { it.lag1Cov }
        var lag1Corr = double("LAG1_CORR").bindTo { it.lag1Corr }
        var vonNeumanLag1Stat = double("VON_NEUMAN_LAG1_STAT").bindTo { it.vonNeumanLag1Stat }
        var numMissingObs = double("NUM_MISSING_OBS").bindTo { it.numMissingObs }
    }

    object WithinRepCounterStats : Table<WithinRepCounterStat>("WITHIN_REP_COUNTER_STAT") {
        var id = int("ID").primaryKey().bindTo { it.id }
        var elementIdFk = int("ELEMENT_ID_FK").bindTo { it.elementIdFk } //not sure how to do references
        var simRunIdFk = int("SIM_RUN_ID_FK").bindTo { it.simRunIdFk } //not sure how to do references
        var repNum = int("REP_NUM").bindTo { it.repNum }
        var statName = varchar("STAT_NAME").bindTo { it.statName }
        var lastValue = double("LAST_VALUE").bindTo { it.lastValue }
    }

    object BatchStats : Table<BatchStat>("BATCH_STAT") {
        var id = int("ID").primaryKey().bindTo { it.id }
        var elementIdFk = int("ELEMENT_ID_FK").bindTo { it.elementIdFk } //not sure how to do references
        var simRunIdFk = int("SIM_RUN_ID_FK").bindTo { it.simRunIdFk } //not sure how to do references
        var repNum = int("REP_NUM").bindTo { it.repNum }
        var statName = varchar("STAT_NAME").bindTo { it.statName }
        var statCount = double("STAT_COUNT").bindTo { it.statCount }
        var average = double("AVERAGE").bindTo { it.average }
        var stdDev = double("STD_DEV").bindTo { it.stdDev }
        var stdError = double("STD_ERR").bindTo { it.stdError }
        var halfWidth = double("HALF_WIDTH").bindTo { it.halfWidth }
        var confLevel = double("CONF_LEVEL").bindTo { it.confLevel }
        var minimum = double("MINIMUM").bindTo { it.minimum }
        var maximum = double("MAXIMUM").bindTo { it.maximum }
        var sumOfObs = double("SUM_OF_OBS").bindTo { it.sumOfObs }
        var devSSQ = double("DEV_SSQ").bindTo { it.devSSQ }
        var lastValue = double("LAST_VALUE").bindTo { it.lastValue }
        var kurtosis = double("KURTOSIS").bindTo { it.kurtosis }
        var skewness = double("SKEWNESS").bindTo { it.skewness }
        var lag1Cov = double("LAG1_COV").bindTo { it.lag1Cov }
        var lag1Corr = double("LAG1_CORR").bindTo { it.lag1Corr }
        var vonNeumanLag1Stat = double("VON_NEUMAN_LAG1_STAT").bindTo { it.vonNeumanLag1Stat }
        var numMissingObs = double("NUM_MISSING_OBS").bindTo { it.numMissingObs }
        var minBatchSize = double("MIN_BATCH_SIZE").bindTo { it.minBatchSize }
        var minNumBatches = double("MIN_NUM_BATCHES").bindTo { it.minNumBatches }
        var maxNumBatchesMultiple = double("MAX_NUM_BATCHES_MULTIPLE").bindTo { it.maxNumBatchesMultiple }
        var maxNumBatches = double("MAX_NUM_BATCHES").bindTo { it.maxNumBatches }
        var numRebatches = double("NUM_REBATCHES").bindTo { it.numRebatches }
        var currentBatchSize = double("CURRENT_BATCH_SIZE").bindTo { it.currentBatchSize }
        var amtUnbatched = double("AMT_UNBATCHED").bindTo { it.amtUnbatched }
        var totalNumObs = double("TOTAL_NUM_OBS").bindTo { it.totalNumObs }
    }

    interface SimulationRun : Entity<SimulationRun> {
        companion object : Entity.Factory<SimulationRun>()

        var id: Int
        var simName: String
        var modelName: String
        var expName: String
        var expStartTimeStamp: Instant?
        var expEndTimeStamp: Instant?
        var numReps: Int
        var lastRep: Int?
        var lengthOfRep: Double?
        var lengthOfWarmUp: Double?
        var hasMoreReps: Boolean?
        var repAllowedExecTime: Long?
        var repInitOption: Boolean?
        var repResetStartStreamOption: Boolean?
        var antitheticOption: Boolean?
        var advNextSubStreamOption: Boolean?
        var numStreamAdvances: Int?
    }

    interface DbModelElement : Entity<DbModelElement> {
        companion object : Entity.Factory<DbModelElement>()

        var simRunIDFk: Int
        var elementId: Int
        var elementName: String
        var elementClassName: String
        var parentIDFk: Int?
        var parentName: String?
        var leftCount: Int
        var rightCount: Int
    }

    interface WithinRepStat : Entity<WithinRepStat> {
        companion object : Entity.Factory<WithinRepStat>()

        var id: Int
        var elementIdFk: Int
        var simRunIdFk: Int
        var repNum: Int
        var statName: String?
        var statCount: Double?
        var average: Double?
        var minimum: Double?
        var maximum: Double?
        var weightedSum: Double?
        var sumOfWeights: Double?
        var weightedSSQ: Double?
        var lastValue: Double?
        var lastWeight: Double?
    }

    interface AcrossRepStat : Entity<AcrossRepStat> {
        companion object : Entity.Factory<AcrossRepStat>()

        var id: Int
        var elementIdFk: Int
        var simRunIdFk: Int
        var statName: String
        var statCount: Double
        var average: Double
        var stdDev: Double
        var stdError: Double
        var halfWidth: Double
        var confLevel: Double
        var minimum: Double
        var maximum: Double
        var sumOfObs: Double
        var devSSQ: Double
        var lastValue: Double
        var kurtosis: Double
        var skewness: Double
        var lag1Cov: Double
        var lag1Corr: Double
        var vonNeumanLag1Stat: Double
        var numMissingObs: Double
    }

    interface WithinRepCounterStat : Entity<WithinRepCounterStat> {
        companion object : Entity.Factory<WithinRepCounterStat>()

        var id: Int
        var elementIdFk: Int
        var simRunIdFk: Int
        var repNum: Int
        var statName: String?
        var lastValue: Double?
    }

    interface BatchStat : Entity<BatchStat> {
        companion object : Entity.Factory<BatchStat>()

        var id: Int
        var elementIdFk: Int
        var simRunIdFk: Int
        var repNum: Int
        var statName: String?
        var statCount: Double?
        var average: Double?
        var stdDev: Double?
        var stdError: Double?
        var halfWidth: Double?
        var confLevel: Double?
        var minimum: Double?
        var maximum: Double?
        var sumOfObs: Double?
        var devSSQ: Double?
        var lastValue: Double?
        var kurtosis: Double?
        var skewness: Double?
        var lag1Cov: Double?
        var lag1Corr: Double?
        var vonNeumanLag1Stat: Double?
        var numMissingObs: Double?
        var minBatchSize: Double?
        var minNumBatches: Double?
        var maxNumBatchesMultiple: Double?
        var maxNumBatches: Double?
        var numRebatches: Double?
        var currentBatchSize: Double?
        var amtUnbatched: Double?
        var totalNumObs: Double?
    }

    companion object {
        val TableNames = listOf(
            "batch_stat", "within_rep_counter_stat",
            "across_rep_stat", "within_rep_stat", "model_element", "simulation_run"
        )

        val ViewNames = listOf(
            "within_rep_response_view",
            "within_rep_counter_view",
            "across_rep_view",
            "batch_stat_view",
            "within_rep_view",
            "pw_diff_within_rep_view"
        )

        private const val SchemaName = "KSL_DB"

        val dbDir: Path = KSL.dbDir
        val dbScriptsDir: Path = KSL.createSubDirectory("dbScript")

        init {
            try {
                val classLoader = this::class.java.classLoader
                val dbCreate = classLoader.getResourceAsStream("KSL_Db.sql")
                val dbDrop = classLoader.getResourceAsStream("KSL_DbDropScript.sql")
                val dbSQLiteCreate = classLoader.getResourceAsStream("KSL_SQLite.sql")
                if (dbCreate != null) {
                    Files.copy(
                        dbCreate, dbScriptsDir.resolve("KSL_Db.sql"),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    DatabaseIfc.logger.trace { "Copied KSL_Db.sql to $dbScriptsDir" }
                }
                if (dbDrop != null) {
                    Files.copy(
                        dbDrop, dbScriptsDir.resolve("KSL_DbDropScript.sql"),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    DatabaseIfc.logger.trace { "Copied KSL_DbDropScript.sql to $dbScriptsDir" }
                }
                if (dbSQLiteCreate != null) {
                    Files.copy(
                        dbSQLiteCreate, dbScriptsDir.resolve("KSL_SQLite.sql"),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    DatabaseIfc.logger.trace { "Copied KSL_SQLite.sql to $dbScriptsDir" }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun createSQLiteKSLDatabase(dbName: String, dbDirectory: Path = dbDir): Database {
            val database = DatabaseFactory.createSQLiteDatabase(dbName, dbDirectory)
            val executed = database.executeScript(dbScriptsDir.resolve("KSL_SQLite.sql"))
            if (!executed) {
                DatabaseIfc.logger.error("Unable to execute KSL_SQLite.sql creation script")
                throw DataAccessException("The execution script KSL_SQLite.sql did not fully execute")
            }
            return database
        }

        /** This method creates the database on disk as configures it to hold KSL simulation data.
         *
         * @param dbName the name of the database
         * @param dbDirectory the directory containing the database. By default, KSL.dbDir.
         * @return an empty embedded Derby database configured to hold KSL simulation results
         */
        fun createEmbeddedDerbyKSLDatabase(dbName: String, dbDirectory: Path = dbDir): Database {
            val derbyDatabase = DatabaseFactory.createEmbeddedDerbyDatabase(dbName, dbDirectory)
            executeKSLDbCreationScriptOnDatabase(derbyDatabase)
            derbyDatabase.defaultSchemaName = SchemaName
            return derbyDatabase
        }

        /** Executes the KSL database creation script on the database if the database does not already
         * have a KSL_DB schema. If the database already contains a KSL_DB schema then the creation
         * script is not executed. This method assumes that the underlying data source supports schemas.
         * For example, SQLite does not support schemas.
         *
         * @param db the database
         */
        fun executeKSLDbCreationScriptOnDatabase(db: Database) {
            if (!db.containsSchema(SchemaName)) {
                DatabaseIfc.logger.warn("The database {} does not contain schema {}", db.label, SchemaName)
                try {
                    DatabaseIfc.logger.warn("Assume the schema has not be made and execute the creation script KSL_Db.sql")
                    val executed = db.executeScript(dbScriptsDir.resolve("KSL_Db.sql"))
                    if (!executed) {
                        throw DataAccessException("The execution script KSL_Db.sql did not fully execute")
                    }
                } catch (e: IOException) {
                    DatabaseIfc.logger.error("Unable to execute KSL_Db.sql creation script")
                    throw DataAccessException("Unable to execute KSL_Db.sql creation script")
                }
            }
        }
    }
}

fun main() {

    val model = Model("Drive Through Pharmacy", autoCSVReports = false)
    model.numberOfReplications = 30
    model.lengthOfReplication = 20000.0
    model.lengthOfReplicationWarmUp = 5000.0
    // add DriveThroughPharmacy to the main model
    val dtp = DriveThroughPharmacy(model, 1)
    dtp.arrivalRV.initialRandomSource = ExponentialRV(6.0, 1)
    dtp.serviceRV.initialRandomSource = ExponentialRV(3.0, 2)

    val sdb = KSLDatabase.createSQLiteKSLDatabase("TestSQLiteKSLDb")
    val kdb = KSLDatabase(sdb)
    KSLDatabaseObserver(kdb, model)

    model.simulate()
    model.print()

    val records = kdb.acrossReplicationRecords()
    println("number of records = ${records.query.totalRecords}")

    val cachedRowSet = DatabaseIfc.createCachedRowSet(records)
    println("size of cachedRowSet = ${cachedRowSet.size()}")
    cachedRowSet.first()
    DatabaseIfc.writeAsText(cachedRowSet, KSL.out)

//    sdb.printAllTablesAsText()
    val file = KSL.createPrintWriter("results.md")
    sdb.writeAllTablesAsMarkdown(out = file)

    val df = kdb.withinReplicationStatistics
    println(df.schema())
    println(df)
    println("Done!")
}

