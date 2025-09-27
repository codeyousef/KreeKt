/**
 * Physics constraint implementations for mechanical joints and connections
 * Provides comprehensive constraint system for realistic physics simulation
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
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Constraint jacobian for physics solver
 */
data class ConstraintJacobian(
    val linearA: Vector3,
    val angularA: Vector3,
    val linearB: Vector3,
    val angularB: Vector3
)

/**
 * Base constraint implementation with common functionality
 */
abstract class PhysicsConstraintImpl(
    override val id: String,
    override val bodyA: RigidBody,
    override val bodyB: RigidBody?
) : PhysicsConstraint {
    private val _enabled = MutableStateFlow(true)
    override var enabled: Boolean
        get() = _enabled.value
        set(value) { _enabled.value = value }
    override var breakingThreshold: Float = Float.POSITIVE_INFINITY
    private val _appliedImpulse = MutableStateFlow(0f)
    private val constraintParams = mutableMapOf<Pair<ConstraintParam, Int>, Float>()
    /**
     * Constraint state tracking
     */
    val enabledFlow: StateFlow<Boolean> = _enabled.asStateFlow()
    val appliedImpulseFlow: StateFlow<Float> = _appliedImpulse.asStateFlow()
    override fun setParam(param: ConstraintParam, value: Float, axis: Int) {
        constraintParams[Pair(param, axis)] = value
        onParameterChanged(param, value, axis)
    }
    override fun getParam(param: ConstraintParam, axis: Int): Float {
        return constraintParams[Pair(param, axis)] ?: getDefaultParamValue(param)
    }

    override fun getAppliedImpulse(): Float = _appliedImpulse.value
    override fun isEnabled(): Boolean = _enabled.value

    override fun getInfo(info: ConstraintInfo) {
        // Default constraint info - subclasses can override
        // For now, this is a placeholder - concrete implementations would populate the info
    }

    /**
     * Update applied impulse (called by physics solver)
     */
    internal fun updateAppliedImpulse(impulse: Float) {
        _appliedImpulse.value = impulse
        // Check for constraint breaking
        if (abs(impulse) > breakingThreshold) {
            enabled = false
        }
    }

    /**
     * Called when constraint parameters change - subclasses can override
     */
    protected open fun onParameterChanged(param: ConstraintParam, value: Float, axis: Int) {
        // Default implementation does nothing
    }

    /**
     * Get default value for constraint parameter
     */
    protected open fun getDefaultParamValue(param: ConstraintParam): Float {
        return when (param) {
            ConstraintParam.ERP -> 0.2f
            ConstraintParam.STOP_ERP -> 0.1f
            ConstraintParam.CFM -> 0f
            ConstraintParam.STOP_CFM -> 0f
            ConstraintParam.LINEAR_LOWER_LIMIT -> -Float.MAX_VALUE
            ConstraintParam.LINEAR_UPPER_LIMIT -> Float.MAX_VALUE
            ConstraintParam.ANGULAR_LOWER_LIMIT -> -Float.MAX_VALUE
            ConstraintParam.ANGULAR_UPPER_LIMIT -> Float.MAX_VALUE
            ConstraintParam.TARGET_VELOCITY -> 0f
            ConstraintParam.MAX_MOTOR_FORCE -> 0f
        }
    }

    /**
     * Calculate relative transform between bodies
     */
    protected fun getRelativeTransform(): Matrix4 {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        return transformA.inverse() * transformB
    }

    /**
     * Check if constraint is valid (both bodies exist and are properly configured)
     */
    fun isValid(): Boolean {
        return bodyA.isActive() && (bodyB?.isActive() != false)
    }
}

/**
 * Point-to-point constraint implementation (ball-socket joint)
 * Constrains two points on different bodies to remain at the same position
 */
