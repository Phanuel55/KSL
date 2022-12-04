package ksl.examples.book.chapter3

import ksl.utilities.math.FunctionIfc
import ksl.utilities.mcintegration.MC1DIntegration
import ksl.utilities.random.rvariable.UniformRV

fun main() {
    class SinFunc : FunctionIfc {
        override fun f(x: Double): Double {
            return Math.PI * Math.sin(x)
        }
    }

    val f = SinFunc()
    val mc = MC1DIntegration(f, UniformRV(0.0, Math.PI))
    println()
    mc.runSimulation()
    println(mc)
}