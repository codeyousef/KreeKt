/**
 * Rapier Physics Engine Integration for Web Platform
 * Provides high-performance physics simulation using Rapier's WASM bindings
 */
package io.kreekt.physics

import io.kreekt.core.math.*
import kotlinx.coroutines.*
import kotlin.js.Promise

/**
 * External Rapier WASM module declarations
 */
@JsModule("@dimforge/rapier3d")
@JsNonModule
external object RAPIER {
    fun init(): Promise<Unit>

    class World(gravity: dynamic) {
        var gravity: dynamic
        var timestep: Float
        var maxVelocityIterations: Int
        var maxPositionIterations: Int
        var maxCcdSubsteps: Int

        fun step()
        fun createRigidBody(desc: RigidBodyDesc): RigidBody
        fun createCollider(desc: ColliderDesc, body: RigidBody?): Collider
        fun removeRigidBody(body: RigidBody)
        fun removeCollider(collider: Collider)
        fun castRay(ray: Ray, maxToi: Float, solid: Boolean, groups: Int): dynamic
        fun castShape(
            shapePos: dynamic,
            shapeRot: dynamic,
            shapeVel: dynamic,
            shape: dynamic,
            maxToi: Float,
            groups: Int
        ): dynamic
        fun intersectionsWithShape(
            shapePos: dynamic,
            shapeRot: dynamic,
            shape: dynamic,
            callback: (Collider) -> Boolean,
            groups: Int
        )
        fun contactPair(collider1: Collider, collider2: Collider): ContactManifold?
        fun intersectionPair(collider1: Collider, collider2: Collider): Boolean
        fun createImpulseJoint(desc: dynamic, body1: RigidBody, body2: RigidBody?): ImpulseJoint
        fun removeImpulseJoint(joint: ImpulseJoint, wakeUp: Boolean)
        fun forEachActiveRigidBody(callback: (RigidBody) -> Unit)
        fun forEachCollider(callback: (Collider) -> Unit)
    }

    class RigidBodyDesc {
        companion object {
            fun dynamic(): RigidBodyDesc
            fun kinematicPositionBased(): RigidBodyDesc
            fun kinematicVelocityBased(): RigidBodyDesc
            fun fixed(): RigidBodyDesc
        }

        fun setTranslation(x: Float, y: Float, z: Float): RigidBodyDesc
        fun setRotation(quaternion: dynamic): RigidBodyDesc
        fun setLinvel(x: Float, y: Float, z: Float): RigidBodyDesc
        fun setAngvel(x: Float, y: Float, z: Float): RigidBodyDesc
        fun setGravityScale(scale: Float): RigidBodyDesc
        fun setCanSleep(canSleep: Boolean): RigidBodyDesc
        fun setCcdEnabled(enabled: Boolean): RigidBodyDesc
        fun setLinearDamping(damping: Float): RigidBodyDesc
        fun setAngularDamping(damping: Float): RigidBodyDesc
        fun setAdditionalMass(mass: Float): RigidBodyDesc
        fun setAdditionalMassProperties(
            mass: Float,
            centerOfMass: dynamic,
            principalAngularInertia: dynamic,
            angularInertiaFrameRotation: dynamic
        ): RigidBodyDesc
        fun restrictRotations(x: Boolean, y: Boolean, z: Boolean): RigidBodyDesc
        fun lockTranslations(): RigidBodyDesc
        fun lockRotations(): RigidBodyDesc
    }

    class RigidBody {
        fun translation(): dynamic
        fun rotation(): dynamic
        fun linvel(): dynamic
        fun angvel(): dynamic
        fun mass(): Float
        fun effectiveMass(): Float
        fun centerOfMass(): dynamic
        fun setTranslation(vector: dynamic, wakeUp: Boolean)
        fun setRotation(quaternion: dynamic, wakeUp: Boolean)
        fun setLinvel(vector: dynamic, wakeUp: Boolean)
        fun setAngvel(vector: dynamic, wakeUp: Boolean)
        fun applyImpulse(impulse: dynamic, wakeUp: Boolean)
        fun applyTorqueImpulse(torque: dynamic, wakeUp: Boolean)
        fun applyImpulseAtPoint(impulse: dynamic, point: dynamic, wakeUp: Boolean)
        fun applyForce(force: dynamic, wakeUp: Boolean)
        fun applyTorque(torque: dynamic, wakeUp: Boolean)
        fun applyForceAtPoint(force: dynamic, point: dynamic, wakeUp: Boolean)
        fun resetForces(wakeUp: Boolean)
        fun resetTorques(wakeUp: Boolean)
        fun addForce(force: dynamic, wakeUp: Boolean)
        fun addTorque(torque: dynamic, wakeUp: Boolean)
        fun addForceAtPoint(force: dynamic, point: dynamic, wakeUp: Boolean)
        fun wakeUp()
        fun sleep()
        fun isSleeping(): Boolean
        fun isKinematic(): Boolean
        fun isFixed(): Boolean
        fun isDynamic(): Boolean
        fun isEnabled(): Boolean
        fun setEnabled(enabled: Boolean)
        fun setBodyType(type: RigidBodyType, wakeUp: Boolean)
        fun setGravityScale(scale: Float, wakeUp: Boolean)
        fun gravityScale(): Float
        fun userData(): Any?
        fun handle(): Int
    }

    class ColliderDesc {
        companion object {
            fun cuboid(hx: Float, hy: Float, hz: Float): ColliderDesc
            fun ball(radius: Float): ColliderDesc
            fun capsule(halfHeight: Float, radius: Float): ColliderDesc
            fun cylinder(halfHeight: Float, radius: Float): ColliderDesc
            fun cone(halfHeight: Float, radius: Float): ColliderDesc
            fun convexHull(vertices: FloatArray): ColliderDesc?
            fun convexMesh(vertices: FloatArray, indices: IntArray): ColliderDesc?
            fun trimesh(vertices: FloatArray, indices: IntArray): ColliderDesc?
            fun heightfield(nrows: Int, ncols: Int, heights: FloatArray, scale: dynamic): ColliderDesc
        }

        fun setTranslation(x: Float, y: Float, z: Float): ColliderDesc
        fun setRotation(quaternion: dynamic): ColliderDesc
        fun setMass(mass: Float): ColliderDesc
        fun setDensity(density: Float): ColliderDesc
        fun setFriction(friction: Float): ColliderDesc
        fun setFrictionCombineRule(rule: CoefficientCombineRule): ColliderDesc
        fun setRestitution(restitution: Float): ColliderDesc
        fun setRestitutionCombineRule(rule: CoefficientCombineRule): ColliderDesc
        fun setSensor(isSensor: Boolean): ColliderDesc
        fun setCollisionGroups(groups: Int): ColliderDesc
        fun setSolverGroups(groups: Int): ColliderDesc
        fun setActiveCollisionTypes(types: Int): ColliderDesc
        fun setActiveEvents(events: Int): ColliderDesc
        fun setActiveHooks(hooks: Int): ColliderDesc
    }

    class Collider {
        fun shape(): ColliderShape
        fun setShape(shape: ColliderShape)
        fun setTranslation(vector: dynamic)
        fun setRotation(quaternion: dynamic)
        fun translation(): dynamic
        fun rotation(): dynamic
        fun setSensor(isSensor: Boolean)
        fun isSensor(): Boolean
        fun setFriction(friction: Float)
        fun friction(): Float
        fun setFrictionCombineRule(rule: CoefficientCombineRule)
        fun frictionCombineRule(): CoefficientCombineRule
        fun setRestitution(restitution: Float)
        fun restitution(): Float
        fun setRestitutionCombineRule(rule: CoefficientCombineRule)
        fun restitutionCombineRule(): CoefficientCombineRule
        fun setCollisionGroups(groups: Int)
        fun collisionGroups(): Int
        fun setSolverGroups(groups: Int)
        fun solverGroups(): Int
        fun setDensity(density: Float)
        fun density(): Float
        fun setMass(mass: Float)
        fun mass(): Float
        fun volume(): Float
        fun parent(): RigidBody?
        fun setEnabled(enabled: Boolean)
        fun isEnabled(): Boolean
        fun userData(): Any?
        fun handle(): Int
    }

    class ColliderShape

    class Ray(origin: dynamic, dir: dynamic)

    class ContactManifold {
        fun normal(): dynamic
        fun localNormal1(): dynamic
        fun localNormal2(): dynamic
        fun numContacts(): Int
        fun contactLocalPoint1(i: Int): dynamic
        fun contactLocalPoint2(i: Int): dynamic
        fun contactDist(i: Int): Float
        fun contactImpulse(i: Int): Float
        fun contactTangentImpulse(i: Int): dynamic
    }

    class ImpulseJoint {
        fun bodyHandle1(): Int
        fun bodyHandle2(): Int?
        fun setContacts(enabled: Boolean)
        fun contactsEnabled(): Boolean
    }

    enum class RigidBodyType {
        Dynamic,
        Fixed,
        KinematicPositionBased,
        KinematicVelocityBased
    }

    enum class CoefficientCombineRule {
        Average,
        Min,
        Multiply,
        Max
    }

    enum class ActiveEvents {
        COLLISION_EVENTS,
        CONTACT_FORCE_EVENTS
    }

    enum class ActiveCollisionTypes {
        DYNAMIC_DYNAMIC,
        DYNAMIC_KINEMATIC,
        DYNAMIC_FIXED,
        KINEMATIC_KINEMATIC,
        KINEMATIC_FIXED,
        FIXED_FIXED
    }

    class Vector3(x: Float, y: Float, z: Float) {
        var x: Float
        var y: Float
        var z: Float
    }

    class Quaternion(x: Float, y: Float, z: Float, w: Float) {
        var x: Float
        var y: Float
        var z: Float
        var w: Float
    }

    class EventQueue {
        fun drainCollisionEvents(handler: (Int, Int, Boolean) -> Unit)
        fun drainContactForceEvents(handler: (ContactForceEvent) -> Unit)
        fun clear()
    }

    class ContactForceEvent {
        fun collider1(): Int
        fun collider2(): Int
        fun totalForce(): dynamic
        fun totalForceMagnitude(): Float
        fun maxForceDirection(): dynamic
        fun maxForceMagnitude(): Float
    }
}

