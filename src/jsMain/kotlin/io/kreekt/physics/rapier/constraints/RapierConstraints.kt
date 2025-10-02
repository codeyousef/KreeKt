/**
 * Rapier Constraints Implementation
 * Provides physics constraints using Rapier's impulse joints
 */
package io.kreekt.physics.rapier.constraints

import io.kreekt.physics.*

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
