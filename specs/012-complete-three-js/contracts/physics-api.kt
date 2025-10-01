/**
 * Physics API Contract
 *
 * This file defines the complete API surface for the physics subsystem,
 * ensuring integration with Rapier (primary) and Bullet (fallback) physics engines.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.physics

import io.kreekt.core.math.*
import io.kreekt.physics.*
import io.kreekt.scene.*
import io.kreekt.geometry.*

// ============================================================================
// Core Physics World API
// ============================================================================

/**
 * PhysicsWorld: Main physics simulation container
 */
interface PhysicsWorldAPI {
    var gravity: Vector3
    val integrationParameters: IntegrationParameters
    val islands: IslandManager
    val broadPhase: BroadPhase
    val narrowPhase: NarrowPhase
    val bodies: RigidBodySet
    val colliders: ColliderSet
    val impulseJoints: ImpulseJointSet
    val multibodyJoints: MultibodyJointSet
    val ccdSolver: CCDSolver

    // Simulation
    fun step(eventQueue: EventQueue? = null)
    fun timestep(): Float
    fun setTimestep(timestep: Float)

    // Body management
    fun createRigidBody(desc: RigidBodyDesc): RigidBodyHandle
    fun removeRigidBody(handle: RigidBodyHandle)
    fun getRigidBody(handle: RigidBodyHandle): RigidBody?

    // Collider management
    fun createCollider(desc: ColliderDesc, bodyHandle: RigidBodyHandle?): ColliderHandle
    fun removeCollider(handle: ColliderHandle, wakeBodies: Boolean = true)
    fun getCollider(handle: ColliderHandle): Collider?

    // Joint management
    fun createImpulseJoint(desc: ImpulseJointDesc, body1: RigidBodyHandle, body2: RigidBodyHandle): ImpulseJointHandle
    fun createMultibodyJoint(desc: MultibodyJointDesc, body1: RigidBodyHandle, body2: RigidBodyHandle): MultibodyJointHandle
    fun removeImpulseJoint(handle: ImpulseJointHandle, wakeBodies: Boolean = true)
    fun removeMultibodyJoint(handle: MultibodyJointHandle, wakeBodies: Boolean = true)

    // Raycasting
    fun castRay(
        ray: Ray,
        maxToi: Float,
        solid: Boolean,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null
    ): RayColliderHit?

    fun castRayAndGetNormal(
        ray: Ray,
        maxToi: Float,
        solid: Boolean,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null
    ): RayColliderIntersection?

    fun intersectionsWithRay(
        ray: Ray,
        maxToi: Float,
        solid: Boolean,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null,
        callback: (RayColliderIntersection) -> Boolean
    )

    // Shape casting
    fun castShape(
        shape: SharedShape,
        shapePos: Vector3,
        shapeRot: Quaternion,
        shapeVel: Vector3,
        maxToi: Float,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null
    ): ShapeCastHit?

    // Intersection queries
    fun intersectionsWithPoint(
        point: Vector3,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null,
        callback: (ColliderHandle) -> Boolean
    )

    fun intersectionsWithShape(
        shape: SharedShape,
        shapePos: Vector3,
        shapeRot: Quaternion,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null,
        callback: (ColliderHandle) -> Boolean
    )

    fun intersectionsWithAabb(
        aabb: AABB,
        callback: (ColliderHandle) -> Boolean
    )

    // Contact queries
    fun contactsWith(
        collider: ColliderHandle,
        callback: (ColliderHandle) -> Boolean
    )

    fun contactPair(
        collider1: ColliderHandle,
        collider2: ColliderHandle
    ): ContactPair?
}

// ============================================================================
// Rigid Body API
// ============================================================================

/**
 * RigidBody: Dynamic, kinematic, or static physics body
 */
interface RigidBodyAPI {
    val handle: RigidBodyHandle