/**
 * ContactInfo implementation for Rapier
 */
private data class ContactInfoImpl(
    override val objectA: CollisionObject,
    override val objectB: CollisionObject,
    override val worldPosA: Vector3,
    override val worldPosB: Vector3,
    override val normalWorldOnB: Vector3,
    override val distance: Float,
    override val impulse: Float,
    override val friction: Float,
    override val restitution: Float
) : ContactInfo

/**
 * Rapier-based implementation of PhysicsWorld
 */
class RapierPhysicsWorld(
    initialGravity: Vector3 = Vector3(0f, -9.81f, 0f)
) : PhysicsWorld {
    private lateinit var world: RAPIER.World
    private val rigidBodies = mutableMapOf<String, RapierRigidBody>()
    private val colliders = mutableMapOf<String, RAPIER.Collider>()
    private val colliderUserData = mutableMapOf<Int, Any>()
    private val constraints = mutableMapOf<String, RapierConstraint>()
    private var collisionCallback: CollisionCallback? = null
    private val eventQueue = RAPIER.EventQueue()

    private var initialized = false

    override var gravity: Vector3 = initialGravity
        set(value) {
            field = value
            if (initialized) {
                world.gravity = toRapierVector3(value)
            }
        }

    override var timeStep = 1f / 60f
    override var maxSubSteps = 1
    override var solverIterations = 4
    override var broadphase = BroadphaseType.SAP

    init {
        GlobalScope.launch {
            initializeRapier()
        }
    }

    private suspend fun initializeRapier() {
        RAPIER.init().await()

        world = RAPIER.World(toRapierVector3(gravity))
        world.timestep = timeStep
        world.maxVelocityIterations = solverIterations
        world.maxPositionIterations = solverIterations * 2
        world.maxCcdSubsteps = maxSubSteps

        initialized = true
    }

    private fun ensureInitialized() {
        if (!initialized) {
            // Note: Initialization is async, should be handled differently in production
            // For now, assuming it's already initialized or will fail gracefully
            console.warn("RapierPhysicsEngine not yet initialized")
        }
    }

    override fun addRigidBody(body: RigidBody): PhysicsResult<Unit> {
        ensureInitialized()

        return try {
            val rapierBody = body as? RapierRigidBody
                ?: return PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Body must be created through RapierPhysicsEngine"))

            rigidBodies[body.id] = rapierBody
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add rigid body"))
        }
    }

    override fun removeRigidBody(body: RigidBody): PhysicsResult<Unit> {
        ensureInitialized()

        return try {
            val rapierBody = rigidBodies.remove(body.id) as? RapierRigidBody
            rapierBody?.let {
                world.removeRigidBody(it.rapierBody)
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove rigid body"))
        }
    }

    override fun getRigidBodies(): List<RigidBody> = rigidBodies.values.toList()

    override fun getRigidBody(id: String): RigidBody? = rigidBodies[id]

    override fun addConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        ensureInitialized()

        return try {
            val rapierConstraint = constraint as? RapierConstraint
                ?: return PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Constraint must be created through RapierPhysicsEngine"))

            constraints[constraint.id] = rapierConstraint
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add constraint"))
        }
    }

    override fun removeConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        ensureInitialized()

        return try {
            val rapierConstraint = constraints.remove(constraint.id) as? RapierConstraint
            rapierConstraint?.let {
                world.removeImpulseJoint(it.joint, true)
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove constraint"))
        }
    }

    override fun getConstraints(): List<PhysicsConstraint> = constraints.values.toList()

    override fun addCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        ensureInitialized()

        return try {
            // For Rapier, collision objects without rigid bodies are static colliders
            val desc = createColliderDesc(obj.collisionShape)
            desc.setTranslation(obj.transform.m03, obj.transform.m13, obj.transform.m23)

            val collider = world.createCollider(desc, null)
            // Note: userData assignment needs proper API - storing in a map for now
            colliderUserData[collider.handle()] = obj
            collider.setCollisionGroups(obj.collisionGroups)
            colliders[obj.id] = collider

            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add collision object"))
        }
    }

    override fun removeCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        ensureInitialized()

        return try {
            val collider = colliders.remove(obj.id)
            collider?.let {
                world.removeCollider(it)
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove collision object"))
        }
    }

    override fun setCollisionCallback(callback: CollisionCallback) {
        collisionCallback = callback
    }

    override fun step(deltaTime: Float): PhysicsResult<Unit> {
        ensureInitialized()

        return try {
            // Update timestep
            world.timestep = deltaTime

            // Step the simulation
            world.step()

            // Process collision events
            processCollisionEvents()

            // Update rigid body transforms
            world.forEachActiveRigidBody { rapierBody ->
                val body = rapierBody.userData() as? RapierRigidBody
                body?.updateFromRapier()
            }

            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Simulation step failed"))
        }
    }

    private fun processCollisionEvents() {
        eventQueue.drainCollisionEvents { handle1, handle2, started ->
            // Find colliders by handle
            val collider1 = colliders.values.find { it.handle() == handle1 }
            val collider2 = colliders.values.find { it.handle() == handle2 }

            if (collider1 != null && collider2 != null) {
                val obj1 = collider1.userData() as? CollisionObject
                val obj2 = collider2.userData() as? CollisionObject

                if (obj1 != null && obj2 != null) {
                    val contact = ContactInfoImpl(
                        objectA = obj1,
                        objectB = obj2,
                        worldPosA = extractContactPoints(collider1, collider2).firstOrNull() ?: Vector3.ZERO,
                        worldPosB = extractContactPoints(collider1, collider2).firstOrNull() ?: Vector3.ZERO,
                        normalWorldOnB = extractContactNormal(collider1, collider2),
                        distance = 0f,
                        impulse = 0f,
                        friction = 0.5f,
                        restitution = 0f
                    )

                    when {
                        started -> collisionCallback?.onContactAdded(contact)
                        else -> collisionCallback?.onContactDestroyed(contact)
                    }
                }
            }
        }
    }

    private fun extractContactPoints(c1: RAPIER.Collider, c2: RAPIER.Collider): List<Vector3> {
        val manifold = world.contactPair(c1, c2) ?: return emptyList()
        val points = mutableListOf<Vector3>()

        for (i in 0 until manifold.numContacts()) {
            val localPoint = manifold.contactLocalPoint1(i)
            points.add(fromRapierVector3(localPoint))
        }

        return points
    }

    private fun extractContactNormal(c1: RAPIER.Collider, c2: RAPIER.Collider): Vector3 {
        val manifold = world.contactPair(c1, c2) ?: return Vector3.ZERO
        return fromRapierVector3(manifold.normal())
    }

    override fun pause() {
        // Rapier doesn't have explicit pause, we just don't step
    }

    override fun resume() {
        // Rapier doesn't have explicit resume
    }

    override fun reset() {
        ensureInitialized()

        // Remove all bodies and constraints
        rigidBodies.clear()
        constraints.clear()
        colliders.clear()

        // Recreate world
        world = RAPIER.World(toRapierVector3(gravity))
        world.timestep = timeStep
        world.maxVelocityIterations = solverIterations
        world.maxPositionIterations = solverIterations * 2
    }

    override fun raycast(from: Vector3, to: Vector3, groups: Int): RaycastResult? {
        ensureInitialized()

        val direction = to.subtract(from).normalize()
        val maxDistance = from.distanceTo(to)

        val ray = RAPIER.Ray(
            toRapierVector3(from),
            toRapierVector3(direction)
        )

        val hit = world.castRay(ray, maxDistance, true, groups)

        return if (hit != null) {
            val distance = hit.asDynamic().toi as Float
            val hitPoint = from + (direction * distance)

            RaycastResult(
                hasHit = true,
                hitObject = null, // Need to get collider from hit result
                hitPoint = hitPoint,
                hitNormal = Vector3.UNIT_Y, // Placeholder
                distance = distance
            )
        } else {
            RaycastResult(hasHit = false)
        }
    }

    override fun sphereCast(center: Vector3, radius: Float, groups: Int): List<CollisionObject> {
        ensureInitialized()

        val results = mutableListOf<CollisionObject>()
        val sphereShape = RAPIER.ColliderDesc.ball(radius)

        world.intersectionsWithShape(
            toRapierVector3(center),
            RAPIER.Quaternion(0f, 0f, 0f, 1f),
            sphereShape,
            { collider ->
                val obj = collider.userData() as? CollisionObject
                obj?.let { results.add(it) }
                true // Continue iteration
            },
            groups
        )

        return results
    }

    override fun boxCast(
        center: Vector3,
        halfExtents: Vector3,
        rotation: Quaternion,
        groups: Int
    ): List<CollisionObject> {
        ensureInitialized()

        val results = mutableListOf<CollisionObject>()
        val boxShape = RAPIER.ColliderDesc.cuboid(halfExtents.x, halfExtents.y, halfExtents.z)

        world.intersectionsWithShape(
            toRapierVector3(center),
            toRapierQuaternion(rotation),
            boxShape,
            { collider ->
                val obj = collider.userData() as? CollisionObject
                obj?.let { results.add(it) }
                true // Continue iteration
            },
            groups
        )

        return results
    }

    override fun overlaps(
        shape: CollisionShape,
        transform: Matrix4,
        groups: Int
    ): List<CollisionObject> {
        ensureInitialized()

        val results = mutableListOf<CollisionObject>()
        val position = Vector3(transform.m03, transform.m13, transform.m23)
        val rotation = transform.extractRotation()

        val rapierShape = createColliderDesc(shape)

        world.intersectionsWithShape(
            toRapierVector3(position),
            toRapierQuaternion(rotation),
            rapierShape,
            { collider ->
                val obj = collider.userData() as? CollisionObject
                obj?.let { results.add(it) }
                true // Continue iteration
            },
            groups
        )

        return results
    }

    private fun createColliderDesc(shape: CollisionShape): RAPIER.ColliderDesc {
        return when (shape) {
            is BoxShape -> RAPIER.ColliderDesc.cuboid(
                shape.halfExtents.x,
                shape.halfExtents.y,
                shape.halfExtents.z
            )
            is SphereShape -> RAPIER.ColliderDesc.ball(shape.radius)
            is CapsuleShape -> RAPIER.ColliderDesc.capsule(
                shape.height / 2f,
                shape.radius
            )
            is CylinderShape -> RAPIER.ColliderDesc.cylinder(
                shape.halfExtents.y,
                shape.halfExtents.x
            )
            is ConeShape -> RAPIER.ColliderDesc.cone(
                shape.height / 2f,
                shape.radius
            )
            is ConvexHullShape -> RAPIER.ColliderDesc.convexHull(shape.vertices)
                ?: RAPIER.ColliderDesc.ball(1f) // Fallback if convex hull generation fails
            is TriangleMeshShape -> RAPIER.ColliderDesc.trimesh(shape.vertices, shape.indices)
                ?: RAPIER.ColliderDesc.ball(1f) // Fallback
            is HeightfieldShape -> RAPIER.ColliderDesc.heightfield(
                shape.height,
                shape.width,
                shape.heightData,
                RAPIER.Vector3(1f, 1f, 1f)
            )
            else -> RAPIER.ColliderDesc.ball(1f) // Default fallback
        }
    }
}

