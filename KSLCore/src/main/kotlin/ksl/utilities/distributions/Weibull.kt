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
package ksl.utilities.distributions

import ksl.utilities.Interval
import ksl.utilities.random.rng.RNStreamIfc
import ksl.utilities.random.rvariable.GetRVariableIfc
import ksl.utilities.random.rvariable.RVariableIfc
import ksl.utilities.random.rvariable.WeibullRV
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** This class defines a Weibull distribution
 * @param theShape The shape parameter of the distribution
 * @param theScale The scale parameter of the distribution
 * @param name an optional name/label
 */
class Weibull(theShape: Double = 1.0, theScale: Double = 1.0, name: String? = null) :
    Distribution<Weibull>(name), ContinuousDistributionIfc, InverseCDFIfc, GetRVariableIfc {
    init {
        require(theShape > 0) { "Shape parameter must be positive" }
        require(theScale > 0) { "Scale parameter must be positive" }
    }

    var shape = theShape
        set(value) {
            require(value > 0) { "Shape parameter must be positive" }
            field = value
        }

    var scale = theScale
        set(value) {
            require(value > 0) { "Scale parameter must be positive" }
            field = value
        }

    /** Constructs a weibull distribution with
     * shape = parameters[0] and scale = parameters[1]
     * @param parameters An array with the shape and scale
     */
    constructor(parameters: DoubleArray) : this(parameters[0], parameters[1], null)

    override fun instance(): Weibull {
        return Weibull(shape, scale)
    }

    override fun domain(): Interval {
        return Interval(0.0, Double.POSITIVE_INFINITY)
    }

    /** Sets the parameters
     * @param theShape The shape parameter must &gt; 0.0
     * @param theScale The scale parameter must be &gt; 0.0
     */
    fun parameters(theShape: Double, theScale: Double) {
        shape = theShape
        scale = theScale
    }

    /** Sets the parameters for the distribution with
     * shape = parameters[0] and scale = parameters[1]
     *
     * @param params an array of doubles representing the parameters for
     * the distribution
     */
    override fun parameters(params: DoubleArray) {
        shape = params[0]
        scale = params[1]
    }

    /** Gets the parameters for the distribution
     *
     * @return Returns an array of the parameters for the distribution
     */
    override fun parameters(): DoubleArray {
        return doubleArrayOf(shape, scale)
    }

    override fun mean(): Double { // shape = alpha, scale = beta
        val ia = 1.0 / shape
        val gia = Gamma.gammaFunction(ia)
        return scale * ia * gia
    }

    override fun variance(): Double {
        val ia = 1.0 / shape
        val gia = Gamma.gammaFunction(ia)
        val g2ia = Gamma.gammaFunction(2.0 * ia)
        return scale * scale * ia * (2.0 * g2ia - ia * gia * gia)
    }

    override fun cdf(x: Double): Double {
        return if (x > 0.0) {
            1 - exp(-(x / scale).pow(shape))
        } else {
            0.0
        }
    }

    override fun pdf(x: Double): Double {
        if (x <= 0) {
            return 0.0
        }
        val e1 = -(x / scale).pow(shape)
        var f = scale * scale.pow(-shape)
        f = f * x.pow(shape - 1.0)
        f = f * exp(e1)
        return f
    }

    override fun invCDF(p: Double): Double {
        require(!(p < 0.0 || p > 1.0)) { "Supplied probability was $p Probability must be [0,1]" }
        if (p <= 0.0) {
            return 0.0
        }
        return if (p >= 1.0) {
            Double.POSITIVE_INFINITY
        } else scale * (-ln(1.0 - p)).pow(1.0 / shape)
    }

    /**
     *
     * @return the 3rd moment
     */
    val moment3: Double
        get() = shape.pow(3.0) * exp(Gamma.logGammaFunction(1.0 + 3.0 * (1.0 / scale)))

    /**
     *
     * @return the 4th moment
     */
    val moment4: Double
        get() = shape.pow(4.0) * exp(Gamma.logGammaFunction(1.0 + 4.0 * (1.0 / scale)))

    /** Gets the kurtosis of the distribution
     * www.mathworld.wolfram.com/WeibullDistribution.html
     * @return the kurtosis
     */
    fun kurtosis(): Double {
        val c1 = (shape + 1.0) / shape
        val c2 = (shape + 2.0) / shape
        val c3 = (shape + 3.0) / shape
        val c4 = (shape + 4.0) / shape
        val gc1 = Gamma.gammaFunction(c1)
        val gc2 = Gamma.gammaFunction(c2)
        val gc3 = Gamma.gammaFunction(c3)
        val gc4 = Gamma.gammaFunction(c4)
        val n = -3.0 * gc1 * gc1 * gc1 * gc1 + 6.0 * gc1 * gc1 * gc2 - 4.0 * gc1 * gc3 + gc4
        val d = (gc1 * gc1 - gc2) * (gc1 * gc1 - gc2)
        return n / d - 3.0
    }

    /** Gets the skewness of the distribution
     * www.mathworld.wolfram.com/WeibullDistribution.html
     * @return the skewness
     */
    fun skewness(): Double {
        val c1 = (shape + 1.0) / shape
        val c2 = (shape + 2.0) / shape
        val c3 = (shape + 3.0) / shape
        val gc1 = Gamma.gammaFunction(c1)
        val gc2 = Gamma.gammaFunction(c2)
        val gc3 = Gamma.gammaFunction(c3)
        val n = 2.0 * gc1 * gc1 * gc1 - 3.0 * gc1 * gc2 + gc3
        val d = sqrt((gc2 - gc1 * gc1) * (gc2 - gc1 * gc1) * (gc2 - gc1 * gc1))
        return n / d
    }

    override fun randomVariable(stream: RNStreamIfc): RVariableIfc {
        return WeibullRV(shape, scale, stream)
    }

}