    // Transform
    fun translation(): Vector3
    fun setTranslation(translation: Vector3, wakeUp: Boolean = true)
    fun rotation(): Quaternion
    fun setRotation(rotation: Quaternion, wakeUp: Boolean = true)
    fun position(): Vector3
    fun setPosition(position: Vector3, wakeUp: Boolean = true)
    fun nextTranslation(): Vector3
    fun nextRotation(): Quaternion
    fun nextPosition(): Vector3
    fun setPredictedTranslation(translation: Vector3, wakeUp: Boolean = true)
    fun setPredictedRotation(rotation: Quaternion, wakeUp: Boolean = true)

    // Velocity
    fun linvel(): Vector3
    fun setLinvel(linvel: Vector3, wakeUp: Boolean = true)
    fun angvel(): Vector3
    fun setAngvel(angvel: Vector3, wakeUp: Boolean = true)

    // Damping
    fun linearDamping(): Float
    fun setLinearDamping(damping: Float)
    fun angularDamping(): Float
    fun setAngularDamping(damping: Float)

    // Mass properties
    fun mass(): Float
    fun setMass(mass: Float, wakeUp: Boolean = true)
    fun invMass(): Float
    fun localCom(): Vector3
    fun centerOfMass(): Vector3
    fun principalInertia(): Vector3
    fun principalInertiaLocalFrame(): Quaternion
    fun setAdditionalMass(mass: Float, wakeUp: Boolean = true)
    fun setAdditionalMassProperties(
        mass: Float,
        centerOfMass: Vector3,
        principalInertia: Vector3,
        inertiaFrame: Quaternion,
        wakeUp: Boolean = true
    )

    // Forces and impulses
    fun addForce(force: Vector3, wakeUp: Boolean = true)
    fun addForceAtPoint(force: Vector3, point: Vector3, wakeUp: Boolean = true)
    fun addTorque(torque: Vector3, wakeUp: Boolean = true)
    fun applyImpulse(impulse: Vector3, wakeUp: Boolean = true)
    fun applyImpulseAtPoint(impulse: Vector3, point: Vector3, wakeUp: Boolean = true)
    fun applyTorqueImpulse(torque: Vector3, wakeUp: Boolean = true)
    fun resetForces(wakeUp: Boolean = true)
    fun resetTorques(wakeUp: Boolean = true)

    // Body type
    fun bodyType(): RigidBodyType
    fun setBodyType(bodyType: RigidBodyType, wakeUp: Boolean = true)
    fun isFixed(): Boolean
    fun isDynamic(): Boolean
    fun isKinematic(): Boolean

    // Sleeping
    fun isSleeping(): Boolean
    fun isMoving(): Boolean
    fun wakeUp()
    fun sleep()
    fun isCcdEnabled(): Boolean
    fun enableCcd(enabled: Boolean)

    // Locking
    fun lockTranslations(locked: Boolean, wakeUp: Boolean = true)
    fun lockRotations(locked: Boolean, wakeUp: Boolean = true)
    fun setEnabledTranslations(x: Boolean, y: Boolean, z: Boolean, wakeUp: Boolean = true)
    fun setEnabledRotations(x: Boolean, y: Boolean, z: Boolean, wakeUp: Boolean = true)

    // Dominance
    fun dominanceGroup(): Int
    fun setDominanceGroup(group: Int)

    // Gravity
    fun gravityScale(): Float
    fun setGravityScale(scale: Float, wakeUp: Boolean = true)

    // User data
    var userData: Any?

    // Colliders
    fun numColliders(): Int
    fun collider(i: Int): ColliderHandle
}

/**
 * RigidBodyDesc: Builder for creating rigid bodies
 */
data class RigidBodyDesc(
    var bodyType: RigidBodyType = RigidBodyType.Dynamic,
    var translation: Vector3 = Vector3(0f, 0f, 0f),
    var rotation: Quaternion = Quaternion(0f, 0f, 0f, 1f),
    var linvel: Vector3 = Vector3(0f, 0f, 0f),
    var angvel: Vector3 = Vector3(0f, 0f, 0f),
    var linearDamping: Float = 0f,
    var angularDamping: Float = 0f,
    var gravityScale: Float = 1f,
    var ccdEnabled: Boolean = false,
    var canSleep: Boolean = true,
    var sleeping: Boolean = false,
    var dominanceGroup: Int = 0,
    var enabledTranslations: Vector3 = Vector3(1f, 1f, 1f),
    var enabledRotations: Vector3 = Vector3(1f, 1f, 1f),
    var userData: Any? = null
)

