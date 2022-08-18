package ksl.utilities.distributions

import ksl.utilities.Interval

/**
 *  Used to represent the set of possible values for continuous distributions
 *  The interval may be infinite
 */
fun interface DomainIfc {

    fun domain(): Interval
}