class PointToPointConstraintImpl(
    id: String,
    bodyA: RigidBody,
    bodyB: RigidBody?,
    override val pivotA: Vector3,
    override val pivotB: Vector3
) : PhysicsConstraintImpl(id, bodyA, bodyB), PointToPointConstraint {
    private var _pivotA = pivotA
    private var _pivotB = pivotB
    override fun setPivotA(pivot: Vector3) {
        _pivotA = pivot
    }

    override fun setPivotB(pivot: Vector3) {
        _pivotB = pivot
    }

    override fun updateRHS(timeStep: Float) {
        if (!isEnabled() || !isValid()) return
        val transformA = bodyA.getWorldTransform()
        val worldPivotA = transformA.transformPoint(_pivotA)
        val worldPivotB = if (bodyB != null) {
            val transformB = bodyB.getWorldTransform()
            transformB.transformPoint(_pivotB)
        } else {
            _pivotB // World space pivot for static constraint
        }

        // Calculate constraint error
        val constraintError = worldPivotA - worldPivotB
        val errorMagnitude = constraintError.length()

        // Apply corrective impulse if error is significant
        if (errorMagnitude > 0.001f) {
            val correctionFactor = getParam(ConstraintParam.ERP, -1) / timeStep
            val impulse = constraintError * correctionFactor
            // Apply impulse to body A
            bodyA.applyCentralImpulse(-impulse)
            // Apply opposite impulse to body B (if it exists)
            bodyB?.applyCentralImpulse(impulse)
            updateAppliedImpulse(impulse.length())
        }
    }

    /**
     * Get constraint jacobian for physics solver
     */
    fun getJacobian(): ConstraintJacobian {
        val transformA = bodyA.getWorldTransform()
        val worldPivotA = transformA.transformPoint(_pivotA)
        val worldPivotB = if (bodyB != null) {
            val transformB = bodyB.getWorldTransform()
            transformB.transformPoint(_pivotB)
        } else {
            _pivotB
        }
        val relativePos = worldPivotA - worldPivotB
        return ConstraintJacobian(
            linearA = Vector3.ONE,
            angularA = relativePos.cross(Vector3.ONE),
            linearB = if (bodyB != null) -Vector3.ONE else Vector3.ZERO,
            angularB = if (bodyB != null) -relativePos.cross(Vector3.ONE) else Vector3.ZERO
        )
    }
}

/**
 * Hinge constraint implementation (revolute joint)
 * Allows rotation around a single axis while constraining translation
 */
