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

/**
 * Represents a multi-variate distribution with the specified dimensions.
 * The sampling of each dimension is independent. That is the resulting
 * distribution has independent marginals that are represented by the same
 * distribution as provided by the supplied random variable
 * @param theDimension         the dimension, must be at least 2
 * @param theRandomVariable the random variable for the marginals
 */
class MVIndependentRV(theDimension: Int, theRandomVariable: RVariableIfc) : MVRVariable (theRandomVariable.rnStream){
    init {
        require(theDimension > 1) { "The multi-variate dimension must be at least 2" }
    }
    override val dimension: Int = theDimension

    private val myRV: RVariableIfc = theRandomVariable

    override fun instance(stream: RNStreamIfc): MVRVariableIfc {
        return MVIndependentRV(dimension, myRV.instance())
    }

    override fun antitheticInstance(): MVRVariableIfc {
        return MVIndependentRV(dimension, myRV.antitheticInstance())
    }

    override fun generate(array: DoubleArray) {
        require(array.size == dimension) { "The size of the array to fill does not match the sampling dimension!" }
        myRV.sample(array)
    }
}