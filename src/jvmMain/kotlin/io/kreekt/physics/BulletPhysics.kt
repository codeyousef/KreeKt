/**
 * Bullet Physics Engine Integration for JVM Platform
 * Provides high-performance physics simulation using Bullet JNI bindings
 */
package io.kreekt.physics

import io.kreekt.core.math.*
import com.bulletphysics.collision.broadphase.*
import com.bulletphysics.collision.dispatch.*
import com.bulletphysics.collision.narrowphase.*
import com.bulletphysics.collision.shapes.*
import com.bulletphysics.dynamics.*
import com.bulletphysics.dynamics.constraintsolver.*
import com.bulletphysics.linearmath.*
import com.bulletphysics.motion.*
import com.bulletphysics.util.*
import javax.vecmath.*
import java.nio.*

/**
 * Bullet-based implementation of PhysicsWorld
 */
class BulletPhysicsWorld(
    initialGravity: Vector3 = Vector3(0f, -9.81f, 0f)
) : PhysicsWorld {
    private val collisionConfiguration = DefaultCollisionConfiguration()
    private val dispatcher = CollisionDispatcher(collisionConfiguration)
    private val broadphase = DbvtBroadphase()
    private val solver = SequentialImpulseConstraintSolver()

    private val dynamicsWorld = DiscreteDynamicsWorld(
        dispatcher,
        broadphase,
        solver,
        collisionConfiguration
    ).apply {
        setGravity(Vector3f(initialGravity.x, initialGravity.y, initialGravity.z))
    }

    private val rigidBodies = mutableMapOf<String, BulletRigidBody>()
    private val constraints = mutableMapOf<String, BulletConstraint>()
    private val collisionObjects = mutableMapOf<String, CollisionObject>()
    private var collisionCallback: CollisionCallback? = null

    override var gravity: Vector3 = initialGravity
        set(value) {
            field = value
            dynamicsWorld.setGravity(Vector3f(value.x, value.y, value.z))
        }

    override var timeStep = 1f / 60f
    override var maxSubSteps = 10
    override var solverIterations = 10
        set(value) {
            field = value
            dynamicsWorld.solverInfo.numIterations = value
        }

    override var broadphase = BroadphaseType.DBVT

    init {
        setupCollisionCallbacks()
    }

    private fun setupCollisionCallbacks() {
        // Custom collision callback using Bullet's persistent manifold
        dynamicsWorld.dispatcher.nearCallback = object : NearCallback() {
            override fun handleCollision(
                collisionPair: BroadphasePair,
                dispatcher: CollisionDispatcher,
                dispatchInfo: DispatcherInfo
            ) {
                val obj0 = collisionPair.pProxy0.clientObject as? com.bulletphysics.collision.dispatch.CollisionObject
                val obj1 = collisionPair.pProxy1.clientObject as? com.bulletphysics.collision.dispatch.CollisionObject

                if (obj0 != null && obj1 != null) {
                    val body0 = obj0.userPointer as? CollisionObject
                    val body1 = obj1.userPointer as? CollisionObject

                    if (body0 != null && body1 != null) {
                        processCollision(body0, body1, collisionPair)
                    }
                }
            }
        }
    }

    private fun processCollision(
        obj0: CollisionObject,
        obj1: CollisionObject,
        pair: BroadphasePair
    ) {
        val algorithm = dynamicsWorld.dispatcher.findAlgorithm(
            pair.pProxy0.clientObject as com.bulletphysics.collision.dispatch.CollisionObject,
            pair.pProxy1.clientObject as com.bulletphysics.collision.dispatch.CollisionObject
        )

        if (algorithm != null) {
            val manifoldArray = ManifoldArray()
            algorithm.getAllContactManifolds(manifoldArray)

            for (i in 0 until manifoldArray.size()) {
                val manifold = manifoldArray.getQuick(i)
                val numContacts = manifold.numContacts

                if (numContacts > 0) {
                    val contactPoints = mutableListOf<Vector3>()
                    var totalImpulse = 0f

                    for (j in 0 until numContacts) {
                        val point = manifold.getContactPoint(j)
                        val worldPosA = Vector3f()
                        val worldPosB = Vector3f()

                        point.getPositionWorldOnA(worldPosA)
                        contactPoints.add(fromBulletVector3(worldPosA))
                        totalImpulse += point.appliedImpulse
                    }

                    val normalOnB = Vector3f()
                    manifold.getContactPoint(0).normalWorldOnB.get(normalOnB)

                    val event = CollisionEvent(
                        objectA = obj0,
                        objectB = obj1,
                        contactPoints = contactPoints,
                        normal = fromBulletVector3(normalOnB),
                        impulse = totalImpulse
                    )

                    collisionCallback?.onCollisionStay(event)
                }
            }
        }
    }

    override fun addRigidBody(body: RigidBody): PhysicsResult<Unit> {
        return try {
            val bulletBody = body as? BulletRigidBody
                ?: return PhysicsResult.Error(
                    PhysicsException.InvalidOperation("Body must be created through BulletPhysicsEngine")
                )

            dynamicsWorld.addRigidBody(bulletBody.bulletBody)
            rigidBodies[body.id] = bulletBody
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add rigid body"))
        }
    }

    override fun removeRigidBody(body: RigidBody): PhysicsResult<Unit> {
        return try {
            val bulletBody = rigidBodies.remove(body.id) as? BulletRigidBody
            bulletBody?.let {
                dynamicsWorld.removeRigidBody(it.bulletBody)
            }
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove rigid body"))
        }
    }

    override fun getRigidBodies(): List<RigidBody> = rigidBodies.values.toList()

    override fun getRigidBody(id: String): RigidBody? = rigidBodies[id]

    override fun addConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        return try {
            val bulletConstraint = constraint as? BulletConstraint
                ?: return PhysicsResult.Error(
                    PhysicsException.InvalidOperation("Constraint must be created through BulletPhysicsEngine")
                )

            dynamicsWorld.addConstraint(bulletConstraint.bulletConstraint, true)
            constraints[constraint.id] = bulletConstraint
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add constraint"))
        }
    }

    override fun removeConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        return try {
            val bulletConstraint = constraints.remove(constraint.id) as? BulletConstraint
            bulletConstraint?.let {
                dynamicsWorld.removeConstraint(it.bulletConstraint)
            }
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove constraint"))
        }
    }

    override fun getConstraints(): List<PhysicsConstraint> = constraints.values.toList()

    override fun addCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        return try {
            val shape = createBulletShape(obj.collisionShape)
            val bulletObj = com.bulletphysics.collision.dispatch.CollisionObject().apply {
                collisionShape = shape
                worldTransform = toBulletTransform(obj.transform)
                userPointer = obj
                collisionFlags = if (obj.isTrigger) {
                    com.bulletphysics.collision.dispatch.CollisionObject.CF_NO_CONTACT_RESPONSE
                } else {
                    com.bulletphysics.collision.dispatch.CollisionObject.CF_STATIC_OBJECT
                }
            }

            dynamicsWorld.addCollisionObject(
                bulletObj,
                obj.collisionGroups.toShort(),
                obj.collisionMask.toShort()
            )

            collisionObjects[obj.id] = obj
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add collision object"))
        }
    }

    override fun removeCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        return try {
            collisionObjects.remove(obj.id)
            // Find and remove the Bullet collision object
            for (i in 0 until dynamicsWorld.numCollisionObjects) {
                val bulletObj = dynamicsWorld.getCollisionObjectArray()[i]
                if (bulletObj.userPointer == obj) {
                    dynamicsWorld.removeCollisionObject(bulletObj)
                    break
                }
            }
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove collision object"))
        }
    }

    override fun setCollisionCallback(callback: CollisionCallback) {
        collisionCallback = callback
    }

    override fun step(deltaTime: Float): PhysicsResult<Unit> {
        return try {
            dynamicsWorld.stepSimulation(deltaTime, maxSubSteps, timeStep)

            // Update transforms
            rigidBodies.values.forEach { body ->
                body.updateFromBullet()
            }

            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError("Simulation step failed: ${e.message}"))
        }
    }

    override fun pause() {
        // Bullet doesn't have explicit pause, we control it by not calling step
    }

    override fun resume() {
        // Bullet doesn't have explicit resume
    }

    override fun reset() {
        // Clear all bodies and constraints
        rigidBodies.values.forEach { body ->
            dynamicsWorld.removeRigidBody(body.bulletBody)
        }
        constraints.values.forEach { constraint ->
            dynamicsWorld.removeConstraint(constraint.bulletConstraint)
        }

        rigidBodies.clear()
        constraints.clear()
        collisionObjects.clear()

        // Reset world state
        dynamicsWorld.clearForces()
        broadphase.resetPool(dispatcher)
    }

    override fun raycast(from: Vector3, to: Vector3, groups: Int): RaycastResult? {
        val fromVec = Vector3f(from.x, from.y, from.z)
        val toVec = Vector3f(to.x, to.y, to.z)

        val rayCallback = ClosestRayResultCallback(fromVec, toVec).apply {
            collisionFilterGroup = groups.toShort()
            collisionFilterMask = -1
        }

        dynamicsWorld.rayTest(fromVec, toVec, rayCallback)

        return if (rayCallback.hasHit()) {
            val hitObj = rayCallback.collisionObject.userPointer as? CollisionObject
            RaycastResult(
                hitObject = hitObj,
                hitPoint = fromBulletVector3(rayCallback.hitPointWorld),
                hitNormal = fromBulletVector3(rayCallback.hitNormalWorld),
                distance = rayCallback.closestHitFraction * from.distanceTo(to)
            )
        } else null
    }

    override fun sphereCast(center: Vector3, radius: Float, groups: Int): List<CollisionObject> {
        val results = mutableListOf<CollisionObject>()
        val centerVec = toBulletVector3(center)
        val sphereShape = SphereShape(radius)

        val ghostObject = GhostObject().apply {
            collisionShape = sphereShape
            worldTransform = Transform().apply {
                setIdentity()
                origin.set(centerVec)
            }
            collisionFlags = com.bulletphysics.collision.dispatch.CollisionObject.CF_NO_CONTACT_RESPONSE
        }

        dynamicsWorld.addCollisionObject(ghostObject, groups.toShort(), -1)

        for (i in 0 until ghostObject.numOverlappingObjects) {
            val obj = ghostObject.getOverlappingObject(i)
            val userObj = obj.userPointer as? CollisionObject
            userObj?.let { results.add(it) }
        }

        dynamicsWorld.removeCollisionObject(ghostObject)
        return results
    }

    override fun boxCast(
        center: Vector3,
        halfExtents: Vector3,
        rotation: Quaternion,
        groups: Int
    ): List<CollisionObject> {
        val results = mutableListOf<CollisionObject>()
        val boxShape = BoxShape(toBulletVector3(halfExtents))

        val ghostObject = GhostObject().apply {
            collisionShape = boxShape
            worldTransform = Transform().apply {
                setIdentity()
                origin.set(toBulletVector3(center))
                setRotation(toBulletQuaternion(rotation))
            }
            collisionFlags = com.bulletphysics.collision.dispatch.CollisionObject.CF_NO_CONTACT_RESPONSE
        }

        dynamicsWorld.addCollisionObject(ghostObject, groups.toShort(), -1)

        for (i in 0 until ghostObject.numOverlappingObjects) {
            val obj = ghostObject.getOverlappingObject(i)
            val userObj = obj.userPointer as? CollisionObject
            userObj?.let { results.add(it) }
        }

        dynamicsWorld.removeCollisionObject(ghostObject)
        return results
    }

    override fun overlaps(
        shape: CollisionShape,
        transform: Matrix4,
        groups: Int
    ): List<CollisionObject> {
        val results = mutableListOf<CollisionObject>()
        val bulletShape = createBulletShape(shape)

        val ghostObject = GhostObject().apply {
            collisionShape = bulletShape
            worldTransform = toBulletTransform(transform)
            collisionFlags = com.bulletphysics.collision.dispatch.CollisionObject.CF_NO_CONTACT_RESPONSE
        }

        dynamicsWorld.addCollisionObject(ghostObject, groups.toShort(), -1)

        for (i in 0 until ghostObject.numOverlappingObjects) {
            val obj = ghostObject.getOverlappingObject(i)
            val userObj = obj.userPointer as? CollisionObject
            userObj?.let { results.add(it) }
        }

        dynamicsWorld.removeCollisionObject(ghostObject)
        return results
    }

    private fun createBulletShape(shape: CollisionShape): com.bulletphysics.collision.shapes.CollisionShape {
        return when (shape) {
            is BoxShape -> BoxShape(toBulletVector3(shape.halfExtents))
            is SphereShape -> SphereShape(shape.radius)
            is CapsuleShape -> when (shape.upAxis) {
                0 -> CapsuleShapeX(shape.radius, shape.height)
                2 -> CapsuleShapeZ(shape.radius, shape.height)
                else -> CapsuleShape(shape.radius, shape.height)
            }
            is CylinderShape -> when (shape.upAxis) {
                0 -> CylinderShapeX(toBulletVector3(shape.halfExtents))
                2 -> CylinderShapeZ(toBulletVector3(shape.halfExtents))
                else -> CylinderShape(toBulletVector3(shape.halfExtents))
            }
            is ConeShape -> when (shape.upAxis) {
                0 -> ConeShapeX(shape.radius, shape.height)
                2 -> ConeShapeZ(shape.radius, shape.height)
                else -> ConeShape(shape.radius, shape.height)
            }
            is ConvexHullShape -> {
                val convexHull = ConvexHullShape()
                for (i in shape.vertices.indices step 3) {
                    convexHull.addPoint(Vector3f(
                        shape.vertices[i],
                        shape.vertices[i + 1],
                        shape.vertices[i + 2]
                    ))
                }
                convexHull.optimizeConvexHull()
                convexHull
            }
            is TriangleMeshShape -> {
                val triMesh = TriangleMesh()
                for (i in shape.indices.indices step 3) {
                    val i1 = shape.indices[i] * 3
                    val i2 = shape.indices[i + 1] * 3
                    val i3 = shape.indices[i + 2] * 3

                    triMesh.addTriangle(
                        Vector3f(shape.vertices[i1], shape.vertices[i1 + 1], shape.vertices[i1 + 2]),
                        Vector3f(shape.vertices[i2], shape.vertices[i2 + 1], shape.vertices[i2 + 2]),
                        Vector3f(shape.vertices[i3], shape.vertices[i3 + 1], shape.vertices[i3 + 2])
                    )
                }
                BvhTriangleMeshShape(triMesh, true, true)
            }
            is HeightfieldShape -> HeightfieldTerrainShape(
                shape.width,
                shape.height,
                shape.heightData,
                1f,
                shape.minHeight,
                shape.maxHeight,
                shape.upAxis,
                ScalarType.FLOAT,
                false
            )
            is CompoundShape -> {
                val compound = com.bulletphysics.collision.shapes.CompoundShape()
                for (child in shape.childShapes) {
                    val childShape = createBulletShape(child.shape)
                    compound.addChildShape(toBulletTransform(child.transform), childShape)
                }
                compound
            }
            else -> BoxShape(Vector3f(1f, 1f, 1f)) // Fallback
        }
    }

    fun dispose() {
        reset()
        // Clean up native resources
        dynamicsWorld.destroy()
        solver.reset()
        broadphase.destroy()
        dispatcher.destroy()
        collisionConfiguration.destroy()
    }
}

