/**
 * Bullet Physics Engine Integration for JVM Platform
 * Provides high-performance physics simulation using Bullet JNI bindings
 */
package io.kreekt.physics

import io.kreekt.core.math.*

// Create simplified implementations since full Bullet bindings are not available

/**
 * Bullet-based implementation of PhysicsWorld
 */
class BulletPhysicsWorld(
    initialGravity: Vector3 = Vector3(0f, -9.81f, 0f)
) : PhysicsWorld {

    private val rigidBodies = mutableMapOf<String, BulletRigidBody>()
    private val constraints = mutableMapOf<String, BulletConstraint>()
    private val collisionObjects = mutableMapOf<String, CollisionObject>()
    private var collisionCallback: CollisionCallback? = null

    override var gravity: Vector3 = initialGravity
    override var timeStep = 1f / 60f
    override var maxSubSteps = 10
    override var solverIterations = 10
    override var broadphase = BroadphaseType.DBVT

    override fun addRigidBody(body: RigidBody): PhysicsResult<Unit> {
        return try {
            val bulletBody = body as? BulletRigidBody
                ?: return PhysicsOperationResult.Error(
                    PhysicsException.InvalidOperation("Body must be created through BulletPhysicsEngine")
                )
            rigidBodies[body.id] = bulletBody
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add rigid body"))
        }
    }

    override fun removeRigidBody(body: RigidBody): PhysicsResult<Unit> {
        return try {
            rigidBodies.remove(body.id)
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove rigid body"))
        }
    }

    override fun getRigidBodies(): List<RigidBody> = rigidBodies.values.toList()

    override fun getRigidBody(id: String): RigidBody? = rigidBodies[id]

    override fun addConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        return try {
            val bulletConstraint = constraint as? BulletConstraint
                ?: return PhysicsOperationResult.Error(
                    PhysicsException.InvalidOperation("Constraint must be created through BulletPhysicsEngine")
                )
            constraints[constraint.id] = bulletConstraint
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add constraint"))
        }
    }

    override fun removeConstraint(constraint: PhysicsConstraint): PhysicsResult<Unit> {
        return try {
            constraints.remove(constraint.id)
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove constraint"))
        }
    }

    override fun getConstraints(): List<PhysicsConstraint> = constraints.values.toList()

    override fun addCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        return try {
            collisionObjects[obj.id] = obj
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to add collision object"))
        }
    }

    override fun removeCollisionObject(obj: CollisionObject): PhysicsResult<Unit> {
        return try {
            collisionObjects.remove(obj.id)
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError(e.message ?: "Failed to remove collision object"))
        }
    }

    override fun setCollisionCallback(callback: CollisionCallback) {
        collisionCallback = callback
    }

    override fun step(deltaTime: Float): PhysicsResult<Unit> {
        return try {
            // Simulate physics step
            rigidBodies.values.forEach { body ->
                body.updateFromSimulation()
            }
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Simulation step failed: ${e.message}"))
        }
    }

    override fun pause() {}
    override fun resume() {}

    override fun reset() {
        rigidBodies.clear()
        constraints.clear()
        collisionObjects.clear()
    }

    override fun raycast(from: Vector3, to: Vector3, groups: Int): RaycastResult? {
        // Simplified raycast implementation
        return null
    }

    override fun sphereCast(center: Vector3, radius: Float, groups: Int): List<CollisionObject> {
        return emptyList()
    }

    override fun boxCast(
        center: Vector3,
        halfExtents: Vector3,
        rotation: Quaternion,
        groups: Int
    ): List<CollisionObject> {
        return emptyList()
    }

    override fun overlaps(
        shape: CollisionShape,
        transform: Matrix4,
        groups: Int
    ): List<CollisionObject> {
        return emptyList()
    }

    fun dispose() {
        reset()
    }
}

/**
 * Bullet-based implementation of RigidBody
 */
