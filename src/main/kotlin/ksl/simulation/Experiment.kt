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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ksl.simulation

import ksl.utilities.Identity
import ksl.utilities.IdentityIfc

/**
 * This class provides the information for running a simulation experiment. An
 * experiment is a specification for the number of replications, the warm-up
 * length, replication length, etc. for controlling the running of a simulation.
 *
 * The defaults include:
 * - length of replication = Double.POSITIVE_INFINITY
 *
 * - length of warm up = 0.0
 *
 * - replication initialization TRUE - The system state is re-initialized prior to each replication
 *
 * - reset start stream option FALSE - Do not reset the streams of the random variables to their
 * starting points prior to running the replications within the experiment. This
 * implies that if the experiment is re-run on the same model in the same code
 * invocation that an independent set of replications will be made.
 *
 * - advance next sub-stream option TRUE - The random variables in a within an experiment
 * will start at the next sub-stream for each new replication
 *
 * - number of times to advance streams = 1 This indicates how many times that the streams should
 * be advanced prior to running the experiment. This can be used to ensure
 * simulations start with different streams
 *
 * - antithetic replication option is off by default
 *
 * Constructs an experiment called "name"
 *
 * @param name The name of the experiment
*/
class Experiment (name: String? = null) : IdentityIfc by Identity(name){

    /**
     * The number of replications to run for this experiment
     *
     */
    var numberOfReplications: Int = 1
        set(value){
            setNumberOfReplications(value)
        }

    /**
     * The current number of replications that have been run for this experiment
     */
    var currentReplicationNumber = 0
        private set

    /**
     * The specified length of each planned replication for this experiment. The
     * default is Double.POSITIVE_INFINITY.
     */
    var lengthOfReplication = Double.POSITIVE_INFINITY
        set(value) {
            require(value > 0.0) { "Simulation replication length must be > 0.0" }
            field = value
        }

    /**
     * The length of time from the start of an individual replication to the
     * warm-up event for that replication.
     */
    var lengthOfWarmUp = 0.0 // zero is no warmup
        set(value){
            require(value >= 0.0) { "Warmup time cannot be less than zero" }
            field = value
        }

    /**
     * A flag to indicate whether each replication within the experiment
     * should be re-initialized at the beginning of each replication. True means
     * that it will be re-initialized.
     */
    var replicationInitializationOption: Boolean = true

    /**
     * The maximum allowable execution time "wall" clock time for an individual
     * replication to complete processing in nanoseconds.
     * Set the maximum allotted (suggested) execution (real) clock for
     * a replication. This is a proposed value because the execution time
     * requirement is only checked after the completion of each replication
     * After it is discovered that cumulative time for executing the replication has
     * exceeded the maximum time, then the process will be ended
     * (perhaps) not completing other replications.
     */
    var maximumAllowedExecutionTimePerReplication: Long = 0 // zero means not used
        set(value) {
            require(value > 0.0) { "The maximum number of execution time (clock time) must be > 0.0" }
            field = value
        }

    /**
     * The reset start stream option This option indicates whether the
     * random variables used during the experiment will be reset to their
     * starting stream prior to running the first replication. The default is
     * FALSE. This ensures that the random variable's streams WILL NOT be reset
     * prior to running the experiment. This will cause different experiments or
     * the same experiment run multiple times that use the same random variables
     * (via the same model) to continue within their current stream. Therefore,
     * the experiments will be independent when invoked within the same program
     * execution. To get common random number (CRN), run the experiments in
     * different program executions OR set this option to true prior to running
     * the experiment again within the same program invocation.
     */
    var resetStartStreamOption: Boolean = false

    /**
     * The reset next sub stream option This option indicates whether the
     * random variables used during the replication within the experiment will
     * be reset to their next sub-stream after running each replication. The
     * default is TRUE. This ensures that the random variables will jump to the
     * next sub-stream within their current stream at the end of a replication.
     * This will cause the random variables in each subsequent replication to
     * start in the same sub-stream in the underlying random number streams if
     * the replication is repeatedly used and the ResetStartStreamOption is set
     * to false (which is the default) and then jump to the next sub-stream (if
     * this option is on). This option has no effect if there is only 1
     * replication in an experiment.
     *
     * Having ResetNextSubStreamOption true assists in synchronizing the random
     * number draws from one replication to another aiding in the implementation
     * of common random numbers. Each replication within the same experiment is
     * still independent.
     */
    var advanceNextSubStreamOption: Boolean = true

    /**
     * Indicates whether antithetic replications should be run. The
     * default is false. If set the user must supply an even number of
     * replications; otherwise an exception will be thrown. The replications
     * will no longer be independent; however, pairs of replications will be
     * independent. Thus, the number of independent samples will be one-half of
     * the specified number of replications
     */
    var antitheticOption: Boolean = false
        private set

    /**
     * Indicates the number of times the streams should be advanced prior to
     * running the experiment
     *
     */
    var numberOfStreamAdvancesPriorToRunning: Int = 0
        set(value){
            require(value > 0) { "The number times to advance the stream must be > 0" }
            field = value
        }

    /**
     * Causes garbage collection System.gc() to be invoked after each
     * replication. The default is false
     *
     */
    var myGCAfterRepFlag: Boolean = false