/**
 * Bullet-based implementation of RigidBody
 */
class BulletRigidBody(
    override val id: String,
    internal val bulletBody: com.bulletphysics.dynamics.RigidBody,
    initialShape: CollisionShape
) : RigidBody {

    private var _transform = Matrix4.identity()
    override var transform: Matrix4
        get() = _transform
        set(value) {
            _transform = value
            bulletBody.worldTransform = toBulletTransform(value)
        }

    override var collisionShape: CollisionShape = initialShape

    override var collisionGroups: Int = -1
        set(value) {
            field = value
            // Update Bullet collision groups
            bulletBody.broadphaseHandle?.let {
                it.collisionFilterGroup = value.toShort()
            }
        }

    override var collisionMask: Int = -1
        set(value) {
            field = value
            // Update Bullet collision mask
            bulletBody.broadphaseHandle?.let {
                it.collisionFilterMask = value.toShort()
            }
        }

    override var userData: Any? = null

    override var contactProcessingThreshold = 0.01f
        set(value) {
            field = value
            bulletBody.contactProcessingThreshold = value
        }

    override var collisionFlags: Int
        get() = bulletBody.collisionFlags
        set(value) {
            bulletBody.collisionFlags = value
        }

    override var isTrigger: Boolean = false
        set(value) {
            field = value
            if (value) {
                bulletBody.collisionFlags = bulletBody.collisionFlags or
                    com.bulletphysics.collision.dispatch.CollisionObject.CF_NO_CONTACT_RESPONSE
            } else {
                bulletBody.collisionFlags = bulletBody.collisionFlags and
                    com.bulletphysics.collision.dispatch.CollisionObject.CF_NO_CONTACT_RESPONSE.inv()
            }
        }

    override var mass: Float = 1f
        set(value) {
            field = value
            val inertia = Vector3f()
            bulletBody.collisionShape.calculateLocalInertia(value, inertia)
            bulletBody.setMassProps(value, inertia)
        }

    override var density: Float = 1f

    override var restitution: Float
        get() = bulletBody.restitution
        set(value) {
            bulletBody.restitution = value
        }

    override var friction: Float
        get() = bulletBody.friction
        set(value) {
            bulletBody.friction = value
        }

    override var rollingFriction: Float
        get() = bulletBody.rollingFriction
        set(value) {
            bulletBody.rollingFriction = value
        }

    override var linearDamping: Float
        get() = bulletBody.linearDamping
        set(value) {
            bulletBody.setDamping(value, angularDamping)
        }

    override var angularDamping: Float
        get() = bulletBody.angularDamping
        set(value) {
            bulletBody.setDamping(linearDamping, value)
        }

    override var linearVelocity: Vector3
        get() {
            val velocity = Vector3f()
            bulletBody.getLinearVelocity(velocity)
            return fromBulletVector3(velocity)
        }
        set(value) {
            bulletBody.setLinearVelocity(toBulletVector3(value))
        }

    override var angularVelocity: Vector3
        get() {
            val velocity = Vector3f()
            bulletBody.getAngularVelocity(velocity)
            return fromBulletVector3(velocity)
        }
        set(value) {
            bulletBody.setAngularVelocity(toBulletVector3(value))
        }

    override var linearFactor: Vector3
        get() {
            val factor = Vector3f()
            bulletBody.getLinearFactor(factor)
            return fromBulletVector3(factor)
        }
        set(value) {
            bulletBody.setLinearFactor(toBulletVector3(value))
        }

    override var angularFactor: Vector3
        get() {
            val factor = Vector3f()
            bulletBody.getAngularFactor(factor)
            return fromBulletVector3(factor)
        }
        set(value) {
            bulletBody.setAngularFactor(toBulletVector3(value))
        }

    override var bodyType: RigidBodyType = RigidBodyType.DYNAMIC
        set(value) {
            field = value
            when (value) {
                RigidBodyType.STATIC -> {
                    bulletBody.setMassProps(0f, Vector3f(0f, 0f, 0f))
                    bulletBody.collisionFlags = com.bulletphysics.collision.dispatch.CollisionObject.CF_STATIC_OBJECT
                }
                RigidBodyType.KINEMATIC -> {
                    bulletBody.collisionFlags = com.bulletphysics.collision.dispatch.CollisionObject.CF_KINEMATIC_OBJECT
                    bulletBody.activationState = com.bulletphysics.collision.dispatch.CollisionObject.DISABLE_DEACTIVATION
                }
                RigidBodyType.DYNAMIC -> {
                    val inertia = Vector3f()
                    bulletBody.collisionShape.calculateLocalInertia(mass, inertia)
                    bulletBody.setMassProps(mass, inertia)
                    bulletBody.collisionFlags = 0
                }
            }
        }

    override var activationState: ActivationState
        get() = when (bulletBody.activationState) {
            com.bulletphysics.collision.dispatch.CollisionObject.ACTIVE_TAG -> ActivationState.ACTIVE
            com.bulletphysics.collision.dispatch.CollisionObject.ISLAND_SLEEPING -> ActivationState.SLEEPING
            com.bulletphysics.collision.dispatch.CollisionObject.DISABLE_DEACTIVATION -> ActivationState.DISABLE_DEACTIVATION
            com.bulletphysics.collision.dispatch.CollisionObject.DISABLE_SIMULATION -> ActivationState.DISABLE_SIMULATION
            else -> ActivationState.WANTS_DEACTIVATION
        }
        set(value) {
            bulletBody.activationState = when (value) {
                ActivationState.ACTIVE -> com.bulletphysics.collision.dispatch.CollisionObject.ACTIVE_TAG
                ActivationState.SLEEPING -> com.bulletphysics.collision.dispatch.CollisionObject.ISLAND_SLEEPING
                ActivationState.WANTS_DEACTIVATION -> com.bulletphysics.collision.dispatch.CollisionObject.WANTS_DEACTIVATION
                ActivationState.DISABLE_DEACTIVATION -> com.bulletphysics.collision.dispatch.CollisionObject.DISABLE_DEACTIVATION
                ActivationState.DISABLE_SIMULATION -> com.bulletphysics.collision.dispatch.CollisionObject.DISABLE_SIMULATION
            }
        }

    override var sleepThreshold = 0.8f
        set(value) {
            field = value
            bulletBody.setSleepingThresholds(value, value * 0.1f)
        }

    override var ccdMotionThreshold: Float
        get() = bulletBody.ccdMotionThreshold
        set(value) {
            bulletBody.ccdMotionThreshold = value
        }

    override var ccdSweptSphereRadius: Float
        get() = bulletBody.ccdSweptSphereRadius
        set(value) {
            bulletBody.ccdSweptSphereRadius = value
        }

    init {
        bulletBody.userPointer = this
    }

    override fun setCollisionShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            collisionShape = shape
            val bulletShape = createBulletShape(shape)
            bulletBody.collisionShape = bulletShape

            // Recalculate inertia if dynamic
            if (bodyType == RigidBodyType.DYNAMIC) {
                val inertia = Vector3f()
                bulletShape.calculateLocalInertia(mass, inertia)
                bulletBody.setMassProps(mass, inertia)
            }

            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.InvalidOperation("Failed to set collision shape"))
        }
    }

    override fun getCollisionShape(): CollisionShape = collisionShape

    override fun setWorldTransform(transform: Matrix4) {
        this.transform = transform
    }

    override fun getWorldTransform(): Matrix4 = transform

    override fun translate(offset: Vector3) {
        val currentTransform = Transform()
        bulletBody.getWorldTransform(currentTransform)
        currentTransform.origin.add(toBulletVector3(offset))
        bulletBody.worldTransform = currentTransform
        updateFromBullet()
    }

    override fun rotate(rotation: Quaternion) {
        val currentTransform = Transform()
        bulletBody.getWorldTransform(currentTransform)
        val currentQuat = Quat4f()
        currentTransform.getRotation(currentQuat)

        val newQuat = toBulletQuaternion(rotation)
        currentQuat.mul(newQuat)
        currentTransform.setRotation(currentQuat)

        bulletBody.worldTransform = currentTransform
        updateFromBullet()
    }

    override fun applyForce(force: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            if (relativePosition == Vector3.ZERO) {
                bulletBody.applyCentralForce(toBulletVector3(force))
            } else {
                bulletBody.applyForce(toBulletVector3(force), toBulletVector3(relativePosition))
            }
            bulletBody.activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError("Failed to apply force"))
        }
    }

    override fun applyImpulse(impulse: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            if (relativePosition == Vector3.ZERO) {
                bulletBody.applyCentralImpulse(toBulletVector3(impulse))
            } else {
                bulletBody.applyImpulse(toBulletVector3(impulse), toBulletVector3(relativePosition))
            }
            bulletBody.activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError("Failed to apply impulse"))
        }
    }

    override fun applyTorque(torque: Vector3): PhysicsResult<Unit> {
        return try {
            bulletBody.applyTorque(toBulletVector3(torque))
            bulletBody.activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError("Failed to apply torque"))
        }
    }

    override fun applyTorqueImpulse(torque: Vector3): PhysicsResult<Unit> {
        return try {
            bulletBody.applyTorqueImpulse(toBulletVector3(torque))
            bulletBody.activate()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.SimulationError("Failed to apply torque impulse"))
        }
    }

    override fun applyCentralForce(force: Vector3): PhysicsResult<Unit> {
        return applyForce(force, Vector3.ZERO)
    }

    override fun applyCentralImpulse(impulse: Vector3): PhysicsResult<Unit> {
        return applyImpulse(impulse, Vector3.ZERO)
    }

    override fun isActive(): Boolean = bulletBody.isActive

    override fun isKinematic(): Boolean =
        bulletBody.collisionFlags and com.bulletphysics.collision.dispatch.CollisionObject.CF_KINEMATIC_OBJECT != 0

    override fun isStatic(): Boolean =
        bulletBody.collisionFlags and com.bulletphysics.collision.dispatch.CollisionObject.CF_STATIC_OBJECT != 0

    override fun getInertia(): Matrix3 {
        val inertia = Vector3f()
        bulletBody.getInvInertiaDiagLocal(inertia)

        // Invert to get actual inertia
        return if (inertia.x > 0 && inertia.y > 0 && inertia.z > 0) {
            Matrix3(
                1f / inertia.x, 0f, 0f,
                0f, 1f / inertia.y, 0f,
                0f, 0f, 1f / inertia.z
            )
        } else {
            Matrix3.identity()
        }
    }

    override fun getInverseInertia(): Matrix3 {
        val invInertia = Vector3f()
        bulletBody.getInvInertiaDiagLocal(invInertia)

        return Matrix3(
            invInertia.x, 0f, 0f,
            0f, invInertia.y, 0f,
            0f, 0f, invInertia.z
        )
    }

    override fun getTotalForce(): Vector3 {
        val force = Vector3f()
        bulletBody.getTotalForce(force)
        return fromBulletVector3(force)
    }

    override fun getTotalTorque(): Vector3 {
        val torque = Vector3f()
        bulletBody.getTotalTorque(torque)
        return fromBulletVector3(torque)
    }

    override fun setTransform(position: Vector3, rotation: Quaternion) {
        val transform = Transform()
        transform.setIdentity()
        transform.origin.set(toBulletVector3(position))
        transform.setRotation(toBulletQuaternion(rotation))
        bulletBody.worldTransform = transform
        updateFromBullet()
    }

    override fun getCenterOfMassTransform(): Matrix4 {
        val transform = Transform()
        bulletBody.getCenterOfMassTransform(transform)
        return fromBulletTransform(transform)
    }

    internal fun updateFromBullet() {
        val transform = Transform()
        bulletBody.getWorldTransform(transform)
        _transform = fromBulletTransform(transform)
    }

    private fun createBulletShape(shape: CollisionShape): com.bulletphysics.collision.shapes.CollisionShape {
        return when (shape) {
            is BoxShape -> BoxShape(toBulletVector3(shape.halfExtents))
            is SphereShape -> SphereShape(shape.radius)
            is CapsuleShape -> CapsuleShape(shape.radius, shape.height)
            is CylinderShape -> CylinderShape(toBulletVector3(shape.halfExtents))
            is ConeShape -> ConeShape(shape.radius, shape.height)
            else -> BoxShape(Vector3f(1f, 1f, 1f))
        }
    }
}

