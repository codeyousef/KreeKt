/**
 * Comprehensive collision shape hierarchy for physics simulation
 * Supports all common shapes: primitives, convex hulls, meshes, compounds
 */
package io.kreekt.physics

import io.kreekt.core.math.*
import kotlin.math.*

/**
 * Base collision shape implementation
 * Provides common functionality for all shape types
 */
abstract class CollisionShapeImpl : CollisionShape {
    override var margin: Float = 0.04f
        protected set
    override var localScaling: Vector3 = Vector3.ONE
        protected set

    protected var _boundingBox: Box3 = Box3()

    override val boundingBox: Box3
        get() {
            if (_boundingBox.isEmpty()) {
                calculateBoundingBox()
            }
            return _boundingBox
        }

    /**
     * Calculate the bounding box for this shape
     * Must be implemented by subclasses
     */
    protected abstract fun calculateBoundingBox()

    /**
     * Invalidate cached bounding box when shape changes
     */
    protected fun invalidateBoundingBox() {
        _boundingBox = Box3()
    }

    override fun calculateInertia(mass: Float): Matrix3 {
        if (mass <= 0f) return Matrix3.ZERO

        val localInertia = calculateLocalInertia(mass)
        return Matrix3(
            localInertia.x, 0f, 0f,
            0f, localInertia.y, 0f,
            0f, 0f, localInertia.z
        )
    }

    override fun serialize(): ByteArray {
        // Basic serialization - can be extended for specific shape types
        return byteArrayOf(
            shapeType.ordinal.toByte(),
            *margin.toBits().let { byteArrayOf(
                (it shr 24).toByte(),
                (it shr 16).toByte(),
                (it shr 8).toByte(),
                it.toByte()
            ) },
            *localScaling.x.toBits().let { byteArrayOf(
                (it shr 24).toByte(),
                (it shr 16).toByte(),
                (it shr 8).toByte(),
                it.toByte()
            ) },
            *localScaling.y.toBits().let { byteArrayOf(
                (it shr 24).toByte(),
                (it shr 16).toByte(),
                (it shr 8).toByte(),
                it.toByte()
            ) },
            *localScaling.z.toBits().let { byteArrayOf(
                (it shr 24).toByte(),
                (it shr 16).toByte(),
                (it shr 8).toByte(),
                it.toByte()
            ) }
        )
    }
}

/**
 * Box collision shape implementation
 */
class BoxShapeImpl(
    override val halfExtents: Vector3
) : CollisionShapeImpl(), BoxShape {

    override val shapeType: ShapeType = ShapeType.BOX

    init {
        require(halfExtents.x > 0f && halfExtents.y > 0f && halfExtents.z > 0f) {
            "Box half-extents must be positive"
        }
        calculateBoundingBox()
    }

    override fun getHalfExtentsWithMargin(): Vector3 {
        return Vector3(
            halfExtents.x + margin,
            halfExtents.y + margin,
            halfExtents.z + margin
        ) * localScaling
    }

    override fun getHalfExtentsWithoutMargin(): Vector3 {
        return halfExtents * localScaling
    }

    override fun getVolume(): Float {
        val scaledExtents = halfExtents * localScaling
        return 8f * scaledExtents.x * scaledExtents.y * scaledExtents.z
    }

    override fun getSurfaceArea(): Float {
        val scaledExtents = halfExtents * localScaling
        return 8f * (scaledExtents.x * scaledExtents.y +
                    scaledExtents.y * scaledExtents.z +
                    scaledExtents.z * scaledExtents.x)
    }

    override fun isConvex(): Boolean = true
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        val scaledExtents = getHalfExtentsWithMargin()
        return Vector3(
            if (direction.x >= 0f) scaledExtents.x else -scaledExtents.x,
            if (direction.y >= 0f) scaledExtents.y else -scaledExtents.y,
            if (direction.z >= 0f) scaledExtents.z else -scaledExtents.z
        )
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        val scaledExtents = getHalfExtentsWithoutMargin()
        return Vector3(
            if (direction.x >= 0f) scaledExtents.x else -scaledExtents.x,
            if (direction.y >= 0f) scaledExtents.y else -scaledExtents.y,
            if (direction.z >= 0f) scaledExtents.z else -scaledExtents.z
        )
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val scaledExtents = halfExtents * localScaling
        val factor = mass / 3f
        return Vector3(
            factor * (scaledExtents.y * scaledExtents.y + scaledExtents.z * scaledExtents.z),
            factor * (scaledExtents.x * scaledExtents.x + scaledExtents.z * scaledExtents.z),
            factor * (scaledExtents.x * scaledExtents.x + scaledExtents.y * scaledExtents.y)
        )
    }

    override fun calculateBoundingBox() {
        val extentsWithMargin = getHalfExtentsWithMargin()
        _boundingBox = Box3(-extentsWithMargin, extentsWithMargin)
    }

    override fun clone(): CollisionShape = BoxShapeImpl(halfExtents).apply {
        margin = this@BoxShapeImpl.margin
        localScaling = this@BoxShapeImpl.localScaling
    }
}

/**
 * Sphere collision shape implementation
 */