enum class RigidBodyType {
    Dynamic,
    Fixed,
    KinematicPositionBased,
    KinematicVelocityBased
}

// ============================================================================
// Collider API
// ============================================================================

/**
 * Collider: Shape for collision detection
 */
interface ColliderAPI {
    val handle: ColliderHandle

    // Transform (relative to parent rigid body)
    fun translation(): Vector3
    fun setTranslation(translation: Vector3)
    fun rotation(): Quaternion
    fun setRotation(rotation: Quaternion)
    fun setTranslationWrtParent(translation: Vector3)
    fun setRotationWrtParent(rotation: Quaternion)

    // Shape
    fun shape(): SharedShape
    fun setShape(shape: SharedShape)

    // Material properties
    fun friction(): Float
    fun setFriction(friction: Float)
    fun restitution(): Float
    fun setRestitution(restitution: Float)
    fun density(): Float
    fun setDensity(density: Float)
    fun mass(): Float
    fun setMass(mass: Float)
    fun volume(): Float

    // Collision groups
    fun collisionGroups(): InteractionGroups
    fun setCollisionGroups(groups: InteractionGroups)
    fun solverGroups(): InteractionGroups
    fun setSolverGroups(groups: InteractionGroups)

    // Flags
    fun isSensor(): Boolean
    fun setSensor(isSensor: Boolean)
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)

    // Parent body
    fun parent(): RigidBodyHandle?

    // Contact force events
    fun setContactForceEventThreshold(threshold: Float)
    fun contactForceEventThreshold(): Float

    // User data
    var userData: Any?

    // Collision detection
    fun computeAabb(): AABB
    fun computeMassProperties(density: Float): MassProperties
}

/**
 * ColliderDesc: Builder for creating colliders
 */
data class ColliderDesc(
    var shape: SharedShape,
    var translation: Vector3 = Vector3(0f, 0f, 0f),
    var rotation: Quaternion = Quaternion(0f, 0f, 0f, 1f),
    var friction: Float = 0.5f,
    var restitution: Float = 0f,
    var density: Float = 1f,
    var mass: Float? = null,
    var isSensor: Boolean = false,
    var collisionGroups: InteractionGroups = InteractionGroups.all(),
    var solverGroups: InteractionGroups = InteractionGroups.all(),
    var contactForceEventThreshold: Float = 0f,
    var userData: Any? = null
) {
    companion object {
        // Convenience constructors for common shapes
        fun ball(radius: Float): ColliderDesc =
            ColliderDesc(SharedShape.ball(radius))

        fun cuboid(hx: Float, hy: Float, hz: Float): ColliderDesc =
            ColliderDesc(SharedShape.cuboid(hx, hy, hz))

        fun capsule(halfHeight: Float, radius: Float): ColliderDesc =
            ColliderDesc(SharedShape.capsule(halfHeight, radius))

        fun cylinder(halfHeight: Float, radius: Float): ColliderDesc =
            ColliderDesc(SharedShape.cylinder(halfHeight, radius))

        fun cone(halfHeight: Float, radius: Float): ColliderDesc =
            ColliderDesc(SharedShape.cone(halfHeight, radius))

        fun trimesh(vertices: FloatArray, indices: IntArray): ColliderDesc =
            ColliderDesc(SharedShape.trimesh(vertices, indices))

        fun convexHull(points: FloatArray): ColliderDesc =
            ColliderDesc(SharedShape.convexHull(points))

        fun heightfield(heights: FloatArray, nrows: Int, ncols: Int, scale: Vector3): ColliderDesc =
            ColliderDesc(SharedShape.heightfield(heights, nrows, ncols, scale))
    }
}

// ============================================================================
// Shapes API
// ============================================================================

/**
 * SharedShape: Collision shape
 */
