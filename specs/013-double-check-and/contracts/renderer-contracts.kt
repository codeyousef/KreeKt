/**
 * Alternative Renderer Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Alternative Renderers (SVG, CSS2D, CSS3D)
 *
 * Requirements covered: FR-001 through FR-004
 */

package io.kreekt.renderer.alternative

import io.kreekt.camera.Camera
import io.kreekt.core.scene.Scene
import io.kreekt.core.scene.Object3D

/**
 * SVG-based renderer for vector output.
 *
 * FR-001: System MUST provide an SVGRenderer that renders 3D scenes as SVG elements for vector-based output
 *
 * Note: JS platform only (requires DOM)
 *
 * @sample
 * ```kotlin
 * val svgRenderer = SVGRenderer(containerElement)
 * svgRenderer.setSize(800, 600)
 * svgRenderer.render(scene, camera)
 * ```
 */
expect class SVGRenderer {
    /**
     * Creates an SVG renderer attached to a container element.
     *
     * @param container DOM element to attach SVG to (JS only)
     */
    constructor(container: Any)

    /**
     * Renders the scene as SVG.
     */
    fun render(scene: Scene, camera: Camera)

    /**
     * Sets the renderer size.
     */
    fun setSize(width: Int, height: Int)

    /**
     * Clears the SVG content.
     */
    fun clear()

    /**
     * Disposes of resources.
     */
    fun dispose()
}

/**
 * Renders 2D CSS content positioned in 3D space.
 *
 * FR-002: System MUST provide a CSS2DRenderer for rendering 2D CSS content positioned in 3D space
 *
 * Note: JS platform only (requires DOM)
 *
 * @sample
 * ```kotlin
 * val css2dRenderer = CSS2DRenderer(containerElement)
 * css2dRenderer.setSize(800, 600)
 *
 * val label = CSS2DObject(createDivElement("Label text"))
 * label.position.set(0f, 1f, 0f)
 * scene.add(label)
 *
 * css2dRenderer.render(scene, camera)
 * ```
 */
expect class CSS2DRenderer {
    constructor(container: Any)

    fun render(scene: Scene, camera: Camera)
    fun setSize(width: Int, height: Int)
    fun dispose()
}

/**
 * Object that renders as a 2D CSS element in 3D space.
 *
 * FR-002: System MUST provide a CSS2DRenderer for rendering 2D CSS content positioned in 3D space
 */
expect class CSS2DObject {
    /**
     * Creates a CSS2D object with a DOM element.
     *
     * @param element The DOM element to render
     */
    constructor(element: Any)

    /**
     * The DOM element.
     */
    val element: Any
}

/**
 * Renders CSS content with 3D transforms.
 *
 * FR-003: System MUST provide a CSS3DRenderer for rendering CSS content with 3D transforms
 *
 * Note: JS platform only (requires DOM)
 *
 * @sample
 * ```kotlin
 * val css3dRenderer = CSS3DRenderer(containerElement)
 * css3dRenderer.setSize(800, 600)
 *
 * val panel = CSS3DObject(createDivElement("3D Panel"))
 * panel.position.set(0f, 0f, -5f)
 * panel.rotation.y = Math.PI.toFloat() / 4f
 * scene.add(panel)
 *
 * css3dRenderer.render(scene, camera)
 * ```
 */
expect class CSS3DRenderer {
    constructor(container: Any)

    fun render(scene: Scene, camera: Camera)
    fun setSize(width: Int, height: Int)
    fun dispose()
}

/**
 * Object that renders as CSS with 3D transforms.
 *
 * FR-003: System MUST provide a CSS3DRenderer for rendering CSS content with 3D transforms
 */
expect class CSS3DObject {
    /**
     * Creates a CSS3D object with a DOM element.
     *
     * @param element The DOM element to render
     */
    constructor(element: Any)

    /**
     * The DOM element.
     */
    val element: Any
}

/**
 * Base interface for custom renderers.
 *
 * FR-004: System MUST support custom renderer implementations following a consistent renderer interface
 */
interface CustomRenderer {
    /**
     * Renders the scene.
     */
    fun render(scene: Scene, camera: Camera)

    /**
     * Sets the output size.
     */
    fun setSize(width: Int, height: Int)

    /**
     * Sets pixel ratio for high-DPI displays.
     */
    fun setPixelRatio(pixelRatio: Float)

    /**
     * Disposes of resources.
     */
    fun dispose()
}
