package io.kreekt.renderer

import io.kreekt.camera.Viewport
import io.kreekt.core.platform.currentTimeMillis
import io.kreekt.core.scene.Material
import io.kreekt.material.CullFace

/**
 * GPU state management and optimization system
 * T120-T125 - GPU state caching, draw call batching, and performance optimization
 */

/**
 * GPU render state
 */
data class RenderState(
    // Depth state
    val depthTest: Boolean = true,
    val depthWrite: Boolean = true,
    val depthFunction: CompareFunction = CompareFunction.LESS,

    // Stencil state
    val stencilTest: Boolean = false,
    val stencilWrite: Boolean = false,
    val stencilFunction: CompareFunction = CompareFunction.ALWAYS,
    val stencilRef: Int = 0,
    val stencilMask: Int = 0xFF,
    val stencilFail: StencilOperation = StencilOperation.KEEP,
    val stencilZFail: StencilOperation = StencilOperation.KEEP,
    val stencilZPass: StencilOperation = StencilOperation.KEEP,

    // Blend state
    val blending: Boolean = false,
    val blendSrc: BlendFactor = BlendFactor.ONE,
    val blendDst: BlendFactor = BlendFactor.ZERO,
    val blendEquation: BlendEquation = BlendEquation.ADD,
    val blendSrcAlpha: BlendFactor = BlendFactor.ONE,
    val blendDstAlpha: BlendFactor = BlendFactor.ZERO,
    val blendEquationAlpha: BlendEquation = BlendEquation.ADD,

    // Culling state
    val cullFace: CullFace = CullFace.BACK,
    val frontFace: FrontFace = FrontFace.CCW,

    // Color state
    val colorWrite: ColorWrite = ColorWrite.ALL,

    // Polygon state
    val polygonMode: PolygonMode = PolygonMode.FILL,
    val lineWidth: Float = 1f,

    // Viewport state
    val viewport: Viewport = Viewport(0, 0, 1, 1),
    val scissorTest: Boolean = false,
    val scissorRect: Viewport = Viewport(0, 0, 1, 1)
)

/**
 * Comparison functions for depth and stencil tests
 */
enum class CompareFunction {
    NEVER,
    LESS,
    EQUAL,
    LESS_EQUAL,
    GREATER,
    NOT_EQUAL,
    GREATER_EQUAL,
    ALWAYS
}

/**
 * Stencil operations
 */
enum class StencilOperation {
    KEEP,
    ZERO,
    REPLACE,
    INCREMENT,
    INCREMENT_WRAP,
    DECREMENT,
    DECREMENT_WRAP,
    INVERT
}

/**
 * Blend factors
 */
enum class BlendFactor {
    ZERO,
    ONE,
    SRC_COLOR,
    ONE_MINUS_SRC_COLOR,
    DST_COLOR,
    ONE_MINUS_DST_COLOR,
    SRC_ALPHA,
    ONE_MINUS_SRC_ALPHA,
    DST_ALPHA,
    ONE_MINUS_DST_ALPHA,
    CONSTANT_COLOR,
    ONE_MINUS_CONSTANT_COLOR,
    CONSTANT_ALPHA,
    ONE_MINUS_CONSTANT_ALPHA,
    SRC_ALPHA_SATURATE
}

/**
 * Blend equations
 */
enum class BlendEquation {
    ADD,
    SUBTRACT,
    REVERSE_SUBTRACT,
    MIN,
    MAX
}

/**
 * Color write masks
 */
enum class ColorWrite {
    NONE,
    RED,
    GREEN,
    BLUE,
    ALPHA,
    RGB,
    ALL
}

/**
 * Polygon rendering modes
 */
enum class PolygonMode {
    POINT,
    LINE,
    FILL
}

/**
 * Front face winding order
 */
enum class FrontFace {
    CW,  // Clockwise
    CCW  // Counter-clockwise
}

/**
 * GPU state manager for efficient state changes
 */
