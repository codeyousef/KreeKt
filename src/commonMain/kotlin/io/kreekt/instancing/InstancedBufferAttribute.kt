package io.kreekt.instancing

import io.kreekt.geometry.BufferAttribute

/**
 * Instanced buffer attribute for per-instance data
 * Extends BufferAttribute with mesh-per-instance divisor support
 */
class InstancedBufferAttribute(
    array: FloatArray,
    itemSize: Int,
    normalized: Boolean = false,
    var meshPerAttribute: Int = 1
) : BufferAttribute(array, itemSize, normalized) {

    data class UpdateRange(
        var offset: Int = 0,
        var count: Int = -1
    )

    override var updateRange: IntRange
        get() = if (_updateRange.count == -1) {
            IntRange.EMPTY
        } else {
            _updateRange.offset until (_updateRange.offset + _updateRange.count)
        }
        set(value) {
            _updateRange = if (value.isEmpty()) {
                UpdateRange(0, -1)
            } else {
                UpdateRange(value.first, value.last - value.first + 1)
            }
        }

    private var _updateRange = UpdateRange()

    override fun clone(): InstancedBufferAttribute {
        return InstancedBufferAttribute(array.copyOf(), itemSize, normalized, meshPerAttribute).apply {
            needsUpdate = this@InstancedBufferAttribute.needsUpdate
            _updateRange = this@InstancedBufferAttribute._updateRange.copy()
        }
    }
}
