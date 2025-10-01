/**
 * ArrayCamera - Multiple camera views in a single render
 *
 * Renders a scene from multiple camera perspectives in a single frame,
 * useful for split-screen, surveillance systems, or multi-view displays.
 */
package io.kreekt.camera

import io.kreekt.core.math.Vector4
import io.kreekt.core.scene.Scene
import io.kreekt.texture.CubeTexture

// import io.kreekt.renderer.WebGPURenderer // TODO: Will be implemented

/**
 * Camera system that manages multiple viewports
 *
 * @property cameras Array of cameras to render
 */
class ArrayCamera(
    cameras: Array<Camera> = emptyArray()
) : Camera() {

    private val _cameras = mutableListOf<Camera>()
    private val _viewports = mutableListOf<Vector4>()
    private val _enabled = mutableListOf<Boolean>()

    var cameras: List<Camera>
        get() = _cameras.toList()
        set(value) {
            _cameras.clear()
            _cameras.addAll(value)
            while (_viewports.size < _cameras.size) {
                _viewports.add(Vector4(0f, 0f, 1f, 1f))
                _enabled.add(true)
            }
        }

    private var screenWidth: Float = 1f
    private var screenHeight: Float = 1f

    init {
        this.cameras = cameras.toList()
    }

    /**
     * Add a camera with its viewport
     * @param camera The camera to add
     * @param viewport Normalized viewport (x, y, width, height) in [0,1] range
     */
    fun addCamera(camera: Camera, viewport: Vector4 = Vector4(0f, 0f, 1f, 1f)) {
        _cameras.add(camera)
        _viewports.add(viewport)
        _enabled.add(true)
    }

    /**
     * Remove a camera
     * @param camera The camera to remove
     */
    fun removeCamera(camera: Camera): Boolean {
        val index = _cameras.indexOf(camera)
        if (index >= 0) {
            _cameras.removeAt(index)
            _viewports.removeAt(index)
            _enabled.removeAt(index)
            return true
        }
        return false
    }

    /**
     * Get viewport for a specific camera
     * @param index Camera index
     * @return Normalized viewport or null if index out of bounds
     */
    fun getViewport(index: Int): Vector4? {
        return _viewports.getOrNull(index)
    }

    /**
     * Set viewport for a specific camera
     * @param index Camera index
     * @param viewport Normalized viewport (x, y, width, height)
     */
    fun setViewport(index: Int, viewport: Vector4) {
        if (index in _viewports.indices) {
            _viewports[index] = viewport
        }
    }

    /**
     * Setup cameras in a grid layout
     * @param rows Number of rows
     * @param cols Number of columns
     */
    fun setupGrid(rows: Int, cols: Int) {
        _cameras.clear()
        _viewports.clear()
        _enabled.clear()

        val cellWidth = 1f / cols
        val cellHeight = 1f / rows

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val camera = PerspectiveCamera(50f, cellWidth / cellHeight, 0.1f, 1000f)
                val viewport = Vector4(
                    col * cellWidth,
                    (rows - 1 - row) * cellHeight, // Flip Y for screen coordinates
                    cellWidth,
                    cellHeight
                )

                _cameras.add(camera)
                _viewports.add(viewport)
                _enabled.add(true)
            }
        }
    }

    /**
     * Render scene through all enabled cameras
     * @param renderer The renderer to use
     * @param scene The scene to render
     */
    fun render(renderer: WebGPURenderer, scene: Scene) {
        for (i in _cameras.indices) {
            if (!_enabled[i]) continue

            val camera = _cameras[i]
            val viewport = _viewports[i]

            // Calculate pixel viewport from normalized coordinates
            val pixelX = viewport.x * screenWidth
            val pixelY = viewport.y * screenHeight
            val pixelWidth = viewport.z * screenWidth
            val pixelHeight = viewport.w * screenHeight

            // Set viewport
            renderer.setViewport(pixelX, pixelY, pixelWidth, pixelHeight)

            // Update camera aspect if it's a perspective camera
            if (camera is PerspectiveCamera) {
                camera.aspect = pixelWidth / pixelHeight
                camera.updateProjectionMatrix()
            }

            // Render
            renderer.render(scene, camera)
        }

        // Reset viewport to full screen
        renderer.setViewport(0f, 0f, screenWidth, screenHeight)
    }

    /**
     * Enable or disable a specific camera
     * @param index Camera index
     * @param enabled Whether the camera should be rendered
     */
    fun setEnabled(index: Int, enabled: Boolean) {
        if (index in _enabled.indices) {
            _enabled[index] = enabled
        }
    }

    /**
     * Check if a camera is enabled
     * @param index Camera index
     * @return True if enabled, false otherwise
     */
    fun isEnabled(index: Int): Boolean {
        return _enabled.getOrNull(index) ?: false
    }

    /**
     * Set the screen size for viewport calculations
     * @param width Screen width in pixels
     * @param height Screen height in pixels
     */
    fun setSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    /**
     * Get pixel viewport for a camera
     * @param index Camera index
     * @return Pixel viewport or null if index out of bounds
     */
    fun getPixelViewport(index: Int): Vector4? {
        val viewport = _viewports.getOrNull(index) ?: return null
        return Vector4(
            viewport.x * screenWidth,
            viewport.y * screenHeight,
            viewport.z * screenWidth,
            viewport.w * screenHeight
        )
    }

    /**
     * Clear all cameras
     */
    fun clearCameras() {
        _cameras.clear()
        _viewports.clear()
        _enabled.clear()
    }

    /**
     * Updates projection matrix - delegates to all cameras
     */
    override fun updateProjectionMatrix() {
        _cameras.forEach { it.updateProjectionMatrix() }
    }

    /**
     * Sets view offset for all cameras
     */
    override fun setViewOffset(
        fullWidth: Int,
        fullHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        _cameras.forEach {
            it.setViewOffset(fullWidth, fullHeight, x, y, width, height)
        }
    }

    /**
     * Clears view offset for all cameras
     */
    override fun clearViewOffset() {
        _cameras.forEach { it.clearViewOffset() }
    }

    /**
     * Get the number of cameras
     */
    fun size(): Int = _cameras.size

    /**
     * Common viewport configurations
     */
    companion object {
        /**
         * Create split screen viewports (2 cameras side by side)
         */
        fun splitScreenHorizontal(): List<Vector4> {
            return listOf(
                Vector4(0f, 0f, 0.5f, 1f),  // Left half
                Vector4(0.5f, 0f, 0.5f, 1f)  // Right half
            )
        }

        /**
         * Create split screen viewports (2 cameras top and bottom)
         */
        fun splitScreenVertical(): List<Vector4> {
            return listOf(
                Vector4(0f, 0.5f, 1f, 0.5f),  // Top half
                Vector4(0f, 0f, 1f, 0.5f)      // Bottom half
            )
        }

        /**
         * Create quad view viewports (2x2 grid)
         */
        fun quadView(): List<Vector4> {
            return listOf(
                Vector4(0f, 0.5f, 0.5f, 0.5f),    // Top-left
                Vector4(0.5f, 0.5f, 0.5f, 0.5f),  // Top-right
                Vector4(0f, 0f, 0.5f, 0.5f),      // Bottom-left
                Vector4(0.5f, 0f, 0.5f, 0.5f)     // Bottom-right
            )
        }

        /**
         * Create picture-in-picture viewport
         * @param mainSize Size of main view (0-1)
         * @param pipPosition Position of PIP (0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right)
         */
        fun pictureInPicture(mainSize: Float = 1f, pipPosition: Int = 1): List<Vector4> {
            val pipSize = 0.25f
            val margin = 0.02f

            val pipViewport = when (pipPosition) {
                0 -> Vector4(margin, 1f - pipSize - margin, pipSize, pipSize) // Top-left
                1 -> Vector4(1f - pipSize - margin, 1f - pipSize - margin, pipSize, pipSize) // Top-right
                2 -> Vector4(margin, margin, pipSize, pipSize) // Bottom-left
                3 -> Vector4(1f - pipSize - margin, margin, pipSize, pipSize) // Bottom-right
                else -> Vector4(1f - pipSize - margin, 1f - pipSize - margin, pipSize, pipSize)
            }

            return listOf(
                Vector4(0f, 0f, mainSize, mainSize), // Main view
                pipViewport // PIP view
            )
        }
    }
}

/**
 * Placeholder WebGPURenderer for testing
 * Will be replaced with actual renderer implementation
 */
open class WebGPURenderer {
    private var currentRenderTarget: WebGPUCubeRenderTarget? = null

    open fun setViewport(x: Float, y: Float, width: Float, height: Float) {
        // Placeholder implementation
    }

    open fun render(scene: Scene, camera: Camera) {
        // Placeholder implementation
    }

    open fun getRenderTarget(): WebGPUCubeRenderTarget? {
        return currentRenderTarget
    }

    open fun setRenderTarget(renderTarget: WebGPUCubeRenderTarget?, activeCubeFace: Int = 0) {
        currentRenderTarget = renderTarget
    }

    open fun clear() {
        // Placeholder implementation
    }
}

/**
 * Placeholder for WebGPUCubeRenderTarget
 */
class WebGPUCubeRenderTarget(
    val width: Int,
    val height: Int,
    val texture: CubeTexture? = null
) {
    fun dispose() {
        // Dispose of render target resources
    }
}