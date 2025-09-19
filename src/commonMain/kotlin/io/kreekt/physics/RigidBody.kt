/**
 * RigidBody and CollisionObject implementations for physics simulation
 * Provides foundation for rigid body dynamics and collision detection
 */
package io.kreekt.physics

import io.kreekt.core.math.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * Rigid body implementation with complete physics simulation support
 * Supports all major physics features: forces, impulses, constraints, collision response
 */
class RigidBodyImpl(
    override val id: String,
    initialShape: CollisionShape,
    initialMass: Float = 1.0f,
    initialTransform: Matrix4 = Matrix4.identity()
) : RigidBody {

    // CollisionObject properties
    private val _transform = MutableStateFlow(initialTransform)
    override var transform: Matrix4
        get() = _transform.value
        set(value) { _transform.value = value }

    override var collisionShape: CollisionShape = initialShape
    override var collisionGroups: Int = 1
    override var collisionMask: Int = -1 // Collide with everything by default
    override var userData: Any? = null
    override var contactProcessingThreshold: Float = 1e30f
    override var collisionFlags: Int = 0
    override var isTrigger: Boolean = false

    // RigidBody specific properties
    private val _mass = MutableStateFlow(initialMass)
    override var mass: Float
        get() = _mass.value
        set(value) {
            require(value >= 0f) { "Mass cannot be negative" }
            _mass.value = value
            updateInertia()
        }

    override var density: Float = 1.0f
        set(value) {
            require(value > 0f) { "Density must be positive" }
            field = value
            // Auto-calculate mass from volume and density
            mass = collisionShape.getVolume() * value
        }

    override var restitution: Float = 0.0f
        set(value) {
            require(value in 0f..1f) { "Restitution must be between 0 and 1" }
            field = value
        }

    override var friction: Float = 0.5f
        set(value) {
            require(value >= 0f) { "Friction cannot be negative" }
            field = value
        }

    override var rollingFriction: Float = 0.0f
        set(value) {
            require(value >= 0f) { "Rolling friction cannot be negative" }
            field = value
        }

    override var linearDamping: Float = 0.04f
        set(value) {
            require(value in 0f..1f) { "Linear damping must be between 0 and 1" }
            field = value
        }

    override var angularDamping: Float = 0.05f
        set(value) {
            require(value in 0f..1f) { "Angular damping must be between 0 and 1" }
            field = value
        }

    // Motion state
    private val _linearVelocity = MutableStateFlow(Vector3.ZERO)
    override var linearVelocity: Vector3
        get() = _linearVelocity.value
        set(value) { _linearVelocity.value = value }

    private val _angularVelocity = MutableStateFlow(Vector3.ZERO)
    override var angularVelocity: Vector3
        get() = _angularVelocity.value
        set(value) { _angularVelocity.value = value }

    override var linearFactor: Vector3 = Vector3.ONE
    override var angularFactor: Vector3 = Vector3.ONE

    // Body type and state
    private val _bodyType = MutableStateFlow(RigidBodyType.DYNAMIC)
    override var bodyType: RigidBodyType
        get() = _bodyType.value
        set(value) {
            _bodyType.value = value
            when (value) {
                RigidBodyType.STATIC -> {
                    mass = 0f
                    linearVelocity = Vector3.ZERO
                    angularVelocity = Vector3.ZERO
                }
                RigidBodyType.KINEMATIC -> {
                    mass = 0f
                    // Kinematic bodies can have velocity but are not affected by forces
                }
                RigidBodyType.DYNAMIC -> {
                    if (mass <= 0f) mass = 1f
                }
            }
        }

    private val _activationState = MutableStateFlow(ActivationState.ACTIVE)
    override var activationState: ActivationState
        get() = _activationState.value
        set(value) { _activationState.value = value }

    override var sleepThreshold: Float = 0.8f
    override var ccdMotionThreshold: Float = 0f
    override var ccdSweptSphereRadius: Float = 0f

    // Internal physics state
    private var _inertia = Matrix3.IDENTITY
    private var _inverseInertia = Matrix3.IDENTITY
    private val _totalForce = MutableStateFlow(Vector3.ZERO)
    private val _totalTorque = MutableStateFlow(Vector3.ZERO)
    private var _sleepTimer = 0f
    private var _lastPosition = Vector3.ZERO
    private var _lastRotation = Quaternion.IDENTITY

    init {
        updateInertia()
    }

    // Force and impulse application
    override fun applyForce(force: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            if (!isActive() || bodyType == RigidBodyType.STATIC) {
                return PhysicsResult.Success(Unit)
            }

            // Apply central force
            _totalForce.value = _totalForce.value + (force * linearFactor)

            // Apply torque from offset
            if (relativePosition != Vector3.ZERO) {
                val torque = relativePosition.cross(force) * angularFactor
                _totalTorque.value = _totalTorque.value + torque
            }

            // Wake up the body
            activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationFailed("Failed to apply force", e))
        }
    }

    override fun applyImpulse(impulse: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            if (!isActive() || bodyType == RigidBodyType.STATIC) {
                return PhysicsResult.Success(Unit)
            }

            // Apply linear impulse
            val deltaVelocity = (impulse * linearFactor) / mass
            linearVelocity = linearVelocity + deltaVelocity

            // Apply angular impulse from offset
            if (relativePosition != Vector3.ZERO) {
                val angularImpulse = relativePosition.cross(impulse) * angularFactor
                val deltaAngularVelocity = _inverseInertia * angularImpulse
                angularVelocity = angularVelocity + deltaAngularVelocity
            }

            activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationFailed("Failed to apply impulse", e))
        }
    }

    override fun applyTorque(torque: Vector3): PhysicsResult<Unit> {
        return try {
            if (!isActive() || bodyType == RigidBodyType.STATIC) {
                return PhysicsResult.Success(Unit)
            }

            _totalTorque.value = _totalTorque.value + (torque * angularFactor)
            activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationFailed("Failed to apply torque", e))
        }
    }

    override fun applyTorqueImpulse(torque: Vector3): PhysicsResult<Unit> {
        return try {
            if (!isActive() || bodyType == RigidBodyType.STATIC) {
                return PhysicsResult.Success(Unit)
            }

            val deltaAngularVelocity = _inverseInertia * (torque * angularFactor)
            angularVelocity = angularVelocity + deltaAngularVelocity
            activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationFailed("Failed to apply torque impulse", e))
        }
    }

    override fun applyCentralForce(force: Vector3): PhysicsResult<Unit> {
        return applyForce(force, Vector3.ZERO)
    }

    override fun applyCentralImpulse(impulse: Vector3): PhysicsResult<Unit> {
        return applyImpulse(impulse, Vector3.ZERO)
    }

    // State queries
    override fun isActive(): Boolean {
        return activationState == ActivationState.ACTIVE ||
               activationState == ActivationState.WANTS_DEACTIVATION
    }

    override fun isKinematic(): Boolean = bodyType == RigidBodyType.KINEMATIC
    override fun isStatic(): Boolean = bodyType == RigidBodyType.STATIC

    override fun getInertia(): Matrix3 = _inertia
    override fun getInverseInertia(): Matrix3 = _inverseInertia
    override fun getTotalForce(): Vector3 = _totalForce.value
    override fun getTotalTorque(): Vector3 = _totalTorque.value

    // Transformation
    override fun setTransform(position: Vector3, rotation: Quaternion) {
        transform = Matrix4.fromTranslationRotation(position, rotation)
    }

    override fun getWorldTransform(): Matrix4 = transform

    override fun getCenterOfMassTransform(): Matrix4 {
        // For now, assume center of mass is at the origin of the body
        return transform
    }

    // CollisionObject interface implementation
    override fun setCollisionShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            collisionShape = shape
            updateInertia()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.ShapeCreationFailed("Failed to set collision shape", e))
        }
    }

    override fun getCollisionShape(): CollisionShape = collisionShape

    override fun setWorldTransform(transform: Matrix4) {
        this.transform = transform
    }

    override fun translate(offset: Vector3) {
        val currentPos = transform.getTranslation()
        val newTransform = Matrix4.fromTranslationRotationScale(
            currentPos + offset,
            transform.getRotation(),
            transform.getScale()
        )
        transform = newTransform
    }

    override fun rotate(rotation: Quaternion) {
        val currentRot = transform.getRotation()
        val newTransform = Matrix4.fromTranslationRotationScale(
            transform.getTranslation(),
            currentRot * rotation,
            transform.getScale()
        )
        transform = newTransform
    }

    // Internal physics integration
    internal fun integrateVelocity(deltaTime: Float, gravity: Vector3) {
        if (!isActive() || bodyType != RigidBodyType.DYNAMIC) return

        // Apply gravity
        if (mass > 0f) {
            _totalForce.value = _totalForce.value + (gravity * mass)
        }

        // Integrate linear velocity
        if (mass > 0f) {
            val acceleration = _totalForce.value / mass
            linearVelocity = (linearVelocity + acceleration * deltaTime) * (1f - linearDamping)
        }

        // Integrate angular velocity
        val angularAcceleration = _inverseInertia * _totalTorque.value
        angularVelocity = (angularVelocity + angularAcceleration * deltaTime) * (1f - angularDamping)

        // Clear forces
        _totalForce.value = Vector3.ZERO
        _totalTorque.value = Vector3.ZERO
    }

    internal fun integratePosition(deltaTime: Float) {
        if (!isActive()) return

        val currentPos = transform.getTranslation()
        val currentRot = transform.getRotation()

        // Integrate position
        val newPos = currentPos + linearVelocity * deltaTime

        // Integrate rotation using quaternion angular velocity
        val angularVelMagnitude = angularVelocity.length()
        val newRot = if (angularVelMagnitude > 0f) {
            val axis = angularVelocity / angularVelMagnitude
            val angle = angularVelMagnitude * deltaTime
            val deltaRot = Quaternion.fromAxisAngle(axis, angle)
            currentRot * deltaRot
        } else {
            currentRot
        }

        // Update transform
        transform = Matrix4.fromTranslationRotationScale(
            newPos,
            newRot,
            transform.getScale()
        )

        // Check for sleep
        checkSleep(deltaTime, newPos, newRot)
    }

    private fun updateInertia() {
        if (mass <= 0f || bodyType == RigidBodyType.STATIC) {
            _inertia = Matrix3.ZERO
            _inverseInertia = Matrix3.ZERO
        } else {
            _inertia = collisionShape.calculateInertia(mass)
            _inverseInertia = _inertia.inverse()
        }
    }

    private fun activate() {
        if (activationState == ActivationState.DEACTIVATED) {
            activationState = ActivationState.ACTIVE
        }
        _sleepTimer = 0f
    }

    private fun checkSleep(deltaTime: Float, newPos: Vector3, newRot: Quaternion) {
        val linearThreshold = sleepThreshold * 0.1f
        val angularThreshold = sleepThreshold * 0.1f

        val linearSpeed = linearVelocity.length()
        val angularSpeed = angularVelocity.length()

        if (linearSpeed < linearThreshold && angularSpeed < angularThreshold) {
            _sleepTimer += deltaTime
            if (_sleepTimer > 2f) { // Sleep after 2 seconds of low activity
                activationState = ActivationState.DEACTIVATED
                linearVelocity = Vector3.ZERO
                angularVelocity = Vector3.ZERO
            }
        } else {
            _sleepTimer = 0f
            if (activationState == ActivationState.DEACTIVATED) {
                activationState = ActivationState.ACTIVE
            }
        }

        _lastPosition = newPos
        _lastRotation = newRot
    }
}

