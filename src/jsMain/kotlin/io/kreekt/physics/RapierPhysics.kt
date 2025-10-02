/**
 * Rapier Physics Engine Integration for Web Platform
 * Provides high-performance physics simulation using Rapier's WASM bindings
 *
 * This file serves as a public API facade, re-exporting modularized components
 */
package io.kreekt.physics

// Re-export RAPIER external declarations
// (RAPIER external object is defined in rapier/RAPIER.kt)

// Re-export core components
import io.kreekt.physics.rapier.RapierPhysicsEngine
import io.kreekt.physics.rapier.body.RapierRigidBody
import io.kreekt.physics.rapier.world.RapierPhysicsWorld
import io.kreekt.physics.rapier.constraints.RapierConstraint

// Re-export shape implementations
import io.kreekt.physics.rapier.shapes.*

// Platform-specific factory function
fun createRapierPhysicsEngine(): PhysicsEngine = RapierPhysicsEngine()
