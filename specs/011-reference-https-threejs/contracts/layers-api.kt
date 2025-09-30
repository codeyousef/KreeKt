/**
 * Layers API Contract
 * Maps FR-L001 through FR-L006
 *
 * Constitutional Requirements:
 * - Efficient layer-based selective rendering
 * - Type-safe layer management
 * - Performance: <1ms layer filtering per frame
 */

package io.kreekt.layers

import io.kreekt.core.Object3D
import io.kreekt.camera.Camera

/**
 * FR-L001, FR-L002: Layer-based visibility system
 *
 * Test Contract:
 * - MUST support 32 layers (0-31) using bitmask
 * - MUST filter objects during rendering
 * - MUST filter objects during raycasting
 * - MUST be memory efficient (single Int per object)
 */
class Layers {
    /**
     * Layer membership bitmask
     * Bit N = member of layer N
     */
    private var mask: Int = 1  // Default: layer 0 only

    /**
     * FR-L003: Set object to single layer
     *
     * @param layer Layer number (0-31)
     *
     * Test Contract:
     * - MUST clear all other layers
     * - MUST set specified layer only
     * - MUST validate layer range
     */
    fun set(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31, got $layer" }
        mask = 1 shl layer
    }

    /**
     * FR-L004: Enable additional layer
     *
     * @param layer Layer number (0-31)
     *
     * Test Contract:
     * - MUST add layer to existing membership
     * - MUST preserve other layer memberships
     */
    fun enable(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31, got $layer" }
        mask = mask or (1 shl layer)
    }

    /**
     * Disable layer
     *
     * @param layer Layer number (0-31)
     *
     * Test Contract:
     * - MUST remove layer from membership
     * - MUST preserve other layer memberships
     */
    fun disable(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31, got $layer" }
        mask = mask and (1 shl layer).inv()
    }

    /**
     * Toggle layer membership
     *
     * @param layer Layer number (0-31)
     *
     * Test Contract:
     * - MUST enable if currently disabled
     * - MUST disable if currently enabled
     */
    fun toggle(layer: Int) {
        require(layer in 0..31) { "Layer must be 0-31, got $layer" }
        mask = mask xor (1 shl layer)
    }

    /**
     * FR-L005: Test layer intersection
     *
     * @param layers Other layers object to test against
     * @return true if any layers overlap
     *
     * Test Contract:
     * - MUST return true if ANY layer is shared
     * - MUST use bitwise AND for efficiency
     * - Used by renderer to filter visible objects
     */
    fun test(layers: Layers): Boolean {
        return (mask and layers.mask) != 0
    }

    /**
     * Check if specific layer is enabled
     *
     * @param layer Layer number (0-31)
     * @return true if layer is enabled
     */
    fun isEnabled(layer: Int): Boolean {
        require(layer in 0..31) { "Layer must be 0-31, got $layer" }
        return (mask and (1 shl layer)) != 0
    }

    /**
     * Enable all layers (visible to all cameras)
     *
     * Test Contract:
     * - MUST set all 32 bits
     */
    fun enableAll() {
        mask = -1  // All bits set (0xFFFFFFFF)
    }

    /**
     * Disable all layers (invisible to all cameras)
     *
     * Test Contract:
     * - MUST clear all 32 bits
     */
    fun disableAll() {
        mask = 0
    }

    /**
     * Get raw bitmask
     */
    fun getMask(): Int = mask

    /**
     * Set raw bitmask directly
     *
     * Test Contract:
     * - MUST allow arbitrary mask values
     * - Useful for serialization
     */
    fun setMask(mask: Int) {
        this.mask = mask
    }

    /**
     * Clone layers
     */
    fun clone(): Layers {
        val cloned = Layers()
        cloned.mask = this.mask
        return cloned
    }

    /**
     * Copy from another layers
     */
    fun copy(layers: Layers) {
        this.mask = layers.mask
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Layers) return false
        return mask == other.mask
    }

    override fun hashCode(): Int = mask

    override fun toString(): String {
        val enabledLayers = (0..31).filter { isEnabled(it) }
        return "Layers(enabled=$enabledLayers, mask=0x${mask.toString(16)})"
    }
}

/**
 * FR-L006: Layer-aware rendering integration
 *
 * Test Contract:
 * - MUST filter objects by camera layers
 * - MUST skip invisible objects early in render pipeline
 * - MUST apply to shadows and raycasting
 */
interface LayerFilterable {
    /**
     * Object's layer membership
     */
    val layers: Layers