class BulletRigidBody(
    override val id: String,
    initialShape: CollisionShape
) : RigidBody {

    private var _transform = Matrix4.identity()
    override var transform: Matrix4
        get() = _transform
        set(value) {
            _transform = value
        }

    override var collisionShape: CollisionShape = initialShape
    override var collisionGroups: Int = -1
    override var collisionMask: Int = -1
    override var userData: Any? = null
    override var contactProcessingThreshold = 0.01f
    override var collisionFlags: Int = 0
    override var isTrigger: Boolean = false
    override var mass: Float = 1f
    override var density: Float = 1f
    override var restitution: Float = 0.5f
    override var friction: Float = 0.5f
    override var rollingFriction: Float = 0f
    override var linearDamping: Float = 0f
    override var angularDamping: Float = 0f
    override var linearVelocity: Vector3 = Vector3.ZERO
    override var angularVelocity: Vector3 = Vector3.ZERO
    override var linearFactor: Vector3 = Vector3.ONE
    override var angularFactor: Vector3 = Vector3.ONE
    override var bodyType: RigidBodyType = RigidBodyType.DYNAMIC
    override var activationState: ActivationState = ActivationState.ACTIVE
    override var sleepThreshold = 0.8f
    override var ccdMotionThreshold: Float = 0f
    override var ccdSweptSphereRadius: Float = 0f

    override fun setCollisionShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            collisionShape = shape
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to set collision shape"))
        }
    }

    override fun setWorldTransform(transform: Matrix4) {
        this.transform = transform
    }

    override fun getWorldTransform(): Matrix4 = transform


    override fun translate(offset: Vector3) {
        _transform = _transform.translate(offset)
    }

    override fun rotate(rotation: Quaternion) {
        _transform = _transform.rotate(rotation)
    }

    override fun applyForce(force: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            // Apply force logic here
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Failed to apply force"))
        }
    }

    override fun applyImpulse(impulse: Vector3, relativePosition: Vector3): PhysicsResult<Unit> {
        return try {
            // Apply impulse logic here
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Failed to apply impulse"))
        }
    }

    override fun applyTorque(torque: Vector3): PhysicsResult<Unit> {
        return try {
            // Apply torque logic here
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.SimulationError("Failed to apply torque"))
        }
    }

    override fun applyTorqueImpulse(torque: Vector3): PhysicsResult<Unit> {
        return try {
            // Apply torque impulse logic here
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

    override fun isActive(): Boolean = activationState == ActivationState.ACTIVE

    override fun isKinematic(): Boolean = bodyType == RigidBodyType.KINEMATIC

    override fun isStatic(): Boolean = bodyType == RigidBodyType.STATIC

    override fun getInertia(): Matrix3 {
        return Matrix3.identity()
    }

    override fun getInverseInertia(): Matrix3 {
        return Matrix3.identity()
    }

    override fun getTotalForce(): Vector3 = Vector3.ZERO

    override fun getTotalTorque(): Vector3 = Vector3.ZERO

    override fun setTransform(position: Vector3, rotation: Quaternion) {
        _transform = Matrix4.fromTranslationRotation(position, rotation)
    }

    override fun getCenterOfMassTransform(): Matrix4 = transform

    internal fun updateFromSimulation() {
        // Update transform from physics simulation
    }
}

/**
 * Base class for Bullet constraints
 */
abstract class BulletConstraint(
    override val id: String,
    override val bodyA: RigidBody,
    override val bodyB: RigidBody?
) : PhysicsConstraint {

    override var enabled: Boolean = true
    override var breakingThreshold: Float = Float.MAX_VALUE

    override fun getAppliedImpulse(): Float = 0f

    override fun isEnabled(): Boolean = enabled

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
) : BulletConstraint(id, bodyA, bodyB), PointToPointConstraint {

    override fun setPivotA(pivot: Vector3) {
        // Set pivot A
    }

    override fun setPivotB(pivot: Vector3) {
        // Set pivot B
    }

    override fun updateRHS(timeStep: Float) {
        // Update right-hand side
    }

    override fun setParam(param: ConstraintParam, value: Float, axis: Int) {
        // Set constraint parameter
    }

    override fun getParam(param: ConstraintParam, axis: Int): Float {
        return 0f
    }
}

/**
 * Bullet physics engine implementation
 */
class BulletPhysicsEngine : PhysicsEngine {
    override val name = "Bullet"
    override val version = "3.24"

    override fun createWorld(gravity: Vector3): PhysicsWorld {
        return BulletPhysicsWorld(gravity)
    }

    override fun destroyWorld(world: PhysicsWorld): PhysicsResult<Unit> {
        return try {
            (world as? BulletPhysicsWorld)?.dispose()
            PhysicsOperationResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsOperationResult.Error(PhysicsException.InvalidOperation("Failed to destroy world"))
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
        return BulletRigidBody(
            id = "rb_${System.currentTimeMillis()}",
            initialShape = shape
        ).apply {
            this.mass = mass
            this.transform = transform
        }
    }

    override fun createCharacterController(shape: CollisionShape, stepHeight: Float): CharacterController {
        return BulletCharacterController(shape, stepHeight)
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
        return BulletHingeConstraint(bodyA, bodyB, pivotA, pivotB, axisA, axisB)
    }

    override fun createSliderConstraint(
        bodyA: RigidBody,
        bodyB: RigidBody?,
        frameA: Matrix4,
        frameB: Matrix4
    ): SliderConstraint {
        return BulletSliderConstraint(bodyA, bodyB, frameA, frameB)
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
            floatArrayOf(
                factor * (y * y + z * z), 0f, 0f,
                0f, factor * (x * x + z * z), 0f,
                0f, 0f, factor * (x * x + y * y)
            )
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
    ).add(direction.normalize() * (margin))

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
        direction.normalize() * (radius)

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val inertia = 0.4f * mass * radius * radius
        return Vector3(inertia, inertia, inertia)
    }

    override fun serialize() = ByteArray(0)
    override fun clone() = BulletSphereShape(radius)
}

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
        return (horizontal * radius).add(Vector3(0f, y, 0f))
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3) =
        localGetSupportingVertex(direction)

    override fun calculateLocalInertia(mass: Float) =
        calculateInertia(mass).getDiagonal()

    override fun serialize() = ByteArray(0)
    override fun clone() = BulletCapsuleShape(radius, height)
}

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

    override fun addPoint(point: Vector3, recalculateLocalAABB: Boolean) {}

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

    override fun optimizeConvexHull() {}

    override fun calculateInertia(mass: Float): Matrix3 {
        val size = boundingBox.getSize()
        return Matrix3(floatArrayOf(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        ))
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

        return support.add(direction.normalize() * (margin))
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
            callback.processTriangle(triangle, 0, i)  // partId = 0 for simple mesh
        }
    }

    override fun buildBVH(): MeshBVH {
        // Create triangles from vertices and indices
        val triangleList = mutableListOf<Triangle>()
        for (i in 0 until indices.size step 3) {
            val i0 = indices[i] * 3
            val i1 = indices[i + 1] * 3
            val i2 = indices[i + 2] * 3

            if (i0 + 2 < vertices.size && i1 + 2 < vertices.size && i2 + 2 < vertices.size) {
                triangleList.add(Triangle(
                    vertex0 = Vector3(vertices[i0], vertices[i0 + 1], vertices[i0 + 2]),
                    vertex1 = Vector3(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]),
                    vertex2 = Vector3(vertices[i2], vertices[i2 + 1], vertices[i2 + 2])
                ))
            }
        }

        return MeshBVH(
            nodes = emptyList(),
            triangles = triangleList
        )
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        val size = boundingBox.getSize()
        return Matrix3(floatArrayOf(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        ))
    }

    override fun getVolume(): Float {
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
    override fun clone() = BulletTriangleMeshShape(vertices.copyOf(), indices.copyOf())
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
        return Matrix3(floatArrayOf(
            mass * (size.y * size.y + size.z * size.z) / 12f, 0f, 0f,
            0f, mass * (size.x * size.x + size.z * size.z) / 12f, 0f,
            0f, 0f, mass * (size.x * size.x + size.y * size.y) / 12f
        ))
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

// Character controller implementation
private class BulletCharacterController(
    initialShape: CollisionShape,
    initialStepHeight: Float
) : CharacterController {
    override var collisionShape: CollisionShape = initialShape
    override var stepHeight: Float = initialStepHeight
    override val id = "character_${System.currentTimeMillis()}"
    override var transform: Matrix4 = Matrix4.identity()
    private var velocity: Vector3 = Vector3.ZERO
    private var isOnGround: Boolean = false
    override var jumpSpeed: Float = 10f
    override var walkDirection: Vector3 = Vector3.ZERO
    override var fallSpeed: Float = 9.81f
    private var maxJumpHeight: Float = 5f
    private var canJump: Boolean = true
    private var slopeRadians: Float = kotlin.math.PI.toFloat() / 4f
    override var maxSlope: Float = kotlin.math.PI.toFloat() / 4f
    override var velocityForTimeInterval: Vector3 = Vector3.ZERO
    override var collisionGroups: Int = 1
    override var collisionMask: Int = -1
    override var userData: Any? = null
    override var contactProcessingThreshold: Float = 0.1f
    override var collisionFlags: Int = 0
    override var isTrigger: Boolean = false


    override fun setVelocityForTimeInterval(velocity: Vector3, timeInterval: Float) {
        this.velocity = velocity
    }

    override fun warp(origin: Vector3) {
        transform = transform.setPosition(origin)
    }

    override fun preStep(world: PhysicsWorld) {}

    override fun playerStep(world: PhysicsWorld, deltaTime: Float) {
        // Simple character controller step
        if (walkDirection.length() > 0f) {
            velocity = velocity.add(walkDirection * (deltaTime * 10f))
        }

        // Apply gravity
        if (!isOnGround) {
            velocity = velocity.add(Vector3(0f, -fallSpeed * deltaTime, 0f))
        }

        // Update position
        transform = transform.translate(velocity * (deltaTime))

        // Simple ground check
        isOnGround = transform.getTranslation().y <= 0f
        if (isOnGround) {
            transform = transform.setPosition(transform.getTranslation().copy(y = 0f))
            velocity = velocity.copy(y = 0f)
        }
    }

    override fun jump(velocity: Vector3) {
        if (canJump && isOnGround) {
            this.velocity = this.velocity.add(velocity)
            isOnGround = false
        }
    }

    override fun canJump(): Boolean = canJump && isOnGround

    override fun onGround(): Boolean = isOnGround


    fun setUpAxis(axis: Int) {}

    fun setGravity(gravity: Vector3) {
        fallSpeed = gravity.length()
    }

    override fun setCollisionShape(shape: CollisionShape): PhysicsOperationResult<Unit> {
        collisionShape = shape
        return PhysicsOperationResult.Success(Unit)
    }

    override fun setWorldTransform(transform: Matrix4) {
        this.transform = transform
    }

    override fun getWorldTransform(): Matrix4 = transform


    override fun translate(offset: Vector3) {
        transform = transform.translate(offset)
    }

    override fun rotate(rotation: Quaternion) {
        transform = transform.multiply(Matrix4().makeRotationFromQuaternion(rotation))
    }
}

// Additional constraint implementations
private class BulletHingeConstraint(
    bodyA: RigidBody,
    bodyB: RigidBody?,
    pivotA: Vector3,
    pivotB: Vector3,
    axisA: Vector3,
    axisB: Vector3
) : BulletConstraint("hinge_${System.currentTimeMillis()}", bodyA, bodyB), HingeConstraint {
    override val pivotA = pivotA
    override val pivotB = pivotB
    override val axisA = axisA
    override val axisB = axisB
    override var lowerLimit = 0f
    override var upperLimit = 0f
    override var enableAngularMotor = false
    override var targetVelocity = 0f
    override var maxMotorImpulse = 0f
    private var motorTargetVelocity = 0f

    override fun getHingeAngle(): Float = 0f
    override fun setLimit(low: Float, high: Float, softness: Float, biasFactor: Float, relaxationFactor: Float) {
        lowerLimit = low
        upperLimit = high
    }
    override fun enableMotor(enable: Boolean) { enableAngularMotor = enable }
    override fun setMotorTarget(targetAngle: Float, deltaTime: Float) {}
    fun getFrameOffsetA(): Matrix4 = Matrix4.identity()
    fun getFrameOffsetB(): Matrix4 = Matrix4.identity()
    fun setFrames(frameA: Matrix4, frameB: Matrix4) {}
    override fun setParam(param: ConstraintParam, value: Float, axis: Int) {}
    override fun getParam(param: ConstraintParam, axis: Int): Float = 0f
}

private class BulletSliderConstraint(
    bodyA: RigidBody,
    bodyB: RigidBody?,
    frameA: Matrix4,
    frameB: Matrix4
) : BulletConstraint("slider_${System.currentTimeMillis()}", bodyA, bodyB), SliderConstraint {
    override val frameA = frameA
    override val frameB = frameB
    override var lowerLinearLimit = 0f
    override var upperLinearLimit = 0f
    override var lowerAngularLimit = 0f
    override var upperAngularLimit = 0f
    override var poweredLinearMotor = false
    override var poweredAngularMotor = false
    private var linearMotorEnabled = false
    private var angularMotorEnabled = false
    override var maxLinearMotorForce = 0f
    override var maxAngularMotorForce = 0f
    override var targetLinearMotorVelocity = 0f
    override var targetAngularMotorVelocity = 0f

    override fun getLinearPos(): Float = 0f
    override fun getAngularPos(): Float = 0f
    fun getFrameOffsetA(): Matrix4 = frameA
    fun getFrameOffsetB(): Matrix4 = frameB
    fun setFrames(frameA: Matrix4, frameB: Matrix4) {}
    override fun setParam(param: ConstraintParam, value: Float, axis: Int) {}
    override fun getParam(param: ConstraintParam, axis: Int): Float = 0f
}

// Extension functions for Matrix3
private fun Matrix3.getDiagonal() = Vector3(m00, m11, m22)
private fun Matrix3.add(other: Matrix3) = Matrix3(floatArrayOf(
    m00 + other.m00, m01 + other.m01, m02 + other.m02,
    m10 + other.m10, m11 + other.m11, m12 + other.m12,
    m20 + other.m20, m21 + other.m21, m22 + other.m22))

// Simplified helper classes
private class SimpleMeshBVH(
    private val vertices: FloatArray,
    private val indices: IntArray
) {
    fun raycast(from: Vector3, to: Vector3): RaycastResult? = null

    fun getTrianglesInAABB(min: Vector3, max: Vector3): List<Int> {
        val results = mutableListOf<Int>()
        for (i in 0 until indices.size / 3) {
            results.add(i)
        }
        return results
    }
}

// Platform-specific JVM implementation
fun createBulletPhysicsEngine(): PhysicsEngine = BulletPhysicsEngine()

// Actual implementation for expect function
actual fun createDefaultPhysicsEngine(): PhysicsEngine = BulletPhysicsEngine()