/*
 *     The KSL provides a discrete-event simulation library for the Kotlin programming language.
 *     Copyright (C) 2023  Manuel D. Rossetti, rossetti@uark.edu
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
package ksl.utilities.random.rvariable

import ksl.utilities.random.rng.RNStreamIfc
import kotlin.math.sqrt

/**
 * Normal(mean, variance)
 * @param mean the mean of the distribution
 * @param variance the variance of the distribution
 */
class NormalRV(
    val mean: Double = 0.0,
    val variance: Double = 1.0,
    stream: RNStreamIfc = KSLRandom.nextRNStream(),
    name: String? = null
) : ParameterizedRV(stream, name) {

    init {
        require(variance > 0) { "Variance must be positive" }
    }

    /**
     * @return the standard deviation of the random variable
     */
    val stdDeviation: Double = sqrt(variance)

    constructor(mean: Double, variance: Double, streamNum: Int) :
            this(mean, variance, KSLRandom.rnStream(streamNum))

    override fun instance(stream: RNStreamIfc): NormalRV {
        return NormalRV(mean, variance, stream)
    }

    override fun generate(): Double {
        return KSLRandom.rNormal(mean, variance, rnStream)
    }

    override fun toString(): String {
        return "NormalRV(mean=$mean, variance=$variance)"
    }

    override val parameters: RVParameters
        get() {
            val parameters: RVParameters = RVParameters.NormalRVParameters()
            parameters.changeDoubleParameter("mean", mean)
            parameters.changeDoubleParameter("variance", variance)
            return parameters
        }
}