package ksl.modeling.entity

import ksl.simulation.KSLEvent
import ksl.utilities.GetValueIfc
import kotlin.coroutines.*

/**
 * Used to exit (terminate) a currently executing ProcessCoroutine.
 */
class ProcessTerminatedException (m: String = "Process Terminated!") : RuntimeException(m)

interface KSLProcess {
    val id: Int
    val name: String
    val isCreated: Boolean
    val isSuspended: Boolean
    val isTerminated: Boolean
    val isCompleted: Boolean
    val isRunning: Boolean
    val isActivated: Boolean
}

interface ProcessResumer {
    fun resume(entity: EntityType.Entity)
}

@RestrictsSuspension
interface KSLProcessBuilder {

    /**
     *  Suspends the execution of the process.  Since the process cannot resume itself, the client
     *  must provide an object that will resume the process.
     *
     *  @param resumer something that knows how to resume the process after it is suspended
     */
    suspend fun suspend(resumer: ProcessResumer) //TODO

    /**
     *  Resumes the process after it was halted (suspended).
     */
    fun resume() //TODO need to remove this

    /**
     *  Causes the process to halt, waiting for the signal to be announced.  Some other process/event
     *  is necessary to cause the signal. The entities stop waiting for the signal and resume their
     *  processes when signaled.
     *
     *  @param signal a general indicator for controlling the process
     *  @param waitPriority a priority indicator to inform ordering when there is more than one process waiting for
     *  the same signal
     *  @param waitStats Indicates whether waiting time statistics should be
     * collected on waiting items, true means collect statistics
     */
    suspend fun waitFor(signal: Signal, waitPriority: Int = KSLEvent.DEFAULT_PRIORITY, waitStats: Boolean = true)

    /**
     *  Causes the process to hold indefinitely within the supplied queue.  Some other process or event
     *  is responsible for removing the entities and causing them to proceed with their processes
     *  NOTE:  The entity is not signaled to resume its process unless you signal it.  The
     *  entity cannot remove itself.  Some other construct must do the removal and resumption.
     *
     *  @param queue a queue to hold the waiting entities
     *  @param priority a priority for the queue discipline if needed
     */
    suspend fun hold(queue: HoldQueue, priority: Int = KSLEvent.DEFAULT_PRIORITY)

    /**
     *  Requests a number of units of the indicated resource.
     *
     *  @param amountNeeded the number of units of the resource needed for the request.
     *   The default is 1 unit.
     *  @param resource the resource from which the units are being requested.
     *  @param seizePriority the priority of the request. This is meant to inform any allocation mechanism for
     *  requests that may be competing for the resource.
     *  @return the Allocation representing the request for the Resource. After returning, the allocation indicates that the units
     *  of the resource have been allocated to the entity making the request. An allocation should not be returned until
     *  all requested units of the resource have been allocated.
     */
    suspend fun seize(resource: Resource, amountNeeded: Int = 1,
                      seizePriority: Int = KSLEvent.DEFAULT_PRIORITY) : Allocation

    /**
     *  Causes the process to delay (suspend execution) for the specified amount of time.
     *
     *  @param delayDuration, the length of time required before the process continues executing, must not be negative and
     *  must be finite.
     *  @param delayPriority, since the delay is scheduled, a priority can be used to determine the order of events for
     *  delays that might be scheduled to complete at the same time.
     */
    suspend fun delay(delayDuration: Double, delayPriority: Int = KSLEvent.DEFAULT_PRIORITY)

    /**
     *  @param delayDuration, the length of time required before the process continues executing, must not be negative and
     *  must be finite.
     *  @param delayPriority, since the delay is scheduled, a priority can be used to determine the order of events for
     *  delays that might be scheduled to complete at the same time.
     */
    suspend fun delay(delayDuration: GetValueIfc, delayPriority: Int = KSLEvent.DEFAULT_PRIORITY) {
        delay(delayDuration.value, delayPriority)
    }

    /**
     *  Releases the allocation of the resource
     *
     *  @param allocation represents an allocation of so many units of a resource to an entity
     */
     fun release(allocation: Allocation)

    /**
     *  Releases ANY(ALL) allocations related to the resource that are allocated
     *  to the entity currently executing this process
     *
     *  @param resource the resource to release
     */
    fun release(resource: Resource)

    /**
     *  Releases ALL the resources that the entity has currently allocated to it
     */
    fun releaseAllResources()

}