class HingeConstraintImpl(
    id: String,
    bodyA: RigidBody,
    bodyB: RigidBody?,
    override val pivotA: Vector3,
    override val pivotB: Vector3,
    override val axisA: Vector3,
    override val axisB: Vector3
) : PhysicsConstraintImpl(id, bodyA, bodyB), HingeConstraint {
    private var _lowerLimit = -Float.MAX_VALUE
    private var _upperLimit = Float.MAX_VALUE
    private var _enableAngularMotor = false
    private var _targetVelocity = 0f
    private var _maxMotorImpulse = 0f
    // Hinge state
    private var _currentAngle = 0f
    private var _referenceSign = 1f
    private var _angularOnly = false
    private var _solveLimit = false
    override var lowerLimit: Float
        get() = _lowerLimit
        set(value) {
            _lowerLimit = value
            updateLimitState()
        }

    override var upperLimit: Float
        get() = _upperLimit
        set(value) {
            _upperLimit = value
            updateLimitState()
        }
    override var enableAngularMotor: Boolean
        get() = _enableAngularMotor
        set(value) { _enableAngularMotor = value }
    override var targetVelocity: Float
        get() = _targetVelocity
        set(value) { _targetVelocity = value }
    override var maxMotorImpulse: Float
        get() = _maxMotorImpulse
        set(value) { _maxMotorImpulse = maxOf(0f, value) }
    override fun setLimit(low: Float, high: Float, softness: Float, biasFactor: Float, relaxationFactor: Float) {
        _lowerLimit = low
        _upperLimit = high
        setParam(ConstraintParam.STOP_ERP, biasFactor, -1)
        setParam(ConstraintParam.STOP_CFM, relaxationFactor, -1)
        updateLimitState()
    }

    override fun enableMotor(enable: Boolean) {
        _enableAngularMotor = enable
    }

    override fun setMotorTarget(targetAngle: Float, deltaTime: Float) {
        val currentAngle = getHingeAngle()
        val angleDiff = normalizeAngle(targetAngle - currentAngle)
        _targetVelocity = angleDiff / deltaTime
    }

    override fun getHingeAngle(): Float {
        updateCurrentAngle()
        return _currentAngle
    }

    private fun updateCurrentAngle() {
        // Get world-space axes
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldAxisA = transformA.transformDirection(axisA.normalized())
        val worldAxisB = transformB.transformDirection(axisB.normalized())
        // Get perpendicular vectors for angle calculation
        val perpA = getPerpendicularVector(worldAxisA)
        val perpB = getPerpendicularVector(worldAxisB)
        // Project perpendicular vectors onto plane perpendicular to hinge axis
        val projectedPerpA = (perpA - worldAxisA * perpA.dot(worldAxisA)).normalized()
        val projectedPerpB = (perpB - worldAxisA * perpB.dot(worldAxisA)).normalized()
        // Calculate angle between projected vectors
        val cosAngle = projectedPerpA.dot(projectedPerpB).coerceIn(-1f, 1f)
        val sinAngle = projectedPerpA.cross(projectedPerpB).dot(worldAxisA)
        _currentAngle = atan2(sinAngle, cosAngle) * _referenceSign
    }

    private fun updateLimitState() {
        val angle = getHingeAngle()
        _solveLimit = angle < _lowerLimit || angle > _upperLimit
    }

    private fun getPerpendicularVector(v: Vector3): Vector3 {
        val absX = abs(v.x)
        val absY = abs(v.y)
        val absZ = abs(v.z)
        return when {
            absX < absY && absX < absZ -> Vector3.UNIT_X
            absY < absZ -> Vector3.UNIT_Y
            else -> Vector3.UNIT_Z
        }.cross(v).normalized()
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle
        while (normalized > PI) normalized = normalized - 2f * PI.toFloat()
        while (normalized < -PI) normalized = normalized + 2f * PI.toFloat()
        return normalized
    }

    /**
     * Apply constraint forces during physics step
     */
    internal fun solveConstraint(deltaTime: Float) {
        // Solve positional constraint (point-to-point part)
        solvePositionalConstraint(deltaTime)
        // Solve angular constraints
        solveAngularConstraint(deltaTime)
        // Apply motor forces if enabled
        if (_enableAngularMotor && _maxMotorImpulse > 0f) {
            applyMotorForces(deltaTime)
        }
        // Enforce limits if active
        if (_solveLimit) {
            enforceLimits(deltaTime)
        }
    }

    private fun solvePositionalConstraint(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldPivotA = transformA.transformPoint(pivotA)
        val worldPivotB = if (bodyB != null) {
            transformB.transformPoint(pivotB)
        } else {
            pivotB
        }

        val constraintError = worldPivotA - worldPivotB
        val errorMagnitude = constraintError.length()
        if (errorMagnitude > 0.001f) {
            val correctionFactor = getParam(ConstraintParam.ERP, -1) / deltaTime
            val impulse = constraintError * correctionFactor
            bodyA.applyCentralImpulse(-impulse)
            bodyB?.applyCentralImpulse(impulse)
        }
    }
    private fun solveAngularConstraint(deltaTime: Float) {
        // Calculate angular error (axes should be aligned)
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldAxisA = transformA.transformDirection(axisA.normalized())
        val worldAxisB = transformB.transformDirection(axisB.normalized())

        val angularError = worldAxisA.cross(worldAxisB)
        val errorMagnitude = angularError.length()

        if (errorMagnitude > 0.001f) {
            val correctionFactor = getParam(ConstraintParam.ERP, -1) / deltaTime
            val torqueImpulse = angularError * correctionFactor
            bodyA.applyTorqueImpulse(-torqueImpulse)
            bodyB?.applyTorqueImpulse(torqueImpulse)
        }
    }
    private fun applyMotorForces(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val worldAxisA = transformA.transformDirection(axisA.normalized())

        val currentAngularVelocityA = bodyA.angularVelocity.dot(worldAxisA)
        val currentAngularVelocityB = bodyB?.angularVelocity?.dot(worldAxisA) ?: 0f
        val relativeAngularVelocity = currentAngularVelocityA - currentAngularVelocityB
        val velocityError = _targetVelocity - relativeAngularVelocity
        val motorImpulse = ((velocityError * deltaTime)).coerceIn(-_maxMotorImpulse, _maxMotorImpulse)
        if (abs(motorImpulse) > 0.001f) {
            val torqueImpulse = worldAxisA * motorImpulse
            bodyA.applyTorqueImpulse(torqueImpulse)
            bodyB?.applyTorqueImpulse(-torqueImpulse)
            updateAppliedImpulse(abs(motorImpulse))
        }
    }

    private fun enforceLimits(deltaTime: Float) {
        val angle = getHingeAngle()
        var limitImpulse = 0f
        when {
            angle < _lowerLimit -> {
                val limitViolation = _lowerLimit - angle
                limitImpulse = limitViolation / deltaTime
            }
            angle > _upperLimit -> {
                val limitViolation = angle - _upperLimit
                limitImpulse = -limitViolation / deltaTime
            }
        }
        if (abs(limitImpulse) > 0.001f) {
            val transformA = bodyA.getWorldTransform()
            val worldAxisA = transformA.transformDirection(axisA.normalized())
            val torqueImpulse = worldAxisA * limitImpulse
            bodyA.applyTorqueImpulse(torqueImpulse)
            bodyB?.applyTorqueImpulse(-torqueImpulse)
        }
    }
}

/**
 * Slider constraint implementation (prismatic joint)
 * Allows linear motion along one axis while constraining rotation
 */