sealed class SharedShape {
    companion object {
        fun ball(radius: Float): SharedShape = Ball(radius)
        fun cuboid(hx: Float, hy: Float, hz: Float): SharedShape = Cuboid(hx, hy, hz)
        fun capsule(halfHeight: Float, radius: Float): SharedShape = Capsule(halfHeight, radius)
        fun cylinder(halfHeight: Float, radius: Float): SharedShape = Cylinder(halfHeight, radius)
        fun cone(halfHeight: Float, radius: Float): SharedShape = Cone(halfHeight, radius)
        fun trimesh(vertices: FloatArray, indices: IntArray): SharedShape = Trimesh(vertices, indices)
        fun convexHull(points: FloatArray): SharedShape = ConvexHull(points)
        fun heightfield(heights: FloatArray, nrows: Int, ncols: Int, scale: Vector3): SharedShape =
            Heightfield(heights, nrows, ncols, scale)
        fun compound(shapes: List<Pair<Vector3, SharedShape>>): SharedShape = Compound(shapes)
    }

    data class Ball(val radius: Float) : SharedShape()
    data class Cuboid(val hx: Float, val hy: Float, val hz: Float) : SharedShape()
    data class Capsule(val halfHeight: Float, val radius: Float) : SharedShape()
    data class Cylinder(val halfHeight: Float, val radius: Float) : SharedShape()
    data class Cone(val halfHeight: Float, val radius: Float) : SharedShape()
    data class Trimesh(val vertices: FloatArray, val indices: IntArray) : SharedShape()
    data class ConvexHull(val points: FloatArray) : SharedShape()
    data class Heightfield(val heights: FloatArray, val nrows: Int, val ncols: Int, val scale: Vector3) : SharedShape()
    data class Compound(val shapes: List<Pair<Vector3, SharedShape>>) : SharedShape()
}

// ============================================================================
// Joints API
// ============================================================================

/**
 * ImpulseJoint: Constraint between two bodies
 */
sealed class ImpulseJointDesc {
    data class Fixed(
        val localAnchor1: Vector3 = Vector3(0f, 0f, 0f),
        val localAnchor2: Vector3 = Vector3(0f, 0f, 0f),
        val localFrame1: Quaternion = Quaternion(0f, 0f, 0f, 1f),
        val localFrame2: Quaternion = Quaternion(0f, 0f, 0f, 1f)
    ) : ImpulseJointDesc()

    data class Revolute(
        val localAnchor1: Vector3 = Vector3(0f, 0f, 0f),
        val localAnchor2: Vector3 = Vector3(0f, 0f, 0f),
        val axis: Vector3 = Vector3(0f, 1f, 0f),
        val limits: Pair<Float, Float>? = null,
        val motorTargetVel: Float = 0f,
        val motorMaxImpulse: Float = Float.MAX_VALUE,
        val motorModel: MotorModel = MotorModel.AccelerationBased
    ) : ImpulseJointDesc()

    data class Prismatic(
        val localAnchor1: Vector3 = Vector3(0f, 0f, 0f),
        val localAnchor2: Vector3 = Vector3(0f, 0f, 0f),
        val axis: Vector3 = Vector3(0f, 1f, 0f),
        val limits: Pair<Float, Float>? = null,
        val motorTargetVel: Float = 0f,
        val motorMaxImpulse: Float = Float.MAX_VALUE,
        val motorModel: MotorModel = MotorModel.AccelerationBased
    ) : ImpulseJointDesc()

    data class Spherical(
        val localAnchor1: Vector3 = Vector3(0f, 0f, 0f),
        val localAnchor2: Vector3 = Vector3(0f, 0f, 0f)
    ) : ImpulseJointDesc()

    data class Spring(
        val localAnchor1: Vector3 = Vector3(0f, 0f, 0f),
        val localAnchor2: Vector3 = Vector3(0f, 0f, 0f),
        val restLength: Float,
        val stiffness: Float,
        val damping: Float
    ) : ImpulseJointDesc()
}

sealed class MultibodyJointDesc {
    // Similar structure to ImpulseJointDesc but for multibody systems
}

enum class MotorModel {
    AccelerationBased,
    ForceBased
}

// ============================================================================
// Character Controller API
// ============================================================================

/**
 * CharacterController: High-level character movement
 */