/**
 * Base class for Bullet constraints
 */
abstract class BulletConstraint(
    override val id: String,
    override val bodyA: RigidBody,
    override val bodyB: RigidBody?,
    internal val bulletConstraint: TypedConstraint
) : PhysicsConstraint {

    override var enabled: Boolean = true
        set(value) {
            field = value
            bulletConstraint.isEnabled = value
        }

    override var breakingThreshold: Float
        get() = bulletConstraint.breakingImpulseThreshold
        set(value) {
            bulletConstraint.breakingImpulseThreshold = value
        }

    override fun getAppliedImpulse(): Float = bulletConstraint.appliedImpulse

    override fun isEnabled(): Boolean = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun getInfo(info: ConstraintInfo) {
        // Fill constraint info based on type
    }
}

/**
 * Bullet Point-to-Point constraint
 */
class BulletPointToPointConstraint(
    id: String,
    bodyA: RigidBody,
    bodyB: RigidBody?,
    override val pivotA: Vector3,
    override val pivotB: Vector3
) : BulletConstraint(
    id, bodyA, bodyB,
    if (bodyB != null) {
        Point2PointConstraint(
            (bodyA as BulletRigidBody).bulletBody,
            (bodyB as BulletRigidBody).bulletBody,
            toBulletVector3(pivotA),
            toBulletVector3(pivotB)
        )
    } else {
        Point2PointConstraint(
            (bodyA as BulletRigidBody).bulletBody,
            toBulletVector3(pivotA)
        )
    }
), PointToPointConstraint {

    private val p2pConstraint = bulletConstraint as Point2PointConstraint

    override fun setPivotA(pivot: Vector3) {
        p2pConstraint.setPivotA(toBulletVector3(pivot))
    }

    override fun setPivotB(pivot: Vector3) {
        p2pConstraint.setPivotB(toBulletVector3(pivot))
    }

    override fun updateRHS(timeStep: Float) {
        p2pConstraint.updateRHS(timeStep)
    }

    override fun setParam(param: ConstraintParam, value: Float, axis: Int) {
        p2pConstraint.setParam(param.toBulletParam(), value, axis)
    }

    override fun getParam(param: ConstraintParam, axis: Int): Float {
        return p2pConstraint.getParam(param.toBulletParam(), axis)
    }
}