class SliderConstraintImpl(
    id: String,
    bodyA: RigidBody,
    bodyB: RigidBody?,
    override val frameA: Matrix4,
    override val frameB: Matrix4
) : PhysicsConstraintImpl(id, bodyA, bodyB), SliderConstraint {
    override var lowerLinearLimit: Float = -1f
    override var upperLinearLimit: Float = 1f
    override var lowerAngularLimit: Float = 0f
    override var upperAngularLimit: Float = 0f
    override var poweredLinearMotor: Boolean = false
    override var targetLinearMotorVelocity: Float = 0f
    override var maxLinearMotorForce: Float = 0f
    override var poweredAngularMotor: Boolean = false
    override var targetAngularMotorVelocity: Float = 0f
    override var maxAngularMotorForce: Float = 0f
    private var _currentLinearPos = 0f
    private var _currentAngularPos = 0f
    override fun getLinearPos(): Float {
        updateCurrentPosition()
        return _currentLinearPos
    }

    override fun getAngularPos(): Float {
        updateCurrentPosition()
        return _currentAngularPos
    }

    private fun updateCurrentPosition() {
        // Calculate relative transform
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val relativeTransform = frameA.inverse() * transformA.inverse() * transformB * frameB
        // Extract linear position along slider axis (Z-axis in frame coordinates)
        val sliderAxis = Vector3.UNIT_Z
        val relativePosition = relativeTransform.getTranslation()
        _currentLinearPos = relativePosition.dot(sliderAxis)
        // Extract angular position around slider axis
        val relativeRotation = relativeTransform.getRotation()
        val eulerAngles = relativeRotation.toEulerAngles()
        _currentAngularPos = eulerAngles.z // Rotation around Z-axis
    }

    /**
     * Solve slider constraint
     */
    internal fun solveConstraint(deltaTime: Float) {
        // Solve positional constraints (maintain alignment except along slider axis)
        solvePositionalConstraints(deltaTime)
        // Solve angular constraints (prevent rotation except around slider axis)
        solveAngularConstraints(deltaTime)
        // Apply linear motor if enabled
        if (poweredLinearMotor && maxLinearMotorForce > 0f) {
            applyLinearMotor(deltaTime)
        }
        // Apply angular motor if enabled
        if (poweredAngularMotor && maxAngularMotorForce > 0f) {
            applyAngularMotor(deltaTime)
        }
        // Enforce limits
        enforceLinearLimits(deltaTime)
        enforceAngularLimits(deltaTime)
    }
    private fun solvePositionalConstraints(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldFrameA = transformA * frameA
        val worldFrameB = transformB * frameB
        val positionA = worldFrameA.getTranslation()
        val positionB = worldFrameB.getTranslation()
        // Calculate constraint error perpendicular to slider axis
        val sliderAxisWorld = worldFrameA.transformDirection(Vector3.UNIT_Z)
        val positionError = positionB - positionA
        val parallelComponent = sliderAxisWorld * positionError.dot(sliderAxisWorld)
        val perpendicularError = positionError - parallelComponent
        if (perpendicularError.length() > 0.001f) {
            val correctionFactor = getParam(ConstraintParam.ERP, -1) / deltaTime
            val impulse = perpendicularError * correctionFactor
            bodyA.applyCentralImpulse(-impulse)
            bodyB?.applyCentralImpulse(impulse)
        }
    }

    private fun solveAngularConstraints(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldFrameA = transformA * frameA
        val worldFrameB = transformB * frameB
        val orientationA = worldFrameA.getRotation()
        val orientationB = worldFrameB.getRotation()
        // Calculate relative rotation
        val relativeRotation = orientationA.inverse() * orientationB
        val axis = Vector3.UNIT_Z // Slider axis in frame coordinates
        // Allow rotation only around slider axis
        val worldSliderAxis = worldFrameA.transformDirection(axis)
        val angularError = relativeRotation.toAxisAngle()
        val rotationAxis = angularError.first.normalized()
        val rotationAngle = angularError.second
        // Project rotation onto plane perpendicular to slider axis
        val perpendicularRotation = rotationAxis - worldSliderAxis * rotationAxis.dot(worldSliderAxis)
        if (perpendicularRotation.length() > 0.001f) {
            val correctionFactor = getParam(ConstraintParam.ERP, -1) / deltaTime
            val torqueImpulse = perpendicularRotation.normalized() * rotationAngle * correctionFactor
            bodyA.applyTorqueImpulse(-torqueImpulse)
            bodyB?.applyTorqueImpulse(torqueImpulse)
        }
    }

    private fun applyLinearMotor(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val worldFrameA = transformA * frameA
        val sliderAxisWorld = worldFrameA.transformDirection(Vector3.UNIT_Z)
        val velocityA = bodyA.linearVelocity.dot(sliderAxisWorld)
        val velocityB = bodyB?.linearVelocity?.dot(sliderAxisWorld) ?: 0f
        val relativeVelocity = velocityA - velocityB
        val velocityError = targetLinearMotorVelocity - relativeVelocity
        val motorForce = (velocityError / deltaTime).coerceIn(-maxLinearMotorForce, maxLinearMotorForce)
        if (abs(motorForce) > 0.001f) {
            val impulse = sliderAxisWorld * motorForce * deltaTime
            bodyA.applyCentralImpulse(impulse)
            bodyB?.applyCentralImpulse(-impulse)
            updateAppliedImpulse(abs(motorForce))
        }
    }

    private fun applyAngularMotor(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val worldFrameA = transformA * frameA
        val sliderAxisWorld = worldFrameA.transformDirection(Vector3.UNIT_Z)
        val angularVelocityA = bodyA.angularVelocity.dot(sliderAxisWorld)
        val angularVelocityB = bodyB?.angularVelocity?.dot(sliderAxisWorld) ?: 0f
        val relativeAngularVelocity = angularVelocityA - angularVelocityB
        val velocityError = targetAngularMotorVelocity - relativeAngularVelocity
        val motorTorque = (velocityError / deltaTime).coerceIn(-maxAngularMotorForce, maxAngularMotorForce)
        if (abs(motorTorque) > 0.001f) {
            val torqueImpulse = sliderAxisWorld * motorTorque * deltaTime
            bodyA.applyTorqueImpulse(torqueImpulse)
            bodyB?.applyTorqueImpulse(-torqueImpulse)
        }
    }

    private fun enforceLinearLimits(deltaTime: Float) {
        val position = getLinearPos()
        var limitImpulse = 0f
        when {
            position < lowerLinearLimit -> {
                limitImpulse = (lowerLinearLimit - position) / deltaTime
            }
            position > upperLinearLimit -> {
                limitImpulse = (upperLinearLimit - position) / deltaTime
            }
        }
        if (abs(limitImpulse) > 0.001f) {
            val transformA = bodyA.getWorldTransform()
            val worldFrameA = transformA * frameA
            val sliderAxisWorld = worldFrameA.transformDirection(Vector3.UNIT_Z)
            val impulse = sliderAxisWorld * limitImpulse
            bodyA.applyCentralImpulse(impulse)
            bodyB?.applyCentralImpulse(-impulse)
        }
    }
    private fun enforceAngularLimits(deltaTime: Float) {
        val angle = getAngularPos()
        var limitTorque = 0f
        when {
            angle < lowerAngularLimit -> {
                limitTorque = (lowerAngularLimit - angle) / deltaTime
            }
            angle > upperAngularLimit -> {
                limitTorque = (upperAngularLimit - angle) / deltaTime
            }
        }
        if (abs(limitTorque) > 0.001f) {
            val transformA = bodyA.getWorldTransform()
            val worldFrameA = transformA * frameA
            val sliderAxisWorld = worldFrameA.transformDirection(Vector3.UNIT_Z)
            val torqueImpulse = sliderAxisWorld * limitTorque
            bodyA.applyTorqueImpulse(torqueImpulse)
            bodyB?.applyTorqueImpulse(-torqueImpulse)
        }
    }
}
/**
 * Cone-twist constraint implementation (shoulder joint)
 * Allows rotation with cone and twist limits, simulating ball-and-socket with limits
 */
