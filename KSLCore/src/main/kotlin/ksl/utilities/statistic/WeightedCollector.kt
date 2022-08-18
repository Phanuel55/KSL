package ksl.utilities.statistic

import ksl.utilities.Identity
import ksl.utilities.IdentityIfc
import ksl.utilities.observers.DoublePairEmitter
import ksl.utilities.observers.DoublePairEmitterIfc
import ksl.utilities.observers.Observable

abstract class WeightedCollector(name: String? = null) : WeightedCollectorIfc, IdentityIfc by Identity(name),
    Observable<Pair<Double, Double>>(),
    DoublePairEmitterIfc by DoublePairEmitter() {

    var lastValue = Double.NaN
        protected set

    var lastWeight = Double.NaN
        protected set

    override val weight: Double
        get() = lastWeight

    override var value: Double
        get() = lastValue
        set(value) {
            collect(value, 1.0)
        }

    override fun collect(obs: Double, weight: Double) {
        lastValue = obs
        lastWeight = weight
        notifyObservers(Pair(lastValue, lastWeight))
        emitter.emit(Pair(lastValue, lastWeight))
    }

    override fun reset() {
        lastValue = Double.NaN
        lastWeight = Double.NaN
    }
}