class SphereShapeImpl(
    override val radius: Float
) : CollisionShapeImpl(), SphereShape {

    override val shapeType: ShapeType = ShapeType.SPHERE

    init {
        require(radius > 0f) { "Sphere radius must be positive" }
        calculateBoundingBox()
    }

    override fun getRadiusWithMargin(): Float {
        return (radius + margin) * localScaling.maxComponent()
    }

    override fun getRadiusWithoutMargin(): Float {
        return radius * localScaling.maxComponent()
    }

    override fun getVolume(): Float {
        val scaledRadius = getRadiusWithoutMargin()
        return (4f / 3f) * PI.toFloat() * scaledRadius * scaledRadius * scaledRadius
    }

    override fun getSurfaceArea(): Float {
        val scaledRadius = getRadiusWithoutMargin()
        return 4f * PI.toFloat() * scaledRadius * scaledRadius
    }

    override fun isConvex(): Boolean = true
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        val normalizedDirection = direction.normalized()
        return normalizedDirection * getRadiusWithMargin()
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        val normalizedDirection = direction.normalized()
        return normalizedDirection * getRadiusWithoutMargin()
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val scaledRadius = getRadiusWithoutMargin()
        val inertia = 0.4f * mass * scaledRadius * scaledRadius
        return Vector3(inertia, inertia, inertia)
    }

    override fun calculateBoundingBox() {
        val radiusWithMargin = getRadiusWithMargin()
        val extent = Vector3(radiusWithMargin, radiusWithMargin, radiusWithMargin)
        _boundingBox = Box3(-extent, extent)
    }

    override fun clone(): CollisionShape = SphereShapeImpl(radius).apply {
        margin = this@SphereShapeImpl.margin
        localScaling = this@SphereShapeImpl.localScaling
    }
}

/**
 * Capsule collision shape implementation (pill shape)
 */
class CapsuleShapeImpl(
    override val radius: Float,
    override val height: Float,
    override val upAxis: Int = 1 // Y-axis by default
) : CollisionShapeImpl(), CapsuleShape {

    override val shapeType: ShapeType = ShapeType.CAPSULE

    init {
        require(radius > 0f) { "Capsule radius must be positive" }
        require(height > 0f) { "Capsule height must be positive" }
        require(upAxis in 0..2) { "Up axis must be 0 (X), 1 (Y), or 2 (Z)" }
        calculateBoundingBox()
    }

    override fun getHalfHeight(): Float = height * 0.5f

    override fun getVolume(): Float {
        val scaledRadius = radius * localScaling.maxComponent()
        val scaledHeight = height * when (upAxis) {
            0 -> localScaling.x
            1 -> localScaling.y
            2 -> localScaling.z
            else -> localScaling.y
        }

        // Volume = cylinder + two hemispheres
        val cylinderVolume = PI.toFloat() * scaledRadius * scaledRadius * scaledHeight
        val sphereVolume = (4f / 3f) * PI.toFloat() * scaledRadius * scaledRadius * scaledRadius
        return cylinderVolume + sphereVolume
    }

    override fun getSurfaceArea(): Float {
        val scaledRadius = radius * localScaling.maxComponent()
        val scaledHeight = height * when (upAxis) {
            0 -> localScaling.x
            1 -> localScaling.y
            2 -> localScaling.z
            else -> localScaling.y
        }

        // Surface area = cylinder side + two hemispheres
        val cylinderSide = 2f * PI.toFloat() * scaledRadius * scaledHeight
        val sphereSurface = 4f * PI.toFloat() * scaledRadius * scaledRadius
        return cylinderSide + sphereSurface
    }

    override fun isConvex(): Boolean = true
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        val scaledRadius = (radius + margin) * localScaling.maxComponent()
        val halfHeight = getHalfHeight()

        val upVector = when (upAxis) {
            0 -> Vector3.UNIT_X
            1 -> Vector3.UNIT_Y
            2 -> Vector3.UNIT_Z
            else -> Vector3.UNIT_Y
        }

        val upComponent = direction.dot(upVector)
        val center = if (upComponent > 0f) upVector * halfHeight else -upVector * halfHeight

        val directionInPlane = direction - upVector * upComponent
        val normalizedPlane = if (directionInPlane.length() > 0f) {
            directionInPlane.normalized()
        } else {
            Vector3.ZERO
        }

        return center + normalizedPlane * scaledRadius
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        val scaledRadius = radius * localScaling.maxComponent()
        val halfHeight = getHalfHeight()

        val upVector = when (upAxis) {
            0 -> Vector3.UNIT_X
            1 -> Vector3.UNIT_Y
            2 -> Vector3.UNIT_Z
            else -> Vector3.UNIT_Y
        }

        val upComponent = direction.dot(upVector)
        val center = if (upComponent > 0f) upVector * halfHeight else -upVector * halfHeight

        val directionInPlane = direction - upVector * upComponent
        val normalizedPlane = if (directionInPlane.length() > 0f) {
            directionInPlane.normalized()
        } else {
            Vector3.ZERO
        }

        return center + normalizedPlane * scaledRadius
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val scaledRadius = radius * localScaling.maxComponent()
        val scaledHeight = height * when (upAxis) {
            0 -> localScaling.x
            1 -> localScaling.y
            2 -> localScaling.z
            else -> localScaling.y
        }

        // Approximate inertia for capsule
        val cylinderMass = mass * (scaledHeight / (scaledHeight + 4f * scaledRadius / 3f))
        val sphereMass = mass - cylinderMass

        val radiusSquared = scaledRadius * scaledRadius
        val heightSquared = scaledHeight * scaledHeight

        return when (upAxis) {
            0 -> Vector3(
                cylinderMass * radiusSquared * 0.5f + sphereMass * radiusSquared * 0.4f,
                cylinderMass * (radiusSquared * 0.25f + heightSquared / 12f) + sphereMass * radiusSquared * 0.4f,
                cylinderMass * (radiusSquared * 0.25f + heightSquared / 12f) + sphereMass * radiusSquared * 0.4f
            )
            2 -> Vector3(
                cylinderMass * (radiusSquared * 0.25f + heightSquared / 12f) + sphereMass * radiusSquared * 0.4f,
                cylinderMass * (radiusSquared * 0.25f + heightSquared / 12f) + sphereMass * radiusSquared * 0.4f,
                cylinderMass * radiusSquared * 0.5f + sphereMass * radiusSquared * 0.4f
            )
            else -> Vector3( // Y-axis default
                cylinderMass * (radiusSquared * 0.25f + heightSquared / 12f) + sphereMass * radiusSquared * 0.4f,
                cylinderMass * radiusSquared * 0.5f + sphereMass * radiusSquared * 0.4f,
                cylinderMass * (radiusSquared * 0.25f + heightSquared / 12f) + sphereMass * radiusSquared * 0.4f
            )
        }
    }

    override fun calculateBoundingBox() {
        val scaledRadius = (radius + margin) * localScaling.maxComponent()
        val halfHeight = getHalfHeight()

        val extent = when (upAxis) {
            0 -> Vector3(halfHeight + scaledRadius, scaledRadius, scaledRadius)
            1 -> Vector3(scaledRadius, halfHeight + scaledRadius, scaledRadius)
            2 -> Vector3(scaledRadius, scaledRadius, halfHeight + scaledRadius)
            else -> Vector3(scaledRadius, halfHeight + scaledRadius, scaledRadius)
        }

        _boundingBox = Box3(-extent, extent)
    }

    override fun clone(): CollisionShape = CapsuleShapeImpl(radius, height, upAxis).apply {
        margin = this@CapsuleShapeImpl.margin
        localScaling = this@CapsuleShapeImpl.localScaling
    }
}