/**
 * Base collision object implementation
 * Provides foundation for all physics objects (rigid bodies, triggers, character controllers)
 */
open class CollisionObjectImpl(
    override val id: String,
    initialShape: CollisionShape,
    initialTransform: Matrix4 = Matrix4.identity()
) : CollisionObject {

    private val _transform = MutableStateFlow(initialTransform)
    override var transform: Matrix4
        get() = _transform.value
        set(value) { _transform.value = value }

    override var collisionShape: CollisionShape = initialShape
    override var collisionGroups: Int = 1
    override var collisionMask: Int = -1
    override var userData: Any? = null
    override var contactProcessingThreshold: Float = 1e30f
    override var collisionFlags: Int = 0
    override var isTrigger: Boolean = false

    override fun setCollisionShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            collisionShape = shape
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.ShapeCreationFailed("Failed to set collision shape", e))
        }
    }

    override fun getCollisionShape(): CollisionShape = collisionShape

    override fun setWorldTransform(transform: Matrix4) {
        this.transform = transform
    }

    override fun getWorldTransform(): Matrix4 = transform

    override fun translate(offset: Vector3) {
        val currentPos = transform.getTranslation()
        val newTransform = Matrix4.fromTranslationRotationScale(
            currentPos + offset,
            transform.getRotation(),
            transform.getScale()
        )
        transform = newTransform
    }

    override fun rotate(rotation: Quaternion) {
        val currentRot = transform.getRotation()
        val newTransform = Matrix4.fromTranslationRotationScale(
            transform.getTranslation(),
            currentRot * rotation,
            transform.getScale()
        )
        transform = newTransform
    }
}

