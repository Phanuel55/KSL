/*
 * Copyright (c) 2018. Manuel D. Rossetti, rossetti@uark.edu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ksl.modeling.nhpp

import ksl.modeling.elements.EventGenerator
import ksl.modeling.elements.GeneratorActionIfc
import ksl.modeling.elements.EventGeneratorIfc
import ksl.simulation.ModelElement
import ksl.utilities.random.RandomIfc
import ksl.modeling.variable.RandomVariableCIfc

//TODO delegate to the interface, fix constructors

/**
 * @author rossetti
 */
class NHPPEventGenerator : ModelElement, EventGeneratorIfc {
    protected var myEventGenerator: EventGenerator
    protected var myTBARV: NHPPTimeBtwEventRV

    /**
     * @param parent the parent
     * @param rateFunction the rate function
     * @param listener   the listener for generation
     */
    constructor(
        parent: ModelElement, rateFunction: InvertibleCumulativeRateFunctionIfc,
        listener: GeneratorActionIfc?
    ) : this(parent, rateFunction, listener, null) {
    }

    /**
     * @param parent the parent
     * @param rateFunction the rate function
     * @param listener   the listener for generation
     * @param name the name to assign
     */
    constructor(
        parent: ModelElement, rateFunction: InvertibleCumulativeRateFunctionIfc,
        listener: GeneratorActionIfc?, name: String?
    ) : super(parent, name) {
        myTBARV = NHPPTimeBtwEventRV(this, rateFunction)
        myEventGenerator = EventGenerator(this, listener, myTBARV, myTBARV)
    }

    /**
     * @param parent the parent
     * @param rateFunction the rate function
     * @param listener   the listener for generation
     * @param lastrate  the last rate
     * @param name the name to assign
     */
    constructor(
        parent: ModelElement, rateFunction: InvertibleCumulativeRateFunctionIfc,
        listener: GeneratorActionIfc?, lastrate: Double, name: String?
    ) : super(parent, name) {
        myTBARV = NHPPTimeBtwEventRV(this, rateFunction, lastrate)
        myEventGenerator = EventGenerator(this, listener, myTBARV, myTBARV)
    }

    override fun turnOnGenerator(t: Double) {
        myEventGenerator.turnOnGenerator(t)
    }

    override fun turnOnGenerator(r: RandomIfc) {
        myEventGenerator.turnOnGenerator(r)
    }

    override fun turnOffGenerator() {
        myEventGenerator.turnOffGenerator()
    }

    override val isStarted: Boolean
        get() = myEventGenerator.isStarted
    override var startOnInitializeOption: Boolean
        get() = myEventGenerator.startOnInitializeOption
        set(value) {
            myEventGenerator.startOnInitializeOption = value
        }

    override fun suspend() {
        myEventGenerator.suspend()
    }

    override val isSuspended: Boolean
        get() = myEventGenerator.isSuspended

    override fun resume() {
        myEventGenerator.resume()
    }

    override val isDone: Boolean
        get() = myEventGenerator.isDone
    override val maximumNumberOfEvents: Long
        get() = myEventGenerator.maximumNumberOfEvents
    override val timeBetweenEvents: RandomIfc
        get() = myEventGenerator.timeBetweenEvents

    override fun setTimeBetweenEvents(timeBtwEvents: RandomIfc, maxNumEvents: Long) {
        myEventGenerator.setTimeBetweenEvents(timeBtwEvents, maxNumEvents)
    }

    override fun setInitialTimeBetweenEventsAndMaxNumEvents(
        initialTimeBtwEvents: RandomIfc,
        initialMaxNumEvents: Long
    ) {
        myEventGenerator.setInitialTimeBetweenEventsAndMaxNumEvents(initialTimeBtwEvents, initialMaxNumEvents)
    }

    override val initialTimeUntilFirstEvent: RandomVariableCIfc
        get() = myEventGenerator.initialTimeUntilFirstEvent
    override val endingTime: Double
        get() = myEventGenerator.endingTime
    override var initialEndingTime: Double
        get() = myEventGenerator.initialEndingTime
        set(value) {
            myEventGenerator.initialEndingTime = value
        }
    override val eventCount: Long
        get() = myEventGenerator.eventCount
    override val initialMaximumNumberOfEvents: Long
        get() = myEventGenerator.initialMaximumNumberOfEvents
    override val initialTimeBtwEvents: RandomIfc
        get() = myEventGenerator.initialTimeBtwEvents
    override val isEventPending: Boolean
        get() = myEventGenerator.isEventPending
}