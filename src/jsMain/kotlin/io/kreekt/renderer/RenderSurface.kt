/**
 * T023: RenderSurface Actual (JS)
 * Feature: 019-we-should-not
 *
 * JS actual declaration for RenderSurface interface.
 */

package io.kreekt.renderer

/**
 * JS actual for RenderSurface interface.
 *
 * Implemented by WebGPUSurface (see io.kreekt.renderer.webgpu.WebGPUSurface).
 */
actual interface RenderSurface {
    actual val width: Int
    actual val height: Int
    actual fun getHandle(): Any
}

/*
 * NOTE: Previous WebGPURenderSurface class replaced with Feature 019 expect/actual pattern.
 * See git history for old implementation.
 * New implementation: io.kreekt.renderer.webgpu.WebGPUSurface
 */