    /**
     * Check if object is visible to camera
     *
     * @param camera Camera to test visibility against
     * @return true if object and camera share any layers
     *
     * Test Contract:
     * - MUST use layers.test() for efficiency
     * - MUST be called before rendering object
     */
    fun isVisibleToCamera(camera: Camera): Boolean {
        return layers.test(camera.layers)
    }
}

/**
 * Layer utilities
 */
object LayerUtils {
    /**
     * Common layer assignments
     */
    object CommonLayers {
        const val DEFAULT = 0
        const val UI = 1
        const val GIZMOS = 2
        const val DEBUG = 3
        const val SHADOWS_ONLY = 4
        const val REFLECTIONS_ONLY = 5
        const val PLAYER = 10
        const val ENEMIES = 11
        const val ENVIRONMENT = 12
        const val PARTICLES = 13
    }

    /**
     * Create layers from layer numbers
     *
     * @param layers Variable number of layer indices
     * @return Layers object with specified layers enabled
     *
     * Test Contract:
     * - MUST enable all specified layers
     * - MUST validate all layer numbers
     */
    fun create(vararg layers: Int): Layers {
        val result = Layers()
        result.disableAll()
        layers.forEach { result.enable(it) }
        return result
    }

    /**
     * Create layers from bitmask
     */
    fun fromMask(mask: Int): Layers {
        val result = Layers()
        result.setMask(mask)
        return result
    }

    /**
     * Get list of enabled layer numbers
     *
     * @return List of layer numbers (0-31) that are enabled
     */
    fun getEnabledLayers(layers: Layers): List<Int> {
        return (0..31).filter { layers.isEnabled(it) }
    }

    /**
     * Combine multiple layers (union)
     *
     * Test Contract:
     * - MUST enable all layers from all inputs
     * - MUST use bitwise OR
     */
    fun union(vararg layerObjects: Layers): Layers {
        val result = Layers()
        var combinedMask = 0
        layerObjects.forEach { combinedMask = combinedMask or it.getMask() }
        result.setMask(combinedMask)
        return result
    }

    /**
     * Intersect multiple layers
     *
     * Test Contract:
     * - MUST enable only layers present in ALL inputs
     * - MUST use bitwise AND
     */
    fun intersection(vararg layerObjects: Layers): Layers {
        if (layerObjects.isEmpty()) {
            return Layers().apply { disableAll() }
        }
        val result = Layers()
        var combinedMask = -1  // Start with all bits set
        layerObjects.forEach { combinedMask = combinedMask and it.getMask() }
        result.setMask(combinedMask)
        return result
    }

    /**
     * Invert layers (toggle all 32 layers)
     *
     * Test Contract:
     * - MUST flip all bits
     * - MUST use bitwise NOT
     */
    fun invert(layers: Layers): Layers {
        val result = Layers()
        result.setMask(layers.getMask().inv())
        return result
    }

    /**
     * Filter objects by layer
     *
     * @param objects Objects to filter
     * @param camera Camera whose layers to test against
     * @return Objects visible to camera
     *
     * Test Contract:
     * - MUST return only objects sharing layers with camera
     * - MUST preserve object order
     */
    fun filterVisibleObjects(
        objects: List<Object3D>,
        camera: Camera
    ): List<Object3D> {
        return objects.filter { it.layers.test(camera.layers) }
    }

    /**
     * Set layer for object and all descendants
     *
     * Test Contract:
     * - MUST set layer on root object
     * - MUST recursively set layer on children
     */
    fun setLayerRecursive(obj: Object3D, layer: Int) {
        obj.layers.set(layer)
        obj.children.forEach { setLayerRecursive(it, layer) }
    }

    /**
     * Enable layer for object and all descendants
     */
    fun enableLayerRecursive(obj: Object3D, layer: Int) {
        obj.layers.enable(layer)
        obj.children.forEach { enableLayerRecursive(it, layer) }
    }

    /**
     * Count objects in scene by layer
     *
     * @return Map of layer number to object count
     */
    fun countObjectsByLayer(root: Object3D): Map<Int, Int> {
        val counts = mutableMapOf<Int, Int>()

        fun traverse(obj: Object3D) {
            (0..31).forEach { layer ->
                if (obj.layers.isEnabled(layer)) {
                    counts[layer] = (counts[layer] ?: 0) + 1
                }
            }
            obj.children.forEach { traverse(it) }
        }

        traverse(root)
        return counts
    }
}