package ksl.utilities.distributions

import ksl.utilities.math.KSLMath
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sqrt

class Binomial {

    companion object {
        /**
         *
         * @param moments the mean is in element 0 and the variance is in element 1, must not be null
         * @return true if n and p can be set to match te moments
         */
        fun canMatchMoments(vararg moments: Double): Boolean {
            require(moments.size >= 2) { "Must provide a mean and a variance. You provided " + moments.size + " moments." }
            val m = moments[0]
            val v = moments[1]
            val validN = v >= m * (1 - m)
            return v > 0 && v < m && validN
        }

        /**
         *
         * @param moments the mean is in element 0 and the variance is in element 1, must not be null
         * @return the values of n and p that match the moments with p as element 0 and n as element 1
         */
        fun getParametersFromMoments(vararg moments: Double): DoubleArray {
            require(canMatchMoments(*moments)) { "Mean and variance must be positive, mean > variance, and variance >= mean*(1-mean). Your mean: " + moments[0] + " and variance: " + moments[1] }
            val m = moments[0]
            val v = moments[1]
            val n = (m * m / (m - v) + 0.5).toInt()
            val p = m / n
            return doubleArrayOf(p, n.toDouble())
        }

        /**
         *
         * @param moments the mean is in element 0 and the variance is in element 1, must not be null
         * @return if the moments can be matched a properly configured Binomial is returned
         */
//        fun createFromMoments(vararg moments: Double): Binomial? {
//            val param = getParametersFromMoments(*moments)
//            return Binomial(param)
//        }

        /** Computes the probability mass function at j using a
         * recursive (iterative) algorithm using logarithms
         *
         * @param j the value to evaluate
         * @param n number of trials
         * @param p success probability
         * @return probability of j
         */
        fun recursivePMF(j: Int, n: Int, p: Double): Double {
            require(n > 0) { "The number of trials must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            if (j < 0) {
                return 0.0
            }
            if (j > n) {
                return 0.0
            }
            val q = 1.0 - p
            val lnq = ln(q)
            var f = n * lnq
            if (j == 0) {
                return if (f <= KSLMath.smallestExponentialArgument) {
                    0.0
                } else {
                    exp(f)
                }
            }
            val lnp = ln(p)
            if (j == n) {
                val g = n * lnp
                return if (g <= KSLMath.smallestExponentialArgument) {
                    0.0
                } else {
                    exp(g)
                }
            }
            val c = lnp - lnq
            for (i in 1..j) {
                f = c + ln(n - i + 1.0) - ln(i.toDouble()) + f
            }
            require(f < KSLMath.largestExponentialArgument) { "Term overflow due to input parameters" }
            return if (f <= KSLMath.smallestExponentialArgument) {
                0.0
            } else exp(f)
        }

        /** Computes the probability mass function at j using a
         * recursive (iterative) algorithm using logarithms
         *
         * @param j the value to evaluate
         * @param n number of trials
         * @param p success probability
         * @return cumulative probability of j
         */
        fun recursiveCDF(j: Int, n: Int, p: Double): Double {
            require(n > 0) { "The number of trials must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            if (j < 0) {
                return 0.0
            }
            if (j >= n) {
                return 1.0
            }
            val q = 1.0 - p
            val lnq = ln(q)
            var f = n * lnq
            if (j == 0) {
                return if (f <= KSLMath.smallestExponentialArgument) {
                    0.0
                } else {
                    exp(f)
                }
            }
            val lnp = ln(p)
            val c = lnp - lnq
            var sum = exp(f)
            for (i in 1..j) {
                f = c + ln(n - i + 1.0) - ln(i.toDouble()) + f
                require(f < KSLMath.largestExponentialArgument) { "Term overflow due to input parameters" }
                sum = if (f <= KSLMath.smallestExponentialArgument) {
                    continue
                } else {
                    sum + exp(f)
                }
            }
            return sum
        }

        /** Allows static computation of prob mass function
         * assumes that distribution's range is {0,1, ..., n}
         * Uses the recursive logarithmic algorithm
         *
         * @param j value for which prob is needed
         * @param n num of trials
         * @param p prob of success
         * @return the probability at j
         */
        fun binomialPMF(j: Int, n: Int, p: Double): Double {
            return binomialPMF(j, n, p, true)
        }

        /** Allows static computation of prob mass function
         * assumes that distribution's range is {0,1, ..., n}
         *
         * @param j value for which prob is needed
         * @param n num of successes
         * @param p prob of success
         * @param recursive true indicates that the recursive logarithmic algorithm should be used
         * @return the probability at j
         */
        fun binomialPMF(j: Int, n: Int, p: Double, recursive: Boolean): Double {
            require(n > 0) { "The number of trials must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            if (j < 0) {
                return 0.0
            }
            if (j > n) {
                return 0.0
            }
            if (recursive) {
                return recursivePMF(j, n, p)
            }
            val q = 1.0 - p
            val lnq = ln(q)
            val f = n * lnq
            if (j == 0) {
                return if (f <= KSLMath.smallestExponentialArgument) {
                    0.0
                } else {
                    exp(f)
                }
            }
            val lnp = ln(p)
            if (j == n) {
                val g = n * lnp
                return if (g <= KSLMath.smallestExponentialArgument) {
                    0.0
                } else {
                    exp(g)
                }
            }
            val lnpj = j * lnp
            if (lnpj <= KSLMath.smallestExponentialArgument) {
                return 0.0
            }
            val lnqnj = (n - j) * lnq
            if (lnqnj <= KSLMath.smallestExponentialArgument) {
                return 0.0
            }
            val c = KSLMath.binomialCoefficient(n, j)
            val pj = exp(lnpj)
            val qnj = exp(lnqnj)
            return c * pj * qnj
        }

        /** Allows static computation of the CDF
         * assumes that distribution's range is {0,1, ...,n}
         * Uses the recursive logarithmic algorithm
         *
         * @param j value for which cdf is needed
         * @param n num of trials
         * @param p prob of success
         * @return the cumulative probability at j
         */
        fun binomialCDF(j: Int, n: Int, p: Double): Double {
            return binomialCDF(j, n, p, true)
        }

        /** Allows static computation of the CDF
         * assumes that distribution's range is {0,1, ..., n}
         *
         * @param j value for which cdf is needed
         * @param n num of trials
         * @param p prob of success
         * @param recursive true indicates that the recursive logarithmic algorithm should be used
         * @return the cumulative probability at j
         */
        fun binomialCDF(j: Int, n: Int, p: Double, recursive: Boolean): Double {
            require(n > 0) { "The number of trials must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            if (j < 0) {
                return 0.0
            }
            if (j >= n) {
                return 1.0
            }
            return if (recursive) {
                recursiveCDF(j, n, p)
            } else Beta.regularizedIncompleteBetaFunction(1.0 - p, (n - j).toDouble(), (j + 1).toDouble())
        }

        /** Allows static computation of complementary cdf function
         * assumes that distribution's range is {0,1, ..., n}
         * Uses the recursive logarithmic algorithm
         *
         * @param j value for which ccdf is needed
         * @param n num of trials
         * @param p prob of success
         * @return the complementary CDF at j
         */
        fun binomialCCDF(j: Int, n: Int, p: Double): Double {
            return binomialCCDF(j, n, p, true)
        }

        /** Allows static computation of complementary cdf function
         * assumes that distribution's range is {0,1, ...,n}
         *
         * @param j value for which ccdf is needed
         * @param n num of trials
         * @param p prob of success
         * @param recursive true indicates that the recursive logarithmic algorithm should be used
         * @return the complementary CDF at j
         */
        fun binomialCCDF(j: Int, n: Int, p: Double, recursive: Boolean): Double {
            require(n > 0) { "The number of trials must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            if (j < 0) {
                return 1.0
            }
            return if (j >= n) {
                0.0
            } else 1.0 - binomialCDF(j, n, p, recursive)
        }

        /** Allows static computation of 1st order loss function
         * assumes that distribution's range is {0,1, ..., n}
         * Uses the recursive logarithmic algorithm
         *
         * @param j value for which 1st order loss function is needed
         * @param n num of trial
         * @param p prob of success
         * @return the first order loss function at j
         */
        fun binomialLF1(j: Double, n: Int, p: Double): Double {
            return binomialLF1(j, n, p, true)
        }

        /** Allows static computation of 1st order loss function
         * assumes that distribution's range is {0,1, ...,n}
         *
         * @param j value for which 1st order loss function is needed
         * @param n num of trials
         * @param p prob of success
         * @param recursive true indicates that the recursive logarithmic algorithm should be used
         * @return the first order loss function at j
         */
        fun binomialLF1(j: Double, n: Int, p: Double, recursive: Boolean): Double {
            require(n > 0) { "The number of trial must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            val mu = n * p // the mean
            return if (j < 0) {
                Math.floor(Math.abs(j)) + mu
            } else if (j > 0) {
                mu - sumCCDF_(j, n, p, recursive)
            } else { // j == 0
                mu
            }
        }

        /** Returns the sum of the complementary CDF
         * from 0 up to but not including x
         *
         * @param x the value to evaluate
         * @param n the number of trials
         * @param recursive the flag to use the recursive algorithm
         * @param p the probability of success
         * @return the sum of the complementary CDF
         */
        protected fun sumCCDF_(x: Double, n: Int, p: Double, recursive: Boolean): Double {
            var x = x
            if (x <= 0.0) {
                return 0.0
            }
            if (x > n) {
                x = n.toDouble()
            }
            var c = 0.0
            var i = 0
            while (i < x) {
                c = c + binomialCCDF(i, n, p, recursive)
                i++
            }
            return c
        }

        /** Allows static computation of 2nd order loss function
         * assumes that distribution's range is {0,1, ..., n}
         * Uses the recursive logarithmic algorithm
         *
         * @param j value for which 2nd order loss function is needed
         * @param n num of trials
         * @param p prob of success
         * @return the 2nd order loss function at j
         */
        fun binomialLF2(j: Double, n: Int, p: Double): Double {
            return binomialLF2(j, n, p, true)
        }

        /** Allows static computation of 2nd order loss function
         * assumes that distribution's range is {0,1, ...,n}
         *
         * @param j value for which 2nd order loss function is needed
         * @param n num of trials
         * @param p prob of success
         * @param recursive true indicates that the recursive logarithmic algorithm should be used
         * @return the 2nd order loss function at j
         */
        fun binomialLF2(j: Double, n: Int, p: Double, recursive: Boolean): Double {
            require(n > 0) { "The number of trials must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            val mu = n * p
            val v = n * p * (1.0 - p)
            val sbm = 0.5 * (v + mu * mu - mu) // 1/2 the 2nd binomial moment
            return if (j < 0) {
                var s = 0.0
                var y = 0
                while (y > j) {
                    s = s + binomialLF1(y.toDouble(), n, p, recursive)
                    y--
                }
                s + sbm
            } else if (j > 0) {
                sbm - sumFirstLoss_(j, n, p, recursive)
            } else { // j== 0
                sbm
            }
        }

        /** Sums the first order loss function from
         * 1 up to and including x. x is interpreted
         * as an integer
         *
         * @param x the value to evaluate
         * @param n the number of trials
         * @param p the probability of success
         * @param recursive true if recursive algorithm is to be used
         * @return the sum
         */
        protected fun sumFirstLoss_(x: Double, n: Int, p: Double, recursive: Boolean): Double {
            val k = x.toInt()
            var sum = 0.0
            for (i in 1..k) {
                sum = sum + binomialLF1(i.toDouble(), n, p, recursive)
            }
            return sum
        }

        /** Returns the quantile associated with the supplied probability, x
         * assumes that distribution's range is {0,1, ..., n}
         * Uses the recursive logarithmic algorithm
         *
         * @param x The probability that the quantile is needed for
         * @param n The number of trials
         * @param p The probability of success, must be in range [0,1)
         * @return the quantile associated with the supplied probability
         */
        fun binomialInvCDF(x: Double, n: Int, p: Double): Int {
            return binomialInvCDF(x, n, p, true)
        }

        /** Returns the quantile associated with the supplied probability, x
         * assumes that distribution's range is {0,1, ...,n}
         *
         * @param x The probability that the quantile is needed for
         * @param n The number of trials
         * @param p The probability of success, must be in range [0,1)
         * @param recursive true indicates that the recursive logarithmic algorithm should be used
         * @return the quantile associated with the supplied probability
         */
        fun binomialInvCDF(x: Double, n: Int, p: Double, recursive: Boolean): Int {
            require(n > 0) { "The number of successes must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            require(!(x < 0.0 || x > 1.0)) { "Supplied probability was $x Probability must be [0,1]" }
            if (x <= 0.0) {
                return 0
            }
            if (x >= 1.0) {
                return n
            }

            // get approximate quantile from normal approximation
            // and Cornish-Fisher expansion
            val start = invCDFViaNormalApprox(x, n, p)
            val cdfAtStart = binomialCDF(start, n, p, recursive)

            //System.out.println("start = " + start);
            //System.out.println("cdfAtStart = " + cdfAtStart);
            //System.out.println("p = " + p);
            //System.out.println();
            return if (x >= cdfAtStart) {
                searchUpCDF(x, n, p, start, cdfAtStart, recursive)
            } else {
                searchDownCDF(x, n, p, start, cdfAtStart, recursive)
            }
        }

        /** Approximates the quantile of x using a normal distribution
         *
         * @param x the value to evaluate
         * @param n the number of trials
         * @param p the probability of success
         * @return the approximate inverse CDF value
         */
        fun invCDFViaNormalApprox(x: Double, n: Int, p: Double): Int {
            require(n > 0) { "The number of trial must be > 0" }
            require(!(p <= 0.0 || p >= 1.0)) { "Success Probability must be in (0,1)" }
            require(!(x < 0.0 || x > 1.0)) { "Supplied probability was $x Probability must be [0,1]" }
            if (x <= 0.0) {
                return 0
            }
            if (x >= 1.0) {
                return n
            }
            val q = 1.0 - p
            val mu = n * p
            val sigma = sqrt(mu * q)
            val g = (q - p) / sigma

            /* y := approx.value (Cornish-Fisher expansion) :  */
            val z = Normal.stdNormalInvCDF(x)
            val y = floor(mu + sigma * (z + g * (z * z - 1.0) / 6.0) + 0.5)
            if (y < 0) {
                return 0
            }
            return if (y > n) {
                n
            } else y.toInt()
        }

        protected fun searchUpCDF(
            x: Double, n: Int, p: Double,
            start: Int, cdfAtStart: Double, recursive: Boolean
        ): Int {
            var i = start
            var cdf = cdfAtStart
            while (x > cdf) {
                i++
                cdf = cdf + binomialPMF(i, n, p, recursive)
            }
            return i
        }

        protected fun searchDownCDF(
            x: Double, n: Int, p: Double,
            start: Int, cdfAtStart: Double, recursive: Boolean
        ): Int {
            var i = start
            var cdfi = cdfAtStart
            while (i > 0) {
                val cdfim1 = cdfi - binomialPMF(i, n, p, recursive)
                if (cdfim1 <= x && x < cdfi) {
                    return if (KSLMath.equal(cdfim1, x)) // must handle invCDF(cdf(x) = x)
                    {
                        i - 1
                    } else {
                        i
                    }
                }
                cdfi = cdfim1
                i--
            }
            return i
        }

    }
}