/**
 * Rapier-based implementation of RigidBody
 */
class RapierRigidBody(
    override val id: String,
    internal val rapierBody: RAPIER.RigidBody,
    private val rapierCollider: RAPIER.Collider,
    initialShape: CollisionShape,
    private val world: RAPIER.World
) : RigidBody {

    override var transform: Matrix4 = Matrix4.identity()
        set(value) {
            field = value
            val position = Vector3(value.m03, value.m13, value.m23)
            val rotation = value.extractRotation()
            rapierBody.setTranslation(toRapierVector3(position), true)
            rapierBody.setRotation(toRapierQuaternion(rotation), true)
        }

    override var collisionShape: CollisionShape = initialShape
    override var collisionGroups: Int = -1
    override var collisionMask: Int = -1
    override var userData: Any? = null
    override var contactProcessingThreshold: Float = 0.01f
    override var collisionFlags: Int = 0
    override var isTrigger: Boolean = false
        set(value) {
            field = value
            rapierCollider.setSensor(value)
        }

    override var mass: Float = 1f
        set(value) {
            field = value
            rapierCollider.setMass(value)
        }

    override var density: Float = 1f
        set(value) {
            field = value
            rapierCollider.setDensity(value)
        }

    override var restitution: Float = 0f
        set(value) {
            field = value
            rapierCollider.setRestitution(value)
        }

    override var friction: Float = 0.5f
        set(value) {
            field = value
            rapierCollider.setFriction(value)
        }

    override var rollingFriction: Float = 0f

    override var linearDamping: Float = 0f
        set(value) {
            field = value
            // Rapier sets damping on body creation, need to recreate
        }

    override var angularDamping: Float = 0f
        set(value) {
            field = value
            // Rapier sets damping on body creation, need to recreate
        }

    override var linearVelocity: Vector3
        get() = fromRapierVector3(rapierBody.linvel())
        set(value) {
            rapierBody.setLinvel(toRapierVector3(value), true)
        }

    override var angularVelocity: Vector3
        get() = fromRapierVector3(rapierBody.angvel())
        set(value) {
            rapierBody.setAngvel(toRapierVector3(value), true)
        }

    override var linearFactor: Vector3 = Vector3.ONE
    override var angularFactor: Vector3 = Vector3.ONE

    override var bodyType: RigidBodyType = RigidBodyType.DYNAMIC
        set(value) {
            field = value
            val rapierType = when (value) {
                RigidBodyType.DYNAMIC -> RAPIER.RigidBodyType.Dynamic
                RigidBodyType.STATIC -> RAPIER.RigidBodyType.Fixed
                RigidBodyType.KINEMATIC -> RAPIER.RigidBodyType.KinematicPositionBased
            }
            rapierBody.setBodyType(rapierType, true)
        }

    override var activationState: ActivationState = ActivationState.ACTIVE
    override var sleepThreshold: Float = 0.1f
    override var ccdMotionThreshold: Float = 0f
    override var ccdSweptSphereRadius: Float = 0f

    override fun setCollisionShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            collisionShape = shape
            // In Rapier, we need to recreate the collider with new shape
            // This is a limitation of the current implementation
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to set collision shape"))
        }
    }

    fun getCollisionShape(): CollisionShape = collisionShape

    override fun setWorldTransform(transform: Matrix4) {
        this.transform = transform
    }

    override fun getWorldTransform(): Matrix4 = transform

    override fun translate(offset: Vector3) {
        val currentPos = fromRapierVector3(rapierBody.translation())
        val newPos = currentPos.add(offset)
        rapierBody.setTranslation(toRapierVector3(newPos), true)
        updateTransformFromRapier()
    }

    override fun rotate(rotation: Quaternion) {
        val currentRot = fromRapierQuaternion(rapierBody.rotation())
        val newRot = currentRot.multiply(rotation)
        rapierBody.setRotation(toRapierQuaternion(newRot), true)
        updateTransformFromRapier()
    }

    override fun applyForce(force: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            if (relativePosition == Vector3.ZERO) {
                rapierBody.addForce(toRapierVector3(force), true)
            } else {
                val worldPos = transform.transformPoint(relativePosition)
                rapierBody.addForceAtPoint(toRapierVector3(force), toRapierVector3(worldPos), true)
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Failed to apply force"))
        }
    }

    override fun applyImpulse(impulse: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            if (relativePosition == Vector3.ZERO) {
                rapierBody.applyImpulse(toRapierVector3(impulse), true)
            } else {
                val worldPos = transform.transformPoint(relativePosition)
                rapierBody.applyImpulseAtPoint(toRapierVector3(impulse), toRapierVector3(worldPos), true)
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Failed to apply impulse"))
        }
    }

    override fun applyTorque(torque: Vector3): PhysicsResult<Unit> {
        return try {
            rapierBody.addTorque(toRapierVector3(torque), true)
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Failed to apply torque"))
        }
    }

    override fun applyTorqueImpulse(torque: Vector3): PhysicsResult<Unit> {
        return try {
            rapierBody.applyTorqueImpulse(toRapierVector3(torque), true)
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Failed to apply torque impulse"))
        }
    }

    override fun applyCentralForce(force: Vector3): PhysicsResult<Unit> {
        return applyForce(force, Vector3.ZERO)
    }

    override fun applyCentralImpulse(impulse: Vector3): PhysicsResult<Unit> {
        return applyImpulse(impulse, Vector3.ZERO)
    }

    override fun isActive(): Boolean = !rapierBody.isSleeping()
    override fun isKinematic(): Boolean = rapierBody.isKinematic()
    override fun isStatic(): Boolean = rapierBody.isFixed()

    override fun getInertia(): Matrix3 {
        // Rapier doesn't expose inertia tensor directly
        // We approximate based on mass and shape
        val m = rapierBody.mass()
        return collisionShape.calculateInertia(m)
    }

    override fun getInverseInertia(): Matrix3 = getInertia().inverse()

    override fun getTotalForce(): Vector3 {
        // Rapier doesn't expose accumulated forces
        return Vector3.ZERO
    }

    override fun getTotalTorque(): Vector3 {
        // Rapier doesn't expose accumulated torques
        return Vector3.ZERO
    }

    override fun setTransform(position: Vector3, rotation: Quaternion) {
        rapierBody.setTranslation(toRapierVector3(position), true)
        rapierBody.setRotation(toRapierQuaternion(rotation), true)
        updateTransformFromRapier()
    }

    override fun getCenterOfMassTransform(): Matrix4 {
        val com = fromRapierVector3(rapierBody.centerOfMass())
        return Matrix4.translation(com.x, com.y, com.z)
    }

    internal fun updateFromRapier() {
        updateTransformFromRapier()
    }

    private fun updateTransformFromRapier() {
        val position = fromRapierVector3(rapierBody.translation())
        val rotation = fromRapierQuaternion(rapierBody.rotation())
        transform = Matrix4()
        transform.compose(position, rotation, Vector3.ONE)
    }
}