class GPUStateManager {
    private var currentState = RenderState()
    private var pendingState = RenderState()
    private var stateChangeCount = 0

    /**
     * Set desired render state
     */
    fun setState(newState: RenderState) {
        pendingState = newState
    }

    /**
     * Apply pending state changes to GPU
     */
    fun applyChanges(): RendererResult<Unit> {
        try {
            var changesApplied = 0

            // Depth state changes
            if (currentState.depthTest != pendingState.depthTest) {
                applyDepthTest(pendingState.depthTest)
                changesApplied++
            }

            if (currentState.depthWrite != pendingState.depthWrite) {
                applyDepthWrite(pendingState.depthWrite)
                changesApplied++
            }

            if (currentState.depthFunction != pendingState.depthFunction) {
                applyDepthFunction(pendingState.depthFunction)
                changesApplied++
            }

            // Blend state changes
            if (currentState.blending != pendingState.blending) {
                applyBlending(pendingState.blending)
                changesApplied++
            }

            if (pendingState.blending && (
                        currentState.blendSrc != pendingState.blendSrc ||
                                currentState.blendDst != pendingState.blendDst ||
                                currentState.blendEquation != pendingState.blendEquation
                        )
            ) {
                applyBlendFunction(
                    pendingState.blendSrc,
                    pendingState.blendDst,
                    pendingState.blendEquation
                )
                changesApplied++
            }

            // Culling state changes
            if (currentState.cullFace != pendingState.cullFace) {
                applyCullFace(pendingState.cullFace)
                changesApplied++
            }

            if (currentState.frontFace != pendingState.frontFace) {
                applyFrontFace(pendingState.frontFace)
                changesApplied++
            }

            // Viewport changes
            if (currentState.viewport != pendingState.viewport) {
                applyViewport(pendingState.viewport)
                changesApplied++
            }

            // Scissor test changes
            if (currentState.scissorTest != pendingState.scissorTest) {
                applyScissorTest(pendingState.scissorTest)
                changesApplied++
            }

            if (pendingState.scissorTest && currentState.scissorRect != pendingState.scissorRect) {
                applyScissorRect(pendingState.scissorRect)
                changesApplied++
            }

            // Color write changes
            if (currentState.colorWrite != pendingState.colorWrite) {
                applyColorWrite(pendingState.colorWrite)
                changesApplied++
            }

            // Polygon mode changes
            if (currentState.polygonMode != pendingState.polygonMode) {
                applyPolygonMode(pendingState.polygonMode)
                changesApplied++
            }

            // Line width changes
            if (currentState.lineWidth != pendingState.lineWidth) {
                applyLineWidth(pendingState.lineWidth)
                changesApplied++
            }

            // Update current state
            currentState = pendingState.copy()
            stateChangeCount += changesApplied

            return RendererResult.Success(Unit)

        } catch (e: Exception) {
            return RendererResult.Error(RendererException.RenderingFailed("State change failed", e))
        }
    }

    /**
     * Get current render state
     */
    fun getCurrentState(): RenderState = currentState

    /**
     * Get state change statistics
     */
    fun getStateChangeCount(): Int = stateChangeCount

    /**
     * Reset state change counter
     */
    fun resetStats() {
        stateChangeCount = 0
    }

    // Platform-specific state application methods
    private fun applyDepthTest(enabled: Boolean) {
        // Platform-specific depth test enable/disable
    }

    private fun applyDepthWrite(enabled: Boolean) {
        // Platform-specific depth write enable/disable
    }

    private fun applyDepthFunction(function: CompareFunction) {
        // Platform-specific depth comparison function
    }

    private fun applyBlending(enabled: Boolean) {
        // Platform-specific blending enable/disable
    }

    private fun applyBlendFunction(src: BlendFactor, dst: BlendFactor, equation: BlendEquation) {
        // Platform-specific blend function setup
    }

    private fun applyCullFace(face: CullFace) {
        // Platform-specific face culling
    }

    private fun applyFrontFace(face: FrontFace) {
        // Platform-specific front face winding
    }