interface CharacterControllerAPI {
    val world: PhysicsWorld
    val collider: ColliderHandle
    val body: RigidBodyHandle

    var offset: Float
    var maxSlopeClimbAngle: Float
    var minSlopeSlideAngle: Float
    var snapToGroundDistance: Float?
    var autostepMaxHeight: Float?
    var autostepMinWidth: Float?
    var autostepIncludeDynamicBodies: Boolean

    fun computeColliderMovement(
        desiredTranslation: Vector3,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null
    ): CharacterMovement

    fun move(
        desiredTranslation: Vector3,
        filterFlags: QueryFilterFlags = QueryFilterFlags.EXCLUDE_SENSORS,
        filterGroups: InteractionGroups? = null,
        filterPredicate: ((ColliderHandle) -> Boolean)? = null
    )

    fun isGrounded(): Boolean
    fun numComputedCollisions(): Int
    fun computedCollision(i: Int): CharacterCollision
}

data class CharacterMovement(
    val translation: Vector3,
    val grounded: Boolean,
    val slidingDown: Boolean
)

data class CharacterCollision(
    val handle: ColliderHandle,
    val translationRemaining: Vector3,
    val translationApplied: Vector3,
    val translationDelta: Vector3,
    val witness1: Vector3,
    val witness2: Vector3,
    val normal1: Vector3,
    val normal2: Vector3
)

// ============================================================================
// Query and Filtering
// ============================================================================

data class InteractionGroups(
    val memberships: Int,
    val filter: Int
) {
    companion object {
        fun all(): InteractionGroups = InteractionGroups(0xFFFF, 0xFFFF)
        fun none(): InteractionGroups = InteractionGroups(0, 0)
        fun group(group: Int): InteractionGroups {
            val bit = 1 shl group
            return InteractionGroups(bit, 0xFFFF)
        }
    }

    fun test(other: InteractionGroups): Boolean {
        return (memberships and other.filter) != 0 && (other.memberships and filter) != 0
    }
}

enum class QueryFilterFlags {
    EXCLUDE_SENSORS,
    EXCLUDE_SOLIDS,
    ONLY_FIXED,
    ONLY_KINEMATIC,
    ONLY_DYNAMIC
}

// ============================================================================
// Collision Events
// ============================================================================

/**
 * EventQueue: Collision and contact events
 */
interface EventQueue {
    fun drainCollisionEvents(callback: (CollisionEvent) -> Unit)
    fun drainContactForceEvents(callback: (ContactForceEvent) -> Unit)
}

sealed class CollisionEvent {
    data class Started(
        val collider1: ColliderHandle,
        val collider2: ColliderHandle
    ) : CollisionEvent()

    data class Stopped(
        val collider1: ColliderHandle,
        val collider2: ColliderHandle
    ) : CollisionEvent()
}

data class ContactForceEvent(
    val collider1: ColliderHandle,
    val collider2: ColliderHandle,
    val totalForce: Vector3,
    val totalForceMagnitude: Float,
    val maxForceDirection: Vector3,
    val maxForceMagnitude: Float
)

data class ContactPair(
    val collider1: ColliderHandle,
    val collider2: ColliderHandle,
    val manifolds: List<ContactManifold>
)

data class ContactManifold(
    val localNormal1: Vector3,
    val localNormal2: Vector3,
    val points: List<ContactPoint>
)

data class ContactPoint(
    val localP1: Vector3,
    val localP2: Vector3,
    val dist: Float,
    val impulse: Float,
    val tangentImpulse: Vector2
)

// ============================================================================
// Raycasting Results
// ============================================================================

data class RayColliderHit(
    val collider: ColliderHandle,
    val toi: Float,  // Time of impact
    val normal: Vector3,
    val point: Vector3
)

data class RayColliderIntersection(
    val collider: ColliderHandle,
    val toi: Float,
    val normal: Vector3,
    val point: Vector3,
    val featureType: FeatureType,
    val featureId: Int
)

data class ShapeCastHit(
    val collider: ColliderHandle,
    val toi: Float,
    val witness1: Vector3,
    val witness2: Vector3,
    val normal1: Vector3,
    val normal2: Vector3
)

