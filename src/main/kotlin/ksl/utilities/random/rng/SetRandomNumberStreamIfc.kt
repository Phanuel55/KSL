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
package ksl.utilities.random.rng

import ksl.utilities.random.rvariable.KSLRandom

interface SetRandomNumberStreamIfc {
    /**
     * Sets the underlying random number stream
     *
     * @param stream the reference to the random number stream, must not be null
     */
    fun setRandomNumberStream(stream: RNStreamIfc)

    /** Assigns the stream associated with the supplied number from the default RNStreamProvider
     *
     * @param streamNumber a stream number, 1, 2, etc.
     */
    fun setRandomNumberStream(streamNumber: Int) {
        setRandomNumberStream(KSLRandom.rnStream(streamNumber))
    }
}