    private fun applyViewport(viewport: Viewport) {
        // Platform-specific viewport setup
    }

    private fun applyScissorTest(enabled: Boolean) {
        // Platform-specific scissor test enable/disable
    }

    private fun applyScissorRect(rect: Viewport) {
        // Platform-specific scissor rectangle
    }

    private fun applyColorWrite(write: ColorWrite) {
        // Platform-specific color write mask
    }

    private fun applyPolygonMode(mode: PolygonMode) {
        // Platform-specific polygon mode
    }

    private fun applyLineWidth(width: Float) {
        // Platform-specific line width
    }
}

/**
 * Draw call optimization and batching system
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

/**
 * GPU resource pool for efficient resource reuse
 */
class GPUResourcePool {
    private val bufferPools = mutableMapOf<BufferPoolKey, MutableList<Buffer>>()
    private val texturePool = mutableListOf<io.kreekt.renderer.Texture>()
    private val shaderProgramPool = mutableListOf<ShaderProgram>()

    /**
     * Get or create a buffer from the pool
     */
    fun getBuffer(type: BufferType, size: Long, usage: BufferUsage): Buffer? {
        val key = BufferPoolKey(type, size, usage)
        val pool = bufferPools[key] ?: return null

        return pool.removeFirstOrNull()
    }

    /**
     * Return a buffer to the pool
     */
    fun returnBuffer(buffer: Buffer) {
        val key = BufferPoolKey(buffer.type, buffer.size, buffer.usage)
        val pool = bufferPools.getOrPut(key) { mutableListOf() }

        // Reset buffer state
        buffer.needsUpdate = false
        pool.add(buffer)
    }

    /**
     * Get or create a texture from the pool
     */
    fun getTexture(width: Int, height: Int): io.kreekt.renderer.Texture? {
        val texture = texturePool.find { it.width == width && it.height == height }
        if (texture != null) {
            texturePool.remove(texture)
        }
        return texture
    }

    /**
     * Return a texture to the pool
     */
    fun returnTexture(texture: io.kreekt.renderer.Texture) {
        texture.needsUpdate = false
        texturePool.add(texture)
    }

    /**
     * Clear all pools
     */
    fun clear() {
        bufferPools.values.forEach { pool ->
            pool.forEach { it.dispose() }
            pool.clear()
        }
        bufferPools.clear()

        texturePool.forEach { it.dispose() }
        texturePool.clear()

        shaderProgramPool.forEach { it.dispose() }
        shaderProgramPool.clear()
    }

    /**
     * Get pool statistics
     */
    fun getStats(): ResourcePoolStats {
        val totalBuffers = bufferPools.values.sumOf { it.size }
        val totalTextures = texturePool.size
        val totalPrograms = shaderProgramPool.size

        return ResourcePoolStats(
            totalBuffers = totalBuffers,
            totalTextures = totalTextures,
            totalPrograms = totalPrograms,
            bufferPools = bufferPools.size
        )
    }

    private data class BufferPoolKey(
        val type: BufferType,
        val size: Long,
        val usage: BufferUsage
    )
}

/**
 * Resource pool statistics
 */
data class ResourcePoolStats(
    val totalBuffers: Int,
    val totalTextures: Int,
    val totalPrograms: Int,
    val bufferPools: Int
)

/**
 * Performance monitor for GPU operations
 */
class GPUPerformanceMonitor {
    private var frameStartTime = 0L
    private var lastFrameTime = 0L
    private var frameCount = 0L
    private var averageFrameTime = 0f
    private var minFrameTime = Float.MAX_VALUE
    private var maxFrameTime = 0f

    private val frameTimeHistory = ArrayDeque<Float>(60) // Last 60 frames

    /**
     * Start frame timing
     */
    fun startFrame() {
        frameStartTime = getCurrentTime()
    }

    /**
     * End frame timing and update statistics
     */
    fun endFrame() {
        val frameTime = getCurrentTime() - frameStartTime
        lastFrameTime = frameTime

        updateFrameStats(frameTime.toFloat())
        frameCount++
    }

