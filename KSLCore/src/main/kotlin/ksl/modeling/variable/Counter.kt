package ksl.modeling.variable

import ksl.simulation.Model
import ksl.simulation.ModelElement
import ksl.utilities.Interval
import ksl.utilities.observers.DoublePairEmitter
import ksl.utilities.observers.DoublePairEmitterIfc
import ksl.utilities.statistic.Statistic
import ksl.utilities.statistic.StatisticIfc

/**
 *  While Counter instances should in general be declared as private within model
 *  elements, this interface provides the modeler the ability to declare a public property
 *  that returns an instance with limited ability to change and use the underlying Counter,
 *  prior to running the model.
 *
 *  For example:
 *
 *   private val myC = Counter(this, "something cool")
 *   val counter: CounterCIfc
 *      get() = myC
 *
 *   Then users of the public property can change the response and do other
 *   controlled changes without fully exposing the private variable.  The implementer of the
 *   model element that contains the private counter does not have to write additional
 *   functions to control the counter and can use this strategy to expose what is needed.
 *   This is most relevant to setting up the model elements prior to running the model or
 *   accessing information after the model has been executed. Changes or use during a model
 *   run is readily available through the general interface presented by Counter.
 *
 *   The naming convention "CIfc" is used to denote controlled interface.
 *
 */
interface CounterCIfc {

    /**
     *  If true, the response will emit pairs Pair(time, value) every time
     *  a new value is assigned
     */
    val emissionsOn: Boolean

    /**
     * Sets the initial value of the count limit. Only relevant prior to each
     * replication. Changing during a replication has no effect until the next replication.
     */
    var initialCounterLimit: Double

    /**
     * Sets the initial value of the variable. Only relevant prior to each
     * replication. Changing during a replication has no effect until the next
     * replication.
     */
    var initialValue: Double
    val acrossReplicationStatistic: StatisticIfc
    var defaultReportingOption: Boolean
    fun addCountLimitAction(action: CountActionIfc)
    fun removeCountLimitAction(action: CountActionIfc)
    fun addCountLimitStoppingAction(): CountActionIfc
}