/**
 * Base class for Rapier constraints
 */
abstract class RapierConstraint(
    override val id: String,
    override val bodyA: RigidBody,
    override val bodyB: RigidBody?,
    internal val joint: RAPIER.ImpulseJoint
) : PhysicsConstraint {

    override var enabled: Boolean = true
        set(value) {
            field = value
            joint.setContacts(value)
        }

    override var breakingThreshold: Float = Float.MAX_VALUE

    override fun setParam(param: ConstraintParam, value: Float, axis: Int) {
        // Implementation depends on constraint type
    }

    override fun getParam(param: ConstraintParam, axis: Int): Float {
        // Implementation depends on constraint type
        return 0f
    }

    override fun getAppliedImpulse(): Float {
        // Rapier doesn't expose applied impulse directly
        return 0f
    }

    override fun isEnabled(): Boolean = enabled

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun getInfo(info: ConstraintInfo) {
        // Fill constraint info
    }
}

/**
 * Rapier physics engine implementation
 */
class RapierPhysicsEngine : PhysicsEngine {
    override val name = "Rapier"
    override val version = "0.20.0"

    private var initialized = false

    init {
        GlobalScope.launch {
            RAPIER.init().await()
            initialized = true
        }
    }

    private fun ensureInitialized() {
        if (!initialized) {
            // Note: Initialization is async, should be handled differently in production
            // For now, assuming it's already initialized or will fail gracefully
            console.warn("RapierPhysicsEngine not yet initialized")
        }
    }

