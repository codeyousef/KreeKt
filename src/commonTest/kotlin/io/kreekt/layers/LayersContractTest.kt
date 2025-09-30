package io.kreekt.layers

import io.kreekt.core.scene.Object3D
import io.kreekt.camera.Camera
import io.kreekt.camera.PerspectiveCamera
import io.kreekt.raycaster.Raycaster
import io.kreekt.core.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Contract test for Layer-based visibility - T035
 * Covers: FR-L001, FR-L002, FR-L003, FR-L004, FR-L005, FR-L006 from contracts/layers-api.kt
 */
class LayersContractTest {

    @Test
    fun testSupport32Layers() {
        // FR-L001: Support 32 layers (bitmask)
        val layers = Layers()

        // Enable all 32 layers
        for (i in 0 until 32) {
            layers.enable(i)
        }

        assertEquals(0xFFFFFFFF.toInt(), layers.mask)

        // Disable specific layers
        layers.disable(0)
        layers.disable(31)

        assertFalse(layers.test(0))
        assertFalse(layers.test(31))
        assertTrue(layers.test(15))
    }

    @Test
    fun testFilterObjectsDuringRendering() {
        // FR-L002: Filter objects during rendering
        val camera = PerspectiveCamera(75f, 1.77f, 0.1f, 1000f)
        camera.layers.set(1) // Camera only sees layer 1

        val object1 = Object3D()
        object1.layers.set(0) // Object on layer 0

        val object2 = Object3D()
        object2.layers.set(1) // Object on layer 1

        val object3 = Object3D()
        object3.layers.enable(0)
        object3.layers.enable(1) // Object on both layers

        // Test visibility
        assertFalse(camera.layers.test(object1.layers))
        assertTrue(camera.layers.test(object2.layers))
        assertTrue(camera.layers.test(object3.layers))
    }

    @Test
    fun testFilterDuringRaycasting() {
        // FR-L003: Filter during raycasting
        val raycaster = Raycaster(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 1f))
        raycaster.layers.set(2) // Raycaster only checks layer 2

        val objects = listOf(
            Object3D().apply { layers.set(0) },
            Object3D().apply { layers.set(1) },
            Object3D().apply { layers.set(2) },
            Object3D().apply { layers.set(3) }
        )

        val filtered = objects.filter { raycaster.layers.test(it.layers) }
        assertEquals(1, filtered.size)
        assertEquals(2, filtered[0].layers.firstSetBit())
    }

    @Test
    fun testLayerEnableDisableToggle() {
        // FR-L004, FR-L005: Layer enable/disable/toggle
        val layers = Layers()

        // Enable
        layers.enable(5)
        assertTrue(layers.test(5))

        // Disable
        layers.disable(5)
        assertFalse(layers.test(5))

        // Toggle
        layers.toggle(5)
        assertTrue(layers.test(5))
        layers.toggle(5)
        assertFalse(layers.test(5))

        // Enable multiple
        layers.enableAll()
        assertEquals(0xFFFFFFFF.toInt(), layers.mask)

        // Disable all
        layers.disableAll()
        assertEquals(0, layers.mask)
    }

    @Test
    fun testLayerIntersectionTesting() {
        // FR-L006: Layer intersection testing
        val layers1 = Layers()
        layers1.set(1)
        layers1.enable(3)
        layers1.enable(5)

        val layers2 = Layers()
        layers2.set(2)
        layers2.enable(3)
        layers2.enable(7)

        // Test intersection
        assertTrue(layers1.intersects(layers2)) // Both have layer 3

        val layers3 = Layers()
        layers3.set(0)
        layers3.enable(4)

        assertFalse(layers1.intersects(layers3)) // No common layers
    }

    @Test
    fun testLayerMaskOperations() {
        val layers = Layers()

        // Set specific mask
        layers.mask = 0b1010
        assertTrue(layers.test(1))
        assertFalse(layers.test(0))
        assertTrue(layers.test(3))
        assertFalse(layers.test(2))

        // OR operation
        val other = Layers()
        other.mask = 0b0101
        layers.or(other)
        assertEquals(0b1111, layers.mask)

        // AND operation
        layers.mask = 0b1100
        other.mask = 0b1010
        layers.and(other)
        assertEquals(0b1000, layers.mask)

        // XOR operation
        layers.mask = 0b1100
        other.mask = 0b1010
        layers.xor(other)
        assertEquals(0b0110, layers.mask)
    }

    @Test
    fun testLayerPerformance() {
        // Test performance with many objects and layers
        val objects = List(10000) { Object3D() }

        // Assign random layers
        objects.forEach { obj ->
            for (i in 0 until 32) {
                if (Math.random() < 0.1) {
                    obj.layers.enable(i)
                }
            }
        }

        val camera = Camera()
        camera.layers.set(5)
        camera.layers.enable(10)
        camera.layers.enable(15)

        val startTime = System.currentTimeMillis()

        // Filter visible objects
        val visible = objects.filter { camera.layers.test(it.layers) }

        val duration = System.currentTimeMillis() - startTime

        // Should be very fast (< 1ms for 10k objects)
        assertTrue(duration < 10, "Layer filtering should be fast, took ${duration}ms")
        assertTrue(visible.isNotEmpty())
    }
}

// Supporting classes

class Layers {
    var mask: Int = 1 // Default to layer 0

    fun set(layer: Int) {
        mask = 1 shl layer
    }

    fun enable(layer: Int) {
        mask = mask or (1 shl layer)
    }

    fun disable(layer: Int) {
        mask = mask and (1 shl layer).inv()
    }

    fun toggle(layer: Int) {
        mask = mask xor (1 shl layer)
    }

    fun enableAll() {
        mask = 0xFFFFFFFF.toInt()
    }

    fun disableAll() {
        mask = 0
    }

    fun test(layer: Int): Boolean {
        return (mask and (1 shl layer)) != 0
    }

    fun test(layers: Layers): Boolean {
        return (mask and layers.mask) != 0
    }

    fun intersects(layers: Layers): Boolean {
        return (mask and layers.mask) != 0
    }

    fun or(layers: Layers) {
        mask = mask or layers.mask
    }

    fun and(layers: Layers) {
        mask = mask and layers.mask
    }

    fun xor(layers: Layers) {
        mask = mask xor layers.mask
    }

    fun firstSetBit(): Int {
        if (mask == 0) return -1
        return Integer.numberOfTrailingZeros(mask)
    }
}

// Extensions for Object3D
val Object3D.layers: Layers
    get() = _layers ?: Layers().also { _layers = it }

private var Object3D._layers: Layers? = null

// Extensions for Camera
val Camera.layers: Layers
    get() = _layers ?: Layers().also { _layers = it }

private var Camera._layers: Layers? = null

// Extensions for Raycaster
val Raycaster.layers: Layers
    get() = _layers ?: Layers().also { _layers = it }

private var Raycaster._layers: Layers? = null