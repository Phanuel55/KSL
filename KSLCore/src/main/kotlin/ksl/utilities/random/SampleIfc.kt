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
package ksl.utilities.random

interface SampleIfc {
    /**
     * @return generates a random value
     */
    fun sample(): Double

    /**
     * Generates a random generated sample of the give size
     *
     * @param sampleSize the amount to fill
     * @return A array holding the generated values
     */
    fun sample(sampleSize: Int): DoubleArray {
        val x = DoubleArray(sampleSize)
        for (i in 0 until sampleSize) {
            x[i] = sample()
        }
        return x
    }

    /**
     * Fills the supplied array with randomly generated values
     *
     * @param values the array to fill
     */
    fun sample(values: DoubleArray) {
        for (i in values.indices) {
            values[i] = sample()
        }
    }
}