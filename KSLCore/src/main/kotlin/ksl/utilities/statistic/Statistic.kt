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
package ksl.utilities.statistic

import ksl.utilities.distributions.Normal
import ksl.utilities.distributions.StudentT
import kotlin.math.*

private var StatCounter: Int = 0

/**
 * The Statistic class allows the collection of summary statistics on data via
 * the collect() methods.  The primary statistical summary is for the statistical moments.
 * Creates a Statistic with the given name
 *
 * @param name an optional String representing the name of the statistic
 * @param values an optional array of values to collect on
 */
class Statistic(name: String = "Statistic_${++StatCounter}", values: DoubleArray? = null) :
    AbstractStatistic(name) {

    /**
     * Holds the first 4 statistical central moments
     */
    private var myMoments: DoubleArray = DoubleArray(5)

    /**
     * Holds the number of observations observed
     */
    private var myNum = 0.0

    /**
     * Holds the last value observed
     */
    private var myValue = 0.0

    /**
     * Holds the sum the lag-1 data, i.e. from the second data point on variable
     * for collecting lag1 covariance
     */
    private var mySumXX = 0.0

    /**
     * Holds the first observed data point, needed for von-neuman statistic
     */
    private var myFirstX = 0.0

    /**
     * Holds sum = sum + j*x
     */
    private var myJsum = 0.0

    private var myMin = Double.POSITIVE_INFINITY

    private var myMax = Double.NEGATIVE_INFINITY

    init {
        if (values != null) {
            for (x in values) {
                collect(x)
            }
        }
    }

    /**
     * Creates a Statistic based on the provided array
     *
     * @param values an array of values to collect statistics on
     */
    constructor(values: DoubleArray?) : this("Statistic_${++StatCounter}", values)

    override val count: Double
        get() = myMoments[0]

    override val sum: Double
        get() = myMoments[1] * myMoments[0]

    override val average: Double
        get() = if (myMoments[0] < 1.0) {
            Double.NaN
        } else myMoments[1]

    override val deviationSumOfSquares: Double
        get() = myMoments[2] * myMoments[0]

    override val variance: Double
        get() = if (myMoments[0] < 2.0) {
            Double.NaN
        } else deviationSumOfSquares / (myMoments[0] - 1.0)

    override val min: Double
        get() = myMin

    override val max: Double
        get() = myMax

    override val kurtosis: Double
        get() {
            if (myMoments[0] < 4.0) {
                return Double.NaN
            }
            val n = myMoments[0]
            val n1 = n - 1.0
            val v = variance
            val d = (n - 1.0) * (n - 2.0) * (n - 3.0) * v * v
            val t = n * (n + 1.0) * n * myMoments[4] - 3.0 * n1 * n1 * n1 * v * v
            return t / d
        }

    override val skewness: Double
        get() {
            if (myMoments[0] < 3.0) {
                return Double.NaN
            }
            val n = myMoments[0]
            val v = variance
            val s = sqrt(v)
            val d = (n - 1.0) * (n - 2.0) * v * s
            val t = n * n * myMoments[3]
            return t / d
        }

    override val standardError: Double
        get() = if (myMoments[0] < 1.0) {
            Double.NaN
        } else standardDeviation / sqrt(myMoments[0])

    override val lag1Covariance: Double
        get() = if (myNum > 2.0) {
            val c1 = mySumXX - (myNum + 1.0) * myMoments[1] * myMoments[1] + myMoments[1] * (myFirstX + myValue)
            c1 / myNum
        } else {
            Double.NaN
        }

    override val lag1Correlation: Double
        get() = if (myNum > 2.0) {
            lag1Covariance / myMoments[2]
        } else {
            Double.NaN
        }

    override val vonNeumannLag1TestStatistic: Double
        get() = if (myNum > 2.0) {
            val r1 = lag1Correlation
            val t =
                (myFirstX - myMoments[1]) * (myFirstX - myMoments[1]) + (myValue - myMoments[1]) * (myValue - myMoments[1])
            val b = 2.0 * myNum * myMoments[2]
            val v = sqrt((myNum * myNum - 1.0) / (myNum - 2.0)) * (r1 + t / b)
            v
        } else {
            Double.NaN
        }

    /**
     * Creates an instance of Statistic that is a copy of the supplied Statistic
     * All internal state is the same. The only exception is for the id of the returned Statistic.
     *
     * @return a copy of the supplied Statistic
     */
    fun instance(): Statistic {
        val s = Statistic(name)
        s.numberMissing = numberMissing
        s.myFirstX = myFirstX
        s.myMax = myMax
        s.myMin = myMin
        s.confidenceLevel = confidenceLevel
        s.myJsum = myJsum
        s.myValue = myValue
        s.myNum = myNum
        s.mySumXX = mySumXX
        s.myMoments = myMoments.copyOf()
        s.lastValue = lastValue
        return s
    }

    /**
     * Returns the 2nd statistical central moment
     *
     * @return the 2nd statistical central moment
     */
    val centralMoment2 = myMoments[2]

    /**
     * Returns the 3rd statistical central moment
     *
     * @return the 3rd statistical central moment
     */
    val centralMoment3 = myMoments[3]

    /**
     * Returns the 4th statistical central moment
     *
     * @return the 4th statistical central moment
     */
    val centralMoment4 = myMoments[4]

    /**
     * The 0th moment is the count, the 1st central moment zero,
     * the 2nd, 3rd, and 4th central moments
     *
     * @return an array holding the central moments, 0, 1, 2, 3, 4
     */
    val centralMoments: DoubleArray
        get() = myMoments.copyOf()

    /**
     * Returns the 2nd statistical raw moment (about zero)
     *
     * @return the 2nd statistical raw moment (about zero)
     */
    val rawMoment2 = myMoments[2] + average * average

    /**
     * Returns the 3rd statistical raw moment (about zero)
     *
     * @return the 3rd statistical raw moment (about zero)
     */
    val rawMoment3: Double
        get() {
            val m3 = centralMoment3
            val mr2 = rawMoment2
            val mu = average
            return m3 + 3.0 * mu * mr2 - 2.0 * mu * mu * mu
        }

    /**
     * Returns the 4th statistical raw moment (about zero)
     *
     * @return the 4th statistical raw moment (about zero)
     */
    val rawMoment4: Double
      get() {
        val m4 = centralMoment4
        val mr3 = rawMoment3
        val mr2 = rawMoment2
        val mu = average
        return m4 + 4.0 * mu * mr3 - 6.0 * mu * mu * mr2 + 3.0 * mu * mu * mu * mu
    }

    /**
     * Checks if the supplied value falls within getAverage() +/- getHalfWidth()
     *
     * @param mean the value to check
     * @return true if the supplied value falls within getAverage() +/-
     * getHalfWidth()
     */
    fun checkMean(mean: Double): Boolean {
        val a = average
        val hw = halfWidth
        val ll = a - hw
        val ul = a + hw
        return mean in ll..ul
    }

    /**
     * Returns the half-width for a confidence interval on the mean with
     * confidence level  based on StudentT distribution
     *
     * @param level the confidence level
     * @return the half-width
     */
    override fun halfWidth(level: Double): Double {
        require(!(level <= 0.0 || level >= 1.0)) { "Confidence Level must be (0,1)" }
        if (count <= 1.0) {
            return Double.NaN
        }
        val dof = count - 1.0
        val alpha = 1.0 - level
        val p = 1.0 - alpha / 2.0
        val t = StudentT.invCDF(dof, p)
        return t * standardError
    }

    override fun leadingDigitRule(multiplier: Double): Int {
        return floor(log10(multiplier * standardError)).toInt()
    }

    /**
     * @return the p-value associated with the current Von Neumann Lag 1 Test Statistic, or Double.NaN
     */
    fun vonNeumannLag1TestStatisticPValue(): Double {
        if (vonNeumannLag1TestStatistic.isNaN()){
            return Double.NaN
        }
        return Normal.stdNormalComplementaryCDF(vonNeumannLag1TestStatistic)
    }

    /**
     * Returns the observation weighted sum of the data i.e. sum = sum + j*x
     * where j is the observation number and x is jth observation
     *
     * @return the observation weighted sum of the data
     */
    val obsWeightedSum: Double = myJsum

    override var value: Double
        get() = lastValue
        set(value) {
            collect(value)
        }

    override fun collect(obs: Double) {
        if (obs.isMissing()) {
            numberMissing++
            return
        }
        // update moments
        myNum = myNum + 1
        myJsum = myJsum + myNum * obs
        val n = myMoments[0]
        val n1 = n + 1.0
        val n2 = n * n
        val delta = (myMoments[1] - obs) / n1
        val d2 = delta * delta
        val d3 = delta * d2
        val r1 = n / n1
        myMoments[4] = (1.0 + n * n2) * d2 * d2 + 6.0 * myMoments[2] * d2 + 4.0 * myMoments[3] * delta + myMoments[4]
        myMoments[4] *= r1
        myMoments[3] = (1.0 - n2) * d3 + 3.0 * myMoments[2] * delta + myMoments[3]
        myMoments[3] *= r1
        myMoments[2] = (1.0 + n) * d2 + myMoments[2]
        myMoments[2] *= r1
        myMoments[1] = myMoments[1] - delta
        myMoments[0] = n1

        // to collect lag 1 cov, we need x(1)
        if (myNum == 1.0) {
            myFirstX = obs
        }

        // to collect lag 1 cov, we must provide new x and previous x
        // to collect lag 1 cov, we must sum x(i) and x(i+1)
        if (myNum >= 2.0) {
            mySumXX = mySumXX + obs * myValue
        }

        // update min, max, current value
        if (obs > myMax) {
            myMax = obs
        }
        if (obs < myMin) {
            myMin = obs
        }
        myValue = obs
        lastValue = obs
        notifyObservers(lastValue)
        emitter.emit(lastValue)
    }

    override fun reset() {
        super.reset()
        myValue = Double.NaN
//        myValue = 0.0
        myMin = Double.POSITIVE_INFINITY
        myMax = Double.NEGATIVE_INFINITY
        myNum = 0.0
        myJsum = 0.0
        mySumXX = 0.0
        for (i in myMoments.indices) {
            myMoments[i] = 0.0
        }
    }

    fun asString(): String {
        val sb = StringBuilder("Statistic{")
        sb.append("name='").append(name).append('\'')
        sb.append(", n=").append(count)
        sb.append(", avg=").append(average)
        sb.append(", sd=").append(standardDeviation)
        sb.append(", ci=").append(confidenceInterval.toString())
        sb.append('}')
        return sb.toString()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("ID ")
        sb.append(id)
        sb.appendLine()
        sb.append("Name ")
        sb.append(name)
        sb.appendLine()
        sb.append("Number ")
        sb.append(count)
        sb.appendLine()
        sb.append("Average ")
        sb.append(average)
        sb.appendLine()
        sb.append("Standard Deviation ")
        sb.append(standardDeviation)
        sb.appendLine()
        sb.append("Standard Error ")
        sb.append(standardError)
        sb.appendLine()
        sb.append("Half-width ")
        sb.append(halfWidth)
        sb.appendLine()
        sb.append("Confidence Level ")
        sb.append(confidenceLevel)
        sb.appendLine()
        sb.append("Confidence Interval ")
        sb.append(confidenceInterval)
        sb.appendLine()
        sb.append("Minimum ")
        sb.append(min)
        sb.appendLine()
        sb.append("Maximum ")
        sb.append(max)
        sb.appendLine()
        sb.append("Sum ")
        sb.append(sum)
        sb.appendLine()
        sb.append("Variance ")
        sb.append(variance)
        sb.appendLine()
        sb.append("Deviation Sum of Squares ")
        sb.append(deviationSumOfSquares)
        sb.appendLine()
        sb.append("Last value collected ")
        sb.append(lastValue)
        sb.appendLine()
        sb.append("Kurtosis ")
        sb.append(kurtosis)
        sb.appendLine()
        sb.append("Skewness ")
        sb.append(skewness)
        sb.appendLine()
        sb.append("Lag 1 Covariance ")
        sb.append(lag1Covariance)
        sb.appendLine()
        sb.append("Lag 1 Correlation ")
        sb.append(lag1Correlation)
        sb.appendLine()
        sb.append("Von Neumann Lag 1 Test Statistic ")
        sb.append(vonNeumannLag1TestStatistic)
        sb.appendLine()
        sb.append("Number of missing observations ")
        sb.append(numberMissing)
        sb.appendLine()
        sb.append("Lead-Digit Rule(1) ")
        sb.append(leadingDigitRule(1.0))
        sb.appendLine()
        return sb.toString()
    }

    /**
     * Returns the summary statistics values Name Count Average Std. Dev.
     *
     * @return the string of summary statistics
     */
    val summaryStatistics: String
        get() {
            val format = "%-50s \t %12d \t %12f \t %12f"
            val n = count.toInt()
            val avg = average
            val std = standardDeviation
            val name = name
            return String.format(format, name, n, avg, std)
        }

    /**
     * Returns the header for the summary statistics Name Count Average Std.
     * Dev.
     *
     * @return the header
     */
    val summaryStatisticsHeader: String
        get() = String.format("%-50s \t %12s \t %12s \t %12s", "Name", "Count", "Average", "Std. Dev.")

    /**
     * Estimates the number of observations needed in order to obtain a
     * getConfidenceLevel() confidence interval with plus/minus the provided
     * half-width
     *
     * @param desiredHW the desired half-width, must be greater than zero
     * @return the estimated sample size
     */
    fun estimateSampleSize(desiredHW: Double): Long {
        require(desiredHW > 0.0) { "The desired half-width must be > 0" }
        val cl = this.confidenceLevel
        val a = 1.0 - cl
        val a2 = a / 2.0
        val z = Normal.stdNormalInvCDF(1.0 - a2)
        val s = standardDeviation
        val m = z * s / desiredHW * (z * s / desiredHW)
        return (m + .5).roundToLong()
    }

    companion object {

        /**
         * Uses definition 7, as per R definitions
         *
         * @param data the array of data. will be sorted
         * @param p the percentile, must be within (0, 1)
         * @return the quantile
         */
        fun quantile(data: DoubleArray, p: Double): Double {
            require((p <= 0.0) || (p < 1.0)) { "Percentile value must be (0,1)" }
            val n = data.size
            if (n == 1) {
                return data[0]
            }
            data.sort()
            val index = 1 + (n - 1) * p
            if (index < 1.0) {
                return data[0]
            }
            if (index >= n) {
                return data[n - 1]
            }
            var lo = floor(index).toInt()
            var hi = ceil(index).toInt()
            val h = index - lo
            // correct for 0 based arrays
            lo = lo - 1
            hi = hi - 1
            return (1.0 - h) * data[lo] + h * data[hi]
        }

        /**
         * As per Apache Math commons
         *
         * @param data the array of data. will be sorted
         * @param p the percentile, must be within (0, 1)
         * @return the percentile
         */
        fun percentile(data: DoubleArray, p: Double): Double {
            require((p <= 0.0) || (p < 1.0)) { "Percentile value must be (0,1)" }
            val n = data.size
            if (n == 1) {
                return data[0]
            }
            data.sort()
            val pos = p * (n + 1)
            return if (pos < 1.0) {
                data[0]
            } else if (pos >= n) {
                data[n - 1]
            } else {
                val d = pos - floor(pos)
                val fpos = floor(pos).toInt() - 1 // correct for 0 based arrays
                val lower = data[fpos]
                val upper = data[fpos + 1]
                lower + d * (upper - lower)
            }
        }

        /**
         * Returns the median of the data. The array is sorted
         *
         * @param data the array of data
         * @return the median of the data
         */
        fun median(data: DoubleArray): Double {
            data.sort()
            val size = data.size
            val median = if (size % 2 == 0) { //even
                val firstIndex = size / 2 - 1
                val secondIndex = firstIndex + 1
                val firstValue = data[firstIndex]
                val secondValue = data[secondIndex]
                (firstValue + secondValue) / 2.0
            } else { //odd
                val index = ceil(size / 2.0).toInt()
                data[index]
            }
            return median
        }

        /**
         * Estimate the sample size based on a normal approximation
         *
         * @param desiredHW the desired half-width (must be bigger than 0)
         * @param stdDev    the standard deviation (must be bigger than or equal to 0)
         * @param level     the confidence level (must be between 0 and 1)
         * @return the estimated sample size
         */
        fun estimateSampleSize(desiredHW: Double, stdDev: Double, level: Double): Long {
            require(desiredHW > 0.0) { "The desired half-width must be > 0" }
            require(stdDev >= 0.0) { "The desired std. dev. must be >= 0" }
            require(!(level <= 0.0 || level >= 1.0)) { "Confidence Level must be (0,1)" }
            val a = 1.0 - level
            val a2 = a / 2.0
            val z = Normal.stdNormalInvCDF(1.0 - a2)
            val m = z * stdDev / desiredHW * (z * stdDev / desiredHW)
            return (m + .5).roundToLong()
        }
    }

}