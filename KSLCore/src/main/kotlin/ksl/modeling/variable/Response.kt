package ksl.modeling.variable

import ksl.simulation.Model
import ksl.simulation.ModelElement
import ksl.utilities.Interval
import ksl.utilities.observers.DoublePairEmitter
import ksl.utilities.observers.DoublePairEmitterIfc
import ksl.utilities.statistic.Statistic
import ksl.utilities.statistic.StatisticIfc
import ksl.utilities.statistic.WeightedStatistic
import ksl.utilities.statistic.WeightedStatisticIfc

/**
 *  While Response instances should in general be declared as private within model
 *  elements, this interface provides the modeler the ability to declare a public property
 *  that returns an instance with limited ability to change and use the underlying Response,
 *  prior to running the model.
 *
 *  For example:
 *
 *   private val myR = Response(this, "something cool")
 *   val response: ResponseCIfc
 *      get() = myR
 *
 *   Then users of the public property can change the response and do other
 *   controlled changes without fully exposing the private variable.  The implementer of the
 *   model element that contains the private response does not have to write additional
 *   functions to control the response and can use this strategy to expose what is needed.
 *   This is most relevant to setting up the model elements prior to running the model or
 *   accessing information after the model has been executed. Changes or use during a model
 *   run is readily available through the general interface presented by Response.
 *
 *   The naming convention "CIfc" is used to denote controlled interface.
 *
 */
interface ResponseCIfc : ResponseIfc {
    /**
     *  If true, the response will emit pairs Pair(time, value) every time
     *  a new value is assigned
     */
    val emissionsOn: Boolean

    /**
     *  The legal range of values for the response
     */
    override val range: Interval

    /**
     * Sets the initial value of the count limit. Only relevant prior to each
     * replication. Changing during a replication has no effect until the next
     * replication.
     */
    var initialCountLimit: Double

    /**
     *  The across replication statistics for the response
     */
    val acrossReplicationStatistic: StatisticIfc

    /**
     *  The within replication statistics associated with the response
     */
    val withinReplicationStatistic: WeightedStatisticIfc

    /**
     *  Add an action that will occur when the count limit is achieved
     */
    fun addCountLimitAction(action: CountActionIfc)

    /**
     *  Remove an action associated with a count limit
     */
    fun removeCountLimitAction(action: CountActionIfc)

    /**
     *  Adds an action that will stop the replication when the count limit is reached.
     */
    fun addCountLimitStoppingAction() : CountActionIfc
}