    override fun createWorld(gravity: Vector3): PhysicsWorld {
        ensureInitialized()
        return RapierPhysicsWorld(gravity)
    }

    override fun destroyWorld(world: PhysicsWorld): PhysicsResult<Unit> {
        return try {
            (world as? RapierPhysicsWorld)?.reset()
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to destroy world"))
        }
    }

    override fun createBoxShape(halfExtents: Vector3): BoxShape {
        return RapierBoxShape(halfExtents)
    }

    override fun createSphereShape(radius: Float): SphereShape {
        return RapierSphereShape(radius)
    }

    override fun createCapsuleShape(radius: Float, height: Float): CapsuleShape {
        return RapierCapsuleShape(radius, height)
    }

    override fun createCylinderShape(halfExtents: Vector3): CylinderShape {
        return RapierCylinderShape(halfExtents)
    }

    override fun createConeShape(radius: Float, height: Float): ConeShape {
        return RapierConeShape(radius, height)
    }

    override fun createConvexHullShape(vertices: FloatArray): ConvexHullShape {
        return RapierConvexHullShape(vertices)
    }

    override fun createTriangleMeshShape(vertices: FloatArray, indices: IntArray): TriangleMeshShape {
        return RapierTriangleMeshShape(vertices, indices)
    }

    override fun createHeightfieldShape(width: Int, height: Int, heightData: FloatArray): HeightfieldShape {
        return RapierHeightfieldShape(width, height, heightData)
    }

    override fun createCompoundShape(): CompoundShape {
        return RapierCompoundShape()
    }

    override fun createRigidBody(shape: CollisionShape, mass: Float, transform: Matrix4): RigidBody {
        ensureInitialized()

        // Create body description
        val desc = if (mass > 0f) {
            RAPIER.RigidBodyDesc.dynamic()
        } else {
            RAPIER.RigidBodyDesc.fixed()
        }

        val position = Vector3(transform.m03, transform.m13, transform.m23)
        val rotation = transform.extractRotation()

        desc.setTranslation(position.x, position.y, position.z)
        desc.setRotation(toRapierQuaternion(rotation))

        // Create world temporarily (this is a limitation of the current design)
        val tempWorld = RAPIER.World(RAPIER.Vector3(0f, -9.81f, 0f))
        val rapierBody = tempWorld.createRigidBody(desc)

        // Create collider
        val colliderDesc = createColliderDesc(shape)
        colliderDesc.setMass(mass)
        val rapierCollider = tempWorld.createCollider(colliderDesc, rapierBody)

        return RapierRigidBody(
            id = "rb_${kotlin.js.Date.now().toLong()}",
            rapierBody = rapierBody,
            rapierCollider = rapierCollider,
            initialShape = shape,
            world = tempWorld
        )
    }

    override fun createCharacterController(shape: CollisionShape, stepHeight: Float): CharacterController {
        // Rapier doesn't have built-in character controller
        // We need to implement it using a kinematic body
        TODO("Character controller implementation for Rapier")
    }

    override fun createPointToPointConstraint(
        bodyA: RigidBody,
        bodyB: RigidBody?,
        pivotA: Vector3,
        pivotB: Vector3
    ): PointToPointConstraint {
        TODO("Point-to-point constraint implementation for Rapier")
    }

    override fun createHingeConstraint(
        bodyA: RigidBody,
        bodyB: RigidBody?,
        pivotA: Vector3,
        pivotB: Vector3,
        axisA: Vector3,
        axisB: Vector3
    ): HingeConstraint {
        TODO("Hinge constraint implementation for Rapier")
    }

    override fun createSliderConstraint(
        bodyA: RigidBody,
        bodyB: RigidBody?,
        frameA: Matrix4,
        frameB: Matrix4
    ): SliderConstraint {
        TODO("Slider constraint implementation for Rapier")
    }

    private fun createColliderDesc(shape: CollisionShape): RAPIER.ColliderDesc {
        return when (shape) {
            is BoxShape -> RAPIER.ColliderDesc.cuboid(
                shape.halfExtents.x,
                shape.halfExtents.y,
                shape.halfExtents.z
            )
            is SphereShape -> RAPIER.ColliderDesc.ball(shape.radius)
            is CapsuleShape -> RAPIER.ColliderDesc.capsule(shape.height / 2f, shape.radius)
            is CylinderShape -> RAPIER.ColliderDesc.cylinder(
                shape.halfExtents.y,
                shape.halfExtents.x
            )
            is ConeShape -> RAPIER.ColliderDesc.cone(shape.height / 2f, shape.radius)
            is ConvexHullShape -> RAPIER.ColliderDesc.convexHull(shape.vertices)
                ?: RAPIER.ColliderDesc.ball(1f)
            is TriangleMeshShape -> RAPIER.ColliderDesc.trimesh(shape.vertices, shape.indices)
                ?: RAPIER.ColliderDesc.ball(1f)
            is HeightfieldShape -> RAPIER.ColliderDesc.heightfield(
                shape.height,
                shape.width,
                shape.heightData,
                RAPIER.Vector3(1f, 1f, 1f)
            )
            else -> RAPIER.ColliderDesc.ball(1f)
        }
    }
}

// Shape implementations
private class RapierBoxShape(override val halfExtents: Vector3) : BoxShape {
    override val shapeType = ShapeType.BOX
    override val margin = 0.04f
    override val localScaling = Vector3.ONE
    override val boundingBox = Box3(
        halfExtents.negate(),
        halfExtents
    )

    override fun getHalfExtentsWithMargin() = halfExtents.add(Vector3(margin, margin, margin))
    override fun getHalfExtentsWithoutMargin() = halfExtents

    override fun calculateInertia(mass: Float): Matrix3 {
        val x = halfExtents.x * 2f
        val y = halfExtents.y * 2f
        val z = halfExtents.z * 2f
        val factor = mass / 12f
        return Matrix3(floatArrayOf(
            factor * (y * y + z * z), 0f, 0f,
            0f, factor * (x * x + z * z), 0f,
            0f, 0f, factor * (x * x + y * y)
        ))
    }

