package ksl.examples.book.chapter5

import ksl.observers.AcrossReplicationHalfWidthChecker
import ksl.observers.ReplicationDataCollector
import ksl.observers.ResponseTrace
import ksl.simulation.Model
import ksl.utilities.io.dbutil.KSLDatabaseObserver

fun main() {
    val model = Model("Pallet Processing Ex 2")
    model.numberOfReplications = 10000
    model.experimentName = "Two Workers"
    // add the model element to the main model
    val palletWorkCenter = PalletWorkCenter(model)

    val hwc = AcrossReplicationHalfWidthChecker(palletWorkCenter.totalProcessingTime)
    hwc.desiredHalfWidth = 5.0

    // simulate the model
    model.simulate()

    // demonstrate that reports can have specified confidence level
    val sr = model.simulationReporter

    sr.printHalfWidthSummaryReport()

}