/**
 * Cylinder collision shape implementation
 */
class CylinderShapeImpl(
    override val halfExtents: Vector3,
    override val upAxis: Int = 1
) : CollisionShapeImpl(), CylinderShape {

    override val shapeType: ShapeType = ShapeType.CYLINDER

    init {
        require(halfExtents.x > 0f && halfExtents.y > 0f && halfExtents.z > 0f) {
            "Cylinder half-extents must be positive"
        }
        require(upAxis in 0..2) { "Up axis must be 0 (X), 1 (Y), or 2 (Z)" }
        calculateBoundingBox()
    }

    override fun getRadius(): Float {
        return when (upAxis) {
            0 -> maxOf(halfExtents.y, halfExtents.z)
            1 -> maxOf(halfExtents.x, halfExtents.z)
            2 -> maxOf(halfExtents.x, halfExtents.y)
            else -> maxOf(halfExtents.x, halfExtents.z)
        } * localScaling.maxComponent()
    }

    override fun getHalfHeight(): Float {
        return when (upAxis) {
            0 -> halfExtents.x
            1 -> halfExtents.y
            2 -> halfExtents.z
            else -> halfExtents.y
        } * localScaling.componentAt(upAxis)
    }

    override fun getVolume(): Float {
        val scaledRadius = getRadius()
        val scaledHeight = getHalfHeight() * 2f
        return PI.toFloat() * scaledRadius * scaledRadius * scaledHeight
    }

    override fun getSurfaceArea(): Float {
        val scaledRadius = getRadius()
        val scaledHeight = getHalfHeight() * 2f
        // Surface area = 2 * base area + side area
        return 2f * PI.toFloat() * scaledRadius * scaledRadius + 2f * PI.toFloat() * scaledRadius * scaledHeight
    }

    override fun isConvex(): Boolean = true
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        val scaledExtents = (halfExtents + Vector3(margin, margin, margin)) * localScaling

        return when (upAxis) {
            0 -> Vector3(
                if (direction.x >= 0f) scaledExtents.x else -scaledExtents.x,
                if (direction.y >= 0f) scaledExtents.y else -scaledExtents.y,
                if (direction.z >= 0f) scaledExtents.z else -scaledExtents.z
            )
            2 -> Vector3(
                if (direction.x >= 0f) scaledExtents.x else -scaledExtents.x,
                if (direction.y >= 0f) scaledExtents.y else -scaledExtents.y,
                if (direction.z >= 0f) scaledExtents.z else -scaledExtents.z
            )
            else -> Vector3( // Y-axis default
                if (direction.x >= 0f) scaledExtents.x else -scaledExtents.x,
                if (direction.y >= 0f) scaledExtents.y else -scaledExtents.y,
                if (direction.z >= 0f) scaledExtents.z else -scaledExtents.z
            )
        }
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        val scaledExtents = halfExtents * localScaling

        return Vector3(
            if (direction.x >= 0f) scaledExtents.x else -scaledExtents.x,
            if (direction.y >= 0f) scaledExtents.y else -scaledExtents.y,
            if (direction.z >= 0f) scaledExtents.z else -scaledExtents.z
        )
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val scaledRadius = getRadius()
        val scaledHeight = getHalfHeight()

        val radiusSquared = scaledRadius * scaledRadius
        val heightSquared = (scaledHeight * 2f) * (scaledHeight * 2f)

        return when (upAxis) {
            0 -> Vector3(
                mass * radiusSquared * 0.5f,
                mass * (radiusSquared * 0.25f + heightSquared / 12f),
                mass * (radiusSquared * 0.25f + heightSquared / 12f)
            )
            2 -> Vector3(
                mass * (radiusSquared * 0.25f + heightSquared / 12f),
                mass * (radiusSquared * 0.25f + heightSquared / 12f),
                mass * radiusSquared * 0.5f
            )
            else -> Vector3( // Y-axis default
                mass * (radiusSquared * 0.25f + heightSquared / 12f),
                mass * radiusSquared * 0.5f,
                mass * (radiusSquared * 0.25f + heightSquared / 12f)
            )
        }
    }

    override fun calculateBoundingBox() {
        val scaledExtents = (halfExtents + Vector3(margin, margin, margin)) * localScaling
        _boundingBox = Box3(-scaledExtents, scaledExtents)
    }

    override fun clone(): CollisionShape = CylinderShapeImpl(halfExtents, upAxis).apply {
        margin = this@CylinderShapeImpl.margin
        localScaling = this@CylinderShapeImpl.localScaling
    }
}