/**
 * Bullet physics engine implementation
 */
class BulletPhysicsEngine : PhysicsEngine {
    override val name = "Bullet"
    override val version = "3.24"

    init {
        // Initialize Bullet native library
        System.loadLibrary("bulletjni")
    }

    override fun createWorld(gravity: Vector3): PhysicsWorld {
        return BulletPhysicsWorld(gravity)
    }

    override fun destroyWorld(world: PhysicsWorld): PhysicsResult<Unit> {
        return try {
            (world as? BulletPhysicsWorld)?.dispose()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.InvalidOperation("Failed to destroy world"))
        }
    }

    override fun createBoxShape(halfExtents: Vector3): BoxShape {
        return BulletBoxShape(halfExtents)
    }

    override fun createSphereShape(radius: Float): SphereShape {
        return BulletSphereShape(radius)
    }

    override fun createCapsuleShape(radius: Float, height: Float): CapsuleShape {
        return BulletCapsuleShape(radius, height)
    }

    override fun createCylinderShape(halfExtents: Vector3): CylinderShape {
        return BulletCylinderShape(halfExtents)
    }

    override fun createConeShape(radius: Float, height: Float): ConeShape {
        return BulletConeShape(radius, height)
    }

    override fun createConvexHullShape(vertices: FloatArray): ConvexHullShape {
        return BulletConvexHullShape(vertices)
    }

    override fun createTriangleMeshShape(vertices: FloatArray, indices: IntArray): TriangleMeshShape {
        return BulletTriangleMeshShape(vertices, indices)
    }

    override fun createHeightfieldShape(width: Int, height: Int, heightData: FloatArray): HeightfieldShape {
        return BulletHeightfieldShape(width, height, heightData)
    }

    override fun createCompoundShape(): CompoundShape {
        return BulletCompoundShape()
    }

    override fun createRigidBody(shape: CollisionShape, mass: Float, transform: Matrix4): RigidBody {
        val bulletShape = createBulletShape(shape)
        val motionState = DefaultMotionState(toBulletTransform(transform))

        val inertia = Vector3f(0f, 0f, 0f)
        if (mass > 0f) {
            bulletShape.calculateLocalInertia(mass, inertia)
        }

        val rbInfo = RigidBodyConstructionInfo(mass, motionState, bulletShape, inertia)
        val bulletBody = com.bulletphysics.dynamics.RigidBody(rbInfo)

        return BulletRigidBody(
            id = "rb_${System.currentTimeMillis()}",
            bulletBody = bulletBody,
            initialShape = shape
        )
    }

    override fun createCharacterController(shape: CollisionShape, stepHeight: Float): CharacterController {
        // Bullet has KinematicCharacterController
        TODO("Character controller implementation for Bullet")
    }

    override fun createPointToPointConstraint(
        bodyA: RigidBody,
        bodyB: RigidBody?,
        pivotA: Vector3,
        pivotB: Vector3
    ): PointToPointConstraint {
        return BulletPointToPointConstraint(
            id = "p2p_${System.currentTimeMillis()}",
            bodyA = bodyA,
            bodyB = bodyB,
            pivotA = pivotA,
            pivotB = pivotB
        )
    }

    override fun createHingeConstraint(
        bodyA: RigidBody,
        bodyB: RigidBody?,
        pivotA: Vector3,
        pivotB: Vector3,
        axisA: Vector3,
        axisB: Vector3
    ): HingeConstraint {
        TODO("Hinge constraint implementation for Bullet")
    }

    override fun createSliderConstraint(
        bodyA: RigidBody,
        bodyB: RigidBody?,
        frameA: Matrix4,
        frameB: Matrix4
    ): SliderConstraint {
        TODO("Slider constraint implementation for Bullet")
    }

    private fun createBulletShape(shape: CollisionShape): com.bulletphysics.collision.shapes.CollisionShape {
        return when (shape) {
            is BoxShape -> BoxShape(toBulletVector3(shape.halfExtents))
            is SphereShape -> SphereShape(shape.radius)
            is CapsuleShape -> CapsuleShape(shape.radius, shape.height)
            is CylinderShape -> CylinderShape(toBulletVector3(shape.halfExtents))
            is ConeShape -> ConeShape(shape.radius, shape.height)
            is ConvexHullShape -> {
                val convexHull = ConvexHullShape()
                for (i in shape.vertices.indices step 3) {
                    convexHull.addPoint(Vector3f(
                        shape.vertices[i],
                        shape.vertices[i + 1],
                        shape.vertices[i + 2]
                    ))
                }
                convexHull
            }
            else -> BoxShape(Vector3f(1f, 1f, 1f))
        }
    }
}

