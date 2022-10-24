/*
 *     The KSL provides a discrete-event simulation library for the Kotlin programming language.
 *     Copyright (C) 2022  Manuel D. Rossetti, rossetti@uark.edu
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ksl.modeling.elements

import ksl.modeling.variable.RandomVariable
import ksl.modeling.variable.RandomSourceCIfc
import ksl.simulation.KSLEvent
import ksl.simulation.ModelElement
import ksl.utilities.GetValueIfc
import ksl.utilities.random.RandomIfc
import ksl.utilities.random.rvariable.ConstantRV

/**
 * This class allows for the periodic generation of events similar to that
 * achieved by "Create" modules in simulation languages. This class works in
 * conjunction with the EventGeneratorActionIfc which is used to listen and
 * react to the events that are generated by this class.
 *
 * Classes can supply an instance of an EventGeneratorActionIfc to provide the
 * actions that take place when the event occurs. Alternatively, if no
 * EventGeneratorActionIfc is supplied, by default the generator(JSLEvent
 * event) method of this class will be called when the event occurs. Thus,
 * subclasses can simply override this method to provide behavior for when the
 * event occurs. If no EventGeneratorActionIfc is supplied and the method generate()
 * method is not overridden, then the events will still occur; however, no
 * meaningful actions will take place.
 *
 * Of particular note is the use of initial parameters:
 *
 * initial time of first event (default = Constant.ZERO)
 * initial time between events (default = Constant.POSITIVE_INFINITY)
 * initial maximum number of events (default = Long.MAX_VALUE)
 * initial ending time (default = Double.POSITIVE_INFINITY)
 *
 * These parameters control the initial state of the generator at the start
 * of each replication. The generator is re-initialized to these values at
 * the start of each replication. There are also parameters for each of these
 * that can be changed during a replication. The effect of that change
 * is only within the current replication.
 *
 * @param parent the parent model element
 * @param theAction The action supplies the generation logic for reacting to the generated event.
 * @param theTimeUntilTheFirstEvent A RandomIfc object that supplies the time until the first event.
 * @param theTimeBtwEvents A RandomIfc object that supplies the time between
 * events. Must not be a RandomIfc that always returns 0.0, if the maximum
 * number of generations is infinite (Long.MAX_VALUE)
 * @param theMaxNumberOfEvents A long that supplies the maximum number of events to
 * generate. Each time an event is to be scheduled the maximum number of
 * events is checked. If the maximum has been reached, then the generator is
 * turned off. The default is Long.MAX_VALUE. This parameter cannot be
 * Long.MAX_VALUE when the time until next always returns a value of 0.0
 * @param theTimeOfTheLastEvent A double that supplies a time to stop generating
 * events. When the generator is created, this variable is used to set the
 * ending time of the generator. Each time an event is to be scheduled the
 * ending time is checked. If the time of the next event is past this time,
 * then the generator is turned off and the event won't be scheduled. The
 * default is Double.POSITIVE_INFINITY.
 * @param theName the name of the generator
 */