    override fun getVolume() = 8f * halfExtents.x * halfExtents.y * halfExtents.z
    override fun getSurfaceArea() = 8f * (halfExtents.x * halfExtents.y +
        halfExtents.y * halfExtents.z + halfExtents.x * halfExtents.z)
    override fun isConvex() = true
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) = Vector3(
        if (direction.x > 0) halfExtents.x else -halfExtents.x,
        if (direction.y > 0) halfExtents.y else -halfExtents.y,
        if (direction.z > 0) halfExtents.z else -halfExtents.z
    ) + (direction.normalize() * margin)

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) = Vector3(
        if (direction.x > 0) halfExtents.x else -halfExtents.x,
        if (direction.y > 0) halfExtents.y else -halfExtents.y,
        if (direction.z > 0) halfExtents.z else -halfExtents.z
    )

    override fun calculateLocalInertia(mass: Float) = Vector3(
        mass * (halfExtents.y * halfExtents.y + halfExtents.z * halfExtents.z) / 3f,
        mass * (halfExtents.x * halfExtents.x + halfExtents.z * halfExtents.z) / 3f,
        mass * (halfExtents.x * halfExtents.x + halfExtents.y * halfExtents.y) / 3f
    )

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierBoxShape(halfExtents)
}

// Similar implementations for other shapes...
private class RapierSphereShape(override val radius: Float) : SphereShape {
    override val shapeType = ShapeType.SPHERE
    override val margin = 0f
    override val localScaling = Vector3.ONE
    override val boundingBox = Box3(
        Vector3(-radius, -radius, -radius),
        Vector3(radius, radius, radius)
    )

    override fun getRadiusWithMargin() = radius + margin
    override fun getRadiusWithoutMargin() = radius

    override fun calculateInertia(mass: Float): Matrix3 {
        val inertia = 0.4f * mass * radius * radius
        return Matrix3(floatArrayOf(inertia, 0f, 0f,
            0f, inertia, 0f,
            0f, 0f, inertia))
    }

    override fun getVolume() = (4f / 3f) * kotlin.math.PI.toFloat() * radius * radius * radius
    override fun getSurfaceArea() = 4f * kotlin.math.PI.toFloat() * radius * radius
    override fun isConvex() = true
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) =
        direction.normalize() * (radius + margin)

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) =
        direction.normalize() * radius

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val inertia = 0.4f * mass * radius * radius
        return Vector3(inertia, inertia, inertia)
    }

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierSphereShape(radius)
}

private class RapierCapsuleShape(
    override val radius: Float,
    override val height: Float
) : CapsuleShape {
    override val shapeType = ShapeType.CAPSULE
    override val margin = 0f
    override val localScaling = Vector3.ONE
    override val upAxis = 1 // Y-axis
    override val boundingBox = Box3(
        Vector3(-radius, -height/2f - radius, -radius),
        Vector3(radius, height/2f + radius, radius)
    )

    override fun getHalfHeight() = height / 2f

    override fun calculateInertia(mass: Float): Matrix3 {
        // Simplified inertia calculation for capsule
        val cylinderMass = mass * height / (height + 2f * radius)
        val hemisphereMass = mass - cylinderMass

        val cylinderInertiaX = cylinderMass * (3f * radius * radius + height * height) / 12f
        val cylinderInertiaY = cylinderMass * radius * radius / 2f

        val hemisphereInertiaX = hemisphereMass * (2f * radius * radius / 5f + height * height / 4f)
        val hemisphereInertiaY = hemisphereMass * 2f * radius * radius / 5f

        return Matrix3(floatArrayOf(cylinderInertiaX + hemisphereInertiaX, 0f, 0f,
            0f, cylinderInertiaY + hemisphereInertiaY, 0f,
            0f, 0f, cylinderInertiaX + hemisphereInertiaX))
    }

    override fun getVolume() = kotlin.math.PI.toFloat() * radius * radius *
        (height + 4f * radius / 3f)

    override fun getSurfaceArea() = 2f * kotlin.math.PI.toFloat() * radius *
        (2f * radius + height)

    override fun isConvex() = true
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        val dir = direction.normalize()
        if (kotlin.math.abs(dir.y) > 0.7071f) {
            return Vector3(0f, if (dir.y > 0) height/2f + radius else -height/2f - radius, 0f)
        }
        val horizontal = Vector3(dir.x, 0f, dir.z).normalize()
        val y = if (dir.y > 0) height/2f else -height/2f
        return (horizontal * radius) + Vector3(0f, y, 0f)
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) =
        localGetSupportingVertex(direction)

    override fun calculateLocalInertia(mass: Float) =
        calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierCapsuleShape(radius, height)
}

// Implement remaining shape classes similarly...
private class RapierCylinderShape(override val halfExtents: Vector3) : CylinderShape {
    override val shapeType = ShapeType.CYLINDER
    override val margin = 0.04f
    override val localScaling = Vector3.ONE
    override val upAxis = 1
    override val boundingBox = Box3(halfExtents.negate(), halfExtents)

    override fun getRadius() = halfExtents.x
    override fun getHalfHeight() = halfExtents.y

    override fun calculateInertia(mass: Float): Matrix3 {
        val radius = halfExtents.x
        val height = halfExtents.y * 2f
        val lateral = mass * (3f * radius * radius + height * height) / 12f
        val vertical = mass * radius * radius / 2f
        return Matrix3(floatArrayOf(lateral, 0f, 0f,
            0f, vertical, 0f,
            0f, 0f, lateral))
    }

    override fun getVolume() = kotlin.math.PI.toFloat() * halfExtents.x * halfExtents.x * halfExtents.y * 2f
    override fun getSurfaceArea(): Float {
        val radius = halfExtents.x
        val height = halfExtents.y * 2f
        return 2f * kotlin.math.PI.toFloat() * radius * (radius + height)
    }

    override fun isConvex() = true
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) = Vector3.ZERO
    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) = Vector3.ZERO
    override fun calculateLocalInertia(mass: Float) = calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierCylinderShape(halfExtents)
}

private class RapierConeShape(
    override val radius: Float,
    override val height: Float
) : ConeShape {
    override val shapeType = ShapeType.CONE
    override val margin = 0.04f
    override val localScaling = Vector3.ONE
    override val upAxis = 1
    override val boundingBox = Box3(
        Vector3(-radius, -height/2f, -radius),
        Vector3(radius, height/2f, radius)
    )

    override fun getConeRadius() = radius
    override fun getConeHeight() = height

    override fun calculateInertia(mass: Float): Matrix3 {
        val lateral = mass * (3f * radius * radius / 20f + 3f * height * height / 80f)
        val vertical = mass * 3f * radius * radius / 10f
        return Matrix3(floatArrayOf(lateral, 0f, 0f,
            0f, vertical, 0f,
            0f, 0f, lateral))
    }

    override fun getVolume() = kotlin.math.PI.toFloat() * radius * radius * height / 3f
    override fun getSurfaceArea(): Float {
        val slant = kotlin.math.sqrt((radius * radius + height * height).toDouble()).toFloat()
        return kotlin.math.PI.toFloat() * radius * (radius + slant)
    }

    override fun isConvex() = true
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) = Vector3.ZERO
    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) = Vector3.ZERO
    override fun calculateLocalInertia(mass: Float) = calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierConeShape(radius, height)
}

