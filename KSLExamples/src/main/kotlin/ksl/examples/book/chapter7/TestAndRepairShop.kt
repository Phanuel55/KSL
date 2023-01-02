package ksl.examples.book.chapter7

import ksl.modeling.elements.EventGeneratorCIfc
import ksl.modeling.elements.REmpiricalList
import ksl.modeling.entity.KSLProcess
import ksl.modeling.entity.ProcessModel
import ksl.modeling.entity.ResourceWithQ
import ksl.modeling.variable.*
import ksl.simulation.ModelElement
import ksl.utilities.random.RandomIfc
import ksl.utilities.random.rvariable.ExponentialRV
import ksl.utilities.random.rvariable.LognormalRV
import ksl.utilities.random.rvariable.TriangularRV
import ksl.utilities.random.rvariable.UniformRV

class TestAndRepairShop(parent: ModelElement, name: String? = null) : ProcessModel(parent, name) {

    // define the random variables
    private val tba = ExponentialRV(20.0)
    private val t11 = RandomVariable(this, LognormalRV(20.0, 4.1))
    private val t21 = RandomVariable(this, LognormalRV(12.0, 4.2))
    private val t31 = RandomVariable(this, LognormalRV(18.0, 4.3))
    private val t41 = RandomVariable(this, LognormalRV(16.0, 4.0))
    private val t12 = RandomVariable(this, LognormalRV(12.0, 4.0))
    private val t22 = RandomVariable(this, LognormalRV(15.0, 4.0))
    private val t13 = RandomVariable(this, LognormalRV(18.0, 4.2))
    private val t23 = RandomVariable(this, LognormalRV(14.0, 4.4))
    private val t33 = RandomVariable(this, LognormalRV(12.0, 4.3))
    private val t14 = RandomVariable(this, LognormalRV(24.0, 4.0))
    private val t24 = RandomVariable(this, LognormalRV(30.0, 4.0))
    private val r1 = RandomVariable(this, TriangularRV(30.0, 60.0, 80.0))
    private val r2 = RandomVariable(this, TriangularRV(45.0, 55.0, 70.0))
    private val r3 = RandomVariable(this, TriangularRV(30.0, 40.0, 60.0))
    private val r4 = RandomVariable(this, TriangularRV(35.0, 65.0, 75.0))
    private val diagnosticTime = RandomVariable(this, ExponentialRV(30.0))
    private val moveTime = RandomVariable(this, UniformRV(2.0, 4.0))

    // define the resources
    private val myDiagnostics: ResourceWithQ = ResourceWithQ(this, "Diagnostics", capacity = 2)
    private val myTest1: ResourceWithQ = ResourceWithQ(this, "Test1")
    private val myTest2: ResourceWithQ = ResourceWithQ(this, "Test2")
    private val myTest3: ResourceWithQ = ResourceWithQ(this, "Test3")
    private val myRepair: ResourceWithQ = ResourceWithQ(this, "Repair", capacity = 3)

    // define steps to represent a plan
    inner class TestPlanStep(val resource: ResourceWithQ, val processTime: RandomIfc)

    // make all the plans
    private val testPlan1 = listOf(
        TestPlanStep(myTest2, t11), TestPlanStep(myTest3, t21),
        TestPlanStep(myTest2, t31), TestPlanStep(myTest1, t41), TestPlanStep(myRepair, r1)
    )
    private val testPlan2 = listOf(
        TestPlanStep(myTest3, t12),
        TestPlanStep(myTest1, t22), TestPlanStep(myRepair, r2)
    )
    private val testPlan3 = listOf(
        TestPlanStep(myTest1, t13), TestPlanStep(myTest3, t23),
        TestPlanStep(myTest1, t33), TestPlanStep(myRepair, r3)
    )
    private val testPlan4 = listOf(
        TestPlanStep(myTest2, t14),
        TestPlanStep(myTest3, t24), TestPlanStep(myRepair, r4)
    )

    // set up the sequences and the random selection of the plan
    private val sequences = listOf(testPlan1, testPlan2, testPlan3, testPlan4)
    private val planCDf = doubleArrayOf(0.25, 0.375, 0.7, 1.0)
    private val planList = REmpiricalList<List<TestPlanStep>>(this, sequences, planCDf)

    private val myArrivalGenerator = EntityGenerator(::Part, tba, tba)
    val generator: EventGeneratorCIfc
        get() = myArrivalGenerator

    // define the responses
    private val wip: TWResponse = TWResponse(this, "${this.name}:NumInSystem")
    val numInSystem: TWResponseCIfc
        get() = wip
    private val timeInSystem: Response = Response(this, "${this.name}:TimeInSystem")
    val systemTime: ResponseCIfc
        get() = timeInSystem
    private val myContractLimit: IndicatorResponse = IndicatorResponse({ x -> x <= 480.0 }, timeInSystem, "ProbWithinLimit")
    val probWithinLimit: ResponseCIfc
        get() = myContractLimit

    // define the process
    private inner class Part : Entity() {
        val testAndRepairProcess: KSLProcess = process {
            wip.increment()
            timeStamp = time
            //every part goes to diagnostics
            use(myDiagnostics, delayDuration = diagnosticTime)
            // determine the test plan
            val plan: List<TestPlanStep> = planList.element
            // get the iterator
            val itr = plan.iterator()
            // iterate through the plan
            while (itr.hasNext()) {
                val tp = itr.next()
                use(tp.resource, delayDuration = tp.processTime)
                if (tp.resource != myRepair) {
                    delay(moveTime)
                }
            }
            timeInSystem.value = time - timeStamp
            wip.decrement()
        }
    }
}