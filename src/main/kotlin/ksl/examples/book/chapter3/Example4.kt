package ksl.examples.book.chapter3

import ksl.utilities.random.rvariable.DEmpiricalRV

/**
 * This example illustrates how to use the classes within the rvariable package.
 * Specifically, a discrete empirical random variable is
 * created and values are obtained via the value property. A discrete
 * empirical random variable requires a set of values and a CDF over the
 * values.
 */
fun main() {
    // values is the set of possible values
    val values = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
    // cdf is the cumulative distribution function over the values
    val cdf = doubleArrayOf(1.0 / 6.0, 3.0 / 6.0, 5.0 / 6.0, 1.0)
    //create a discrete empirical random variable
    val n1 = DEmpiricalRV(values, cdf)
    println(n1)
    System.out.printf("%3s %15s %n", "n", "Values")
    for (i in 1..5) {
        System.out.printf("%3d %15f %n", i, n1.value)
    }
}