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
 * JohnsonB(alpha1, alpha2, min, max) random variable
 * @param alpha1 alpha1 parameter
 * @param alpha2 alpha2 parameter, must be greater than zero
 * @param min    the min, must be less than max
 * @param max    the max
 * @param stream    the random number stream
 */
class JohnsonBRV (
    val alpha1: Double,
    val alpha2: Double,
    val min: Double,
    val max: Double,
    stream: RNStreamIfc = KSLRandom.nextRNStream()
) : RVariable(stream) {
    init {
        require(alpha2 > 0) { "alpha2 must be > 0" }
        require(max > min) { "the min must be < than the max" }
    }

    constructor(alpha1: Double, alpha2: Double, min: Double, max: Double, streamNum: Int) :
            this(alpha1, alpha2, min, max, KSLRandom.rnStream(streamNum))

    override fun instance(stream: RNStreamIfc): JohnsonBRV {
        return JohnsonBRV(alpha1, alpha2, min, max, stream)
    }

    override fun generate(): Double {
        return KSLRandom.rJohnsonB(alpha1, alpha2, min, max, rnStream)
    }

    override fun toString(): String {
        return "JohnsonBRV(alpha1=$alpha1, alpha2=$alpha2, min=$min, max=$max)"
    }

}