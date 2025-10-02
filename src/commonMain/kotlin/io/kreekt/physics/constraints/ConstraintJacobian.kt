/**
 * Constraint jacobian for physics solver
 */
package io.kreekt.physics.constraints

import io.kreekt.core.math.Vector3

/**
 * Constraint jacobian for physics solver
 */
data class ConstraintJacobian(
    val linearA: Vector3,
    val angularA: Vector3,
    val linearB: Vector3,
    val angularB: Vector3
)