private class RapierConvexHullShape(
    override val vertices: FloatArray
) : ConvexHullShape {
    override val shapeType = ShapeType.CONVEX_HULL
    override val margin = 0.04f
    override val localScaling = Vector3.ONE
    override val numVertices = vertices.size / 3
    override val boundingBox: Box3

    init {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        for (i in vertices.indices step 3) {
            minX = minOf(minX, vertices[i])
            minY = minOf(minY, vertices[i + 1])
            minZ = minOf(minZ, vertices[i + 2])
            maxX = maxOf(maxX, vertices[i])
            maxY = maxOf(maxY, vertices[i + 1])
            maxZ = maxOf(maxZ, vertices[i + 2])
        }

        boundingBox = Box3(
            Vector3(minX, minY, minZ),
            Vector3(maxX, maxY, maxZ)
        )
    }

    override fun addPoint(point: Vector3, recalculateLocalAABB: Boolean) {
        // Not implemented for immutable shape
    }

    override fun getScaledPoint(index: Int): Vector3 {
        val i = index * 3
        return Vector3(vertices[i], vertices[i + 1], vertices[i + 2])
    }

    override fun getUnscaledPoints(): List<Vector3> {
        val points = mutableListOf<Vector3>()
        for (i in vertices.indices step 3) {
            points.add(Vector3(vertices[i], vertices[i + 1], vertices[i + 2]))
        }
        return points
    }

    override fun optimizeConvexHull() {
        // Already optimized by Rapier
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        // Simplified inertia for convex hull
        val size = boundingBox.getSize()
        return Matrix3(floatArrayOf(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        ))
    }

    override fun getVolume(): Float {
        // Approximate volume using bounding box
        val size = boundingBox.getSize()
        return size.x * size.y * size.z * 0.5f // Rough approximation
    }

    override fun getSurfaceArea(): Float {
        // Approximate surface area
        val size = boundingBox.getSize()
        return 2f * (size.x * size.y + size.y * size.z + size.x * size.z)
    }

    override fun isConvex() = true
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        var maxDot = Float.MIN_VALUE
        var support = Vector3.ZERO

        for (i in vertices.indices step 3) {
            val v = Vector3(vertices[i], vertices[i + 1], vertices[i + 2])
            val dot = v.dot(direction)
            if (dot > maxDot) {
                maxDot = dot
                support = v
            }
        }

        return support + (direction.normalize() * margin)
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        var maxDot = Float.MIN_VALUE
        var support = Vector3.ZERO

        for (i in vertices.indices step 3) {
            val v = Vector3(vertices[i], vertices[i + 1], vertices[i + 2])
            val dot = v.dot(direction)
            if (dot > maxDot) {
                maxDot = dot
                support = v
            }
        }

        return support
    }

    override fun calculateLocalInertia(mass: Float) = calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierConvexHullShape(vertices.copyOf())
}

private class RapierTriangleMeshShape(
    override val vertices: FloatArray,
    override val indices: IntArray
) : TriangleMeshShape {
    override val shapeType = ShapeType.TRIANGLE_MESH
    override val margin = 0.04f
    override val localScaling = Vector3.ONE
    override val triangleCount = indices.size / 3
    override val boundingBox: Box3

    init {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        for (i in vertices.indices step 3) {
            minX = minOf(minX, vertices[i])
            minY = minOf(minY, vertices[i + 1])
            minZ = minOf(minZ, vertices[i + 2])
            maxX = maxOf(maxX, vertices[i])
            maxY = maxOf(maxY, vertices[i + 1])
            maxZ = maxOf(maxZ, vertices[i + 2])
        }

        boundingBox = Box3(
            Vector3(minX, minY, minZ),
            Vector3(maxX, maxY, maxZ)
        )
    }

    override fun getTriangle(index: Int): Triangle {
        val i = index * 3
        val i1 = indices[i] * 3
        val i2 = indices[i + 1] * 3
        val i3 = indices[i + 2] * 3

        return Triangle(
            Vector3(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]),
            Vector3(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]),
            Vector3(vertices[i3], vertices[i3 + 1], vertices[i3 + 2])
        )
    }

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3, aabbMax: Vector3) {
        for (i in 0 until triangleCount) {
            val triangle = getTriangle(i)
            // Check if triangle intersects AABB
            if (triangleIntersectsAABB(triangle, aabbMin, aabbMax)) {
                callback.processTriangle(triangle, 0, i)
            }
        }
    }

    override fun buildBVH(): MeshBVH {
        // Simplified BVH implementation
        return SimpleMeshBVH(this).toMeshBVH()
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        // Use bounding box for mesh inertia
        val size = boundingBox.getSize()
        return Matrix3(floatArrayOf(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        ))
    }

    override fun getVolume(): Float {
        // Calculate volume using divergence theorem
        var volume = 0f
        for (i in 0 until triangleCount) {
            val tri = getTriangle(i)
            volume += tri.vertex0.dot(tri.vertex1.cross(tri.vertex2)) / 6f
        }
        return kotlin.math.abs(volume)
    }

    override fun getSurfaceArea(): Float {
        var area = 0f
        for (i in 0 until triangleCount) {
            val tri = getTriangle(i)
            val ab = tri.vertex1.subtract(tri.vertex0)
            val ac = tri.vertex2.subtract(tri.vertex0)
            area += ab.cross(ac).length() * 0.5f
        }
        return area
    }

    override fun isConvex() = false
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) = Vector3.ZERO
    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) = Vector3.ZERO
    override fun calculateLocalInertia(mass: Float) = calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierTriangleMeshShape(vertices.copyOf(), indices.copyOf())

    private fun triangleIntersectsAABB(triangle: Triangle, aabbMin: Vector3, aabbMax: Vector3): Boolean {
        // Simplified AABB-triangle intersection test
        val triMin = Vector3(
            minOf(triangle.vertex0.x, triangle.vertex1.x, triangle.vertex2.x),
            minOf(triangle.vertex0.y, triangle.vertex1.y, triangle.vertex2.y),
            minOf(triangle.vertex0.z, triangle.vertex1.z, triangle.vertex2.z)
        )
        val triMax = Vector3(
            maxOf(triangle.vertex0.x, triangle.vertex1.x, triangle.vertex2.x),
            maxOf(triangle.vertex0.y, triangle.vertex1.y, triangle.vertex2.y),
            maxOf(triangle.vertex0.z, triangle.vertex1.z, triangle.vertex2.z)
        )

        return !(triMax.x < aabbMin.x || triMin.x > aabbMax.x ||
                triMax.y < aabbMin.y || triMin.y > aabbMax.y ||
                triMax.z < aabbMin.z || triMin.z > aabbMax.z)
    }
}