enum class FeatureType {
    Vertex,
    Edge,
    Face,
    Unknown
}

// ============================================================================
// Supporting Types
// ============================================================================

data class AABB(
    val min: Vector3,
    val max: Vector3
) {
    fun center(): Vector3 = Vector3(
        (min.x + max.x) * 0.5f,
        (min.y + max.y) * 0.5f,
        (min.z + max.z) * 0.5f
    )

    fun halfExtents(): Vector3 = Vector3(
        (max.x - min.x) * 0.5f,
        (max.y - min.y) * 0.5f,
        (max.z - min.z) * 0.5f
    )

    fun contains(point: Vector3): Boolean =
        point.x >= min.x && point.x <= max.x &&
        point.y >= min.y && point.y <= max.y &&
        point.z >= min.z && point.z <= max.z

    fun intersects(other: AABB): Boolean =
        min.x <= other.max.x && max.x >= other.min.x &&
        min.y <= other.max.y && max.y >= other.min.y &&
        min.z <= other.max.z && max.z >= other.min.z
}

data class MassProperties(
    val mass: Float,
    val centerOfMass: Vector3,
    val principalInertia: Vector3,
    val principalInertiaFrame: Quaternion
)

data class IntegrationParameters(
    var dt: Float = 1f / 60f,
    var minCcdDt: Float = 1f / 60f / 100f,
    var erp: Float = 0.8f,
    var dampingRatio: Float = 0.25f,
    var jointErp: Float = 1f,
    var jointDampingRatio: Float = 1f,
    var allowedLinearError: Float = 0.001f,
    var maxPenetrationCorrection: Float = Float.MAX_VALUE,
    var predictionDistance: Float = 0.002f,
    var maxVelocityIterations: Int = 4,
    var maxVelocityFrictionIterations: Int = 8,
    var maxStabilizationIterations: Int = 1,
    var interposeRestitutionAndFrictionResolution: Boolean = true,
    var minIslandSize: Int = 128,
    var maxCcdSubsteps: Int = 1
)

// Opaque types (platform-specific implementations)
typealias RigidBodyHandle = Int
typealias ColliderHandle = Int
typealias ImpulseJointHandle = Int
typealias MultibodyJointHandle = Int

interface RigidBodySet
interface ColliderSet
interface ImpulseJointSet
interface MultibodyJointSet
interface IslandManager
interface BroadPhase
interface NarrowPhase
interface CCDSolver

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for PhysicsWorld
 */
fun physicsWorld(init: PhysicsWorld.() -> Unit = {}): PhysicsWorld {
    val world = PhysicsWorld()
    world.init()
    return world
}

/**
 * Extension function to sync Object3D with RigidBody
 */
fun Object3D.syncWithPhysics(body: RigidBody) {
    val pos = body.translation()
    val rot = body.rotation()
    position.copy(pos)
    quaternion.copy(rot)
}

/**
 * Extension function to create RigidBody from Object3D
 */