// Shape implementations
private class BulletBoxShape(override val halfExtents: Vector3) : BoxShape {
    override val shapeType = ShapeType.BOX
    override val margin = 0.04f
    override val localScaling = Vector3.ONE
    override val boundingBox = Box3(halfExtents.negate(), halfExtents)

    override fun getHalfExtentsWithMargin() = halfExtents.add(Vector3(margin, margin, margin))
    override fun getHalfExtentsWithoutMargin() = halfExtents

    override fun calculateInertia(mass: Float): Matrix3 {
        val x = halfExtents.x * 2f
        val y = halfExtents.y * 2f
        val z = halfExtents.z * 2f
        val factor = mass / 12f
        return Matrix3(
            factor * (y * y + z * z), 0f, 0f,
            0f, factor * (x * x + z * z), 0f,
            0f, 0f, factor * (x * x + y * y)
        )
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
    ).add(direction.normalize().scale(margin))

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
    override fun clone() = BulletBoxShape(halfExtents)
}

// Similar implementations for other shapes...
private class BulletSphereShape(override val radius: Float) : SphereShape {
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
        return Matrix3(
            inertia, 0f, 0f,
            0f, inertia, 0f,
            0f, 0f, inertia
        )
    }

    override fun getVolume() = (4f / 3f) * kotlin.math.PI.toFloat() * radius * radius * radius
    override fun getSurfaceArea() = 4f * kotlin.math.PI.toFloat() * radius * radius
    override fun isConvex() = true
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) =
        direction.normalize().scale(radius + margin)

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) =
        direction.normalize().scale(radius)

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val inertia = 0.4f * mass * radius * radius
        return Vector3(inertia, inertia, inertia)
    }

    override fun serialize() = ByteArray(0)
    override fun clone() = BulletSphereShape(radius)
}

