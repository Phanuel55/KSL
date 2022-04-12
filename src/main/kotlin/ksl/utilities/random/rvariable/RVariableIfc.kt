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
package ksl.utilities.random.rvariable

import ksl.utilities.PreviousValueIfc
import ksl.utilities.random.RandomIfc
import ksl.utilities.random.rng.RNStreamIfc

/**
 * An interface for defining random variables. The methods sample() and getValue() gets a new
 * value of the random variable sampled accordingly.  The method getPreviousValue() returns
 * the value from the last call to sample() or getValue(). The value returned by getPreviousValue() stays
 * the same until the next call to sample() or getValue().  The methods sample() or getValue() always get
 * the next random value.  If sample() or getValue() is never called then getPreviousValue() returns Double.NaN.
 * Use sample() or getValue() to get a new random value and use getPreviousValue() to get the last sampled value.
 *
 *
 * The preferred approach to creating random variables is to sub-class AbstractRVariable.
 */
interface RVariableIfc : RandomIfc, NewAntitheticInstanceIfc, PreviousValueIfc {
    /**
     * The set of pre-defined distribution types
     */
    enum class RVType {
        Bernoulli, Beta, ChiSquared, Binomial, Constant, DUniform, Exponential, Gamma,
        GeneralizedBeta, Geometric, JohnsonB, Laplace, LogLogistic, Lognormal, NegativeBinomial,
        Normal, PearsonType5, PearsonType6, Poisson, ShiftedGeometric, Triangular,
        Uniform, Weibull, DEmpirical, Empirical
    }

    /**
     * The randomly generated value. Each value
     * will be different
     */
    val value: Double
        get() = sample()

    /**
     * The randomly generated value. Each value
     * will be different
     * @return the randomly generated value, same as using property value
     */
    override fun value(): Double = value

    /**
     * @param n the number of values to sum, must be 1 or more
     * @return the sum of n values of getValue()
     */
    fun sum(n: Int): Double {
        require(n >= 1) { "There must be at least 1 in the sum" }
        var sum = 0.0
        for (i in 1..n) {
            sum = sum + value
        }
        return sum
    }

    /**
     * @param stream the RNStreamIfc to use
     * @return a new instance with same parameter values
     */
    fun instance(stream: RNStreamIfc): RVariableIfc

    /**
     * @return a new instance with same parameter values, with a different stream
     */
    fun instance(): RVariableIfc {
        return instance(KSLRandom.nextRNStream())
    }

    override fun antitheticInstance(): RVariableIfc{
        return instance(rnStream.antitheticInstance())
    }
}