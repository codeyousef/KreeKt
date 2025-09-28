package io.kreekt.physics

/**
 * Stub implementation of physics engine for JVM platforms
 * TODO: Implement native library integration (Bullet JNI, etc.)
 */
actual fun createDefaultPhysicsEngine(): PhysicsEngine {
    throw NotImplementedError("Physics engine is not yet implemented for JVM platforms")
}