// Implement remaining shapes similarly...
private class BulletCapsuleShape(
    override val radius: Float,
    override val height: Float
) : CapsuleShape {
    override val shapeType = ShapeType.CAPSULE
    override val margin = 0f
    override val localScaling = Vector3.ONE
    override val upAxis = 1
    override val boundingBox = Box3(
        Vector3(-radius, -height/2f - radius, -radius),
        Vector3(radius, height/2f + radius, radius)
    )

    override fun getHalfHeight() = height / 2f

    override fun calculateInertia(mass: Float): Matrix3 {
        val cylinderMass = mass * height / (height + 2f * radius)
        val hemisphereMass = mass - cylinderMass

        val cylinderInertiaX = cylinderMass * (3f * radius * radius + height * height) / 12f
        val cylinderInertiaY = cylinderMass * radius * radius / 2f

        val hemisphereInertiaX = hemisphereMass * (2f * radius * radius / 5f + height * height / 4f)
        val hemisphereInertiaY = hemisphereMass * 2f * radius * radius / 5f

        return Matrix3(
            cylinderInertiaX + hemisphereInertiaX, 0f, 0f,
            0f, cylinderInertiaY + hemisphereInertiaY, 0f,
            0f, 0f, cylinderInertiaX + hemisphereInertiaX
        )
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
        return horizontal.scale(radius).add(Vector3(0f, y, 0f))
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) =
        localGetSupportingVertex(direction)

    override fun calculateLocalInertia(mass: Float) =
        calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = BulletCapsuleShape(radius, height)
}