/**
 * Cone collision shape implementation
 */
class ConeShapeImpl(
    override val radius: Float,
    override val height: Float,
    override val upAxis: Int = 1
) : CollisionShapeImpl(), ConeShape {

    override val shapeType: ShapeType = ShapeType.CONE

    init {
        require(radius > 0f) { "Cone radius must be positive" }
        require(height > 0f) { "Cone height must be positive" }
        require(upAxis in 0..2) { "Up axis must be 0 (X), 1 (Y), or 2 (Z)" }
        calculateBoundingBox()
    }

    override fun getConeRadius(): Float = radius * localScaling.maxComponent()
    override fun getConeHeight(): Float = height * localScaling.componentAt(upAxis)

    override fun getVolume(): Float {
        val scaledRadius = getConeRadius()
        val scaledHeight = getConeHeight()
        return (1f / 3f) * PI.toFloat() * scaledRadius * scaledRadius * scaledHeight
    }

    override fun getSurfaceArea(): Float {
        val scaledRadius = getConeRadius()
        val scaledHeight = getConeHeight()
        val slantHeight = sqrt(scaledRadius * scaledRadius + scaledHeight * scaledHeight)
        // Surface area = base area + lateral area
        return PI.toFloat() * scaledRadius * scaledRadius + PI.toFloat() * scaledRadius * slantHeight
    }

    override fun isConvex(): Boolean = true
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        val scaledRadius = getConeRadius() + margin
        val scaledHeight = getConeHeight()

        val upVector = when (upAxis) {
            0 -> Vector3.UNIT_X
            1 -> Vector3.UNIT_Y
            2 -> Vector3.UNIT_Z
            else -> Vector3.UNIT_Y
        }

        val upComponent = direction.dot(upVector)

        // If direction points toward the tip
        if (upComponent > cos(atan2(scaledRadius, scaledHeight))) {
            return upVector * scaledHeight * 0.5f
        }

        // Otherwise, support is on the base
        val directionInPlane = direction - upVector * upComponent
        val normalizedPlane = if (directionInPlane.length() > 0f) {
            directionInPlane.normalized()
        } else {
            Vector3.ZERO
        }

        return -upVector * scaledHeight * 0.5f + normalizedPlane * scaledRadius
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        val scaledRadius = getConeRadius()
        val scaledHeight = getConeHeight()

        val upVector = when (upAxis) {
            0 -> Vector3.UNIT_X
            1 -> Vector3.UNIT_Y
            2 -> Vector3.UNIT_Z
            else -> Vector3.UNIT_Y
        }

        val upComponent = direction.dot(upVector)

        // If direction points toward the tip
        if (upComponent > cos(atan2(scaledRadius, scaledHeight))) {
            return upVector * scaledHeight * 0.5f
        }

        // Otherwise, support is on the base
        val directionInPlane = direction - upVector * upComponent
        val normalizedPlane = if (directionInPlane.length() > 0f) {
            directionInPlane.normalized()
        } else {
            Vector3.ZERO
        }

        return -upVector * scaledHeight * 0.5f + normalizedPlane * scaledRadius
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        val scaledRadius = getConeRadius()
        val scaledHeight = getConeHeight()

        val radiusSquared = scaledRadius * scaledRadius
        val heightSquared = scaledHeight * scaledHeight

        return when (upAxis) {
            0 -> Vector3(
                mass * radiusSquared * 0.3f,
                mass * (radiusSquared * 0.15f + heightSquared * 0.2f),
                mass * (radiusSquared * 0.15f + heightSquared * 0.2f)
            )
            2 -> Vector3(
                mass * (radiusSquared * 0.15f + heightSquared * 0.2f),
                mass * (radiusSquared * 0.15f + heightSquared * 0.2f),
                mass * radiusSquared * 0.3f
            )
            else -> Vector3( // Y-axis default
                mass * (radiusSquared * 0.15f + heightSquared * 0.2f),
                mass * radiusSquared * 0.3f,
                mass * (radiusSquared * 0.15f + heightSquared * 0.2f)
            )
        }
    }

    override fun calculateBoundingBox() {
        val scaledRadius = getConeRadius() + margin
        val scaledHeight = getConeHeight()

        val extent = when (upAxis) {
            0 -> Vector3(scaledHeight * 0.5f, scaledRadius, scaledRadius)
            1 -> Vector3(scaledRadius, scaledHeight * 0.5f, scaledRadius)
            2 -> Vector3(scaledRadius, scaledRadius, scaledHeight * 0.5f)
            else -> Vector3(scaledRadius, scaledHeight * 0.5f, scaledRadius)
        }

        _boundingBox = Box3(-extent, extent)
    }

    override fun clone(): CollisionShape = ConeShapeImpl(radius, height, upAxis).apply {
        margin = this@ConeShapeImpl.margin
        localScaling = this@ConeShapeImpl.localScaling
    }
}

/**
 * Convex hull collision shape implementation
 */