    private fun updateFrameStats(frameTime: Float) {
        // Update min/max
        minFrameTime = kotlin.math.min(minFrameTime, frameTime)
        maxFrameTime = kotlin.math.max(maxFrameTime, frameTime)

        // Add to history
        frameTimeHistory.addLast(frameTime)
        if (frameTimeHistory.size > 60) {
            frameTimeHistory.removeFirst()
        }

        // Calculate average
        averageFrameTime = frameTimeHistory.average().toFloat()
    }

    /**
     * Get current FPS
     */
    fun getFPS(): Float {
        return if (averageFrameTime > 0) 1000f / averageFrameTime else 0f
    }

    /**
     * Get frame time statistics
     */
    fun getFrameStats(): FrameStats {
        return FrameStats(
            currentFrameTime = lastFrameTime.toFloat(),
            averageFrameTime = averageFrameTime,
            minFrameTime = if (minFrameTime == Float.MAX_VALUE) 0f else minFrameTime,
            maxFrameTime = maxFrameTime,
            fps = getFPS(),
            frameCount = frameCount
        )
    }

    /**
     * Reset statistics
     */
    fun reset() {
        frameCount = 0
        averageFrameTime = 0f
        minFrameTime = Float.MAX_VALUE
        maxFrameTime = 0f
        frameTimeHistory.clear()
    }

    private fun getCurrentTime(): Long {
        return currentTimeMillis()
    }
}

/**
 * Frame timing statistics
 */
data class FrameStats(
    val currentFrameTime: Float,
    val averageFrameTime: Float,
    val minFrameTime: Float,
    val maxFrameTime: Float,
    val fps: Float,
    val frameCount: Long
)

/**
 * GPU memory tracker
 */
class GPUMemoryTracker {
    private var allocatedBuffers = 0L
    private var allocatedTextures = 0L
    private var allocatedPrograms = 0L
    private var peakBufferMemory = 0L
    private var peakTextureMemory = 0L

    fun trackBufferAllocation(size: Long) {
        allocatedBuffers += size
        peakBufferMemory = kotlin.math.max(peakBufferMemory, allocatedBuffers)
    }

    fun trackBufferDeallocation(size: Long) {
        allocatedBuffers = kotlin.math.max(0, allocatedBuffers - size)
    }

    fun trackTextureAllocation(size: Long) {
        allocatedTextures += size
        peakTextureMemory = kotlin.math.max(peakTextureMemory, allocatedTextures)
    }

    fun trackTextureDeallocation(size: Long) {
        allocatedTextures = kotlin.math.max(0, allocatedTextures - size)
    }

    fun getMemoryStats(): GPUMemoryStats {
        return GPUMemoryStats(
            allocatedBuffers = allocatedBuffers,
            allocatedTextures = allocatedTextures,
            allocatedPrograms = allocatedPrograms,
            peakBufferMemory = peakBufferMemory,
            peakTextureMemory = peakTextureMemory,
            totalAllocated = allocatedBuffers + allocatedTextures + allocatedPrograms
        )
    }

    fun reset() {
        allocatedBuffers = 0
        allocatedTextures = 0
        allocatedPrograms = 0
        peakBufferMemory = 0
        peakTextureMemory = 0
    }
}

/**
 * GPU memory statistics
 */
data class GPUMemoryStats(
    val allocatedBuffers: Long,
    val allocatedTextures: Long,
    val allocatedPrograms: Long,
    val peakBufferMemory: Long,
    val peakTextureMemory: Long,
    val totalAllocated: Long
) {
    fun getFormattedSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "${(bytes / 1_073_741_824.0 * 100).toInt() / 100.0} GB"
            bytes >= 1_048_576 -> "${(bytes / 1_048_576.0 * 100).toInt() / 100.0} MB"
            bytes >= 1024 -> "${(bytes / 1024.0 * 100).toInt() / 100.0} KB"
            else -> "$bytes B"
        }
    }
}