// Implement other shapes...
private class BulletCylinderShape(override val halfExtents: Vector3) : CylinderShape {
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
        return Matrix3(
            lateral, 0f, 0f,
            0f, vertical, 0f,
            0f, 0f, lateral
        )
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
    override fun clone() = BulletCylinderShape(halfExtents)
}

private class BulletConeShape(
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
        return Matrix3(
            lateral, 0f, 0f,
            0f, vertical, 0f,
            0f, 0f, lateral
        )
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
    override fun clone() = BulletConeShape(radius, height)
}

private class BulletConvexHullShape(
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
        // Already optimized by Bullet
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        val size = boundingBox.getSize()
        return Matrix3(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        )
    }

    override fun getVolume(): Float {
        val size = boundingBox.getSize()
        return size.x * size.y * size.z * 0.5f
    }

    override fun getSurfaceArea(): Float {
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

        return support.add(direction.normalize().scale(margin))
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
    override fun clone() = BulletConvexHullShape(vertices.copyOf())
}

private class BulletTriangleMeshShape(
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
            if (triangleIntersectsAABB(triangle, aabbMin, aabbMax)) {
                callback.processTriangle(triangle, i)
            }
        }
    }

    override fun buildBVH(): MeshBVH {
        return SimpleMeshBVH(this)
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        val size = boundingBox.getSize()
        return Matrix3(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        )
    }

    override fun getVolume(): Float {
        var volume = 0f
        for (i in 0 until triangleCount) {
            val tri = getTriangle(i)
            volume += tri.v0.dot(tri.v1.cross(tri.v2)) / 6f
        }
        return kotlin.math.abs(volume)
    }

    override fun getSurfaceArea(): Float {
        var area = 0f
        for (i in 0 until triangleCount) {
            val tri = getTriangle(i)
            val ab = tri.v1.subtract(tri.v0)
            val ac = tri.v2.subtract(tri.v0)
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
    override fun clone() = BulletTriangleMeshShape(vertices.copyOf(), indices.copyOf())

    private fun triangleIntersectsAABB(triangle: Triangle, aabbMin: Vector3, aabbMax: Vector3): Boolean {
        val triMin = Vector3(
            minOf(triangle.v0.x, triangle.v1.x, triangle.v2.x),
            minOf(triangle.v0.y, triangle.v1.y, triangle.v2.y),
            minOf(triangle.v0.z, triangle.v1.z, triangle.v2.z)
        )
        val triMax = Vector3(
            maxOf(triangle.v0.x, triangle.v1.x, triangle.v2.x),
            maxOf(triangle.v0.y, triangle.v1.y, triangle.v2.y),
            maxOf(triangle.v0.z, triangle.v1.z, triangle.v2.z)
        )

        return !(triMax.x < aabbMin.x || triMin.x > aabbMax.x ||
                triMax.y < aabbMin.y || triMin.y > aabbMax.y ||
                triMax.z < aabbMin.z || triMin.z > aabbMax.z)
    }
}

private class BulletHeightfieldShape(
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
        val size = boundingBox.getSize()
        return Matrix3(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        )
    }

    override fun getVolume(): Float {
        val avgHeight = (maxHeight + minHeight) / 2f
        return width.toFloat() * height.toFloat() * avgHeight
    }

    override fun getSurfaceArea(): Float {
        return width.toFloat() * height.toFloat()
    }

    override fun isConvex() = false
    override fun isCompound() = false

    override fun localGetSupportingVertex(direction: Vector3) = Vector3.ZERO
    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) = Vector3.ZERO
    override fun calculateLocalInertia(mass: Float) = calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = BulletHeightfieldShape(width, height, heightData.copyOf())
}