class ConvexHullShapeImpl(
    initialVertices: FloatArray
) : CollisionShapeImpl(), ConvexHullShape {

    override val shapeType: ShapeType = ShapeType.CONVEX_HULL

    private val _vertices = mutableListOf<Float>()
    override val vertices: FloatArray get() = _vertices.toFloatArray()
    override val numVertices: Int get() = _vertices.size / 3

    init {
        require(initialVertices.size % 3 == 0) { "Vertices array size must be multiple of 3" }
        require(initialVertices.size >= 12) { "Convex hull needs at least 4 vertices (12 floats)" }

        _vertices.addAll(initialVertices.toList())
        calculateBoundingBox()
    }

    override fun addPoint(point: Vector3, recalculateLocalAABB: Boolean) {
        _vertices.addAll(listOf(point.x, point.y, point.z))
        if (recalculateLocalAABB) {
            invalidateBoundingBox()
        }
    }

    override fun getScaledPoint(index: Int): Vector3 {
        require(index in 0 until numVertices) { "Vertex index out of range" }
        val baseIndex = index * 3
        return Vector3(
            _vertices[baseIndex] * localScaling.x,
            _vertices[baseIndex + 1] * localScaling.y,
            _vertices[baseIndex + 2] * localScaling.z
        )
    }

    override fun getUnscaledPoints(): List<Vector3> {
        return (0 until numVertices).map { index ->
            val baseIndex = index * 3
            Vector3(
                _vertices[baseIndex],
                _vertices[baseIndex + 1],
                _vertices[baseIndex + 2]
            )
        }
    }

    override fun optimizeConvexHull() {
        // Simplified optimization - remove duplicate points
        val uniqueVertices = mutableSetOf<Vector3>()
        val optimizedVertices = mutableListOf<Float>()

        for (i in 0 until numVertices) {
            val vertex = getScaledPoint(i)
            if (uniqueVertices.add(vertex)) {
                optimizedVertices.addAll(listOf(vertex.x, vertex.y, vertex.z))
            }
        }

        _vertices.clear()
        _vertices.addAll(optimizedVertices)
        invalidateBoundingBox()
    }

    override fun getVolume(): Float {
        // Simplified volume calculation for convex hull
        // In practice, this would use a more sophisticated algorithm
        val boundingVolume = boundingBox.let { box ->
            val size = box.max - box.min
            size.x * size.y * size.z
        }
        return boundingVolume * 0.5f // Rough approximation
    }

    override fun getSurfaceArea(): Float {
        // Simplified surface area calculation
        // In practice, this would compute the actual hull surface
        val boundingArea = boundingBox.let { box ->
            val size = box.max - box.min
            2f * (size.x * size.y + size.y * size.z + size.z * size.x)
        }
        return boundingArea * 0.7f // Rough approximation
    }

    override fun isConvex(): Boolean = true
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        var maxDot = Float.NEGATIVE_INFINITY
        var supportVertex = Vector3.ZERO

        for (i in 0 until numVertices) {
            val vertex = getScaledPoint(i)
            val dot = vertex.dot(direction)
            if (dot > maxDot) {
                maxDot = dot
                supportVertex = vertex
            }
        }

        // Add margin in the direction of the normal
        val normalizedDirection = direction.normalized()
        return supportVertex + normalizedDirection * margin
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        var maxDot = Float.NEGATIVE_INFINITY
        var supportVertex = Vector3.ZERO

        for (i in 0 until numVertices) {
            val vertex = getScaledPoint(i)
            val dot = vertex.dot(direction)
            if (dot > maxDot) {
                maxDot = dot
                supportVertex = vertex
            }
        }

        return supportVertex
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        // Simplified inertia calculation for convex hull
        // Uses bounding box approximation
        val size = boundingBox.max - boundingBox.min
        val factor = mass / 12f
        return Vector3(
            factor * (size.y * size.y + size.z * size.z),
            factor * (size.x * size.x + size.z * size.z),
            factor * (size.x * size.x + size.y * size.y)
        )
    }

    override fun calculateBoundingBox() {
        if (numVertices == 0) {
            _boundingBox = Box3()
            return
        }

        var min = getScaledPoint(0)
        var max = min

        for (i in 1 until numVertices) {
            val vertex = getScaledPoint(i)
            min = Vector3(
                minOf(min.x, vertex.x),
                minOf(min.y, vertex.y),
                minOf(min.z, vertex.z)
            )
            max = Vector3(
                maxOf(max.x, vertex.x),
                maxOf(max.y, vertex.y),
                maxOf(max.z, vertex.z)
            )
        }

        // Add margin
        val marginVec = Vector3(margin, margin, margin)
        _boundingBox = Box3(min - marginVec, max + marginVec)
    }

    override fun clone(): CollisionShape = ConvexHullShapeImpl(vertices).apply {
        margin = this@ConvexHullShapeImpl.margin
        localScaling = this@ConvexHullShapeImpl.localScaling
    }
}

/**
 * Triangle mesh collision shape implementation (for static geometry)
 */