// not subclassing from Variable, Response does not have an initial value, but does have limits
// should be observable
open class Response(
    parent: ModelElement,
    name: String? = null,
    allowedRange: Interval = Interval(),
    countLimit: Double = Double.POSITIVE_INFINITY,
) : ModelElement(parent, name), ResponseIfc, ResponseStatisticsIfc, ResponseCIfc, DoublePairEmitterIfc by DoublePairEmitter() {

    override var emissionsOn: Boolean = false

    private val counterActions: MutableList<CountActionIfc> = mutableListOf()
    private var stoppingAction: StoppingAction? = null

    override val range: Interval = allowedRange

    var timeOfWarmUp: Double = 0.0
        protected set

    var lastTimedUpdate: Double = 0.0
        protected set

    init {
        require(countLimit >= 0) { "The initial count limit value $countLimit must be >= 0" }
    }

    /**
     * Sets the initial value of the count limit. Only relevant prior to each
     * replication. Changing during a replication has no effect until the next
     * replication.
     */
    override var initialCountLimit: Double = countLimit
        set(value) {
            require(value >= 0) { "The initial count stop limit, when set, must be >= 0" }
            if (model.isRunning) {
                Model.logger.info { "The user set the initial count stop limit during the replication. The next replication will use a different initial value" }
            }
            field = value
        }

    /**
     * indicates the count when the simulation should stop
     * zero indicates no limit
     */
    var countActionLimit: Double = countLimit
        set(limit) {
            require(limit >= 0) { "The count stop limit, when set, must be >= 0" }
            if (model.isRunning) {
                if (limit < field) {
                    Model.logger.info { "The count stop limit was reduced to $limit from $field for $name during the replication" }
                } else if (limit > field) {
                    Model.logger.info { "The count stop limit was increased to $limit from $field for $name during the replication" }
                    if (limit.isInfinite()) {
                        Model.logger.warn { "Setting the count stop limit to infinity during the replication may cause the replication to not stop." }
                    }
                }
            }
            field = limit
            if (model.isRunning) {
                if (myValue >= limit) {
                    notifyCountLimitActions()
                }
            }
        }

    protected var myValue: Double = Double.NaN

    override var value: Double
        get() = myValue
        set(newValue) = assignValue(newValue)

    protected open fun assignValue(newValue: Double){
        require(range.contains(newValue)) { "The value $newValue was not within the limits $range" }
        previousValue = myValue
        previousTimeOfChange = timeOfChange
        myValue = newValue
        timeOfChange = time
        myWithinReplicationStatistic.value = myValue
        notifyModelElementObservers(Status.UPDATE)
        if (emissionsOn){
            emitter.emit(Pair(timeOfChange, myValue))
        }
        if(myWithinReplicationStatistic.count == countActionLimit){
            notifyCountLimitActions()
        }
    }

    override var previousValue: Double = Double.NaN
        protected set

    override var timeOfChange: Double = Double.NaN
        protected set

    override var previousTimeOfChange: Double = Double.NaN
        protected set

    protected val myAcrossReplicationStatistic: Statistic = Statistic(this.name)

    override val acrossReplicationStatistic: StatisticIfc
        get() = myAcrossReplicationStatistic.instance()

    protected val myWithinReplicationStatistic: WeightedStatistic = WeightedStatistic(this.name)

    override val withinReplicationStatistic: WeightedStatisticIfc
        get() = myWithinReplicationStatistic.instance()

    override var defaultReportingOption: Boolean = true

    override fun beforeExperiment() {
        super.beforeExperiment()
        myValue = Double.NaN
        previousValue = Double.NaN
        timeOfChange = Double.NaN
        previousTimeOfChange = Double.NaN
        myWithinReplicationStatistic.reset()
        myAcrossReplicationStatistic.reset()
        timeOfWarmUp = 0.0
        lastTimedUpdate = 0.0
        countActionLimit = initialCountLimit
    }

    override fun beforeReplication() {
        super.beforeReplication()
        myValue = Double.NaN
        previousValue = Double.NaN
        timeOfChange = Double.NaN
        previousTimeOfChange = Double.NaN
        myWithinReplicationStatistic.reset()
        timeOfWarmUp = 0.0
        lastTimedUpdate = 0.0
        countActionLimit = initialCountLimit
    }

    override fun timedUpdate() {
        super.timedUpdate()
        lastTimedUpdate = time
    }

    override fun warmUp() {
        super.warmUp()
        myWithinReplicationStatistic.reset()
        timeOfWarmUp = time
    }

    override fun afterReplication() {
        super.afterReplication()
        myAcrossReplicationStatistic.value = myWithinReplicationStatistic.average()
    }

    override fun addCountLimitAction(action: CountActionIfc) {
        counterActions.add(action)
    }

    override fun removeCountLimitAction(action: CountActionIfc) {
        counterActions.remove(action)
    }

    override fun addCountLimitStoppingAction() : CountActionIfc{
        if (stoppingAction == null){
            stoppingAction = StoppingAction()
            addCountLimitAction(stoppingAction!!)
        }
        return stoppingAction!!
    }

    protected fun notifyCountLimitActions() {
        for (a in counterActions) {
            a.action(this)
        }
    }

    private inner class StoppingAction : CountActionIfc {
        override fun action(response: ResponseIfc) {
            executive.stop("Stopped because counter limit $countActionLimit was reached for $name")
        }
    }
}