private class RapierHeightfieldShape(
    override val width: Int,
    override val height: Int,
    override val heightData: FloatArray
) : HeightfieldShape {
    override val shapeType = ShapeType.HEIGHTFIELD
    override val margin = 0.04f
    override val localScaling = Vector3.ONE
    override val upAxis = 1
    override val maxHeight = heightData.maxOrNull() ?: 0f
    override val minHeight = heightData.minOrNull() ?: 0f
    override val boundingBox = Box3(
        Vector3(0f, minHeight, 0f),
        Vector3(width.toFloat(), maxHeight, height.toFloat())
    )

    override fun getHeightAtPoint(x: Float, z: Float): Float {
        val ix = x.toInt().coerceIn(0, width - 1)
        val iz = z.toInt().coerceIn(0, height - 1)
        return heightData[iz * width + ix]
    }

    override fun setHeightValue(x: Int, z: Int, height: Float) {
        if (x in 0 until width && z in 0 until this.height) {
            heightData[z * width + x] = height
        }
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        // Approximate using bounding box
        val size = boundingBox.getSize()
        return Matrix3(floatArrayOf(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        ))
    }

    override fun getVolume(): Float {
        // Approximate volume
        val avgHeight = (maxHeight + minHeight) / 2f
        return width.toFloat() * height.toFloat() * avgHeight
    }

    override fun getSurfaceArea(): Float {
        // Approximate surface area
        return width.toFloat() * height.toFloat()
    }

    override fun isConvex() = false
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) = Vector3.ZERO
    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) = Vector3.ZERO
    override fun calculateLocalInertia(mass: Float) = calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierHeightfieldShape(width, height, heightData.copyOf())
}

private class RapierCompoundShape : CompoundShape {
    override val shapeType = ShapeType.COMPOUND
    override val margin = 0f
    override val localScaling = Vector3.ONE
    override val childShapes = mutableListOf<ChildShape>()
    override var boundingBox = Box3(Vector3.ZERO, Vector3.ZERO)
        private set

    override fun addChildShape(transform: Matrix4, shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            childShapes.add(ChildShape(transform, shape))
            recalculateLocalAabb()
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to add child shape"))
        }
    }

    override fun removeChildShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            childShapes.removeAll { it.shape == shape }
            recalculateLocalAabb()
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to remove child shape"))
        }
    }

    override fun removeChildShapeByIndex(index: Int): PhysicsResult<Unit> {
        return try {
            if (index in childShapes.indices) {
                childShapes.removeAt(index)
                recalculateLocalAabb()
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to remove child shape"))
        }
    }

    override fun updateChildTransform(index: Int, transform: Matrix4): PhysicsResult<Unit> {
        return try {
            if (index in childShapes.indices) {
                childShapes[index] = ChildShape(transform, childShapes[index].shape)
                recalculateLocalAabb()
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to update child transform"))
        }
    }

    override fun recalculateLocalAabb() {
        if (childShapes.isEmpty()) {
            boundingBox = Box3(Vector3.ZERO, Vector3.ZERO)
            return
        }

        var min = Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var max = Vector3(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

        for (child in childShapes) {
            val childBounds = child.shape.boundingBox
            val transformedMin = child.transform.transformPoint(childBounds.min)
            val transformedMax = child.transform.transformPoint(childBounds.max)

            min = min.min(transformedMin)
            max = max.max(transformedMax)
        }

        boundingBox = Box3(min, max)
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        // Composite inertia calculation
        var totalInertia = Matrix3.identity()
        val massPerChild = mass / childShapes.size.toFloat()

        for (child in childShapes) {
            val childInertia = child.shape.calculateInertia(massPerChild)
            // Transform to compound space
            totalInertia = totalInertia.add(childInertia)
        }

        return totalInertia
    }

    override fun getVolume(): Float {
        return childShapes.sumOf { it.shape.getVolume().toDouble() }.toFloat()
    }

    override fun getSurfaceArea(): Float {
        return childShapes.sumOf { it.shape.getSurfaceArea().toDouble() }.toFloat()
    }

    override fun isConvex() = false
    override fun isCompound() = true

    override fun localGetSupportingVertex(direction: Vector3) = Vector3.ZERO
    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) = Vector3.ZERO
    override fun calculateLocalInertia(mass: Float) = calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = RapierCompoundShape().apply {
        for (child in this@RapierCompoundShape.childShapes) {
            addChildShape(child.transform, child.shape.clone())
        }
    }
}

// Helper functions for Rapier <-> KreeKt conversions
private fun toRapierVector3(v: Vector3): RAPIER.Vector3 {
    return RAPIER.Vector3(v.x, v.y, v.z)
}

private fun fromRapierVector3(v: dynamic): Vector3 {
    return Vector3(v.x as Float, v.y as Float, v.z as Float)
}

private fun toRapierQuaternion(q: Quaternion): RAPIER.Quaternion {
    return RAPIER.Quaternion(q.x, q.y, q.z, q.w)
}

private fun fromRapierQuaternion(q: dynamic): Quaternion {
    return Quaternion(q.x as Float, q.y as Float, q.z as Float, q.w as Float)
}

// Simplified helper classes
private class SimpleMeshBVH(private val mesh: TriangleMeshShape) {
    private val triangles = (0 until mesh.triangleCount).map { mesh.getTriangle(it) }
    private val nodes = listOf(BVHNode(
        bounds = mesh.boundingBox,
        leftChild = -1,
        rightChild = -1,
        triangleOffset = 0,
        triangleCount = mesh.triangleCount
    ))

    fun toMeshBVH(): MeshBVH = MeshBVH(nodes, triangles)
}

// Extension functions for Matrix3
private fun Matrix3.getDiagonal() = Vector3(m00, m11, m22)
private fun Matrix3.add(other: Matrix3) = Matrix3(floatArrayOf(m00 + other.m00, m01 + other.m01, m02 + other.m02,
    m10 + other.m10, m11 + other.m11, m12 + other.m12,
    m20 + other.m20, m21 + other.m21, m22 + other.m22))

// Extension function for Matrix4
private fun Matrix4.extractRotation(): Quaternion {
    // Simplified quaternion extraction from rotation matrix
    val trace = m00 + m11 + m22

    return when {
        trace > 0 -> {
            val s = 0.5f / kotlin.math.sqrt(trace + 1f)
            Quaternion(
                (m21 - m12) * s,
                (m02 - m20) * s,
                (m10 - m01) * s,
                0.25f / s
            )
        }
        m00 > m11 && m00 > m22 -> {
            val s = 2f * kotlin.math.sqrt(1f + m00 - m11 - m22)
            Quaternion(
                0.25f * s,
                (m01 + m10) / s,
                (m02 + m20) / s,
                (m21 - m12) / s
            )
        }
        m11 > m22 -> {
            val s = 2f * kotlin.math.sqrt(1f + m11 - m00 - m22)
            Quaternion(
                (m01 + m10) / s,
                0.25f * s,
                (m12 + m21) / s,
                (m02 - m20) / s
            )
        }
        else -> {
            val s = 2f * kotlin.math.sqrt(1f + m22 - m00 - m11)
            Quaternion(
                (m02 + m20) / s,
                (m12 + m21) / s,
                0.25f * s,
                (m10 - m01) / s
            )
        }
    }
}

// Platform-specific actual implementations
actual fun createDefaultPhysicsEngine(): PhysicsEngine = RapierPhysicsEngine()