class TriangleMeshShapeImpl(
    override val vertices: FloatArray,
    override val indices: IntArray
) : CollisionShapeImpl(), TriangleMeshShape {

    override val shapeType: ShapeType = ShapeType.TRIANGLE_MESH
    override val triangleCount: Int = indices.size / 3

    private var _bvh: MeshBVH? = null

    init {
        require(vertices.size % 3 == 0) { "Vertices array size must be multiple of 3" }
        require(indices.size % 3 == 0) { "Indices array size must be multiple of 3" }
        require(indices.all { it >= 0 && it < vertices.size / 3 }) { "All indices must be valid vertex indices" }

        calculateBoundingBox()
    }

    override fun getTriangle(index: Int): Triangle {
        require(index in 0 until triangleCount) { "Triangle index out of range" }

        val baseIndex = index * 3
        val v0Index = indices[baseIndex] * 3
        val v1Index = indices[baseIndex + 1] * 3
        val v2Index = indices[baseIndex + 2] * 3

        return Triangle(
            Vector3(vertices[v0Index], vertices[v0Index + 1], vertices[v0Index + 2]) * localScaling,
            Vector3(vertices[v1Index], vertices[v1Index + 1], vertices[v1Index + 2]) * localScaling,
            Vector3(vertices[v2Index], vertices[v2Index + 1], vertices[v2Index + 2]) * localScaling
        )
    }

    override fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3, aabbMax: Vector3) {
        for (i in 0 until triangleCount) {
            val triangle = getTriangle(i)

            // Simple AABB test
            val triangleBounds = Box3().apply {
                expandByPoint(triangle.vertex0)
                expandByPoint(triangle.vertex1)
                expandByPoint(triangle.vertex2)
            }

            if (triangleBounds.intersectsBox(Box3(aabbMin, aabbMax))) {
                callback.processTriangle(triangle, 0, i)
            }
        }
    }

    override fun buildBVH(): MeshBVH {
        if (_bvh == null) {
            _bvh = buildBVHRecursive(0, triangleCount)
        }
        return _bvh!!
    }

    private fun buildBVHRecursive(startTriangle: Int, triangleCount: Int): MeshBVH {
        // Simplified BVH construction
        val triangles = (startTriangle until startTriangle + triangleCount).map { getTriangle(it) }

        // Calculate bounding box for all triangles
        val bounds = Box3()
        triangles.forEach { triangle ->
            bounds.expandByPoint(triangle.vertex0)
            bounds.expandByPoint(triangle.vertex1)
            bounds.expandByPoint(triangle.vertex2)
        }

        // Create leaf node for small triangle counts
        if (triangleCount <= 4) {
            val node = BVHNode(
                bounds = bounds,
                leftChild = -1,
                rightChild = -1,
                triangleOffset = startTriangle,
                triangleCount = triangleCount
            )
            return MeshBVH(listOf(node), triangles)
        }

        // For larger counts, this would implement proper spatial partitioning
        // For now, create a simple single-node BVH
        val node = BVHNode(
            bounds = bounds,
            leftChild = -1,
            rightChild = -1,
            triangleOffset = startTriangle,
            triangleCount = triangleCount
        )

        return MeshBVH(listOf(node), triangles)
    }

    override fun getVolume(): Float {
        // Triangle meshes are typically hollow, so volume calculation is complex
        // Return bounding box volume as approximation
        val size = boundingBox.max - boundingBox.min
        return size.x * size.y * size.z
    }

    override fun getSurfaceArea(): Float {
        var totalArea = 0f
        for (i in 0 until triangleCount) {
            val triangle = getTriangle(i)
            val edge1 = triangle.vertex1 - triangle.vertex0
            val edge2 = triangle.vertex2 - triangle.vertex0
            val cross = edge1.cross(edge2)
            totalArea += cross.length() * 0.5f
        }
        return totalArea
    }

    override fun isConvex(): Boolean = false
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        var maxDot = Float.NEGATIVE_INFINITY
        var supportVertex = Vector3.ZERO

        // Check all vertices
        for (i in 0 until vertices.size / 3) {
            val vertex = Vector3(
                vertices[i * 3] * localScaling.x,
                vertices[i * 3 + 1] * localScaling.y,
                vertices[i * 3 + 2] * localScaling.z
            )
            val dot = vertex.dot(direction)
            if (dot > maxDot) {
                maxDot = dot
                supportVertex = vertex
            }
        }

        return supportVertex
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        return localGetSupportingVertex(direction)
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        // Triangle mesh shapes are typically static, so return zero inertia
        return Vector3.ZERO
    }

    override fun calculateBoundingBox() {
        if (vertices.isEmpty()) {
            _boundingBox = Box3()
            return
        }

        var min = Vector3(vertices[0], vertices[1], vertices[2]) * localScaling
        var max = min

        for (i in 1 until vertices.size / 3) {
            val vertex = Vector3(
                vertices[i * 3] * localScaling.x,
                vertices[i * 3 + 1] * localScaling.y,
                vertices[i * 3 + 2] * localScaling.z
            )
            min = Vector3(
                minOf(min.x, vertex.x),
                minOf(min.y, vertex.y),
                minOf(min.z, vertex.z)
            )
            max = Vector3(
                maxOf(max.x, vertex.x),
                maxOf(max.y, vertex.y),
                maxOf(max.z, vertex.z)
            )
        }

        _boundingBox = Box3(min, max)
    }

    override fun clone(): CollisionShape = TriangleMeshShapeImpl(vertices.copyOf(), indices.copyOf()).apply {
        margin = this@TriangleMeshShapeImpl.margin
        localScaling = this@TriangleMeshShapeImpl.localScaling
    }
}

/**
 * Heightfield collision shape implementation (for terrain)
 */
