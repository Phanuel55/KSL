package ksl.utilities.random.mcintegration

import ksl.utilities.math.FunctionIfc
import ksl.utilities.random.rvariable.RVariableIfc
import ksl.utilities.random.rvariable.UniformRV


/**
 * Provides for the integration of a 1-D function via Monte-Carlo sampling.
 * See the detailed discussion for the class MCExperiment.
 *
 * The evaluation will automatically utilize
 * antithetic sampling to reduce the variance of the estimates unless the user specifies not to do so. In the case of
 * using antithetic sampling, the micro replication sample size refers to the number of independent antithetic pairs observed. Thus, this
 * will require two function evaluations for each micro replication. The user can consider the implication of the cost of
 * function evaluation versus the variance reduction obtained.
 * The default confidence level has been set to 99 percent.
 * @param function the representation of h(x), must not be null
 * @param sampler  the sampler over the interval, must not be null
 * @param antitheticOptionOn  true represents use of antithetic sampling
 */
class MC1DIntegration (
    function: FunctionIfc,
    sampler: RVariableIfc,
    antitheticOption: Boolean = true
) : MCExperiment() {
    protected val myFunction: FunctionIfc
    protected val mySampler: RVariableIfc
    protected var myAntitheticSampler: RVariableIfc? = null
    /**
     *
     * @param function the representation of h(x), must not be null
     * @param sampler  the sampler over the interval, must not be null
     */
    init {
        myFunction = function
        mySampler = sampler
        if (antitheticOption){
            myAntitheticSampler = sampler.antitheticInstance()
        }
        confidenceLevel = 0.99
    }

    override fun runSimulation(): Double {
        if (resetStreamOption) {
            mySampler.resetStartStream()
            if (isAntitheticOptionOn) {
                myAntitheticSampler!!.resetStartStream()
            }
        }
        return super.runSimulation()
    }

    override fun replication(r: Int): Double {
        return if (isAntitheticOptionOn) {
            val y1 = myFunction.f(mySampler.sample())
            val y2 = myFunction.f(myAntitheticSampler!!.sample())
            (y1 + y2) / 2.0
        } else {
            myFunction.f(mySampler.sample())
        }
    }

    /**
     *
     * @return true if the antithetic option is on
     */
    val isAntitheticOptionOn: Boolean
        get() = myAntitheticSampler != null

}

fun main() {
    class SinFunc : FunctionIfc {
        override fun f(x: Double): Double {
            return Math.PI * Math.sin(x)
        }
    }

    val f = SinFunc()
    val mc = MC1DIntegration(f, UniformRV(0.0, Math.PI))

//        mc.runInitialSample();
    println(mc)
    println()
    mc.runSimulation()
    println(mc)
}