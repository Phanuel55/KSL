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
package ksl.modeling.variable

import ksl.utilities.statistic.StatisticIfc

interface AcrossReplicationStatisticIfc {
    /** Returns a StatisticAccessorIfc for the across replication statistics
     * that have been collected on this Counter
     *
     * @return an accessor to get statistics
     */
    val acrossReplicationStatistic: StatisticIfc

    /** A convenience method to get the across replication average from
     * the underlying statistic
     *
     * @return the across replication average
     */
    val acrossReplicationAverage: Double
        get() = acrossReplicationStatistic.average
}