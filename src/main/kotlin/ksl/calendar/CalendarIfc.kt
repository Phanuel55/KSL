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
package ksl.calendar

import ksl.simulation.JSLEvent

/**
 * The interface defines behavior for holding, adding and retrieving JSLEvents.
 *
 *
 */
interface CalendarIfc {
    /**
     * The add method will place the provided JSLEvent into the
     * underlying data structure ensuring the ordering of the events
     * to be processed
     *
     * @param event The JSLEvent to be added to the calendar
     */
    fun add(event: JSLEvent<*>)

    /**
     * Returns the next JSLEvent to be executed. The event is removed from
     * the calendar if it exists
     *
     * @return The JSLEvent to be executed next
     */
    fun nextEvent(): JSLEvent<*>?

    /**
     * Peeks at the next event without removing it
     *
     * @return
     */
    fun peekNext(): JSLEvent<*>?

    /**
     * Checks to see if the calendar is empty
     *
     * @return true is empty, false is not empty
     */
    val isEmpty: Boolean

    /**
     * Checks to see if the calendar is not empty
     *
     * @return true is not empty, false is empty
     */
    val isNotEmpty: Boolean
        get() = !isEmpty

    /**
     * Clears or cancels every event in the data structure. Removes all
     * JSLEvents
     * from the data structure.
     */
    fun clear()

    /**
     * Cancels the supplied JSLEvent in the calendar. Canceling does not remove
     * the event from the data structure. It simply indicates that the
     * scheduled event must not be executed.
     *
     * @param event The JSLEvent to be canceled
     */
    fun cancel(event: JSLEvent<*>){
        event.cancelled = true;
    }

    /**
     * Returns the number of events in the calendar
     *
     * @return An int representing the number of events.
     */
    fun size(): Int
}