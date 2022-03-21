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

import ksl.utilities.random.rng.RNStreamIfc


/**
 * Beta(alpha1, alpha2) random variable, range (0,1)
 */
class BetaRV constructor(
    val alpha1: Double = 1.0, val alpha2: Double = 1.0,
    stream: RNStreamIfc = KSLRandom.nextRNStream(), name: String? = null
) : RVariable(stream, name) {
    init {
        require(alpha1 > 0) { "The 1st shape parameter must be > 0" }
        require(alpha2 > 0) { "The 2nd shape parameter must be > 0" }
    }

    constructor(alpha1: Double, alpha2: Double, streamNum: Int) : this(alpha1, alpha2, KSLRandom.rnStream(streamNum)) {}

    /**
     * @param stream the RNStreamIfc to use
     * @return a new instance with same parameter value
     */
    override fun instance(stream: RNStreamIfc): BetaRV {
        return BetaRV(alpha1, alpha2, stream)
    }

    override fun generate(): Double {
        return KSLRandom.rBeta(alpha1, alpha2, rnStream)
    }

}