/**
 * Physics material properties for combining material behaviors
 */
class PhysicsMaterialImpl(
    val friction: Float = 0.5f,
    val restitution: Float = 0.0f,
    val rollingFriction: Float = 0.0f,
    val spinningFriction: Float = 0.0f,
    val frictionCombineMode: CombineMode = CombineMode.AVERAGE,
    val restitutionCombineMode: CombineMode = CombineMode.AVERAGE
) {

    /**
     * Combine two physics materials using the specified combine mode
     */
    fun combineWith(other: PhysicsMaterialImpl): PhysicsMaterialImpl {
        return PhysicsMaterialImpl(
            friction = combineMaterialProperty(friction, other.friction, frictionCombineMode, other.frictionCombineMode),
            restitution = combineMaterialProperty(restitution, other.restitution, restitutionCombineMode, other.restitutionCombineMode),
            rollingFriction = combineMaterialProperty(rollingFriction, other.rollingFriction, frictionCombineMode, other.frictionCombineMode),
            spinningFriction = combineMaterialProperty(spinningFriction, other.spinningFriction, frictionCombineMode, other.frictionCombineMode)
        )
    }

    private fun combineMaterialProperty(value1: Float, value2: Float, mode1: CombineMode, mode2: CombineMode): Float {
        // Use the most restrictive combine mode
        val mode = if (mode1.ordinal > mode2.ordinal) mode1 else mode2

        return when (mode) {
            CombineMode.AVERAGE -> (value1 + value2) * 0.5f
            CombineMode.MINIMUM -> minOf(value1, value2)
            CombineMode.MAXIMUM -> maxOf(value1, value2)
            CombineMode.MULTIPLY -> value1 * value2
        }
    }
}