class ConeTwistConstraintImpl(
    id: String,
    bodyA: RigidBody,
    bodyB: RigidBody?,
    override val frameA: Matrix4,
    override val frameB: Matrix4
) : PhysicsConstraintImpl(id, bodyA, bodyB), ConeTwistConstraint {
    override var swingSpan1: Float = PI.toFloat() / 4f // 45 degrees
    override var swingSpan2: Float = PI.toFloat() / 4f // 45 degrees
    override var twistSpan: Float = PI.toFloat() / 6f  // 30 degrees
    override var damping: Float = 0.01f
    private var _motorEnabled = false
    private var _motorTarget = Quaternion.IDENTITY
    private var _maxMotorImpulse = 0f

    override fun setLimit(swingSpan1: Float, swingSpan2: Float, twistSpan: Float, softness: Float, biasFactor: Float, relaxationFactor: Float) {
        this.swingSpan1 = swingSpan1.coerceIn(0f, PI.toFloat())
        this.swingSpan2 = swingSpan2.coerceIn(0f, PI.toFloat())
        this.twistSpan = twistSpan.coerceIn(0f, PI.toFloat())
        setParam(ConstraintParam.STOP_ERP, biasFactor, -1)
        setParam(ConstraintParam.STOP_CFM, relaxationFactor, -1)
    }

    override fun enableMotor(enable: Boolean) {
        _motorEnabled = enable
    }
    override fun setMaxMotorImpulse(maxMotorImpulse: Float) {
        _maxMotorImpulse = maxOf(0f, maxMotorImpulse)
    }

    override fun setMotorTarget(q: Quaternion) {
        _motorTarget = q.normalized()
    }

    /**
     * Solve cone-twist constraint
     */
    internal fun solveConstraint(deltaTime: Float) {
        // Solve positional constraints
        solvePositionalConstraint(deltaTime)
        // Solve angular constraints with cone and twist limits
        solveAngularConstraints(deltaTime)
        // Apply motor if enabled
        if (_motorEnabled && _maxMotorImpulse > 0f) {
            applyMotor(deltaTime)
        }
    }

    private fun solvePositionalConstraint(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldFrameA = transformA * frameA
        val worldFrameB = transformB * frameB
        val positionA = worldFrameA.getTranslation()
        val positionB = worldFrameB.getTranslation()
        val constraintError = positionA - positionB

        if (constraintError.length() > 0.001f) {
            val correctionFactor = getParam(ConstraintParam.ERP, -1) / deltaTime
            val impulse = constraintError * correctionFactor
            bodyA.applyCentralImpulse(-impulse)
            bodyB?.applyCentralImpulse(impulse)
        }
    }

    private fun solveAngularConstraints(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldFrameA = transformA * frameA
        val worldFrameB = transformB * frameB
        val orientationA = worldFrameA.getRotation()
        val orientationB = worldFrameB.getRotation()
        val relativeRotation = orientationA.inverse() * orientationB
        // Decompose into swing and twist components
        val (swingRotation, twistRotation) = decomposeSwingTwist(relativeRotation, Vector3.UNIT_X)
        // Check swing limits
        val swingAxis = swingRotation.toAxisAngle()
        val swingAngle = swingAxis.second
        val swingAxisNormalized = swingAxis.first.normalized()
        if (swingAngle > 0.001f) {
            // Project swing axis onto Y-Z plane
            val swingY = swingAxisNormalized.y
            val swingZ = swingAxisNormalized.z
            val normalizedSwingY = if (abs(swingY) > 0.001f) swingY / sqrt(swingY * swingY + (swingZ * swingZ)) else 0f
            val normalizedSwingZ = if (abs(swingZ) > 0.001f) swingZ / sqrt(swingY * swingY + (swingZ * swingZ)) else 0f
            // Calculate elliptical cone limits
            val maxSwingY = swingSpan1
            val maxSwingZ = swingSpan2
            val ellipseLimit = sqrt(
                ((normalizedSwingY * normalizedSwingY)) / ((maxSwingY * maxSwingY)) +
                ((normalizedSwingZ * normalizedSwingZ)) / ((maxSwingZ * maxSwingZ))
            )
            if (ellipseLimit > 1f) {
                // Swing limit violated - apply corrective torque
                val limitViolation = swingAngle * (ellipseLimit - 1f) / ellipseLimit
                val correctionTorque = swingAxisNormalized * limitViolation / deltaTime
                val worldCorrectionTorque = worldFrameA.transformDirection(correctionTorque)
                bodyA.applyTorqueImpulse(-worldCorrectionTorque)
                bodyB?.applyTorqueImpulse(worldCorrectionTorque)
            }
        }

        // Check twist limits
        val twistAxis = twistRotation.toAxisAngle()
        val twistAngle = twistAxis.second
        if (abs(twistAngle) > twistSpan) {
            // Twist limit violated - apply corrective torque
            val limitViolation = if (twistAngle > 0f) {
                twistAngle - twistSpan
            } else {
                twistAngle + twistSpan
            }
            val correctionTorque = Vector3.UNIT_X * limitViolation / deltaTime
            val worldCorrectionTorque = worldFrameA.transformDirection(correctionTorque)
            bodyA.applyTorqueImpulse(-worldCorrectionTorque)
            bodyB?.applyTorqueImpulse(worldCorrectionTorque)
        }

        // Apply damping
        if (damping > 0f) {
            val relativeAngularVelocity = bodyA.angularVelocity - (bodyB?.angularVelocity ?: Vector3.ZERO)
            val dampingTorque = relativeAngularVelocity * damping
            bodyA.applyTorqueImpulse(-(dampingTorque * deltaTime))
            bodyB?.applyTorqueImpulse((dampingTorque * deltaTime))
        }
    }

    private fun applyMotor(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldFrameA = transformA * frameA
        val worldFrameB = transformB * frameB

        val currentOrientation = worldFrameA.getRotation().inverse() * worldFrameB.getRotation()
        val targetOrientation = _motorTarget
        // Calculate rotation needed to reach target
        val rotationError = targetOrientation * currentOrientation.inverse()
        val (errorAxis, errorAngle) = rotationError.toAxisAngle()
        if (errorAngle > 0.001f) {
            val motorTorque = (errorAxis.normalized() * errorAngle / deltaTime)
                .coerceLength(0f, _maxMotorImpulse)
            val worldMotorTorque = worldFrameA.transformDirection(motorTorque)
            bodyA.applyTorqueImpulse((worldMotorTorque * deltaTime))
            bodyB?.applyTorqueImpulse(-(worldMotorTorque * deltaTime))
            updateAppliedImpulse(motorTorque.length())
        }
    }

    private fun decomposeSwingTwist(rotation: Quaternion, twistAxis: Vector3): Pair<Quaternion, Quaternion> {
        // Decompose rotation into swing (around Y,Z axes) and twist (around X axis)
        val rotationVector = Vector3(rotation.x, rotation.y, rotation.z)
        val twistComponent = twistAxis * rotationVector.dot(twistAxis)
        val swingComponent = rotationVector - twistComponent

        val twistRotation = Quaternion(
            twistComponent.x,
            twistComponent.y,
            twistComponent.z,
            rotation.w
        ).normalized()

        val swingRotation = rotation * twistRotation.inverse()
        return Pair(swingRotation, twistRotation)
    }
}

