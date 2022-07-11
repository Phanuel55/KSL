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
package ksl.utilities.statistic

import ksl.utilities.KSLArrays
import ksl.utilities.math.KSLMath
import ksl.utilities.random.rvariable.ExponentialRV
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow


/**
 * A Histogram tabulates data into bins.  The user must specify the break points
 * of the bins, b0, b1, b2, ..., bk, where there are k+1 break points, and k bins.
 * b0 may be Double.NEGATIVE_INFINITY and bk may be Double.POSITIVE_INFINITY.
 *
 *
 * If only one break point is supplied, then the bins are automatically defined as:
 * (Double.NEGATIVE_INFINITY, b0] and (b0, Double.POSITIVE_INFINITY).
 *
 *
 * If two break points are provided, then there is one bin: [b0, b1), any values
 * less than b0 will be counted as underflow and any values [b1, +infinity) will
 * be counted as overflow.
 *
 *
 * If k+1 break points are provided then the bins are defined as:
 * [b0,b1), [b1,b2), [b2,b3), ..., [bk-1,bk) and
 * any values in (-infinity, b0) will be counted as underflow and any values [bk, +infinity) will
 * be counted as overflow. If b0 equals Double.NEGATIVE_INFINITY then there can
 * be no underflow. Similarly, if bk equals Double.POSITIVE_INFINITY there can be no
 * overflow.
 *
 *
 * The break points do not have to define equally sized bins. Static methods within HistogramBIfc
 * are provided to create equal width bins and to create histograms with common
 * characteristics.
 *
 *
 * If any presented value is Double.NaN, then the value is counted as missing
 * and the observation is not tallied towards the total number of observations. Underflow and
 * overflow counts also do not count towards the total number of observations.
 *
 *
 * Statistics are also automatically collected on the collected observations. The statistics
 * do not include missing, underflow, and overflow observations. Statistics are only computed
 * on those observations that were placed (counted) within some bin.
 *
 * @param breakPoints the break points for the histogram, must be strictly increasing
 * @param name an optional name for the histogram
 */