open class Counter(
    parent: ModelElement,
    name: String? = null,
    initialValue: Double = 0.0,
    countLimit: Double = Double.POSITIVE_INFINITY,
) : ModelElement(parent, name), CounterIfc, CounterCIfc, DoublePairEmitterIfc by DoublePairEmitter() {
    //TODO timed update stuff
    override var emissionsOn: Boolean = false
    private val counterActions: MutableList<CountActionIfc> = mutableListOf()

    override var range: Interval = Interval(0.0, Double.POSITIVE_INFINITY)

    var timeOfWarmUp: Double = 0.0
        protected set

    var lastTimedUpdate: Double = 0.0
        protected set

    init {
        require(initialValue >= 0.0) { "The initial value $initialValue must be >= 0" }
        require(range.contains(initialValue)) {"The initial value must be in counter's range $range"}
        require(countLimit >= 0.0) { "The initial count limit value $countLimit must be >= 0" }
        require(range.contains(countLimit)) {"The count limit must be in counter's range $range"}
    }

    /**
     * Sets the initial value of the count limit. Only relevant prior to each
     * replication. Changing during a replication has no effect until the next replication.
     */
    override var initialCounterLimit: Double = countLimit
        set(value) {
            require(value >= 0) { "The initial counter limit, when set, must be >= 0" }
            require(range.contains(value)) {"The initial counter limit must be in counter's range $range"}
            if (model.isRunning) {
                Model.logger.info { "The user set the initial counter stop limit during the replication. The next replication will use a different initial value" }
            }
            field = value
        }

    /**
     * Indicates the count when the simulation should stop. Zero indicates no limit.
     */
    var countActionLimit: Double = countLimit
        set(limit) {
            require(limit >= 0) { "The counter stop limit, when set, must be >= 0" }
            if (model.isRunning) {
                if (limit < field) {
                    Model.logger.info { "The counter stop limit was reduced to $limit from $field for $name during the replication" }
                } else if (limit > field) {
                    Model.logger.info { "The counter stop limit was increased to $limit from $field for $name during the replication" }
                    if (limit.isInfinite()) {
                        Model.logger.warn { "Setting the counter stop limit to infinity during the replication may cause the replication to not stop." }
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

    val isLimitReached: Boolean
        get() = myValue >= countActionLimit

    /**
     * Sets the initial value of the variable. Only relevant prior to each
     * replication. Changing during a replication has no effect until the next
     * replication.
     */
    override var initialValue: Double = initialValue
        set(value) {
            require(value >= 0) { "The initial value $value must be >= 0" }
            require(range.contains(value)) {"The initial value must be in counter's range $range"}
            if (model.isRunning) {
                Model.logger.info { "The user set the initial value during the replication. The next replication will use a different initial value" }
            }
            field = value
        }

    protected var myValue: Double = initialValue

    override var value: Double
        get() = myValue
        protected set(newValue) = assignValue(newValue)

    private var stoppingAction: StoppingAction? = null

    /**
     * Increments the value of the variable by the amount supplied. Throws an
     * IllegalArgumentException if the value is negative.
     *
     * @param increase The amount to increase by. Must be non-negative.
     */
    fun increment(increase: Double = 1.0) {
        require(increase >= 0) { "Invalid argument. Attempted an negative increment." }
        value = value + increase
    }

    override fun addCountLimitAction(action: CountActionIfc) {
        counterActions.add(action)
    }

    override fun removeCountLimitAction(action: CountActionIfc) {
        counterActions.remove(action)
    }

    override fun addCountLimitStoppingAction(): CountActionIfc {
        if (stoppingAction == null) {
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

    protected open fun assignValue(newValue: Double) {
        require(newValue >= 0) { "The value $newValue was not >= 0" }
        require(range.contains(newValue)) {"The assigned value must be in counter's range $range"}
        previousValue = myValue
        previousTimeOfChange = timeOfChange
        myValue = newValue
        timeOfChange = time
        notifyModelElementObservers(Status.UPDATE)
        if (emissionsOn){
            emitter.emit(Pair(timeOfChange, myValue))
        }
        if (myValue == countActionLimit) {
            notifyCountLimitActions()
        }
    }

    /**
     * Assigns the value of the counter to the supplied value. Ensures that
     * time of change is 0.0 and previous value and previous time of
     * change are the same as the current value and current time without
     * notifying any update observers
     *
     * @param value the initial value to assign
     */
    protected fun assignInitialValue(value: Double) {
        require(value >= 0.0) { "The initial value $value must be >= 0" }
        require(value < initialCounterLimit) { "The initial value, $value, of the counter must be < the initial counter limit, $initialCounterLimit" }
        myValue = value
        timeOfChange = 0.0
        previousValue = Double.NaN
        previousTimeOfChange = Double.NaN
        countActionLimit = initialCounterLimit
    }

    /**
     *  The previous value, before the current value changed
     */
    override var previousValue: Double = Double.NaN
        protected set

    override var timeOfChange: Double = Double.NaN
        protected set

    override var previousTimeOfChange: Double = Double.NaN
        protected set

    protected val myAcrossReplicationStatistic: Statistic = Statistic(this.name)

    override val acrossReplicationStatistic: StatisticIfc
        get() = myAcrossReplicationStatistic.instance()

    override var defaultReportingOption: Boolean = true

    override fun beforeExperiment() {
        super.beforeExperiment()
        lastTimedUpdate = 0.0
        timeOfWarmUp = 0.0
        assignInitialValue(initialValue)
        myAcrossReplicationStatistic.reset()
    }

    override fun beforeReplication() {
        super.beforeReplication()
        lastTimedUpdate = 0.0
        timeOfWarmUp = 0.0
        assignInitialValue(initialValue)
    }

    override fun initialize() {
        super.initialize()
        lastTimedUpdate = 0.0
        timeOfWarmUp = 0.0
        resetCounter(initialValue, false)
        assignInitialValue(initialValue)
    }

    override fun warmUp() {
        super.warmUp()
        timeOfWarmUp = time
        resetCounter(0.0, false)
    }

    override fun afterReplication() {
        super.afterReplication()
        myAcrossReplicationStatistic.value = value
    }

    /**
     * Resets the counter to the supplied value.
     *
     * @param value, must be &lt; counterLimit and &gt;=0
     * @param notifyUpdateObservers If true, any update observers will be
     * notified otherwise they will not be notified
     */
    fun resetCounter(value: Double, notifyUpdateObservers: Boolean) {
        require(value >= 0) { "The counter's value must be >= 0" }
        require(value < countActionLimit) { "The counter's value, $value must be < the counter limit = $countActionLimit" }
        previousValue = value
        previousTimeOfChange = time
        myValue = value
        timeOfChange = previousTimeOfChange
        if (notifyUpdateObservers) {
            notifyModelElementObservers(Status.UPDATE)
        }
    }

    private inner class StoppingAction : CountActionIfc {
        override fun action(response: ResponseIfc) {
            executive.stop("Stopped because counter limit $countActionLimit was reached for $name")
        }
    }
}