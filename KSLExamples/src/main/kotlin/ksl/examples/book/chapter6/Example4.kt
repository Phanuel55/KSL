package ksl.examples.book.chapter6

import ksl.simulation.Model
import ksl.utilities.random.rvariable.ExponentialRV


/**
 * This example illustrates the running of the DriveThroughPharmacy instance.
 * The model is run for 30 replications, of length 20,000 minutes, with a
 * warmup of 5000.0 minutes. The number of servers can be supplied. In
 * addition, the user can supply the distribution associated with the time
 * between arrivals and the service time distribution.
 */
fun main() {
    val model = Model("Drive Through Pharmacy")
    model.numberOfReplications = 2
    model.lengthOfReplication = 20.0
    model.lengthOfReplicationWarmUp = 5.0
    // add DriveThroughPharmacy to the main model
    val dtp = DriveThroughPharmacy(model, 1)
    dtp.setTimeBtwArrivalRandomSource(ExponentialRV(6.0, 1))
    dtp.setServiceTimeRandomSource(ExponentialRV(3.0, 2))
    model.simulate()
//    sim.simulationReporter.printHalfWidthSummaryReport()
    //TODO results do not match JSL model, twresponse slightly off, counter very off
}