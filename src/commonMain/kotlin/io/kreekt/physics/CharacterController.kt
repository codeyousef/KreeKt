/**
 * Character controller implementation for player movement and interaction
 * Provides game-ready character physics with robust collision handling
 */
package io.kreekt.physics

import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Quaternion
import io.kreekt.core.math.Vector3
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos

/**
 * Comprehensive character controller implementation
 * Handles movement, jumping, slopes, steps, and platform interaction
 */
class CharacterControllerImpl(
    override val id: String,
    initialShape: CollisionShape,
    initialStepHeight: Float = 0.35f,
    initialTransform: Matrix4 = Matrix4.identity()
) : CharacterController {

    // CollisionObject implementation
    override var transform: Matrix4 = initialTransform
    override var collisionShape: CollisionShape = initialShape
    override var collisionGroups: Int = 1
    override var collisionMask: Int = -1
    override var userData: Any? = null
    override var contactProcessingThreshold: Float = 0.0f
    override var collisionFlags: Int = 0
    override var isTrigger: Boolean = false

    // CollisionObject methods
    override fun setCollisionShape(shape: CollisionShape): PhysicsResult<Unit> {
        collisionShape = shape
        return PhysicsOperationResult.Success(Unit)
    }


    override fun setWorldTransform(transform: Matrix4) {
        this.transform = transform
    }

    override fun getWorldTransform(): Matrix4 = transform

    override fun translate(offset: Vector3) {
        val position = transform.getPosition()
        position.add(offset)
        transform.setPosition(position)
    }

    override fun rotate(rotation: Quaternion) {
        // TODO: Implement rotation when Matrix4 quaternion methods are available
        // val currentRotation = transform.getRotation()
        // currentRotation.multiply(rotation)
        // transform.setRotationFromQuaternion(currentRotation)
    }

    // Movement properties
    override var stepHeight: Float = initialStepHeight
        set(value) {
            require(value >= 0f) { "Step height cannot be negative" }
            field = value
        }

    override var maxSlope: Float = PI.toFloat() / 4f // 45 degrees
        set(value) {
            require(value in 0f..PI.toFloat() / 2f) { "Max slope must be between 0 and 90 degrees" }
            field = value
        }

    override var jumpSpeed: Float = 5f
        set(value) {
            require(value >= 0f) { "Jump speed cannot be negative" }
            field = value
        }

    override var fallSpeed: Float = 20f
        set(value) {
            require(value >= 0f) { "Fall speed cannot be negative" }
            field = value
        }

    private val _walkDirection = MutableStateFlow(Vector3.ZERO)
    override var walkDirection: Vector3
        get() = _walkDirection.value
        set(value) { _walkDirection.value = value }

    private val _velocityForTimeInterval = MutableStateFlow(Vector3.ZERO)
    override var velocityForTimeInterval: Vector3
        get() = _velocityForTimeInterval.value
        set(value) { _velocityForTimeInterval.value = value }

    // Character state
    private val _onGround = MutableStateFlow(false)
    private val _canJump = MutableStateFlow(true)
    private var _verticalVelocity = 0f
    private var _wasOnGround = false
    private var _wasJumping = false
    private var _jumpTimeout = 0f
    private var _gravityEnabled = true

    // Ground detection
    private var _groundNormal = Vector3.UNIT_Y
    private var _groundObject: CollisionObject? = null
    private var _groundDistance = 0f
    private var _groundHitPoint = Vector3.ZERO

    // Movement settings
    var gravity: Float = -9.81f
    var maxStepHeight: Float = 0.35f
    var maxSlopeAngle: Float = PI.toFloat() / 4f // 45 degrees
    var skinWidth: Float = 0.08f
    var pushForce: Float = 1f
    var airControl: Float = 0.2f
    var groundStickiness: Float = 1f
    var jumpGraceTime: Float = 0.1f // Time after leaving ground when jump is still allowed
    var coyoteTime: Float = 0.15f // Time window for jump after leaving platform

    // Platform interaction
    private var _currentPlatform: CollisionObject? = null
    private var _platformVelocity = Vector3.ZERO
    private var _lastPlatformPosition = Vector3.ZERO

    // State flows for reactive updates
    val onGroundFlow: StateFlow<Boolean> = _onGround.asStateFlow()
    val canJumpFlow: StateFlow<Boolean> = _canJump.asStateFlow()

    // Character dimensions (assumes capsule shape)
    private val characterRadius: Float
        get() = when (val shape = collisionShape) {
            is CapsuleShape -> shape.radius
            is SphereShape -> shape.radius
            is BoxShape -> minOf(shape.halfExtents.x, shape.halfExtents.z)
            else -> 0.5f
        }

    private val characterHeight: Float
        get() = when (val shape = collisionShape) {
            is CapsuleShape -> shape.height + shape.radius * 2f
            is SphereShape -> shape.radius * 2f
            is BoxShape -> shape.halfExtents.y * 2f
            else -> 1.8f
        }

    override fun onGround(): Boolean = _onGround.value

    override fun canJump(): Boolean = _canJump.value && (_onGround.value || _jumpTimeout > 0f)

    override fun jump(direction: Vector3) {
        if (!canJump()) return

        _verticalVelocity = jumpSpeed
        _wasJumping = true
        _jumpTimeout = 0f
        _onGround.value = false
        _canJump.value = false

        // Apply directional jump if specified
        if (direction != Vector3.UNIT_Y && direction.length() > 0.001f) {
            val horizontalDirection = direction.copy().normalize()
            val horizontalSpeed = jumpSpeed * 0.5f // Reduced horizontal component
            velocityForTimeInterval = horizontalDirection * horizontalSpeed
        }
    }


    override fun setVelocityForTimeInterval(velocity: Vector3, timeInterval: Float) {
        this.velocityForTimeInterval = velocity
    }

    override fun warp(origin: Vector3) {
        val newTransform = Matrix4.fromTranslationRotationScale(
            origin,
            transform.getRotation(),
            transform.getScale()
        )
        transform = newTransform
        _verticalVelocity = 0f
        _onGround.value = false
        _groundObject = null
    }

    override fun preStep(world: PhysicsWorld) {
        // Update platform tracking
        updatePlatformInteraction()

        // Update ground detection
        performGroundCheck(world)

        // Update jump state
        updateJumpState()
    }

    override fun playerStep(world: PhysicsWorld, deltaTime: Float) {
        if (deltaTime <= 0f) return

        val currentPosition = transform.getTranslation()
        var newPosition = currentPosition

        // Handle gravity and vertical movement
        if (_gravityEnabled) {
            if (!_onGround.value || _verticalVelocity > 0f) {
                _verticalVelocity = _verticalVelocity + gravity * deltaTime

                // Terminal velocity
                if (_verticalVelocity < -fallSpeed) {
                    _verticalVelocity = -fallSpeed
                }
            } else {
                // Stick to ground
                _verticalVelocity = 0f
            }
        }

        // Calculate movement vector
        var movement = Vector3.ZERO

        // Add walk direction (horizontal movement)
        if (walkDirection.length() > 0.001f) {
            val horizontalMovement = Vector3(walkDirection.x, 0f, walkDirection.z).normalize()
            val movementSpeed = walkDirection.length()

            if (_onGround.value) {
                movement = horizontalMovement * (movementSpeed * deltaTime)
            } else {
                // Reduced air control
                movement = horizontalMovement * movementSpeed * (deltaTime * airControl)
            }
        }

        // Add velocity for time interval (external forces, pushes, etc.)
        if (velocityForTimeInterval.length() > 0.001f) {
            movement = movement + velocityForTimeInterval * deltaTime
            // Gradually reduce external velocity
            velocityForTimeInterval = velocityForTimeInterval * 0.95f
        }

        // Add vertical movement
        movement.y = movement.y + _verticalVelocity * deltaTime

        // Add platform velocity
        if (_currentPlatform != null && _onGround.value) {
            movement = movement + (_platformVelocity * deltaTime)
        }

        // Perform movement with collision detection
        newPosition = performMovement(world, currentPosition, movement, deltaTime)

        // Update transform
        val newTransform = Matrix4.fromTranslationRotationScale(
            newPosition,
            transform.getRotation(),
            transform.getScale()
        )
        transform = newTransform

        // Update state
        _wasOnGround = _onGround.value
        updateTimers(deltaTime)
    }

    private fun performMovement(
        world: PhysicsWorld,
        startPosition: Vector3,
        movement: Vector3,
        deltaTime: Float
    ): Vector3 {
        var currentPosition = startPosition
        var remainingMovement = movement

        // Separate horizontal and vertical movement
        val horizontalMovement = Vector3(remainingMovement.x, 0f, remainingMovement.z)
        val verticalMovement = Vector3(0f, remainingMovement.y, 0f)

        // Perform horizontal movement first
        if (horizontalMovement.length() > 0.001f) {
            currentPosition = performHorizontalMovement(world, currentPosition, horizontalMovement)
        }

        // Then perform vertical movement
        if (abs(verticalMovement.y) > 0.001f) {
            currentPosition = performVerticalMovement(world, currentPosition, verticalMovement)
        }

        return currentPosition
    }

    private fun performHorizontalMovement(world: PhysicsWorld, startPosition: Vector3, movement: Vector3): Vector3 {
        val movementDistance = movement.length()
        if (movementDistance < 0.001f) return startPosition

        val movementDirection = movement.normalize()
        var currentPosition = startPosition

        // Slide along surfaces
        var remainingDistance = movementDistance
        var slideIterations = 0
        val maxSlideIterations = 4

        while (remainingDistance > 0.001f && slideIterations < maxSlideIterations) {
            slideIterations++

            val testMovement = movementDirection * remainingDistance
            val testPosition = currentPosition + testMovement

            // Perform sweep test
            val sweepResult = performSweepTest(world, currentPosition, testPosition)

            if (sweepResult.hasHit) {
                // Move to hit point minus skin width
                val safeDistance = sweepResult.distance - skinWidth
                if (safeDistance > 0.001f) {
                    currentPosition = currentPosition + movementDirection * safeDistance
                    remainingDistance = remainingDistance - safeDistance
                } else {
                    remainingDistance = 0f
                }

                // Calculate slide direction
                val slideDirection = calculateSlideDirection(movementDirection, sweepResult.normal)

                if (slideDirection.length() > 0.001f) {
                    movementDirection.set(slideDirection.normalize())
                } else {
                    break // Cannot slide further
                }
            } else {
                // No collision, move full distance
                currentPosition = testPosition
                break
            }
        }

        return currentPosition
    }

    private fun performVerticalMovement(world: PhysicsWorld, startPosition: Vector3, movement: Vector3): Vector3 {
        val movementDirection = if (movement.y > 0f) Vector3.UNIT_Y else -Vector3.UNIT_Y
        val movementDistance = abs(movement.y)

        if (movementDistance < 0.001f) return startPosition

        val testPosition = startPosition + movement
        val sweepResult = performSweepTest(world, startPosition, testPosition)

        return if (sweepResult.hasHit) {
            // Hit something during vertical movement
            val safeDistance = sweepResult.distance - skinWidth
            val newPosition = startPosition + movementDirection * maxOf(0f, safeDistance)

            // Check if we hit the ground or ceiling
            if (movement.y < 0f) {
                // Falling - check if we hit the ground
                val slopeAngle = acos(sweepResult.normal.dot(Vector3.UNIT_Y))
                if (slopeAngle <= maxSlopeAngle) {
                    _onGround.value = true
                    _groundNormal = sweepResult.normal
                    _groundObject = sweepResult.hitObject
                    _verticalVelocity = 0f
                }
            } else {
                // Jumping/rising - hit ceiling
                _verticalVelocity = 0f
            }

            newPosition
        } else {
            // No collision during vertical movement
            if (movement.y < 0f && _onGround.value) {
                // Was on ground but now falling
                _onGround.value = false
                _jumpTimeout = coyoteTime
            }
            testPosition
        }
    }

    private fun performSweepTest(world: PhysicsWorld, from: Vector3, to: Vector3): SweepResult {
        // Simplified sweep test - in a real implementation, this would use the physics world's sweep functionality
        val direction = (to - from).normalize()
        val distance = (to - from).length()

        // Perform raycast from character center
        val rayStart = from + Vector3(0f, characterHeight * 0.5f, 0f)
        val rayEnd = rayStart + direction * (distance + characterRadius)

        val raycastResult = world.raycast(rayStart, rayEnd)

        return if (raycastResult?.hasHit == true) {
            SweepResult(
                hasHit = true,
                distance = maxOf(0f, raycastResult.distance - characterRadius),
                normal = raycastResult.hitNormal,
                hitPoint = raycastResult.hitPoint,
                hitObject = raycastResult.hitObject
            )
        } else {
            SweepResult(
                hasHit = false,
                distance = distance,
                normal = Vector3.UNIT_Y,
                hitPoint = to,
                hitObject = null
            )
        }
    }

    private fun calculateSlideDirection(movementDirection: Vector3, surfaceNormal: Vector3): Vector3 {
        // Project movement direction onto surface plane
        val projectedMovement = movementDirection - surfaceNormal * movementDirection.dot(surfaceNormal)

        // Check if surface is too steep to walk on
        val slopeAngle = acos(surfaceNormal.dot(Vector3.UNIT_Y))
        if (slopeAngle > maxSlopeAngle) {
            // Too steep - slide down the slope
            val downSlope = Vector3.UNIT_Y - surfaceNormal * Vector3.UNIT_Y.dot(surfaceNormal)
            return downSlope.normalize()
        }

        return projectedMovement
    }

    private fun performGroundCheck(world: PhysicsWorld) {
        val currentPosition = transform.getTranslation()
        val rayStart = currentPosition + Vector3(0f, stepHeight, 0f)
        val rayEnd = currentPosition - Vector3(0f, stepHeight + skinWidth, 0f)

        val raycastResult = world.raycast(rayStart, rayEnd)

        val wasOnGround = _onGround.value

        if (raycastResult?.hasHit == true) {
            _groundDistance = raycastResult.distance - stepHeight
            _groundHitPoint = raycastResult.hitPoint
            _groundNormal = raycastResult.hitNormal
            _groundObject = raycastResult.hitObject

            // Check if surface is walkable
            val slopeAngle = acos(_groundNormal.dot(Vector3.UNIT_Y))

            if (slopeAngle <= maxSlopeAngle && _groundDistance <= stepHeight) {
                _onGround.value = true

                // Reset jump ability when landing
                if (!wasOnGround) {
                    _canJump.value = true
                    _wasJumping = false
                }
            } else {
                _onGround.value = false
                _groundObject = null
            }
        } else {
            _onGround.value = false
            _groundObject = null
            _groundDistance = Float.MAX_VALUE
        }

        // Start coyote time if just left ground
        if (wasOnGround && !_onGround.value && !_wasJumping) {
            _jumpTimeout = coyoteTime
        }
    }

    private fun updatePlatformInteraction() {
        if (_groundObject == _currentPlatform) {
            // Still on same platform - calculate platform velocity
            val currentPlatformPos = _groundObject?.getWorldTransform()?.getTranslation() ?: Vector3.ZERO
            _platformVelocity = if (_lastPlatformPosition != Vector3.ZERO) {
                (currentPlatformPos - _lastPlatformPosition) * 60f // Assume 60 FPS for velocity calculation
            } else {
                Vector3.ZERO
            }
            _lastPlatformPosition = currentPlatformPos
        } else {
            // Platform changed
            _currentPlatform = _groundObject
            _lastPlatformPosition = _groundObject?.getWorldTransform()?.getTranslation() ?: Vector3.ZERO
            _platformVelocity = Vector3.ZERO
        }
    }

    private fun updateJumpState() {
        // Update jump ability based on ground state
        if (_onGround.value && !_wasJumping) {
            _canJump.value = true
        }

        // Reset jumping state when landing
        if (_onGround.value && _wasJumping && _verticalVelocity <= 0f) {
            _wasJumping = false
        }
    }

    private fun updateTimers(deltaTime: Float) {
        // Update jump timeout (coyote time)
        if (_jumpTimeout > 0f) {
            _jumpTimeout = _jumpTimeout - deltaTime
            if (_jumpTimeout <= 0f) {
                _jumpTimeout = 0f
                if (!_onGround.value) {
                    _canJump.value = false
                }
            }
        }
    }

    /**
     * Configure character movement parameters
     */
    fun configureMovement(
        gravity: Float = this.gravity,
        jumpSpeed: Float = this.jumpSpeed,
        fallSpeed: Float = this.fallSpeed,
        stepHeight: Float = this.stepHeight,
        maxSlope: Float = this.maxSlope,
        airControl: Float = this.airControl
    ) {
        this.gravity = gravity
        this.jumpSpeed = jumpSpeed
        this.fallSpeed = fallSpeed
        this.stepHeight = stepHeight
        this.maxSlope = maxSlope
        this.airControl = airControl
    }

    /**
     * Configure collision parameters
     */
    fun configureCollision(
        skinWidth: Float = this.skinWidth,
        pushForce: Float = this.pushForce,
        groundStickiness: Float = this.groundStickiness
    ) {
        this.skinWidth = skinWidth
        this.pushForce = pushForce
        this.groundStickiness = groundStickiness
    }

    /**
     * Configure timing parameters
     */
    fun configureTiming(
        jumpGraceTime: Float = this.jumpGraceTime,
        coyoteTime: Float = this.coyoteTime
    ) {
        this.jumpGraceTime = jumpGraceTime
        this.coyoteTime = coyoteTime
    }

    /**
     * Get character state information
     */
    fun getCharacterState(): CharacterState {
        return CharacterState(
            position = transform.getTranslation(),
            onGround = _onGround.value,
            canJump = _canJump.value,
            verticalVelocity = _verticalVelocity,
            groundNormal = _groundNormal,
            groundDistance = _groundDistance,
            platformVelocity = _platformVelocity,
            currentPlatform = _currentPlatform
        )
    }

    /**
     * Push the character (for external forces)
     */
    fun pushCharacter(force: Vector3, mode: PushMode = PushMode.VELOCITY_CHANGE) {
        when (mode) {
            PushMode.FORCE -> {
                // Apply as continuous force (would be integrated over time)
                velocityForTimeInterval = velocityForTimeInterval + force * 0.1f
            }
            PushMode.IMPULSE -> {
                // Apply as immediate impulse
                velocityForTimeInterval = velocityForTimeInterval + force
            }
            PushMode.VELOCITY_CHANGE -> {
                // Replace current velocity
                velocityForTimeInterval = force
            }
        }
    }

    /**
     * Teleport character to new position
     */
    fun teleport(newPosition: Vector3, resetVelocity: Boolean = true) {
        warp(newPosition)
        if (resetVelocity) {
            velocityForTimeInterval = Vector3.ZERO
            _verticalVelocity = 0f
        }
    }

    /**
     * Check if character can move to a specific position
     */
    fun canMoveTo(world: PhysicsWorld, targetPosition: Vector3): Boolean {
        val currentPosition = transform.getTranslation()
        val sweepResult = performSweepTest(world, currentPosition, targetPosition)
        return !sweepResult.hasHit
    }

    /**
     * Get the closest walkable position to a target
     */
    fun getClosestWalkablePosition(world: PhysicsWorld, targetPosition: Vector3): Vector3 {
        val currentPosition = transform.getTranslation()
        val sweepResult = performSweepTest(world, currentPosition, targetPosition)

        return if (sweepResult.hasHit) {
            val direction = (targetPosition - currentPosition).normalize()
            val safeDistance = maxOf(0f, sweepResult.distance - skinWidth)
            currentPosition + (direction * safeDistance)
        } else {
            targetPosition
        }
    }
}