open class EventGenerator(
    parent: ModelElement,
    theAction: GeneratorActionIfc?,
    theTimeUntilTheFirstEvent: RandomIfc = ConstantRV.ZERO,
    theTimeBtwEvents: RandomIfc = ConstantRV.POSITIVE_INFINITY,
    theMaxNumberOfEvents: Long = Long.MAX_VALUE,
    theTimeOfTheLastEvent: Double = Double.POSITIVE_INFINITY,
    theName: String? = null
) : ModelElement(parent, theName), EventGeneratorIfc {
    init {
        require(theMaxNumberOfEvents >= 0) { "The maximum number of events to generate was < 0!" }
        if (theMaxNumberOfEvents == Long.MAX_VALUE) {
            if (theTimeBtwEvents is ConstantRV) {
                //TODO ranges will make this easier to check
                require(theTimeBtwEvents.value != 0.0) { "Maximum number of events is $theMaxNumberOfEvents and time between events is 0.0" }
            }
        }
        require(theTimeOfTheLastEvent >= 0) { "The time of the last event was < 0!" }
        //TODO need to implement ranges so that time until first can be checked
    }

    /**
     * The action for the events for generation
     */
    private val generatorAction: GeneratorActionIfc? = theAction

    /**
     * Determines the priority of the event generator's events The default is
     * DEFAULT_PRIORITY - 1 A lower number implies higher priority.
     */
    var eventPriority = EVENT_PRIORITY

    /**
     * Holds the random source for the time until first event. Used to
     * initialize the generator at the beginning of each replication
     */
    private var myInitialTimeUntilFirstEvent: RandomIfc = theTimeUntilTheFirstEvent

    /**
     * A RandomVariable that uses the time until first random source
     */
    private val myTimeUntilFirstEventRV: RandomVariable = RandomVariable(
        this, myInitialTimeUntilFirstEvent,
        "$name:TimeUntilFirstRV"
    )
    override val initialTimeUntilFirstEvent: RandomSourceCIfc
        get() = myTimeUntilFirstEventRV

    /**
     * Holds the random source for the time between events. Used to initialize
     * the generator at the beginning of each replication
     */
    private var myInitialTimeBtwEvents: RandomIfc = theTimeBtwEvents

    /**
     * Used to initialize the maximum number of events at the beginning of each
     * replication
     */
    private var myInitialMaxNumEvents: Long = theMaxNumberOfEvents

    /**
     * A random variable for the time between events
     */
    private val myTimeBtwEventsRV: RandomVariable =
        RandomVariable(this, myInitialTimeBtwEvents, "$name:TimeBtwEventsRV")

    /**
     * The time to stop generating for the current replication
     */
    override var endingTime: Double = theTimeOfTheLastEvent
        set(value) {
            require(value >= 0) { "The ending time was < 0.0!" }
            if (value < time) {
                turnOffGenerator()
            } else {// now set the time to turn off
                field = value
            }
        }

    /**
     * Used to set the ending time when the generator is initialized
     * at the start of each replication.
     */
    override var initialEndingTime: Double = theTimeOfTheLastEvent
        set(value) {
            require(value >= 0) { "The time until last was < 0.0!" }
            field = value
        }

    /**
     * The next event to be executed for the generator
     */
    private var myNextEvent: KSLEvent<Nothing>? = null

    /**
     * This flag controls whether the generator starts automatically when
     * initialized at the beginning of a replication By default this option is
     * true. If it is changed then it remains at the set value until changed
     * again.
     */
    override var startOnInitializeOption = true

    private val myEventHandler: EventHandler = EventHandler()

    /**
     * indicates whether the generator has been started (turned on)
     */
    override var isStarted = false
        protected set


    override var initialTimeBtwEvents: RandomIfc
        get() = myInitialTimeBtwEvents
        set(timeBtwEvents) {
            setInitialTimeBetweenEventsAndMaxNumEvents(timeBtwEvents, myInitialMaxNumEvents)
        }

    override var initialMaximumNumberOfEvents: Long
        get() = myInitialMaxNumEvents
        set(maxNumEvents) {
            setInitialTimeBetweenEventsAndMaxNumEvents(myInitialTimeBtwEvents, maxNumEvents)
        }

    // now set the time to turn off

    /**
     * The number of events currently generated during the replication
     */
    override var eventCount: Long = 0
        protected set

    interface ActionStepIfc {
        fun action(action: GeneratorActionIfc): TimeBetweenEventsStepIfc
    }

    interface TimeBetweenEventsStepIfc {
        fun timeBetweenEvents(timeBtwEvents: RandomIfc): BuildStepIfc
    }

    interface BuildStepIfc {
        fun timeUntilFirst(timeUntilFirst: RandomIfc): BuildStepIfc
        fun maxNumberOfEvents(maxNum: Long): BuildStepIfc
        fun name(name: String?): BuildStepIfc
        fun timeUntilLastEvent(timeUntilLastEvent: Double): BuildStepIfc
        fun build(): EventGenerator
    }

    private class EventGeneratorBuilder(parent: ModelElement) : ActionStepIfc, TimeBetweenEventsStepIfc,
        BuildStepIfc {
        private val parent: ModelElement
        private var action: GeneratorActionIfc? = null
        private var timeUntilFirst: RandomIfc = ConstantRV.ZERO
        private var timeBtwEvents: RandomIfc? = null
        private var maxNum = Long.MAX_VALUE
        private var name: String? = null
        private var timeUntilLastEvent = Double.POSITIVE_INFINITY

        init {
            this.parent = parent
        }

        override fun action(action: GeneratorActionIfc): TimeBetweenEventsStepIfc {
            this.action = action
            return this
        }

        override fun timeUntilFirst(timeUntilFirst: RandomIfc): BuildStepIfc {
            this.timeUntilFirst = timeUntilFirst
            return this
        }

        override fun timeBetweenEvents(timeBtwEvents: RandomIfc): BuildStepIfc {
            this.timeBtwEvents = timeBtwEvents
            return this
        }

        override fun maxNumberOfEvents(maxNum: Long): BuildStepIfc {
            require(maxNum > 0) { "The maximum number of events must be > 0." }
            this.maxNum = maxNum
            return this
        }

        override fun name(name: String?): BuildStepIfc {
            this.name = name
            return this
        }

        override fun timeUntilLastEvent(timeUntilLastEvent: Double): BuildStepIfc {
            require(timeUntilLastEvent > 0) { "The time until the last event must be > 0." }
            this.timeUntilLastEvent = timeUntilLastEvent
            return this
        }

        override fun build(): EventGenerator {
            return EventGenerator(parent, action!!, timeUntilFirst, timeBtwEvents!!, maxNum, timeUntilLastEvent, name)
        }
    }


    override fun turnOnGenerator(t: Double) {
        if (isSuspended) {
            return
        }
        if (isDone) {
            return
        }
        if (myMaxNumEvents == 0L) {
            return
        }
        if (eventCount >= myMaxNumEvents) {
            return
        }
        if (myNextEvent != null) {
            return
        }
        isStarted = true
        scheduleFirstEvent(t)
    }

    override fun turnOnGenerator(r: RandomIfc) {
        turnOnGenerator(r.value)
    }

    override fun turnOffGenerator() {
        isDone = true
        isStarted = false
        if (myNextEvent != null) {
            if (myNextEvent!!.scheduled) {
                myNextEvent!!.cancelled = true
            }
        }
    }

    // must be scheduled and not canceled to be pending
    override val isEventPending: Boolean
        get() = if (myNextEvent == null) {
            false
        } else {
            // must be scheduled and not canceled to be pending
            myNextEvent!!.scheduled && !myNextEvent!!.cancelled
        }

    override fun suspend() {
        isSuspended = true
        if (myNextEvent != null) {
            if (myNextEvent!!.scheduled) {
                myNextEvent!!.cancelled = true
            }
        }
    }

    /**
     * Whether the generator has been suspended
     */
    override var isSuspended: Boolean = false
        protected set

    override fun resume() {
        if (isSuspended) {
            isSuspended = false
            // get the time until next event
            val t: Double = myTimeBtwEventsRV.value
            // check if it is past end time
            if (t + time > endingTime) {
                turnOffGenerator()
            }
            if (!isDone) {
                // I'm not done generating, schedule the event
                myNextEvent = myEventHandler.schedule(t, priority = eventPriority)
            }
        }
    }

    /**
     * Whether the generator is done generating
     */
    override var isDone: Boolean = false
        protected set

    private var myMaxNumEvents: Long = theMaxNumberOfEvents
    /**
     * The number of events to generate for the current replication
     */
    override var maximumNumberOfEvents: Long = theMaxNumberOfEvents
        set(maxNum) {
            setTimeBetweenEvents(myTimeBtwEventsRV.randomSource, maxNum)
        }

    /**
     * The time between events for the current replication
     */
    override var timeBetweenEvents: RandomIfc
        get() = myTimeBtwEventsRV.randomSource
        set(timeUntilNext) {
            setTimeBetweenEvents(timeUntilNext, myMaxNumEvents)
        }

    override fun setTimeBetweenEvents(timeBtwEvents: RandomIfc, maxNumEvents: Long) {
        require(maxNumEvents >= 0) { "The maximum number of actions was < 0!" }
        if (maxNumEvents == Long.MAX_VALUE) {
            if (timeBtwEvents is ConstantRV) {
                require(timeBtwEvents.value != 0.0) { "Maximum number of actions is infinite and time between actions is 0.0" }
            }
        }
        // time btw events is okay and max num events is okay
        myMaxNumEvents = maxNumEvents
        myTimeBtwEventsRV.randomSource = timeBtwEvents
        // if number of events is >= desired number of events, turn off the generator
        if (eventCount >= maxNumEvents) {
            turnOffGenerator()
        }
    }

    override fun setInitialTimeBetweenEventsAndMaxNumEvents(
        initialTimeBtwEvents: RandomIfc,
        initialMaxNumEvents: Long
    ) {
        require(initialMaxNumEvents >= 0) { "The maximum number of events to generate was < 0!" }
        if (initialMaxNumEvents == Long.MAX_VALUE) {
            if (initialTimeBtwEvents is ConstantRV) {
                require(initialTimeBtwEvents.value != 0.0) { "Maximum number of events is infinite and time between events is 0.0" }
            }
        }
        myInitialMaxNumEvents = initialMaxNumEvents
        myInitialTimeBtwEvents = initialTimeBtwEvents
    }

    override fun initialize() {
        isDone = false
        isStarted = false
        isSuspended = false
        eventCount = 0
        myNextEvent = null
        // set ending time based on the value to be used for each replication
        endingTime = initialEndingTime
        // set the time until first event based on the value to be used for each replication
        myTimeUntilFirstEventRV.randomSource = myInitialTimeUntilFirstEvent

        //set the time between events, maximum number of events based on the values to be used for each replication
        setTimeBetweenEvents(myInitialTimeBtwEvents, myInitialMaxNumEvents)
        if (startOnInitializeOption) {
            if (myMaxNumEvents > 0) {
                scheduleFirstEvent(myTimeUntilFirstEventRV)
            }
        }
    }

    /**
     * Schedules the first event at current time + r.getValue()
     *
     * @param r the time to the first event
     */
    private fun scheduleFirstEvent(r: GetValueIfc) {
        scheduleFirstEvent(r.value)
    }

    /**
     * Schedules the first event at current time + t
     *
     * @param t the time to the first event
     */
    private fun scheduleFirstEvent(t: Double) {
        if (t + time > endingTime) {
            turnOffGenerator()
        }
        if (!isDone) {
            // I'm not done generating, schedule the first event
            myNextEvent = myEventHandler.schedule(t, priority = eventPriority)
        }
    }

    /**
     * Increments the number of actions and checks if the number of actions is
     * greater than the maximum number of actions. If so, the generator is told
     * to shut down.
     */
    private fun incrementNumberOfEvents() {
        eventCount++
        if (eventCount > myMaxNumEvents) {
            turnOffGenerator()
        }
    }

    //TODO
//    protected fun removedFromModel() {
//        super.removedFromModel()
//        myInitialTimeUntilFirstEvent = null
//        myTimeUntilFirstEventRV = null
//        myInitialTimeBtwEvents = null
//        myTimeBtwEventsRV = null
//        myNextEvent = null
//        myGenerateListener = null
//    }

    private inner class EventHandler : EventAction<Nothing>() {
        override fun action(event: KSLEvent<Nothing>) {
            incrementNumberOfEvents()
            if (!isDone) {
                if (generatorAction != null){
                    generatorAction.generate(this@EventGenerator)
                } else {
                    generate()
                }
                // get the time until next event
                val t: Double = myTimeBtwEventsRV.value
                // check if it is past end time
                if (t + time > endingTime) {
                    turnOffGenerator()
                }
                if (!isSuspended) {
                    if (!isDone) {// I'm not done generating, schedule the next event
                        schedule(t, priority = eventPriority)
                    }
                }
            }
        }
    }

    protected open fun generate(){
        TODO("subclasses of EventGenerator should override generate() if no GeneratorActionIfc action is supplied ")
    }

    companion object {
        /**
         * Determines the priority of the event generator's events The default is
         * DEFAULT_PRIORITY - 1 A lower number implies higher priority.
         */
        const val EVENT_PRIORITY: Int = KSLEvent.DEFAULT_PRIORITY - 1

        fun builder(parent: ModelElement): ActionStepIfc {
            return EventGeneratorBuilder(parent)
        }
    }
}