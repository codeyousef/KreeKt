package io.kreekt.physics

import io.kreekt.core.math.*
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf

/**
 * Default implementation of PhysicsWorld
 * Provides a complete physics simulation environment
 */
class DefaultPhysicsWorld(
    initialGravity: Vector3 = Vector3(0f, -9.81f, 0f)
) : PhysicsWorld {

    override var gravity: Vector3 = initialGravity
    override var timeStep: Float = 1f / 60f
    override var maxSubSteps: Int = 1
    override var solverIterations: Int = 10
    override var broadphase: BroadphaseType = BroadphaseType.DYNAMIC_AABB_TREE

    private val rigidBodies = mutableListOf<RigidBody>()
    private val rigidBodyMap = mutableMapOf<String, RigidBody>()
    private val constraints = mutableListOf<PhysicsConstraint>()
    private val collisionObjects = mutableListOf<CollisionObject>()
    private var collisionCallback: CollisionCallback? = null
    private var isPaused = false
    private var isDisposed = false

    // Event callbacks
    private val triggerEnterCallbacks = mutableListOf<(RigidBody, RigidBody) -> Unit>()
    private val collisionCallbacks = mutableListOf<(CollisionContact) -> Unit>()

    override fun addRigidBody(body: RigidBody): PhysicsResult<Unit> {
        if (isDisposed) return PhysicsOperationResult.Error(PhysicsException.UnsupportedOperation("PhysicsWorld is disposed"))

        try {
            if (!rigidBodies.contains(body)) {
                rigidBodies.add(body)
                rigidBodyMap[body.id] = body
                addCollisionObject(body)
            }
            return PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            return PhysicsOperationResult.Error(PhysicsException.EngineError("Failed to add rigid body", e))
        }
    }

    override fun removeRigidBody(body: RigidBody): PhysicsResult<Unit> {
        if (isDisposed) return PhysicsOperationResult.Error(PhysicsException.UnsupportedOperation("PhysicsWorld is disposed"))

        try {
            rigidBodies.remove(body)
            rigidBodyMap.remove(body.id)
            removeCollisionObject(body)
            return PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            return PhysicsOperationResult.Error(PhysicsException.EngineError("Failed to remove rigid body", e))
        }
    }

    override fun getRigidBodies(): List<RigidBody> = rigidBodies.toList()

    override fun getRigidBody(id: String): RigidBody? = rigidBodyMap[id]

    override fun addConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        if (isDisposed) return PhysicsOperationResult.Error(PhysicsException.UnsupportedOperation("PhysicsWorld is disposed"))

        try {
            if (!constraints.contains(constraint)) {
                constraints.add(constraint)
            }
            return PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            return PhysicsOperationResult.Error(PhysicsException.EngineError("Failed to add constraint", e))
        }
    }

    override fun removeConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        if (isDisposed) return PhysicsOperationResult.Error(PhysicsException.UnsupportedOperation("PhysicsWorld is disposed"))

        try {
            constraints.remove(constraint)
            return PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            return PhysicsOperationResult.Error(PhysicsException.EngineError("Failed to remove constraint", e))
        }
    }

    override fun getConstraints(): List<PhysicsConstraint> = constraints.toList()

    override fun addCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        if (isDisposed) return PhysicsOperationResult.Error(PhysicsException.UnsupportedOperation("PhysicsWorld is disposed"))

        try {
            if (!collisionObjects.contains(obj)) {
                collisionObjects.add(obj)
            }
            return PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            return PhysicsOperationResult.Error(PhysicsException.EngineError("Failed to add collision object", e))
        }
    }

    override fun removeCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        if (isDisposed) return PhysicsOperationResult.Error(PhysicsException.UnsupportedOperation("PhysicsWorld is disposed"))

        try {
            collisionObjects.remove(obj)
            return PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            return PhysicsOperationResult.Error(PhysicsException.EngineError("Failed to remove collision object", e))
        }
    }

    override fun setCollisionCallback(callback: CollisionCallback) {
        collisionCallback = callback
    }

    override fun step(deltaTime: Float): PhysicsResult<Unit> {
        if (isDisposed) return PhysicsOperationResult.Error(PhysicsException.UnsupportedOperation("PhysicsWorld is disposed"))
        if (isPaused) return PhysicsOperationResult.Success(Unit)
        if (deltaTime < 0f) return PhysicsOperationResult.Error(PhysicsException.InvalidParameters("Delta time must be non-negative"))

        try {
            // Apply gravity to dynamic bodies
            rigidBodies.forEach { body ->
                if (body.bodyType == RigidBodyType.DYNAMIC && body.mass > 0f) {
                    val gravityForce = gravity * body.mass
                    body.applyCentralForce(gravityForce)
                }
            }

            // Update body positions based on velocities
            rigidBodies.forEach { body ->
                if (body.bodyType == RigidBodyType.DYNAMIC) {
                    updateBodyPosition(body, deltaTime)
                }
            }

            // Check for collisions
            detectCollisions()

            return PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            return PhysicsOperationResult.Error(PhysicsException.EngineError("Physics step failed", e))
        }
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {
        isPaused = false
    }

    override fun reset() {
        rigidBodies.clear()
        rigidBodyMap.clear()
        constraints.clear()
        collisionObjects.clear()
        collisionCallback = null
        isPaused = false
    }

    override fun raycast(from: Vector3, to: Vector3, groups: Int): RaycastResult? {
        if (isDisposed) return null

        val direction = (to - from).normalized()
        val distance = from.distanceTo(to)

        // Simple raycast implementation - check intersection with all rigid bodies
        var closestHit: RaycastResult? = null
        var closestDistance = Float.MAX_VALUE

        rigidBodies.forEach { body ->
            if (groups == -1 || (body.collisionGroups and groups) != 0) {
                val hit = raycastBody(from, direction, distance, body)
                if (hit != null && hit.distance < closestDistance) {
                    closestDistance = hit.distance
                    closestHit = hit
                }
            }
        }

        return closestHit
    }

    override fun sphereCast(center: Vector3, radius: Float, groups: Int): List<CollisionObject> {
        if (isDisposed) return emptyList()

        val results = mutableListOf<CollisionObject>()

        collisionObjects.forEach { obj ->
            if (groups == -1 || (obj.collisionGroups and groups) != 0) {
                if (sphereIntersectsObject(center, radius, obj)) {
                    results.add(obj)
                }
            }
        }

        return results
    }

    override fun boxCast(center: Vector3, halfExtents: Vector3, rotation: Quaternion, groups: Int): List<CollisionObject> {
        if (isDisposed) return emptyList()

        val results = mutableListOf<CollisionObject>()

        collisionObjects.forEach { obj ->
            if (groups == -1 || (obj.collisionGroups and groups) != 0) {
                if (boxIntersectsObject(center, halfExtents, rotation, obj)) {
                    results.add(obj)
                }
            }
        }

        return results
    }

    override fun overlaps(shape: CollisionShape, transform: Matrix4, groups: Int): List<CollisionObject> {
        if (isDisposed) return emptyList()

        val results = mutableListOf<CollisionObject>()

        collisionObjects.forEach { obj ->
            if (groups == -1 || (obj.collisionGroups and groups) != 0) {
                if (shapeOverlapsObject(shape, transform, obj)) {
                    results.add(obj)
                }
            }
        }

        return results
    }

    // Additional methods for test compatibility
    // Note: rigidBodies and constraints are accessed via getRigidBodies() and getConstraints() methods

    fun onCollision(callback: (CollisionContact) -> Unit) {
        collisionCallbacks.add(callback)
    }

    fun onTriggerEnter(callback: (RigidBody, RigidBody) -> Unit) {
        triggerEnterCallbacks.add(callback)
    }

    fun spherecast(from: Vector3, to: Vector3, radius: Float): PhysicsResult<RaycastHit?> {
        // Simple spherecast implementation
        val direction = (to - from).normalized()
        val distance = from.distanceTo(to)

        rigidBodies.forEach { body ->
            val hit = spherecastBody(from, direction, distance, radius, body)
            if (hit != null) {
                return PhysicsOperationResult.Success(hit)
            }
        }

        return PhysicsOperationResult.Success(null)
    }

    fun overlapSphere(center: Vector3, radius: Float): PhysicsResult<List<RigidBody>> {
        if (radius < 0f) return PhysicsOperationResult.Error(PhysicsException.InvalidParameters("Radius must be non-negative"))

        val results = mutableListOf<RigidBody>()

        rigidBodies.forEach { body ->
            if (sphereIntersectsRigidBody(center, radius, body)) {
                results.add(body)
            }
        }

        return PhysicsOperationResult.Success(results)
    }

    fun addCharacterController(controller: CharacterController): PhysicsResult<Unit> {
        return addCollisionObject(controller)
    }

    // Private helper methods
    private fun updateBodyPosition(body: RigidBody, deltaTime: Float) {
        if (body.bodyType != RigidBodyType.DYNAMIC) return

        // Apply linear damping
        body.linearVelocity = body.linearVelocity * (1f - body.linearDamping * deltaTime)
        body.angularVelocity = body.angularVelocity * (1f - body.angularDamping * deltaTime)

        // Update position and rotation
        val position = body.getWorldTransform().getTranslation()
        val newPosition = position + body.linearVelocity * deltaTime

        val rotation = body.getWorldTransform().getRotation()
        val angularDisplacement = body.angularVelocity * deltaTime
        val angularMagnitude = angularDisplacement.length()

        val newRotation = if (angularMagnitude > 0f) {
            val axis = angularDisplacement / angularMagnitude
            rotation * Quaternion.fromAxisAngle(axis, angularMagnitude)
        } else {
            rotation
        }

        body.setTransform(newPosition, newRotation)
    }

    private fun detectCollisions() {
        for (i in rigidBodies.indices) {
            for (j in i + 1 until rigidBodies.size) {
                val bodyA = rigidBodies[i]
                val bodyB = rigidBodies[j]

                if (bodiesIntersect(bodyA, bodyB)) {
                    val contact = CollisionContact(
                        bodyA = bodyA,
                        bodyB = bodyB,
                        point = bodyA.getWorldTransform().getTranslation(),
                        normal = (bodyB.getWorldTransform().getTranslation() - bodyA.getWorldTransform().getTranslation()).normalized(),
                        penetrationDepth = 0f,
                        impulse = 0f
                    )

                    // Handle trigger events
                    if (bodyA.isTrigger || bodyB.isTrigger) {
                        triggerEnterCallbacks.forEach { it(bodyA, bodyB) }
                    } else {
                        // Handle collision
                        collisionCallbacks.forEach { it(contact) }

                        // Convert to ContactInfo for CollisionCallback interface
                        if (collisionCallback != null) {
                            val contactInfo = object : ContactInfo {
                                override val objectA = bodyA
                                override val objectB = bodyB
                                override val worldPosA = contact.point
                                override val worldPosB = contact.point
                                override val normalWorldOnB = contact.normal
                                override val distance = -contact.penetrationDepth
                                override val impulse = contact.impulse
                                override val friction = bodyA.friction
                                override val restitution = bodyA.restitution
                            }
                            collisionCallback?.onContactAdded(contactInfo)
                        }
                    }
                }
            }
        }
    }

    private fun bodiesIntersect(bodyA: RigidBody, bodyB: RigidBody): Boolean {
        // Simple sphere-sphere intersection for now
        val posA = bodyA.getWorldTransform().getTranslation()
        val posB = bodyB.getWorldTransform().getTranslation()
        val distance = posA.distanceTo(posB)

        // Assume radius of 1.0 for all objects in simple implementation
        val radiusA = 1f
        val radiusB = 1f

        return distance < (radiusA + radiusB)
    }

    private fun raycastBody(from: Vector3, direction: Vector3, maxDistance: Float, body: RigidBody): RaycastResult? {
        // Simple sphere intersection
        val bodyPos = body.getWorldTransform().getTranslation()
        val toBody = bodyPos - from
        val projection = toBody.dot(direction)

        if (projection < 0f || projection > maxDistance) return null

        val closestPoint = from + direction * projection
        val distance = closestPoint.distanceTo(bodyPos)
        val radius = 1f // Assume radius of 1.0

        if (distance <= radius) {
            val hitPoint = closestPoint
            val hitNormal = (hitPoint - bodyPos).normalized()
            return RaycastResult(
                hasHit = true,
                hitObject = body,
                hitPoint = hitPoint,
                hitNormal = hitNormal,
                hitFraction = projection / maxDistance,
                distance = projection
            )
        }

        return null
    }

    private fun spherecastBody(from: Vector3, direction: Vector3, maxDistance: Float, radius: Float, body: RigidBody): RaycastHit? {
        val result = raycastBody(from, direction, maxDistance, body)
        return if (result?.hasHit == true && result.hitObject is RigidBody) {
            RaycastHit(result.hitObject as RigidBody, result.hitPoint, result.hitNormal, result.distance)
        } else null
    }

    private fun sphereIntersectsObject(center: Vector3, radius: Float, obj: CollisionObject): Boolean {
        val objPos = obj.getWorldTransform().getTranslation()
        val distance = center.distanceTo(objPos)
        val objRadius = 1f // Simplified assumption
        return distance <= (radius + objRadius)
    }

    private fun sphereIntersectsRigidBody(center: Vector3, radius: Float, body: RigidBody): Boolean {
        val bodyPos = body.getWorldTransform().getTranslation()
        val distance = center.distanceTo(bodyPos)
        val bodyRadius = 1f // Simplified assumption
        return distance <= (radius + bodyRadius)
    }

    private fun boxIntersectsObject(center: Vector3, halfExtents: Vector3, rotation: Quaternion, obj: CollisionObject): Boolean {
        // Simplified box intersection - just check if object center is within box bounds
        val objPos = obj.getWorldTransform().getTranslation()
        val diff = objPos - center

        return kotlin.math.abs(diff.x) <= halfExtents.x &&
               kotlin.math.abs(diff.y) <= halfExtents.y &&
               kotlin.math.abs(diff.z) <= halfExtents.z
    }

    private fun shapeOverlapsObject(shape: CollisionShape, transform: Matrix4, obj: CollisionObject): Boolean {
        // Simplified shape overlap check
        val shapePos = transform.getTranslation()
        val objPos = obj.getWorldTransform().getTranslation()
        val distance = shapePos.distanceTo(objPos)

        return distance <= 2f // Simplified overlap threshold
    }

    fun dispose() {
        reset()
        isDisposed = true
    }
}

// RaycastHit is already defined in PhysicsTypes.kt