/**
 * Provides full control over all 6 degrees of freedom (3 linear + 3 angular)
 */
class Generic6DofConstraintImpl(
    id: String,
    bodyA: RigidBody,
    bodyB: RigidBody?,
    override val frameA: Matrix4,
    override val frameB: Matrix4
) : PhysicsConstraintImpl(id, bodyA, bodyB), Generic6DofConstraint {
    private var _linearLowerLimit = Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
    private var _linearUpperLimit = Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
    private var _angularLowerLimit = Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
    private var _angularUpperLimit = Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
    private val _motorEnabled = BooleanArray(6) { false } // 3 linear + 3 angular
    private val _motorTargetVelocity = FloatArray(6) { 0f }
    private val _motorMaxForce = FloatArray(6) { 0f }
    override fun setLinearLowerLimit(linearLower: Vector3) {
        _linearLowerLimit = linearLower
    }

    override fun setLinearUpperLimit(linearUpper: Vector3) {
        _linearUpperLimit = linearUpper
    }
    override fun getLinearLowerLimit(): Vector3 = _linearLowerLimit
    override fun getLinearUpperLimit(): Vector3 = _linearUpperLimit
    override fun setAngularLowerLimit(angularLower: Vector3) {
        _angularLowerLimit = angularLower
    }

    override fun setAngularUpperLimit(angularUpper: Vector3) {
        _angularUpperLimit = angularUpper
    }
    override fun getAngularLowerLimit(): Vector3 = _angularLowerLimit
    override fun getAngularUpperLimit(): Vector3 = _angularUpperLimit
    override fun enableMotor(index: Int, enable: Boolean) {
        require(index in 0..5) { "Motor index must be 0-5 (0-2 linear, 3-5 angular)" }
        _motorEnabled[index] = enable
    }

    override fun setMotorTargetVelocity(index: Int, velocity: Float) {
        require(index in 0..5) { "Motor index must be 0-5 (0-2 linear, 3-5 angular)" }
        _motorTargetVelocity[index] = velocity
    }

    override fun setMotorMaxForce(index: Int, force: Float) {
        require(index in 0..5) { "Motor index must be 0-5 (0-2 linear, 3-5 angular)" }
        _motorMaxForce[index] = maxOf(0f, force)
    }

    /**
     * Solve 6DOF constraint
     */
    internal fun solveConstraint(deltaTime: Float) {
        val transformA = bodyA.getWorldTransform()
        val transformB = bodyB?.getWorldTransform() ?: Matrix4.IDENTITY
        val worldFrameA = transformA * frameA
        val worldFrameB = transformB * frameB

        // Solve linear constraints
        solveLinearConstraints(worldFrameA, worldFrameB, deltaTime)
        // Solve angular constraints
        solveAngularConstraints(worldFrameA, worldFrameB, deltaTime)
        // Apply motors
        applyMotors(worldFrameA, worldFrameB, deltaTime)
    }

    private fun solveLinearConstraints(worldFrameA: Matrix4, worldFrameB: Matrix4, deltaTime: Float) {
        val positionA = worldFrameA.getTranslation()
        val positionB = worldFrameB.getTranslation()
        val relativePosition = positionA - positionB
        // Transform to frame A coordinates
        val localRelativePosition = worldFrameA.inverse().transformDirection(relativePosition)

        for (axis in 0..2) {
            val position = localRelativePosition.componentAt(axis)
            val lowerLimit = _linearLowerLimit.componentAt(axis)
            val upperLimit = _linearUpperLimit.componentAt(axis)
            var limitImpulse = 0f
            when {
                position < lowerLimit && lowerLimit > -Float.MAX_VALUE -> {
                    limitImpulse = (lowerLimit - position) / deltaTime
                }
                position > upperLimit && upperLimit < Float.MAX_VALUE -> {
                    limitImpulse = (upperLimit - position) / deltaTime
                }
            }
            if (abs(limitImpulse) > 0.001f) {
                val axisVector = when (axis) {
                    0 -> Vector3.UNIT_X
                    1 -> Vector3.UNIT_Y
                    else -> Vector3.UNIT_Z
                }
                val worldAxisVector = worldFrameA.transformDirection(axisVector)
                val impulse = worldAxisVector * limitImpulse
                bodyA.applyCentralImpulse(impulse)
                bodyB?.applyCentralImpulse(-impulse)
            }
        }
    }

    private fun solveAngularConstraints(worldFrameA: Matrix4, worldFrameB: Matrix4, deltaTime: Float) {
        val orientationA = worldFrameA.getRotation()
        val orientationB = worldFrameB.getRotation()
        val relativeRotation = orientationA.inverse() * orientationB
        val eulerAngles = relativeRotation.toEulerAngles()

        for (axis in 0..2) {
            val angle = eulerAngles.componentAt(axis)
            val lowerLimit = _angularLowerLimit.componentAt(axis)
            val upperLimit = _angularUpperLimit.componentAt(axis)
            var limitTorque = 0f
            when {
                angle < lowerLimit && lowerLimit > -Float.MAX_VALUE -> {
                    limitTorque = (lowerLimit - angle) / deltaTime
                }
                angle > upperLimit && upperLimit < Float.MAX_VALUE -> {
                    limitTorque = (upperLimit - angle) / deltaTime
                }
            }
            if (abs(limitTorque) > 0.001f) {
                val axisVector = when (axis) {
                    0 -> Vector3.UNIT_X
                    1 -> Vector3.UNIT_Y
                    else -> Vector3.UNIT_Z
                }
                val worldAxisVector = worldFrameA.transformDirection(axisVector)
                val torqueImpulse = worldAxisVector * limitTorque
                bodyA.applyTorqueImpulse(torqueImpulse)
                bodyB?.applyTorqueImpulse(-torqueImpulse)
            }
        }
    }

    private fun applyMotors(worldFrameA: Matrix4, worldFrameB: Matrix4, deltaTime: Float) {
        // Linear motors (0-2)
        for (axis in 0..2) {
            if (!_motorEnabled[axis] || _motorMaxForce[axis] <= 0f) continue
            val axisVector = when (axis) {
                0 -> Vector3.UNIT_X
                1 -> Vector3.UNIT_Y
                else -> Vector3.UNIT_Z
            }
            val worldAxisVector = worldFrameA.transformDirection(axisVector)
            val velocityA = bodyA.linearVelocity.dot(worldAxisVector)
            val velocityB = bodyB?.linearVelocity?.dot(worldAxisVector) ?: 0f
            val relativeVelocity = velocityA - velocityB
            val velocityError = _motorTargetVelocity[axis] - relativeVelocity
            val motorForce = (velocityError / deltaTime).coerceIn(-_motorMaxForce[axis], _motorMaxForce[axis])
            if (abs(motorForce) > 0.001f) {
                val impulse = worldAxisVector * motorForce * deltaTime
                bodyA.applyCentralImpulse(impulse)
                bodyB?.applyCentralImpulse(-impulse)
            }
        }

        // Angular motors (3-5)
        for (axis in 3..5) {
            if (!_motorEnabled[axis] || _motorMaxForce[axis] <= 0f) continue
            val axisIndex = axis - 3
            val axisVector = when (axisIndex) {
                0 -> Vector3.UNIT_X
                1 -> Vector3.UNIT_Y
                else -> Vector3.UNIT_Z
            }
            val worldAxisVector = worldFrameA.transformDirection(axisVector)
            val angularVelocityA = bodyA.angularVelocity.dot(worldAxisVector)
            val angularVelocityB = bodyB?.angularVelocity?.dot(worldAxisVector) ?: 0f
            val relativeAngularVelocity = angularVelocityA - angularVelocityB
            val velocityError = _motorTargetVelocity[axis] - relativeAngularVelocity
            val motorTorque = (velocityError / deltaTime).coerceIn(-_motorMaxForce[axis], _motorMaxForce[axis])
            if (abs(motorTorque) > 0.001f) {
                val torqueImpulse = worldAxisVector * motorTorque * deltaTime
                bodyA.applyTorqueImpulse(torqueImpulse)
                bodyB?.applyTorqueImpulse(-torqueImpulse)
                updateAppliedImpulse(abs(motorTorque))
            }
        }
    }
}

