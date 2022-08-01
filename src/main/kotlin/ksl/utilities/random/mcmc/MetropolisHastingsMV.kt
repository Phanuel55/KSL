package ksl.utilities.random.mcmc

import ksl.utilities.observers.Observable
import ksl.utilities.observers.ObservableIfc
import ksl.utilities.random.rng.RNStreamChangeIfc
import ksl.utilities.random.rng.RNStreamControlIfc
import ksl.utilities.random.rng.RNStreamIfc
import ksl.utilities.random.rvariable.KSLRandom
import ksl.utilities.statistic.Statistic

open class MetropolisHastingsMV(
    theInitialX: DoubleArray,
    theTargetFun: FunctionMVIfc,
    theProposalFun: ProposalFunctionMVIfc,
    stream: RNStreamIfc = KSLRandom.nextRNStream()
) :
    RNStreamControlIfc by stream, RNStreamChangeIfc, ObservableIfc<MetropolisHastingsMV> by Observable() {

    override var rnStream: RNStreamIfc = stream

    private val targetFun = theTargetFun
    private val proposalFun = theProposalFun

    var initialX = theInitialX.copyOf()
        get() = field.copyOf()
        set(value) {
            field = value.copyOf()
        }

    var isInitialized = false
        protected set

    var isBurnedIn = false
        protected set

    val acceptanceStatistics: Statistic = Statistic("Acceptance Statistics")
        get() = field.instance()

    private val myObservedStatistics: List<Statistic> = buildList {
        for (i in initialX.indices) {
            this[i] = Statistic("X:" + (i + 1))
        }
    }

    var currentX: DoubleArray = DoubleArray(initialX.size)
        get() = field.copyOf()
        protected set

    var proposedY: DoubleArray = DoubleArray(initialX.size)
        get() = field.copyOf()
        protected set

    var previousX: DoubleArray = DoubleArray(initialX.size)
        get() = field.copyOf()
        protected set

    var lastAcceptanceProbability = Double.NaN
        protected set

    var targetFunctionAtProposedY = Double.NaN
        protected set

    var targetFunctionAtCurrentX = Double.NaN
        protected set

    fun observedStatistics(): List<Statistic> {
        val mutableList = mutableListOf<Statistic>()
        for (statistic in myObservedStatistics) {
            mutableList.add(statistic.instance())
        }
        return mutableList
    }

    /**
     * Resets the automatically collected statistics
     */
    fun resetStatistics() {
        for (s in myObservedStatistics) {
            s.reset()
        }
        acceptanceStatistics.reset()
    }

    /** Runs a burn in period and assigns the initial value of the process to the last
     * value from the burn in process.
     *
     * @param burnInAmount the amount to burn in
     */
    fun runBurnInPeriod(burnInAmount: Int) {
        val x: DoubleArray = runAll(burnInAmount)
        isBurnedIn = true
        isInitialized = false
        initialX = x
        resetStatistics()
    }

    /**  Resets statistics and sets the initial state the the initial value or to the value
     * found via the burn in period (if the burn in period was run).
     *
     */
    fun initialize() {
        lastAcceptanceProbability = Double.NaN
        targetFunctionAtProposedY = Double.NaN
        targetFunctionAtCurrentX = Double.NaN
        isInitialized = true
        currentX = initialX
        resetStatistics()
    }

    /**
     *
     * @param n  runs the process for n steps
     * @return the value of the process after n steps
     */
    fun runAll(n: Int): DoubleArray {
        require(n > 0) { "The number of iterations to run was less than or equal to zero." }
        initialize()
        var value = DoubleArray(initialX.size)
        for (i in 1..n) {
            value = next()
        }
        return value.copyOf()
    }

    /** Moves the process one step
     *
     * @return the next value of the process after proposing the next state (y)
     */
    fun next(): DoubleArray {
        if (!isInitialized) {
            initialize()
        }
        previousX = currentX
        proposedY = proposalFun.generateProposedGivenCurrent(currentX)
        lastAcceptanceProbability = acceptanceFunction(currentX, proposedY)
        if (rnStream.randU01() <= lastAcceptanceProbability) {
            currentX = proposedY
            acceptanceStatistics.collect(1.0)
        } else {
            acceptanceStatistics.collect(0.0)
        }
        for (i in currentX.indices) {
            myObservedStatistics[0].collect(currentX[i])
        }
        notifyObservers(this, this)
        return currentX
    }

    /** Computes the acceptance function for each step
     *
     * @param currentX the current state
     * @param proposedY the proposed state
     * @return the evaluated acceptance function
     */
    protected fun acceptanceFunction(currentX: DoubleArray, proposedY: DoubleArray): Double {
        val fRatio = getFunctionRatio(currentX, proposedY)
        val pRatio = proposalFun.proposalRatio(currentX, proposedY)
        val ratio = fRatio * pRatio
        return Math.min(ratio, 1.0)
    }

    /**
     *
     * @param currentX the current state
     * @param proposedY the proposed state
     * @return the ratio of f(y)/f(x) for the generation step
     */
    protected fun getFunctionRatio(currentX: DoubleArray, proposedY: DoubleArray): Double {
        val fx = targetFun.fx(currentX)
        val fy = targetFun.fx(proposedY)
        check(fx >= 0.0) { "The target function was < 0 at current state" }
        check(fy >= 0.0) { "The proposal function was < 0 at proposed state" }
        val ratio: Double
        if (fx != 0.0) {
            ratio = fy / fx
            targetFunctionAtCurrentX = fx
            targetFunctionAtProposedY = fy
        } else {
            ratio = Double.POSITIVE_INFINITY
            targetFunctionAtCurrentX = fx
            targetFunctionAtProposedY = fy
        }
        return ratio
    }

    override fun toString(): String {
        return asString()
    }

    open fun asString(): String {
        val sb = StringBuilder("MetropolisHastings1D")
        sb.appendLine()
        sb.append("Initialized Flag = ").append(isInitialized)
        sb.appendLine()
        sb.append("Burn In Flag = ").append(isBurnedIn)
        sb.appendLine()
        sb.append("Initial X =").append(initialX.contentToString())
        sb.appendLine()
        sb.append("Current X = ").append(currentX.contentToString())
        sb.appendLine()
        sb.append("Previous X = ").append(previousX.contentToString())
        sb.appendLine()
        sb.append("Last Proposed Y= ").append(proposedY.contentToString())
        sb.appendLine()
        sb.append("Last Prob. of Acceptance = ").append(lastAcceptanceProbability)
        sb.appendLine()
        sb.append("Last f(Y) = ").append(targetFunctionAtProposedY)
        sb.appendLine()
        sb.append("Last f(X) = ").append(targetFunctionAtCurrentX)
        sb.appendLine()
        sb.append("Acceptance Statistics")
        sb.appendLine()
        sb.append(acceptanceStatistics.asString())
        sb.appendLine()
        for (s in myObservedStatistics) {
            sb.append(s.asString())
            sb.appendLine()
        }
        return sb.toString()
    }

    companion object{
        /**
         *
         * @param initialX the initial value to start the burn in period
         * @param burnInAmount the number of samples in the burn in period
         * @param targetFun the target function
         * @param proposalFun the proposal function
         */
        fun create(
            initialX: DoubleArray, burnInAmount: Int, targetFun: FunctionMVIfc,
            proposalFun: ProposalFunctionMVIfc
        ): MetropolisHastingsMV {
            val m = MetropolisHastingsMV(initialX, targetFun, proposalFun)
            m.runBurnInPeriod(burnInAmount)
            return m
        }
    }

}