/**
 * Body type management utilities
 */
object RigidBodyTypeUtils {

    /**
     * Check if body type allows motion from forces
     */
    fun allowsForces(type: RigidBodyType): Boolean {
        return type == RigidBodyType.DYNAMIC
    }

    /**
     * Check if body type allows kinematic motion
     */
    fun allowsKinematicMotion(type: RigidBodyType): Boolean {
        return type == RigidBodyType.KINEMATIC || type == RigidBodyType.DYNAMIC
    }

    /**
     * Check if body type participates in collision response
     */
    fun participatesInCollisionResponse(type: RigidBodyType): Boolean {
        return type != RigidBodyType.STATIC
    }

    /**
     * Get default mass for body type
     */
    fun getDefaultMass(type: RigidBodyType): Float {
        return when (type) {
            RigidBodyType.STATIC, RigidBodyType.KINEMATIC -> 0f
            RigidBodyType.DYNAMIC -> 1f
        }
    }
}

/**
 * Activation state management utilities
 */
object ActivationStateUtils {

    /**
     * Check if activation state allows physics simulation
     */
    fun allowsSimulation(state: ActivationState): Boolean {
        return state != ActivationState.DISABLE_SIMULATION
    }

    /**
     * Check if activation state allows deactivation/sleeping
     */
    fun allowsDeactivation(state: ActivationState): Boolean {
        return state != ActivationState.DISABLE_DEACTIVATION
    }

    /**
     * Get next activation state for sleeping logic
     */
    fun getNextSleepState(current: ActivationState, shouldSleep: Boolean): ActivationState {
        return when (current) {
            ActivationState.ACTIVE -> {
                if (shouldSleep) ActivationState.WANTS_DEACTIVATION else ActivationState.ACTIVE
            }
            ActivationState.WANTS_DEACTIVATION -> {
                if (shouldSleep) ActivationState.DEACTIVATED else ActivationState.ACTIVE
            }
            ActivationState.DEACTIVATED -> {
                if (!shouldSleep) ActivationState.ACTIVE else ActivationState.DEACTIVATED
            }
            ActivationState.DISABLE_DEACTIVATION -> ActivationState.ACTIVE
            ActivationState.DISABLE_SIMULATION -> current
        }
    }
}