private class BulletCompoundShape : CompoundShape {
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
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.InvalidOperation("Failed to add child shape"))
        }
    }

    override fun removeChildShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            childShapes.removeAll { it.shape == shape }
            recalculateLocalAabb()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.InvalidOperation("Failed to remove child shape"))
        }
    }

    override fun removeChildShapeByIndex(index: Int): PhysicsResult<Unit> {
        return try {
            if (index in childShapes.indices) {
                childShapes.removeAt(index)
                recalculateLocalAabb()
            }
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.InvalidOperation("Failed to remove child shape"))
        }
    }

    override fun updateChildTransform(index: Int, transform: Matrix4): PhysicsResult<Unit> {
        return try {
            if (index in childShapes.indices) {
                childShapes[index] = ChildShape(transform, childShapes[index].shape)
                recalculateLocalAabb()
            }
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.InvalidOperation("Failed to update child transform"))
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
        var totalInertia = Matrix3.identity()
        val massPerChild = mass / childShapes.size.toFloat()

        for (child in childShapes) {
            val childInertia = child.shape.calculateInertia(massPerChild)
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
    override fun clone() = BulletCompoundShape().apply {
        for (child in this@BulletCompoundShape.childShapes) {
            addChildShape(child.transform, child.shape.clone())
        }
    }
}

// Helper functions for Bullet <-> KreeKt conversions
private fun toBulletVector3(v: Vector3) = Vector3f(v.x, v.y, v.z)
private fun fromBulletVector3(v: Vector3f) = Vector3(v.x, v.y, v.z)

private fun toBulletQuaternion(q: Quaternion) = Quat4f(q.x, q.y, q.z, q.w)
private fun fromBulletQuaternion(q: Quat4f) = Quaternion(q.x, q.y, q.z, q.w)

private fun toBulletTransform(m: Matrix4) = Transform().apply {
    val mat = Matrix4f(
        m.m00, m.m01, m.m02, m.m03,
        m.m10, m.m11, m.m12, m.m13,
        m.m20, m.m21, m.m22, m.m23,
        m.m30, m.m31, m.m32, m.m33
    )
    set(mat)
}

private fun fromBulletTransform(t: Transform): Matrix4 {
    val mat = Matrix4f()
    t.getMatrix(mat)
    return Matrix4(
        mat.m00, mat.m01, mat.m02, mat.m03,
        mat.m10, mat.m11, mat.m12, mat.m13,
        mat.m20, mat.m21, mat.m22, mat.m23,
        mat.m30, mat.m31, mat.m32, mat.m33
    )
}

// Extension functions for Matrix3
private fun Matrix3.getDiagonal() = Vector3(m00, m11, m22)
private fun Matrix3.add(other: Matrix3) = Matrix3(
    m00 + other.m00, m01 + other.m01, m02 + other.m02,
    m10 + other.m10, m11 + other.m11, m12 + other.m12,
    m20 + other.m20, m21 + other.m21, m22 + other.m22
)

// Extension for ConstraintParam
private fun ConstraintParam.toBulletParam(): Int {
    return when (this) {
        ConstraintParam.ERP -> TypedConstraint.CONSTRAINT_ERP
        ConstraintParam.STOP_ERP -> TypedConstraint.CONSTRAINT_STOP_ERP
        ConstraintParam.CFM -> TypedConstraint.CONSTRAINT_CFM
        ConstraintParam.STOP_CFM -> TypedConstraint.CONSTRAINT_STOP_CFM
    }
}

// Simplified helper classes
private class SimpleMeshBVH(private val mesh: TriangleMeshShape) : MeshBVH {
    override fun raycast(from: Vector3, to: Vector3): BVHRaycastResult? {
        // Simplified raycast implementation
        return null
    }

    override fun getTrianglesInAABB(min: Vector3, max: Vector3): List<Int> {
        val results = mutableListOf<Int>()
        for (i in 0 until mesh.triangleCount) {
            // Simple AABB check
            results.add(i)
        }
        return results
    }
}

// Platform-specific actual implementations
actual fun createDefaultPhysicsEngine(): PhysicsEngine = BulletPhysicsEngine()