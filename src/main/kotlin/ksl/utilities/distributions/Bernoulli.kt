package ksl.utilities.distributions

import ksl.utilities.math.KSLMath
import ksl.utilities.random.rng.RNStreamIfc
import ksl.utilities.random.rvariable.BernoulliRV
import ksl.utilities.random.rvariable.RVariableIfc
import kotlin.math.floor

/**
 * Provides an implementation of the Bernoulli
 * distribution with success probability (p)
 * P(X=1) = p
 * P(X=0) = 1-p
 * @param successProb the probability of success, must be between (0,1)
 * @param name an optional name
 */
class Bernoulli(successProb: Double= 0.5, name: String? = null) : Distribution<Bernoulli>(name), DiscreteDistributionIfc {

    init {
        require(!(successProb <= 0.0 || successProb >= 1.0)) { "Probability must be (0,1)" }
    }

    constructor(params: DoubleArray, name: String?) : this(params[0], name)

    // private data members
    var probOfSuccess = successProb
        set(prob) {
            require(!(prob <= 0.0 || prob >= 1.0)) { "Probability must be (0,1)" }
            field = prob
        }

    override fun instance(): Bernoulli {
        return Bernoulli(probOfSuccess)
    }

    override fun randomVariable(stream: RNStreamIfc): RVariableIfc {
        return BernoulliRV(probOfSuccess)
    }

    override fun cdf(x: Double): Double {
        val xx: Int = floor(x).toInt()
        return if (xx < 0) {
            0.0
        } else if (xx == 0) {
            (1.0 - probOfSuccess)
        } else  //if (x >= 1)
        {
            1.0
        }
    }

    override fun pmf(x: Double): Double {
        return if (KSLMath.equal(x, 0.0)) {
            (1.0 - probOfSuccess)
        } else if (KSLMath.equal(x, 1.0)) {
            probOfSuccess
        } else {
            0.0
        }
    }

    fun pmf(x: Int): Double {
        return if (x == 0) {
            (1.0 - probOfSuccess)
        } else if (x == 1) {
            probOfSuccess
        } else {
            0.0
        }
    }

    override fun mean(): Double {
        return probOfSuccess
    }

    override fun variance(): Double {
        return probOfSuccess * (1.0 - probOfSuccess)
    }

    override fun invCDF(p: Double): Double {
        require(!(p < 0.0 || p > 1.0)) { "Probability must be [0,1]" }
        return if (p <= probOfSuccess) {
            1.0
        } else {
            0.0
        }
    }

    override fun parameters(params: DoubleArray) {
        probOfSuccess = params[0]
    }

    override fun parameters(): DoubleArray {
        return doubleArrayOf(probOfSuccess)
    }

}