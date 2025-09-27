/**
 * Physics types and data structures for the physics system
 * Contains enums, data classes, and interfaces referenced by physics implementations
 */
package io.kreekt.physics

import io.kreekt.core.math.*
import kotlin.math.*

/**
 * Enums for physics system
 */
enum class RigidBodyType {
    DYNAMIC, KINEMATIC, STATIC
}

enum class ActivationState {
    ACTIVE, DEACTIVATED, WANTS_DEACTIVATION, DISABLE_DEACTIVATION, DISABLE_SIMULATION
}

enum class ShapeType {
    BOX, SPHERE, CAPSULE, CYLINDER, CONE,
    CONVEX_HULL, TRIANGLE_MESH, HEIGHTFIELD,
    COMPOUND, PLANE, EMPTY
}

enum class BroadphaseType {
    SIMPLE, AXIS_SWEEP_3, DBVT, SAP,
    DYNAMIC_AABB_TREE,
    SORT_AND_SWEEP,
    HASH_GRID,
    SPATIAL_HASH
}

enum class ConstraintParam {
    ERP, STOP_ERP, CFM, STOP_CFM,
    LINEAR_LOWER_LIMIT, LINEAR_UPPER_LIMIT,
    ANGULAR_LOWER_LIMIT, ANGULAR_UPPER_LIMIT,
    TARGET_VELOCITY, MAX_MOTOR_FORCE
}

enum class CombineMode {
    AVERAGE, MINIMUM, MAXIMUM, MULTIPLY
}

/**
 * Data structures for physics system
 */
data class ChildShape(
    val transform: Matrix4,
    val shape: CollisionShape
)

data class Triangle(
    val vertex0: Vector3,
    val vertex1: Vector3,
    val vertex2: Vector3
)

data class MeshBVH(
    val nodes: List<BVHNode>,
    val triangles: List<Triangle>
)

data class BVHNode(
    val bounds: Box3,
    val leftChild: Int,
    val rightChild: Int,
    val triangleOffset: Int,
    val triangleCount: Int
)

data class ConstraintInfo(
    var m_numIterations: Int,
    var m_tau: Float,
    var m_damping: Float,
    var m_impulseClamp: Float
)

/**
 * Callback interfaces
 */
interface TriangleCallback {
    fun processTriangle(triangle: Triangle, partId: Int, triangleIndex: Int)
}

interface CollisionCallback {
    fun onContactAdded(contact: ContactInfo): Boolean
    fun onContactProcessed(contact: ContactInfo): Boolean
    fun onContactDestroyed(contact: ContactInfo)
}

interface ContactInfo {
    val objectA: CollisionObject
    val objectB: CollisionObject
    val worldPosA: Vector3
    val worldPosB: Vector3
    val normalWorldOnB: Vector3
    val distance: Float
    val impulse: Float
    val friction: Float
    val restitution: Float
}

interface RaycastResult {
    val hasHit: Boolean
    val hitObject: CollisionObject?
    val hitPoint: Vector3
    val hitNormal: Vector3
    val hitFraction: Float
    val distance: Float
}

/**
 * Collision contact information
 */
data class CollisionContact(
    val bodyA: RigidBody,
    val bodyB: RigidBody,
    val point: Vector3,
    val normal: Vector3,
    val distance: Float = 0f,
    val impulse: Float = 0f
)

/**
 * Physics material data class
 */
data class PhysicsMaterial(
    val friction: Float = 0.5f,
    val restitution: Float = 0.0f,
    val rollingFriction: Float = 0.0f,
    val spinningFriction: Float = 0.0f,
    val frictionCombineMode: CombineMode = CombineMode.AVERAGE,
    val restitutionCombineMode: CombineMode = CombineMode.AVERAGE
)

/**
 * Result types for physics operations
 */
sealed class PhysicsOperationResult<T> {
    data class Success<T>(val value: T) : PhysicsOperationResult<T>()
    data class Error<T>(val exception: PhysicsException) : PhysicsOperationResult<T>()
}

// Typealias for backward compatibility
typealias PhysicsResult<T> = PhysicsOperationResult<T>

/**
 * Exception types for physics system
 */
sealed class PhysicsException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class WorldCreationFailed(message: String, cause: Throwable? = null) : PhysicsException(message, cause)
    class EngineError(message: String, cause: Throwable? = null) : PhysicsException(message, cause)
    class BodyCreationFailed(message: String, cause: Throwable? = null) : PhysicsException(message, cause)
    class ShapeCreationFailed(message: String, cause: Throwable? = null) : PhysicsException(message, cause)
    class ConstraintCreationFailed(message: String, cause: Throwable? = null) : PhysicsException(message, cause)
    class SimulationFailed(message: String, cause: Throwable? = null) : PhysicsException(message, cause)
    class InvalidParameters(message: String) : PhysicsException(message)
    class UnsupportedOperation(operation: String) : PhysicsException("Unsupported operation: $operation")
    class EngineNotInitialized() : PhysicsException("Physics engine not initialized")
    class InvalidOperation(message: String) : PhysicsException(message)
    class SimulationError(message: String, cause: Throwable? = null) : PhysicsException(message, cause)
}

