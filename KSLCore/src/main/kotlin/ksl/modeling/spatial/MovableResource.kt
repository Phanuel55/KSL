package ksl.modeling.spatial

import ksl.modeling.entity.Resource
import ksl.modeling.variable.RandomSourceCIfc
import ksl.modeling.variable.RandomVariable
import ksl.simulation.ModelElement
import ksl.utilities.GetValueIfc
import ksl.utilities.observers.ObservableComponent
import ksl.utilities.observers.ObserverIfc
import ksl.utilities.random.RandomIfc


/**
 * A movable resource is a resource that resides within a spatial model and thus can be moved.
 * @param parent the parent model element
 * @param initLocation the initial starting location of the resource within the spatial model
 * @param defaultVelocity the default velocity for movement within the spatial model
 * @param name the name of the resource
 */
class MovableResource(
    parent: ModelElement,
    initLocation: LocationIfc,
    defaultVelocity: RandomIfc,
    name: String? = null,
) : Resource(parent, name, 1), SpatialElementIfc, VelocityIfc {
    protected val mySpatialElement = SpatialElement(this, initLocation, name)
    protected val myVelocity = RandomVariable(this, defaultVelocity)
    val velocityRV: RandomSourceCIfc
        get() = myVelocity
    override val velocity: GetValueIfc
        get() = myVelocity

    override var isMoving: Boolean
        get() = mySpatialElement.isMoving
        set(value) {
            mySpatialElement.isMoving = value
        }
    override val isTracked: Boolean
        get() = mySpatialElement.isTracked
    override val spatialID: Int
        get() = mySpatialElement.spatialID
    override val spatialName: String
        get() = mySpatialElement.spatialName
    override val status: SpatialModel.Status
        get() = mySpatialElement.status
    override var initialLocation: LocationIfc
        get() = mySpatialElement.initialLocation
        set(value) {
            mySpatialElement.initialLocation = value
        }
    override var currentLocation: LocationIfc
        get() = mySpatialElement.currentLocation
        set(value) {
            mySpatialElement.currentLocation = value
        }
    override val previousLocation: LocationIfc
        get() = mySpatialElement.previousLocation
    override val modelElement: ModelElement
        get() = mySpatialElement.modelElement
    override val observableComponent: ObservableComponent<SpatialElementIfc>
        get() = mySpatialElement.observableComponent

    override fun initialize() {
        super.initialize()
        initializeSpatialElement()
    }

    override fun initializeSpatialElement() {
        mySpatialElement.initializeSpatialElement()
    }

    override fun attachObserver(observer: ObserverIfc<SpatialElementIfc>) {
        mySpatialElement.attachObserver(observer)
    }

    override fun detachObserver(observer: ObserverIfc<SpatialElementIfc>) {
        mySpatialElement.detachObserver(observer)
    }

    override fun isAttached(observer: ObserverIfc<SpatialElementIfc>): Boolean {
        return mySpatialElement.isAttached(observer)
    }

    override fun detachAllObservers() {
        mySpatialElement.detachAllObservers()
    }

    override fun countObservers(): Int {
        return mySpatialElement.countObservers()
    }

}