    /**
     * Holds values for each controllable parameter of the simulation
     * model.
     */
    private var myControls: Map<String, Double>? = null

    /**
     *
     * @return true if a control map has been supplied
     */
    fun hasControls(): Boolean {
        return myControls != null
    }

    /** Indicates that the experiment should be run with these control values.
     *
     * @param controlMap the controls to use, may be null to stop use of controls
     */
    fun useControls(controlMap: Map<String, Double>) {
        myControls = controlMap
    }

    /**
     *
     * @return the control map if it was set
     */
    fun getControls(): Map<String, Double>? {
        return myControls
    }

    /**
     * Sets the desired number of replications for the experiment
     *
     * @param numReps must be &gt; 0, and even (divisible by 2) if antithetic
     * option is true
     * @param antitheticOption controls whether antithetic replications occur
     */
    fun setNumberOfReplications(numReps: Int, antitheticOption: Boolean = false) {
        require(numReps > 0) { "Number of replications <= 0" }
        if (antitheticOption) {
            require(numReps % 2 == 0) { "Number of replications must be even if antithetic option is on." }
            this.antitheticOption = true
        }
        numberOfReplications = numReps
    }

    /**
     * Checks if the current number of replications that have been executed is
     * less than the number of replications specified.
     *
     * @return true if more
     */
    fun hasMoreReplications(): Boolean {
        return currentReplicationNumber < numberOfReplications
    }

    /**
     * Sets all attributes of this experiment to the same values as the supplied
     * experiment (except for id and name)
     *
     * @param e the experiment to copy
     */
    fun setExperiment(e: Experiment) {
        numberOfReplications = e.numberOfReplications
        currentReplicationNumber = e.currentReplicationNumber
        lengthOfReplication = e.lengthOfReplication
        lengthOfWarmUp = e.lengthOfWarmUp
        replicationInitializationOption = e.replicationInitializationOption
        resetStartStreamOption = e.resetStartStreamOption
        advanceNextSubStreamOption = e.advanceNextSubStreamOption
        antitheticOption = e.antitheticOption
        numberOfStreamAdvancesPriorToRunning = e.numberOfStreamAdvancesPriorToRunning
        maximumAllowedExecutionTimePerReplication = e.maximumAllowedExecutionTimePerReplication
        myGCAfterRepFlag = e.myGCAfterRepFlag
    }

    /**
     * Returns a new Experiment based on this experiment.
     *
     * Essentially a clone, except for the id
     *
     * @return a new Experiment
     */
    fun instance(name: String? = null): Experiment {
        val n = Experiment(name)
        n.numberOfReplications = numberOfReplications
        n.currentReplicationNumber = currentReplicationNumber
        n.lengthOfReplication = lengthOfReplication
        n.lengthOfWarmUp = lengthOfWarmUp
        n.replicationInitializationOption = replicationInitializationOption
        n.resetStartStreamOption = resetStartStreamOption
        n.advanceNextSubStreamOption = advanceNextSubStreamOption
        n.antitheticOption = antitheticOption
        n.numberOfStreamAdvancesPriorToRunning = numberOfStreamAdvancesPriorToRunning
        n.maximumAllowedExecutionTimePerReplication = maximumAllowedExecutionTimePerReplication
        n.myGCAfterRepFlag = myGCAfterRepFlag
        return n
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Experiment Name: ")
        sb.append(name)
        sb.appendLine()
        sb.append("Experiment ID: ")
        sb.append(id)
        sb.appendLine()
        sb.append("Planned number of replications: ")
        sb.append(numberOfReplications)
        sb.appendLine()
        sb.append("Replication initialization option: ")
        sb.append(replicationInitializationOption)
        sb.appendLine()
        sb.append("Antithetic option: ")
        sb.append(antitheticOption)
        sb.appendLine()
        sb.append("Reset start stream option: ")
        sb.append(resetStartStreamOption)
        sb.appendLine()
        sb.append("Reset next sub-stream option: ")
        sb.append(advanceNextSubStreamOption)
        sb.appendLine()
        sb.append("Number of stream advancements: ")
        sb.append(numberOfStreamAdvancesPriorToRunning)
        sb.appendLine()
        sb.append("Planned time horizon for replication: ")
        sb.append(lengthOfReplication)
        sb.appendLine()
        sb.append("Warm up time period for replication: ")
        sb.append(lengthOfWarmUp)
        sb.appendLine()
        val et = maximumAllowedExecutionTimePerReplication
        if (et == 0L) {
            sb.append("Maximum allowed replication execution time not specified.")
        } else {
            sb.append("Maximum allowed replication execution time: ")
            sb.append(et)
            sb.append(" nanoseconds.")
        }
        sb.appendLine()
        sb.append("Current Replication Number: ")
        sb.append(currentReplicationNumber)
        sb.appendLine()
        return sb.toString()
    }

    /**
     * Resets the current replication number to zero
     *
     */
    internal fun resetCurrentReplicationNumber() {
        currentReplicationNumber = 0
    }

    /**
     * Increments the number of replications that has been executed
     *
     */
    internal fun incrementCurrentReplicationNumber() {
        currentReplicationNumber = currentReplicationNumber + 1
    }

}