class HeightfieldShapeImpl(
    override val width: Int,
    override val height: Int,
    initialHeightData: FloatArray,
    override val maxHeight: Float,
    override val minHeight: Float,
    override val upAxis: Int = 1
) : CollisionShapeImpl(), HeightfieldShape {

    override val shapeType: ShapeType = ShapeType.HEIGHTFIELD

    private val _heightData = initialHeightData.copyOf()
    override val heightData: FloatArray get() = _heightData.copyOf()

    init {
        require(width > 0 && height > 0) { "Heightfield dimensions must be positive" }
        require(initialHeightData.size == width * height) { "Height data size must match width * height" }
        require(maxHeight >= minHeight) { "Max height must be >= min height" }
        require(upAxis in 0..2) { "Up axis must be 0 (X), 1 (Y), or 2 (Z)" }

        calculateBoundingBox()
    }

    override fun getHeightAtPoint(x: Float, z: Float): Float {
        // Clamp coordinates to heightfield bounds
        val clampedX = x.coerceIn(0f, width - 1f)
        val clampedZ = z.coerceIn(0f, height - 1f)

        // Get integer coordinates
        val x0 = clampedX.toInt()
        val z0 = clampedZ.toInt()
        val x1 = minOf(x0 + 1, width - 1)
        val z1 = minOf(z0 + 1, height - 1)

        // Get fractional parts
        val fx = clampedX - x0
        val fz = clampedZ - z0

        // Sample height values
        val h00 = _heightData[z0 * width + x0]
        val h10 = _heightData[z0 * width + x1]
        val h01 = _heightData[z1 * width + x0]
        val h11 = _heightData[z1 * width + x1]

        // Bilinear interpolation
        val h0 = h00 * (1f - fx) + h10 * fx
        val h1 = h01 * (1f - fx) + h11 * fx
        return h0 * (1f - fz) + h1 * fz
    }

    override fun setHeightValue(x: Int, z: Int, height: Float) {
        require(x in 0 until width && z in 0 until this.height) { "Coordinates out of bounds" }
        require(height in minHeight..maxHeight) { "Height value out of range" }

        _heightData[z * width + x] = height
        invalidateBoundingBox()
    }

    override fun getVolume(): Float {
        // Volume calculation for heightfield is complex and depends on interpretation
        // Return approximate volume based on average height
        val averageHeight = _heightData.average().toFloat()
        val baseArea = width * height * localScaling.x * localScaling.z
        return baseArea * averageHeight * localScaling.y
    }

    override fun getSurfaceArea(): Float {
        // Approximate surface area calculation
        var totalArea = 0f

        for (z in 0 until height - 1) {
            for (x in 0 until width - 1) {
                // Calculate area of two triangles forming each heightfield cell
                val h00 = _heightData[z * width + x]
                val h10 = _heightData[z * width + x + 1]
                val h01 = _heightData[(z + 1) * width + x]
                val h11 = _heightData[(z + 1) * width + x + 1]

                val v00 = Vector3(x.toFloat(), h00, z.toFloat()) * localScaling
                val v10 = Vector3((x + 1).toFloat(), h10, z.toFloat()) * localScaling
                val v01 = Vector3(x.toFloat(), h01, (z + 1).toFloat()) * localScaling
                val v11 = Vector3((x + 1).toFloat(), h11, (z + 1).toFloat()) * localScaling

                // Triangle 1: v00, v10, v01
                val edge1 = v10 - v00
                val edge2 = v01 - v00
                totalArea += edge1.cross(edge2).length() * 0.5f

                // Triangle 2: v10, v11, v01
                val edge3 = v11 - v10
                val edge4 = v01 - v10
                totalArea += edge3.cross(edge4).length() * 0.5f
            }
        }

        return totalArea
    }

    override fun isConvex(): Boolean = false
    override fun isCompound(): Boolean = false

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        // Find the heightfield vertex most in the given direction
        var maxDot = Float.NEGATIVE_INFINITY
        var supportVertex = Vector3.ZERO

        for (z in 0 until height) {
            for (x in 0 until width) {
                val heightValue = _heightData[z * width + x]
                val vertex = Vector3(x.toFloat(), heightValue, z.toFloat()) * localScaling
                val dot = vertex.dot(direction)
                if (dot > maxDot) {
                    maxDot = dot
                    supportVertex = vertex
                }
            }
        }

        return supportVertex
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        return localGetSupportingVertex(direction)
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        // Heightfields are typically static, return zero inertia
        return Vector3.ZERO
    }

    override fun calculateBoundingBox() {
        val minVec = Vector3(0f, minHeight, 0f) * localScaling
        val maxVec = Vector3(width.toFloat(), maxHeight, height.toFloat()) * localScaling
        _boundingBox = Box3(minVec, maxVec)
    }

    override fun clone(): CollisionShape = HeightfieldShapeImpl(
        width, height, _heightData, maxHeight, minHeight, upAxis
    ).apply {
        margin = this@HeightfieldShapeImpl.margin
        localScaling = this@HeightfieldShapeImpl.localScaling
    }
}

/**
 * Compound collision shape implementation (combines multiple shapes)
 */
class CompoundShapeImpl : CollisionShapeImpl(), CompoundShape {

    override val shapeType: ShapeType = ShapeType.COMPOUND

    private val _childShapes = mutableListOf<ChildShape>()
    override val childShapes: List<ChildShape> get() = _childShapes.toList()