/**
 * Character state data class
 */
data class CharacterState(
    val position: Vector3,
    val onGround: Boolean,
    val canJump: Boolean,
    val verticalVelocity: Float,
    val groundNormal: Vector3,
    val groundDistance: Float,
    val platformVelocity: Vector3,
    val currentPlatform: CollisionObject?
)

/**
 * Sweep test result
 */
data class SweepResult(
    val hasHit: Boolean,
    val distance: Float,
    val normal: Vector3,
    val hitPoint: Vector3,
    val hitObject: CollisionObject?
)

/**
 * Push mode for character forces
 */
enum class PushMode {
    FORCE,           // Apply as continuous force
    IMPULSE,         // Apply as immediate impulse
    VELOCITY_CHANGE  // Replace current velocity
}

/**
 * Factory for creating character controllers
 */
object CharacterControllerFactory {

    /**
     * Create a standard humanoid character controller
     */
    fun createHumanoidController(
        id: String,
        height: Float = 1.8f,
        radius: Float = 0.3f,
        position: Vector3 = Vector3.ZERO
    ): CharacterController {
        val capsuleShape = CapsuleShapeImpl(radius, height - (radius * 2f))
        val transform = Matrix4.fromTranslation(position)

        return CharacterControllerImpl(id, capsuleShape, height * 0.2f, transform).apply {
            configureMovement(
                gravity = -9.81f,
                jumpSpeed = 5f,
                fallSpeed = 20f,
                stepHeight = height * 0.2f,
                maxSlope = PI.toFloat() / 4f,
                airControl = 0.2f
            )
        }
    }

