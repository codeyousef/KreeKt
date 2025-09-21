package io.kreekt.physics

/**
 * Native implementation - creates a no-op physics engine that throws at runtime
 */
actual fun createDefaultPhysicsEngine(): PhysicsEngine {
    throw UnsupportedOperationException("Physics engine not supported on Native platforms")
}