    override fun addChildShape(transform: Matrix4, shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            _childShapes.add(ChildShape(transform, shape))
            invalidateBoundingBox()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.ShapeCreationFailed("Failed to add child shape", e))
        }
    }

    override fun removeChildShape(shape: CollisionShape): PhysicsResult<Unit> {
        return try {
            _childShapes.removeAll { it.shape == shape }
            invalidateBoundingBox()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.ShapeCreationFailed("Failed to remove child shape", e))
        }
    }

    override fun removeChildShapeByIndex(index: Int): PhysicsResult<Unit> {
        return try {
            require(index in 0 until _childShapes.size) { "Child shape index out of range" }
            _childShapes.removeAt(index)
            invalidateBoundingBox()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.ShapeCreationFailed("Failed to remove child shape by index", e))
        }
    }

    override fun updateChildTransform(index: Int, transform: Matrix4): PhysicsResult<Unit> {
        return try {
            require(index in 0 until _childShapes.size) { "Child shape index out of range" }
            val childShape = _childShapes[index]
            _childShapes[index] = ChildShape(transform, childShape.shape)
            invalidateBoundingBox()
            PhysicsResult.Success(Unit)
        } catch (e: Exception) {
            PhysicsResult.Error(PhysicsException.ShapeCreationFailed("Failed to update child transform", e))
        }
    }

    override fun recalculateLocalAabb() {
        invalidateBoundingBox()
    }

    override fun getVolume(): Float {
        return _childShapes.sumOf { it.shape.getVolume().toDouble() }.toFloat()
    }

    override fun getSurfaceArea(): Float {
        return _childShapes.sumOf { it.shape.getSurfaceArea().toDouble() }.toFloat()
    }

    override fun isConvex(): Boolean = false
    override fun isCompound(): Boolean = true

    override fun localGetSupportingVertex(direction: Vector3): Vector3 {
        var maxDot = Float.NEGATIVE_INFINITY
        var supportVertex = Vector3.ZERO

        for (childShape in _childShapes) {
            // Transform direction to child's local space
            val localDirection = childShape.transform.inverse().transformDirection(direction)
            val localSupport = childShape.shape.localGetSupportingVertex(localDirection)

            // Transform support vertex to compound's local space
            val worldSupport = childShape.transform.transformPoint(localSupport)

            val dot = worldSupport.dot(direction)
            if (dot > maxDot) {
                maxDot = dot
                supportVertex = worldSupport
            }
        }

        return supportVertex
    }

    override fun localGetSupportingVertexWithoutMargin(direction: Vector3): Vector3 {
        var maxDot = Float.NEGATIVE_INFINITY
        var supportVertex = Vector3.ZERO

        for (childShape in _childShapes) {
            // Transform direction to child's local space
            val localDirection = childShape.transform.inverse().transformDirection(direction)
            val localSupport = childShape.shape.localGetSupportingVertexWithoutMargin(localDirection)

            // Transform support vertex to compound's local space
            val worldSupport = childShape.transform.transformPoint(localSupport)

            val dot = worldSupport.dot(direction)
            if (dot > maxDot) {
                maxDot = dot
                supportVertex = worldSupport
            }
        }

        return supportVertex
    }

    override fun calculateLocalInertia(mass: Float): Vector3 {
        if (_childShapes.isEmpty()) return Vector3.ZERO

        // Distribute mass among child shapes based on their volume
        val totalVolume = getVolume()
        if (totalVolume <= 0f) return Vector3.ZERO

        var totalInertia = Matrix3.ZERO

        for (childShape in _childShapes) {
            val childVolume = childShape.shape.getVolume()
            val childMass = mass * (childVolume / totalVolume)
            val childInertia = childShape.shape.calculateInertia(childMass)

            // Transform inertia tensor to compound's coordinate system
            val rotation = childShape.transform.getRotation()
            val rotationMatrix = Matrix3.fromQuaternion(rotation)
            val transformedInertia = rotationMatrix * childInertia * rotationMatrix.transpose()

            totalInertia = totalInertia + transformedInertia
        }

        return Vector3(totalInertia.m00, totalInertia.m11, totalInertia.m22)
    }

    override fun calculateBoundingBox() {
        if (_childShapes.isEmpty()) {
            _boundingBox = Box3()
            return
        }

        _boundingBox = Box3()

        for (childShape in _childShapes) {
            val childBounds = childShape.shape.boundingBox

            // Transform child bounding box to compound's coordinate system
            val corners = listOf(
                Vector3(childBounds.min.x, childBounds.min.y, childBounds.min.z),
                Vector3(childBounds.max.x, childBounds.min.y, childBounds.min.z),
                Vector3(childBounds.min.x, childBounds.max.y, childBounds.min.z),
                Vector3(childBounds.max.x, childBounds.max.y, childBounds.min.z),
                Vector3(childBounds.min.x, childBounds.min.y, childBounds.max.z),
                Vector3(childBounds.max.x, childBounds.min.y, childBounds.max.z),
                Vector3(childBounds.min.x, childBounds.max.y, childBounds.max.z),
                Vector3(childBounds.max.x, childBounds.max.y, childBounds.max.z)
            )

            for (corner in corners) {
                val transformedCorner = childShape.transform.transformPoint(corner)
                _boundingBox.expandByPoint(transformedCorner)
            }
        }

        // Apply local scaling and margin
        val marginVec = Vector3(margin, margin, margin)
        _boundingBox = Box3(
            (_boundingBox.min - marginVec) * localScaling,
            (_boundingBox.max + marginVec) * localScaling
        )
    }

    override fun clone(): CollisionShape = CompoundShapeImpl().apply {
        margin = this@CompoundShapeImpl.margin
        localScaling = this@CompoundShapeImpl.localScaling
        for (childShape in this@CompoundShapeImpl._childShapes) {
            addChildShape(childShape.transform, childShape.shape.clone())
        }
    }
}

/**
 * Factory object for creating collision shapes
 */
object CollisionShapeFactory {

    fun createBox(halfExtents: Vector3): BoxShape = BoxShapeImpl(halfExtents)

    fun createSphere(radius: Float): SphereShape = SphereShapeImpl(radius)

    fun createCapsule(radius: Float, height: Float, upAxis: Int = 1): CapsuleShape =
        CapsuleShapeImpl(radius, height, upAxis)

    fun createCylinder(halfExtents: Vector3, upAxis: Int = 1): CylinderShape =
        CylinderShapeImpl(halfExtents, upAxis)

    fun createCone(radius: Float, height: Float, upAxis: Int = 1): ConeShape =
        ConeShapeImpl(radius, height, upAxis)

    fun createConvexHull(vertices: FloatArray): ConvexHullShape =
        ConvexHullShapeImpl(vertices)

    fun createTriangleMesh(vertices: FloatArray, indices: IntArray): TriangleMeshShape =
        TriangleMeshShapeImpl(vertices, indices)

    fun createHeightfield(
        width: Int,
        height: Int,
        heightData: FloatArray,
        maxHeight: Float,
        minHeight: Float,
        upAxis: Int = 1
    ): HeightfieldShape = HeightfieldShapeImpl(width, height, heightData, maxHeight, minHeight, upAxis)

    fun createCompound(): CompoundShape = CompoundShapeImpl()
}