/**
 * Factory for creating physics constraints
 */
object PhysicsConstraintFactory {
    fun createPointToPointConstraint(
        id: String,
        bodyA: RigidBody,
        bodyB: RigidBody?,
        pivotA: Vector3,
        pivotB: Vector3
    ): PointToPointConstraint = PointToPointConstraintImpl(id, bodyA, bodyB, pivotA, pivotB)

    fun createHingeConstraint(
        id: String,
        bodyA: RigidBody,
        bodyB: RigidBody?,
        pivotA: Vector3,
        pivotB: Vector3,
        axisA: Vector3,
        axisB: Vector3
    ): HingeConstraint = HingeConstraintImpl(id, bodyA, bodyB, pivotA, pivotB, axisA, axisB)

    fun createSliderConstraint(
        id: String,
        bodyA: RigidBody,
        bodyB: RigidBody?,
        frameA: Matrix4,
        frameB: Matrix4
    ): SliderConstraint = SliderConstraintImpl(id, bodyA, bodyB, frameA, frameB)

    fun createConeTwistConstraint(
        id: String,
        bodyA: RigidBody,
        bodyB: RigidBody?,
        frameA: Matrix4,
        frameB: Matrix4
    ): ConeTwistConstraint = ConeTwistConstraintImpl(id, bodyA, bodyB, frameA, frameB)

    fun createGeneric6DofConstraint(
        id: String,
        bodyA: RigidBody,
        bodyB: RigidBody?,
        frameA: Matrix4,
        frameB: Matrix4
    ): Generic6DofConstraint = Generic6DofConstraintImpl(id, bodyA, bodyB, frameA, frameB)
}
