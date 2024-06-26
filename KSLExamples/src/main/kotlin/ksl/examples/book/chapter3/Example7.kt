/*
 *     The KSL provides a discrete-event simulation library for the Kotlin programming language.
 *     Copyright (C) 2022  Manuel D. Rossetti, rossetti@uark.edu
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

package ksl.examples.book.chapter3

import ksl.utilities.io.StatisticReporter
import ksl.utilities.random.rvariable.NormalRV
import ksl.utilities.statistic.Statistic

fun main() {
    val rv = NormalRV(10.0, 4.0)
    val estimateX = Statistic("Estimated X")
    val estOfProb = Statistic("Pr(X>8)")
    val r = StatisticReporter(mutableListOf(estOfProb, estimateX))
    val n = 20 // sample size
    for (i in 1..n) {
        val x = rv.value
        estimateX.collect(x)
        estOfProb.collect(x > 8)
    }
    println(r.halfWidthSummaryReport())
}
