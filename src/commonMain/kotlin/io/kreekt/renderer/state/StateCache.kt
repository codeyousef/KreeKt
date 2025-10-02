package io.kreekt.renderer.state

import io.kreekt.camera.Viewport
import io.kreekt.material.CullFace
import io.kreekt.renderer.RendererException
import io.kreekt.renderer.RendererResult

/**
 * State caching layer for efficient GPU state changes.
 * Minimizes redundant state changes through change tracking.
 */
class StateCache {
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
    fun applyChanges(applier: StateApplier): RendererResult<Unit> {
        try {
            var changesApplied = 0

            // Depth state changes
            if (currentState.depthTest != pendingState.depthTest) {
                applier.applyDepthTest(pendingState.depthTest)
                changesApplied++
            }

            if (currentState.depthWrite != pendingState.depthWrite) {
                applier.applyDepthWrite(pendingState.depthWrite)
                changesApplied++
            }

            if (currentState.depthFunction != pendingState.depthFunction) {
                applier.applyDepthFunction(pendingState.depthFunction)
                changesApplied++
            }

            // Blend state changes
            if (currentState.blending != pendingState.blending) {
                applier.applyBlending(pendingState.blending)
                changesApplied++
            }

            if (pendingState.blending && (
                        currentState.blendSrc != pendingState.blendSrc ||
                                currentState.blendDst != pendingState.blendDst ||
                                currentState.blendEquation != pendingState.blendEquation
                        )
            ) {
                applier.applyBlendFunction(
                    pendingState.blendSrc,
                    pendingState.blendDst,
                    pendingState.blendEquation
                )
                changesApplied++
            }

            // Culling state changes
            if (currentState.cullFace != pendingState.cullFace) {
                applier.applyCullFace(pendingState.cullFace)
                changesApplied++
            }

            if (currentState.frontFace != pendingState.frontFace) {
                applier.applyFrontFace(pendingState.frontFace)
                changesApplied++
            }

            // Viewport changes
            if (currentState.viewport != pendingState.viewport) {
                applier.applyViewport(pendingState.viewport)
                changesApplied++
            }

            // Scissor test changes
            if (currentState.scissorTest != pendingState.scissorTest) {
                applier.applyScissorTest(pendingState.scissorTest)
                changesApplied++
            }

            if (pendingState.scissorTest && currentState.scissorRect != pendingState.scissorRect) {
                applier.applyScissorRect(pendingState.scissorRect)
                changesApplied++
            }

            // Color write changes
            if (currentState.colorWrite != pendingState.colorWrite) {
                applier.applyColorWrite(pendingState.colorWrite)
                changesApplied++
            }

            // Polygon mode changes
            if (currentState.polygonMode != pendingState.polygonMode) {
                applier.applyPolygonMode(pendingState.polygonMode)
                changesApplied++
            }

            // Line width changes
            if (currentState.lineWidth != pendingState.lineWidth) {
                applier.applyLineWidth(pendingState.lineWidth)
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
}

/**
 * Interface for applying state changes to GPU
 */
interface StateApplier {
    fun applyDepthTest(enabled: Boolean)
    fun applyDepthWrite(enabled: Boolean)
    fun applyDepthFunction(function: CompareFunction)
    fun applyBlending(enabled: Boolean)
    fun applyBlendFunction(src: BlendFactor, dst: BlendFactor, equation: BlendEquation)
    fun applyCullFace(face: CullFace)
    fun applyFrontFace(face: FrontFace)
    fun applyViewport(viewport: Viewport)
    fun applyScissorTest(enabled: Boolean)
    fun applyScissorRect(rect: Viewport)
    fun applyColorWrite(write: ColorWrite)
    fun applyPolygonMode(mode: PolygonMode)
    fun applyLineWidth(width: Float)
}
