/**
 * CubeCamera - Renders scene to a cube texture
 *
 * Creates a cube texture by rendering the scene from a single point
 * in six directions (±X, ±Y, ±Z). Used for dynamic environment mapping
 * and reflections.
 */
package io.kreekt.camera

import io.kreekt.core.math.Matrix4
import io.kreekt.core.math.Vector3
import io.kreekt.core.scene.Scene
// import io.kreekt.renderer.WebGPUCubeRenderTarget // TODO: Will be implemented
// import io.kreekt.renderer.WebGPURenderer // TODO: Will be implemented
import io.kreekt.texture.CubeTexture

/**
 * Camera that renders to 6 faces of a cube texture
 *
 * @property near Near clipping plane
 * @property far Far clipping plane
 * @property renderTarget The cube render target
 */
class CubeCamera(
    nearClip: Float = 0.1f,
    farClip: Float = 1000f,
    cubeResolution: Int = 256
) : Camera() {

    init {
        near = nearClip
        far = farClip
    }

    /**
     * The cube render target where the scene is rendered
     */
    val renderTarget: WebGPUCubeRenderTarget = WebGPUCubeRenderTarget(
        cubeResolution, cubeResolution
    )

    /**
     * Internal cameras for each cube face
     */
    private val cameraPX = PerspectiveCamera(90f, 1f, near, far)
    private val cameraNX = PerspectiveCamera(90f, 1f, near, far)
    private val cameraPY = PerspectiveCamera(90f, 1f, near, far)
    private val cameraNY = PerspectiveCamera(90f, 1f, near, far)
    private val cameraPZ = PerspectiveCamera(90f, 1f, near, far)
    private val cameraNZ = PerspectiveCamera(90f, 1f, near, far)

    private val cameras = listOf(cameraPX, cameraNX, cameraPY, cameraNY, cameraPZ, cameraNZ)

    init {
        // Set up camera orientations for each cube face
        setupCameraOrientations()

        // All cameras start at the same position
        updateCameraPositions()
    }

    /**
     * Setup the orientation for each camera to look at the correct cube face
     */
    private fun setupCameraOrientations() {
        // Positive X - looking right
        cameraPX.rotation.set(0f, kotlin.math.PI.toFloat() / 2f, 0f)

        // Negative X - looking left
        cameraNX.rotation.set(0f, -kotlin.math.PI.toFloat() / 2f, 0f)

        // Positive Y - looking up
        cameraPY.rotation.set(-kotlin.math.PI.toFloat() / 2f, 0f, 0f)

        // Negative Y - looking down
        cameraNY.rotation.set(kotlin.math.PI.toFloat() / 2f, 0f, 0f)

        // Positive Z - looking forward (default)
        cameraPZ.rotation.set(0f, 0f, 0f)

        // Negative Z - looking backward
        cameraNZ.rotation.set(0f, kotlin.math.PI.toFloat(), 0f)
    }

    /**
     * Update all camera positions to match the CubeCamera position
     */
    private fun updateCameraPositions() {
        cameras.forEach { camera ->
            camera.position.copy(this.position)
        }
    }

    /**
     * Update the cube map for a specific face
     * @param activeCubeFace The face index (0-5)
     */
    fun updateCubeMap(activeCubeFace: Int) {
        if (activeCubeFace !in 0..5) return

        val camera = cameras[activeCubeFace]
        camera.position.copy(this.position)
        camera.updateMatrixWorld()
    }

    /**
     * Render the scene to the cube texture
     * @param renderer The renderer to use
     * @param scene The scene to render
     */
    fun update(renderer: WebGPURenderer, scene: Scene) {
        updateCameraPositions()

        val originalRenderTarget = renderer.getRenderTarget()

        // Render each face
        for (i in 0 until 6) {
            updateCubeMap(i)

            // Set render target to specific cube face
            renderer.setRenderTarget(renderTarget, i)

            // Clear and render
            renderer.clear()
            renderer.render(scene, cameras[i])
        }

        // Restore original render target
        renderer.setRenderTarget(originalRenderTarget)
    }

    /**
     * Get the environment texture for use in materials
     */
    fun getTexture(): CubeTexture? {
        return renderTarget.texture
    }

    /**
     * Clear the cube texture
     */
    fun clear(renderer: WebGPURenderer) {
        val originalRenderTarget = renderer.getRenderTarget()

        for (i in 0 until 6) {
            renderer.setRenderTarget(renderTarget, i)
            renderer.clear()
        }

        renderer.setRenderTarget(originalRenderTarget)
    }

    /**
     * Dispose of resources
     */
    fun dispose() {
        renderTarget.dispose()
    }

    /**
     * Updates projection matrix for all internal cameras
     */
    override fun updateProjectionMatrix() {
        cameras.forEach { it.updateProjectionMatrix() }
    }

    /**
     * Sets view offset for all internal cameras
     */
    override fun setViewOffset(
        fullWidth: Int,
        fullHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        cameras.forEach {
            it.setViewOffset(fullWidth, fullHeight, x, y, width, height)
        }
    }

    /**
     * Clears view offset for all internal cameras
     */
    override fun clearViewOffset() {
        cameras.forEach { it.clearViewOffset() }
    }

    companion object {
        /**
         * Create view matrices for each cube face
         * These are the standard cube map view matrices
         */
        fun getCubeViewMatrices(): List<Matrix4> {
            return listOf(
                // Positive X
                Matrix4().lookAt(
                    Vector3(0f, 0f, 0f),
                    Vector3(1f, 0f, 0f),
                    Vector3(0f, -1f, 0f)
                ),
                // Negative X
                Matrix4().lookAt(
                    Vector3(0f, 0f, 0f),
                    Vector3(-1f, 0f, 0f),
                    Vector3(0f, -1f, 0f)
                ),
                // Positive Y
                Matrix4().lookAt(
                    Vector3(0f, 0f, 0f),
                    Vector3(0f, 1f, 0f),
                    Vector3(0f, 0f, 1f)
                ),
                // Negative Y
                Matrix4().lookAt(
                    Vector3(0f, 0f, 0f),
                    Vector3(0f, -1f, 0f),
                    Vector3(0f, 0f, -1f)
                ),
                // Positive Z
                Matrix4().lookAt(
                    Vector3(0f, 0f, 0f),
                    Vector3(0f, 0f, 1f),
                    Vector3(0f, -1f, 0f)
                ),
                // Negative Z
                Matrix4().lookAt(
                    Vector3(0f, 0f, 0f),
                    Vector3(0f, 0f, -1f),
                    Vector3(0f, -1f, 0f)
                )
            )
        }

        /**
         * Face names for debugging
         */
        val FACE_NAMES = listOf("+X", "-X", "+Y", "-Y", "+Z", "-Z")
    }
}

// WebGPURenderer and WebGPUCubeRenderTarget are defined in ArrayCamera.kt