class Histogram(breakPoints: DoubleArray, name: String? = null) : AbstractStatistic(name),
    HistogramIfc {

    /**
     * holds the binned data
     */
    private val myBins: List<HistogramBin> = makeBins(breakPoints)

    /**
     * Lower limit of first histogram bin.
     */
    override val firstBinLowerLimit: Double
        get() = myBins[0].lowerLimit

    /**
     * Upper limit of last histogram bin.
     */
    override val lastBinUpperLimit: Double
        get() = myBins[myBins.size - 1].upperLimit

    /**
     * Counts of values located below first bin.
     */
    override var underFlowCount: Double = 0.0
        private set

    /**
     * Counts of values located above last bin.
     */
    override var overFlowCount: Double = 0.0
        private set

    /**
     * Collects statistical information
     */
    private val myStatistic: Statistic = Statistic("$name Histogram")

    override val totalCount: Double
        get() = myStatistic.count + overFlowCount + underFlowCount

    override val count: Double
        get() = myStatistic.count
    override val sum: Double
        get() = myStatistic.sum
    override val average: Double
        get() = myStatistic.average
    override val deviationSumOfSquares: Double
        get() = myStatistic.deviationSumOfSquares
    override val variance: Double
        get() = myStatistic.variance
    override val min: Double
        get() = myStatistic.min
    override val max: Double
        get() = myStatistic.max
    override val kurtosis: Double
        get() = myStatistic.kurtosis
    override val skewness: Double
        get() = myStatistic.skewness
    override val standardError: Double
        get() = myStatistic.standardError
    override val lag1Covariance: Double
        get() = myStatistic.lag1Covariance
    override val lag1Correlation: Double
        get() = myStatistic.lag1Correlation
    override val vonNeumannLag1TestStatistic: Double
        get() = myStatistic.vonNeumannLag1TestStatistic

    override fun halfWidth(level: Double): Double {
        return myStatistic.halfWidth(level)
    }

    override fun leadingDigitRule(multiplier: Double): Int {
        return myStatistic.leadingDigitRule(multiplier)
    }

    override fun collect(obs: Double) {
        if (obs.isMissing()) {
            numberMissing++
            return
        }
        if (obs < firstBinLowerLimit) {
            underFlowCount++
        } else if (obs >= lastBinUpperLimit) {
            overFlowCount++
        } else {
            val bin = findBin(obs)
            bin.increment()
            // collect statistics on only binned observations
            myStatistic.collect(obs)
        }
    }

    override fun findBin(x: Double): HistogramBin {
        for (bin in myBins) {
            if (x < bin.upperLimit) {
                return bin
            }
        }
        // bin must be found, but just in case
        val s = "The observation = $x could not be binned!"
        throw IllegalStateException(s)
    }

    override fun reset() {
        numberMissing = 0.0
        myStatistic.reset()
        overFlowCount = 0.0
        underFlowCount = 0.0
        for (bin in myBins) {
            bin.reset()
        }
    }

    override fun binNumber(x: Double): Int {
        val bin = findBin(x)
        return bin.binNumber
    }

    override val numberBins: Int
        get() = myBins.size

    override fun bin(x: Double): HistogramBin {
        val bin = findBin(x)
        return bin.instance()
    }

    override fun bin(binNum: Int): HistogramBin {
        return myBins[binNum - 1].instance()
    }

    override val bins: List<HistogramBin>
        get() {
            val bins: MutableList<HistogramBin> = ArrayList()
            for (bin in myBins) {
                bins.add(bin.instance())
            }
            return bins
        }

    override val binArray: Array<HistogramBin>
        get() {
            return bins.toTypedArray()
        }

    override val breakPoints: DoubleArray
        get() {
            val b = DoubleArray(myBins.size + 1)
            for ((i, bin) in myBins.withIndex()) {
                b[i] = bin.lowerLimit
            }
            b[myBins.size] = bin(numberBins).upperLimit
            return b
        }

    override val binCounts: DoubleArray
        get() {
            val counts = DoubleArray(numberBins)
            for ((i, bin) in myBins.withIndex()) {
                counts[i] = bin.count()
            }
            return counts
        }

    override fun binCount(x: Double): Double {
        return findBin(x).count()
    }

    override fun binCount(binNum: Int): Double {
        return myBins[binNum - 1].count()
    }

    override fun binFraction(binNum: Int): Double {
        val n = myStatistic.count
        return if (n > 0.0) {
            binCount(binNum) / n
        } else {
            Double.NaN
        }
    }

    override fun binFraction(x: Double): Double {
        return binFraction(binNumber(x))
    }

    override fun cumulativeBinCount(x: Double): Double {
        return cumulativeBinCount(binNumber(x))
    }

    override fun cumulativeBinCount(binNum: Int): Double {
        if (binNum < 0) {
            return 0.0
        }
        if (binNum > myBins.size) {
            return myStatistic.count
        }
        var sum = 0.0
        for (i in 1..binNum) {
            sum = sum + binCount(i)
        }
        return sum
    }

    override fun cumulativeBinFraction(binNum: Int): Double {
        val n = myStatistic.count
        return if (n > 0.0) {
            cumulativeBinCount(binNum) / n
        } else {
            Double.NaN
        }
    }

    override fun cumulativeBinFraction(x: Double): Double {
        return cumulativeBinFraction(binNumber(x))
    }

    override fun cumulativeCount(binNum: Int): Double {
        if (binNum < 0) {
            return underFlowCount
        }
        return if (binNum > myBins.size) {
            totalCount
        } else underFlowCount + cumulativeBinCount(binNum)
    }

    override fun cumulativeCount(x: Double): Double {
        return cumulativeCount(binNumber(x))
    }

    override fun cumulativeFraction(binNum: Int): Double {
        val n = totalCount
        return if (n > 0.0) {
            cumulativeCount(binNum) / n
        } else {
            Double.NaN
        }
    }

    override fun cumulativeFraction(x: Double): Double {
        return cumulativeFraction(binNumber(x))
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Histogram: ").append(name)
        sb.append(System.lineSeparator())
        sb.append("-------------------------------------")
        sb.append(System.lineSeparator())
        sb.append("Number of bins = ").append(numberBins)
        sb.append(System.lineSeparator())
        sb.append("First bin starts at = ").append(firstBinLowerLimit)
        sb.append(System.lineSeparator())
        sb.append("Last bin ends at = ").append(lastBinUpperLimit)
        sb.append(System.lineSeparator())
        sb.append("Under flow count = ").append(underFlowCount)
        sb.append(System.lineSeparator())
        sb.append("Over flow count = ").append(overFlowCount)
        sb.append(System.lineSeparator())
        val n = count
        sb.append("Total bin count = ").append(n)
        sb.append(System.lineSeparator())
        sb.append("Total count = ").append(totalCount)
        sb.append(System.lineSeparator())
        sb.append("-------------------------------------")
        sb.append(System.lineSeparator())
        sb.append(String.format("%3s %-12s %-5s %-5s %-5s %-6s", "Bin", "Range", "Count", "CumTot", "Frac", "CumFrac"))
        sb.append(System.lineSeparator())
        //        sb.append("Bin \t Range \t Count \t\t tc \t\t p \t\t cp\n");
        var ct = 0.0
        for (bin in myBins) {
            val c: Double = bin.count()
            ct = ct + c
            val s = String.format("%s %5.1f %5f %6f %n", bin, ct, c / n, ct / n)
            sb.append(s)
        }
        sb.append("-------------------------------------")
        sb.append(System.lineSeparator())
        sb.append("Statistics on data collected within bins:")
        sb.append(System.lineSeparator())
        sb.append("-------------------------------------")
        sb.append(System.lineSeparator())
        sb.append(myStatistic)
        sb.append("-------------------------------------")
        sb.append(System.lineSeparator())
        return sb.toString()
    }


    companion object {
        /**
         * Create a histogram with lower limit set to zero
         *
         * @param upperLimit the upper limit of the last bin, cannot be positive infinity
         * @param numBins    the number of bins to create, must be greater than 0
         * @return the histogram
         */
        fun create(upperLimit: Double, numBins: Int): HistogramIfc {
            return create(0.0, upperLimit, numBins, null)
        }

        /**
         * Create a histogram
         *
         * @param lowerLimit lower limit of first bin, cannot be negative infinity
         * @param upperLimit the upper limit of the last bin, cannot be positive infinity
         * @param numBins    the number of bins to create, must be greater than 0
         * @return the histogram
         */
        fun create(lowerLimit: Double, upperLimit: Double, numBins: Int): HistogramIfc {
            return create(lowerLimit, upperLimit, numBins, null)
        }

        /**
         * Create a histogram with the given name based on the provided values
         *
         * @param lowerLimit lower limit of first bin, cannot be negative infinity
         * @param upperLimit the upper limit of the last bin, cannot be positive infinity
         * @param numBins    the number of bins to create, must be greater than zero
         * @param name       the name of the histogram
         * @return the histogram
         */
        fun create(lowerLimit: Double, upperLimit: Double, numBins: Int, name: String?): HistogramIfc {
            return Histogram(createBreakPoints(lowerLimit, upperLimit, numBins), name)
        }

        /**
         * @param numBins    the number of bins to make, must be greater than zero
         * @param lowerLimit the lower limit of the first bin, cannot be negative infinity
         * @param width      the width of each bin, must be greater than zero
         * @return the created histogram
         */
        fun create(lowerLimit: Double, numBins: Int, width: Double): HistogramIfc {
            return Histogram(createBreakPoints(lowerLimit, numBins, width))
        }

        /**
         * Divides the range equally across the number of bins.
         *
         * @param lowerLimit lower limit of first bin, cannot be negative infinity
         * @param upperLimit the upper limit of the last bin, cannot be positive infinity
         * @param numBins    the number of bins to create, must be greater than zero
         * @return the break points
         */
        fun createBreakPoints(lowerLimit: Double, upperLimit: Double, numBins: Int): DoubleArray {
            require(!lowerLimit.isInfinite()) { "The lower limit of the range cannot be infinite." }
            require(!upperLimit.isInfinite()) { "The upper limit of the range cannot be infinite." }
            require(lowerLimit < upperLimit) { "The lower limit must be < the upper limit of the range" }
            require(numBins > 0) { "The number of bins must be > 0" }
            val binWidth = KSLMath.roundToScale((upperLimit - lowerLimit) / numBins, false)
            return createBreakPoints(lowerLimit, numBins, binWidth)
        }

        /**
         * @param numBins    the number of bins to make, must be greater than 0
         * @param lowerLimit the lower limit of the first bin, cannot be negative infinity
         * @param width      the width of each bin, must be greater than 0
         * @return the constructed break points
         */
        fun createBreakPoints(lowerLimit: Double, numBins: Int, width: Double): DoubleArray {
            require(!lowerLimit.isInfinite()) { "The lower limit of the range cannot be infinite." }
            require(numBins > 0) { "The number of bins must be > 0" }
            require(width > 0) { "The width of the bins must be > 0" }
            val points = DoubleArray(numBins + 1)
            points[0] = lowerLimit
            for (i in 1 until points.size) {
                points[i] = points[i - 1] + width
            }
            return points
        }

        /**
         * @param breakPoints the break points w/o negative infinity
         * @return the break points with Double.NEGATIVE_INFINITY as the first break point
         */
        fun addNegativeInfinity(breakPoints: DoubleArray): DoubleArray {
            require(breakPoints.isNotEmpty()) { "The break points array was empty" }
            val b = DoubleArray(breakPoints.size + 1)
            System.arraycopy(breakPoints, 0, b, 1, breakPoints.size)
            b[0] = kotlin.Double.NEGATIVE_INFINITY
            return b
        }

        /**
         * @param breakPoints the break points w/o positive infinity
         * @return the break points with Double.POSITIVE_INFINITY as the last break point
         */
        fun addPositiveInfinity(breakPoints: DoubleArray): DoubleArray {
            require(breakPoints.isNotEmpty()) { "The break points array was empty" }
            val b = breakPoints.copyOf(breakPoints.size + 1)
            b[b.size - 1] = Double.POSITIVE_INFINITY
            return b
        }

        /**
         * http://www.fmrib.ox.ac.uk/analysis/techrep/tr00mj2/tr00mj2/node24.html
         *
         * @param observations observations for a histogram
         * @return a set of break points based on some theory
         */
        fun recommendBreakPoints(observations: DoubleArray): DoubleArray {
            require(observations.isNotEmpty()) { "The supplied observations array was empty" }
            if (observations.size == 1) {
                // use the sole observation
                val b = DoubleArray(1)
                b[0] = floor(observations[0])
                return b
            }
            // 2 or more observations
            val statistic = Statistic(observations)
            val LL = statistic.min
            val UL = statistic.max
            if (KSLMath.equal(LL, UL)) {
                // essentially the same, go back to 1 observation
                val b = DoubleArray(1)
                b[0] = floor(LL)
                return b
            }
            // more than 2 and some spread
            // try to approximate a reasonable number of bins from the observations
            // first determine a reasonable bin width
            val s = statistic.standardDeviation
            val n = statistic.count
            // http://www.fmrib.ox.ac.uk/analysis/techrep/tr00mj2/tr00mj2/node24.html
            //double iqr = 1.35*s;
            // use the more "optimal" estimate
            val width = 3.49 * s * n.pow(-1.0 / 3.0)
            // round the width to a reasonable scale
            val binWidth = KSLMath.roundToScale(width, false)
            // now compute a number of bins for this width
            val nb = (ceil(UL) - floor(LL)) / binWidth
            val numBins = ceil(nb).toInt()
            return createBreakPoints(floor(LL), numBins, binWidth)
        }

        /** Creates a list of ordered bins for use in a histogram
         *
         * @param breakPoints the break points
         * @return the list of histogram bins
         */
        fun makeBins(breakPoints: DoubleArray): List<HistogramBin> {
            require(KSLArrays.isStrictlyIncreasing(breakPoints)) { "The break points were not strictly increasing." }
            val binList: MutableList<HistogramBin> = ArrayList()
            // case of 1 break point must be handled
            if (breakPoints.size == 1) {
                // two bins, 1 below and 1 above
                binList.add(HistogramBin(1, Double.NEGATIVE_INFINITY, breakPoints[0]))
                binList.add(HistogramBin(2, breakPoints[0], Double.POSITIVE_INFINITY))
                return binList
            }

            // two or more break points
            for (i in 1 until breakPoints.size) {
                binList.add(HistogramBin(i, breakPoints[i - 1], breakPoints[i]))
            }
            return binList
        }
    }

}

fun main() {
    val d = ExponentialRV(2.0)
    val points = Histogram.createBreakPoints(0.0, 10, 0.25)
    val h1: HistogramIfc = Histogram(points)
    val h2: HistogramIfc = Histogram(Histogram.addPositiveInfinity(points))
    for (i in 1..100) {
        val x = d.value
        h1.collect(x)
        h2.collect(x)
    }
    println(h1)
    println(h2)
}