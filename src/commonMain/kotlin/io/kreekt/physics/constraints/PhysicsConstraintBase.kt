/**
 * Base constraint implementation with common functionality
 */
package io.kreekt.physics.constraints

import io.kreekt.core.math.Matrix4
import io.kreekt.physics.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Constraint jacobian for physics solver
 */
data class ConstraintJacobian(
    val linearA: io.kreekt.core.math.Vector3,
    val angularA: io.kreekt.core.math.Vector3,
    val linearB: io.kreekt.core.math.Vector3,
    val angularB: io.kreekt.core.math.Vector3
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
     * Called when constraint parameters change
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
     * Check if constraint is valid
     */
    fun isValid(): Boolean {
        return bodyA.isActive() && (bodyB?.isActive() != false)
    }
}
