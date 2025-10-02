package io.kreekt.renderer.state

import io.kreekt.core.scene.Material
import io.kreekt.renderer.Buffer
import io.kreekt.renderer.RenderItem

/**
 * Draw call optimization and batching system.
 * Groups and batches render items for efficient rendering.
 */
class DrawCallOptimizer {
    private val drawCallStats = DrawCallStats()

    /**
     * Optimize draw calls for a list of render items
     */
    fun optimizeDrawCalls(items: List<RenderItem>): List<OptimizedDrawCall> {
        val optimizedCalls = mutableListOf<OptimizedDrawCall>()

        // Group by material and geometry for potential batching
        val groups = items.groupBy { "${it.material?.id}_${it.geometry?.uuid}" }

        for ((key, groupItems) in groups) {
            if (groupItems.size > 1 && canBatch(groupItems)) {
                // Create instanced draw call
                val instancedCall = OptimizedDrawCall(
                    items = groupItems,
                    type = DrawCallType.INSTANCED,
                    instanceCount = groupItems.size
                )
                optimizedCalls.add(instancedCall)
                drawCallStats.instancedCalls++
                drawCallStats.savedDrawCalls += groupItems.size - 1
            } else {
                // Individual draw calls
                for (item in groupItems) {
                    val call = OptimizedDrawCall(
                        items = listOf(item),
                        type = DrawCallType.INDIVIDUAL,
                        instanceCount = 1
                    )
                    optimizedCalls.add(call)
                    drawCallStats.individualCalls++
                }
            }
        }

        drawCallStats.totalDrawCalls = optimizedCalls.size
        return optimizedCalls
    }

    private fun canBatch(items: List<RenderItem>): Boolean {
        if (items.isEmpty()) return false

        val first = items.first()
        return items.all {
            it.material?.id == first.material?.id &&
                    it.geometry?.uuid == first.geometry?.uuid &&
                    !isTransparent(it.material)
        }
    }

    private fun isTransparent(material: Material?): Boolean {
        // Check if material requires transparency
        return false // Simplified implementation
    }

    fun getStats(): DrawCallStats = drawCallStats

    fun resetStats() {
        drawCallStats.reset()
    }
}

/**
 * Types of optimized draw calls
 */
enum class DrawCallType {
    INDIVIDUAL,
    INSTANCED,
    INDIRECT
}

/**
 * Optimized draw call representation
 */
data class OptimizedDrawCall(
    val items: List<RenderItem>,
    val type: DrawCallType,
    val instanceCount: Int,
    val indirectBuffer: Buffer? = null
)

/**
 * Draw call statistics
 */
data class DrawCallStats(
    var totalDrawCalls: Int = 0,
    var individualCalls: Int = 0,
    var instancedCalls: Int = 0,
    var indirectCalls: Int = 0,
    var savedDrawCalls: Int = 0
) {
    fun reset() {
        totalDrawCalls = 0
        individualCalls = 0
        instancedCalls = 0
        indirectCalls = 0
        savedDrawCalls = 0
    }

    fun getOptimizationRatio(): Float {
        return if (totalDrawCalls > 0) {
            savedDrawCalls.toFloat() / (totalDrawCalls + savedDrawCalls).toFloat()
        } else {
            0f
        }
    }
}