/**
 * Extension functions for Vector3 to support physics operations
 */
fun Vector3.componentAt(index: Int): Float = when (index) {
    0 -> x
    1 -> y
    2 -> z
    else -> throw IndexOutOfBoundsException("Vector3 index must be 0, 1, or 2")
}

fun Vector3.maxComponent(): Float = maxOf(x, maxOf(y, z))

fun Vector3.coerceLength(minLength: Float, maxLength: Float): Vector3 {
    val currentLength = length()
    return when {
        currentLength < minLength -> if (currentLength > 0f) normalized() * minLength else Vector3.ZERO
        currentLength > maxLength -> normalized() * maxLength
        else -> this
    }
}

/**
 * Extension functions for Quaternion to support physics operations
 */
fun Quaternion.toAxisAngle(): Pair<Vector3, Float> {
    val length = sqrt(x * x + y * y + (z * z))
    return if (length > 0.001f) {
        val angle = 2f * acos(w.coerceIn(-1f, 1f))
        val axis = Vector3(x / length, y / length, z / length)
        Pair(axis, angle)
    } else {
        Pair(Vector3.UNIT_Y, 0f)
    }
}

fun Quaternion.toEulerAngles(): Vector3 {
    // Convert quaternion to Euler angles (in radians)
    // Order: XYZ (Tait-Bryan angles)

    val sinr_cosp = 2f * (w * x + (y * z))
    val cosr_cosp = 1f - 2f * (x * x + (y * y))
    val roll = atan2(sinr_cosp, cosr_cosp)

    val sinp = 2f * (w * y - (z * x))
    val pitch = if (abs(sinp) >= 1f) {
        if (sinp > 0f) PI.toFloat() / 2f else -PI.toFloat() / 2f
    } else {
        asin(sinp)
    }

    val siny_cosp = 2f * (w * z + (x * y))
    val cosy_cosp = 1f - 2f * (y * y + (z * z))
    val yaw = atan2(siny_cosp, cosy_cosp)

    return Vector3(roll, pitch, yaw)
}

/**
 * Extension functions for Matrix4 to support physics operations
 */
fun Matrix4.transformDirection(direction: Vector3): Vector3 {
    // Transform direction (no translation)
    return Vector3(
        m00 * direction.x + m01 * direction.y + m02 * direction.z,
        m10 * direction.x + m11 * direction.y + m12 * direction.z,
        m20 * direction.x + m21 * direction.y + m22 * direction.z
    )
}

fun Matrix4.transformPoint(point: Vector3): Vector3 {
    // Transform point (with translation)
    return Vector3(
        m00 * point.x + m01 * point.y + m02 * point.z + m03,
        m10 * point.x + m11 * point.y + m12 * point.z + m13,
        m20 * point.x + m21 * point.y + m22 * point.z + m23
    )
}

/**
 * Physics utility functions
 */
object PhysicsUtils {

    /**
     * Calculate moment of inertia for common shapes
     */
    fun calculateBoxInertia(mass: Float, dimensions: Vector3): Matrix3 {
        val factor = mass / 12f
        return Matrix3(
            floatArrayOf(
                factor * (dimensions.y * dimensions.y + dimensions.z * dimensions.z), 0f, 0f,
                0f, factor * (dimensions.x * dimensions.x + dimensions.z * dimensions.z), 0f,
                0f, 0f, factor * (dimensions.x * dimensions.x + dimensions.y * dimensions.y)
            )
        )
    }

    fun calculateSphereInertia(mass: Float, radius: Float): Matrix3 {
        val inertia = 0.4f * mass * radius * radius
        return Matrix3(
            floatArrayOf(
                inertia, 0f, 0f,
                0f, inertia, 0f,
                0f, 0f, inertia
            )
        )
    }

    fun calculateCylinderInertia(mass: Float, radius: Float, height: Float): Matrix3 {
        val radiusSquared = radius * radius
        val heightSquared = height * height

        return Matrix3(
            floatArrayOf(
                mass * (radiusSquared * 0.25f + heightSquared / 12f), 0f, 0f,
                0f, mass * radiusSquared * 0.5f, 0f,
                0f, 0f, mass * (radiusSquared * 0.25f + heightSquared / 12f)
            )
        )
    }

    /**
     * Clamp angle to [-PI, PI] range
     */
    fun normalizeAngle(angle: Float): Float {
        var result = angle
        while (result > PI) result = result - 2f * PI.toFloat()
        while (result < -PI) result = result + 2f * PI.toFloat()
        return result
    }

    /**
     * Convert degrees to radians
     */
    fun toRadians(degrees: Float): Float = degrees * PI.toFloat() / 180f

    /**
     * Convert radians to degrees
     */
    fun toDegrees(radians: Float): Float = radians * 180f / PI.toFloat()
}