fun Object3D.toRigidBody(
    world: PhysicsWorld,
    bodyType: RigidBodyType = RigidBodyType.Dynamic,
    colliderDesc: ColliderDesc
): Pair<RigidBodyHandle, ColliderHandle> {
    val bodyDesc = RigidBodyDesc(
        bodyType = bodyType,
        translation = position.clone(),
        rotation = quaternion.clone()
    )
    val bodyHandle = world.createRigidBody(bodyDesc)
    val colliderHandle = world.createCollider(colliderDesc, bodyHandle)
    return Pair(bodyHandle, colliderHandle)
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Create physics world
 */
fun examplePhysicsWorld(): PhysicsWorld {
    return physicsWorld {
        gravity = Vector3(0f, -9.81f, 0f)
        integrationParameters.dt = 1f / 60f
    }
}

/**
 * Example: Create dynamic sphere
 */
fun exampleDynamicSphere(world: PhysicsWorld): Pair<RigidBodyHandle, ColliderHandle> {
    val bodyDesc = RigidBodyDesc(
        bodyType = RigidBodyType.Dynamic,
        translation = Vector3(0f, 5f, 0f)
    )
    val bodyHandle = world.createRigidBody(bodyDesc)

    val colliderDesc = ColliderDesc.ball(0.5f).copy(
        density = 1f,
        restitution = 0.7f
    )
    val colliderHandle = world.createCollider(colliderDesc, bodyHandle)

    return Pair(bodyHandle, colliderHandle)
}

/**
 * Example: Create static ground plane
 */
fun exampleGroundPlane(world: PhysicsWorld): ColliderHandle {
    val bodyDesc = RigidBodyDesc(
        bodyType = RigidBodyType.Fixed,
        translation = Vector3(0f, 0f, 0f)
    )
    val bodyHandle = world.createRigidBody(bodyDesc)

    val colliderDesc = ColliderDesc.cuboid(50f, 0.1f, 50f).copy(
        friction = 0.7f,
        restitution = 0.3f
    )
    return world.createCollider(colliderDesc, bodyHandle)
}

/**
 * Example: Raycast from camera
 */
fun exampleRaycast(world: PhysicsWorld, camera: Camera): RayColliderHit? {
    val direction = Vector3(0f, 0f, -1f)
        .applyQuaternion(camera.quaternion)
        .normalize()

    val ray = Ray(camera.position, direction)

    return world.castRay(
        ray = ray,
        maxToi = 100f,
        solid = true
    )
}

/**
 * Example: Character controller
 */
fun exampleCharacterController(world: PhysicsWorld): CharacterController {
    // Create character body
    val bodyDesc = RigidBodyDesc(
        bodyType = RigidBodyType.KinematicPositionBased,
        translation = Vector3(0f, 2f, 0f)
    )
    val bodyHandle = world.createRigidBody(bodyDesc)

    // Create capsule collider
    val colliderDesc = ColliderDesc.capsule(0.5f, 0.3f)
    val colliderHandle = world.createCollider(colliderDesc, bodyHandle)

    return CharacterController(world, colliderHandle, bodyHandle).apply {
        offset = 0.01f
        maxSlopeClimbAngle = 45f * Math.PI.toFloat() / 180f
        minSlopeSlideAngle = 30f * Math.PI.toFloat() / 180f
        snapToGroundDistance = 0.5f
        autostepMaxHeight = 0.3f
        autostepMinWidth = 0.2f
    }
}

/**
 * Example: Handle collision events
 */
fun exampleCollisionEvents(eventQueue: EventQueue) {
    eventQueue.drainCollisionEvents { event ->
        when (event) {
            is CollisionEvent.Started -> {
                println("Collision started: ${event.collider1} <-> ${event.collider2}")
            }
            is CollisionEvent.Stopped -> {
                println("Collision stopped: ${event.collider1} <-> ${event.collider2}")
            }
        }
    }

    eventQueue.drainContactForceEvents { event ->
        println("Contact force: ${event.totalForceMagnitude} N")
    }
}

/**
 * Example: Create revolute joint (hinge)
 */
fun exampleRevoluteJoint(
    world: PhysicsWorld,
    body1: RigidBodyHandle,
    body2: RigidBodyHandle
): ImpulseJointHandle {
    val jointDesc = ImpulseJointDesc.Revolute(
        localAnchor1 = Vector3(0f, 0f, 0f),
        localAnchor2 = Vector3(0f, 1f, 0f),
        axis = Vector3(1f, 0f, 0f),
        limits = Pair(-Math.PI.toFloat() / 2f, Math.PI.toFloat() / 2f)
    )

    return world.createImpulseJoint(jointDesc, body1, body2)
}

/**
 * Example: Physics update loop
 */
fun examplePhysicsLoop(
    world: PhysicsWorld,
    eventQueue: EventQueue,
    bodies: Map<RigidBodyHandle, Object3D>
) {
    // Step physics simulation
    world.step(eventQueue)

    // Sync Object3D transforms with physics
    bodies.forEach { (handle, object3d) ->
        world.getRigidBody(handle)?.let { body ->
            object3d.syncWithPhysics(body)
        }
    }

    // Handle collision events
    exampleCollisionEvents(eventQueue)
}