    /**
     * Create a small character controller (for child characters, creatures, etc.)
     */
    fun createSmallController(
        id: String,
        height: Float = 1.2f,
        radius: Float = 0.2f,
        position: Vector3 = Vector3.ZERO
    ): CharacterController {
        val capsuleShape = CapsuleShapeImpl(radius, height - (radius * 2f))
        val transform = Matrix4.fromTranslation(position)

        return CharacterControllerImpl(id, capsuleShape, height * 0.15f, transform).apply {
            configureMovement(
                gravity = -9.81f,
                jumpSpeed = 3f,
                fallSpeed = 15f,
                stepHeight = height * 0.15f,
                maxSlope = PI.toFloat() / 3f, // Steeper slopes allowed
                airControl = 0.3f
            )
        }
    }

    /**
     * Create a vehicle-like character controller
     */
    fun createVehicleController(
        id: String,
        length: Float = 4f,
        width: Float = 2f,
        height: Float = 1.5f,
        position: Vector3 = Vector3.ZERO
    ): CharacterController {
        val boxShape = BoxShapeImpl(Vector3(length * 0.5f, height * 0.5f, width * 0.5f))
        val transform = Matrix4.fromTranslation(position)

        return CharacterControllerImpl(id, boxShape, 0.1f, transform).apply {
            configureMovement(
                gravity = -9.81f,
                jumpSpeed = 0f, // Vehicles don't jump
                fallSpeed = 30f,
                stepHeight = 0.1f,
                maxSlope = PI.toFloat() / 6f, // 30 degrees max
                airControl = 0.1f
            )
        }
    }

    /**
     * Create a floating character controller (for flying characters)
     */
    fun createFloatingController(
        id: String,
        radius: Float = 0.5f,
        position: Vector3 = Vector3.ZERO
    ): CharacterController {
        val sphereShape = SphereShapeImpl(radius)
        val transform = Matrix4.fromTranslation(position)

        return CharacterControllerImpl(id, sphereShape, 0f, transform).apply {
            gravity = 0f // No gravity for floating controllers
            configureMovement(
                gravity = 0f,
                jumpSpeed = 5f,
                fallSpeed = 5f,
                stepHeight = 0f,
                maxSlope = PI.toFloat() / 2f, // Can move on any surface
                airControl = 1f // Full air control
            )
        }
    }
}