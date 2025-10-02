/**
 * Post-Processing System Contracts
 *
 * Feature: 013-double-check-and (Three.js r180 Feature Parity)
 * Category: Post-Processing Effects and Pipeline
 *
 * These contracts define the API surface for the post-processing system,
 * including the EffectComposer and all Pass types. This system provides
 * a flexible pipeline for applying full-screen effects to rendered scenes.
 *
 * Requirements covered: FR-005 through FR-016
 */

package io.kreekt.postprocessing

import io.kreekt.camera.Camera
import io.kreekt.core.math.Color
import io.kreekt.core.scene.Scene
import io.kreekt.material.Material
import io.kreekt.material.Shader
import io.kreekt.material.Uniform
import io.kreekt.renderer.Renderer
import io.kreekt.renderer.RenderTarget

/**
 * Effect composer manages a sequence of post-processing passes.
 *
 * FR-005: System MUST provide an EffectComposer class to manage post-processing pipelines
 * FR-015: System MUST support chaining multiple post-processing passes in sequence
 * FR-016: Post-processing system MUST work efficiently with WebGPU render pipelines
 *
 * @sample
 * ```kotlin
 * val composer = EffectComposer(renderer, renderTarget)
 * composer.addPass(RenderPass(scene, camera))
 * composer.addPass(BloomPass(strength = 1.5f))
 * composer.addPass(OutputPass())
 *
 * // In render loop
 * composer.render(deltaTime = 0.016f)
 * ```
 */
expect class EffectComposer {
    /**
     * The renderer used for rendering passes.
     */
    val renderer: Renderer

    /**
     * Primary render target for ping-pong buffering.
     */
    val renderTarget1: RenderTarget

    /**
     * Secondary render target for ping-pong buffering.
     */
    val renderTarget2: RenderTarget

    /**
     * List of all registered passes in execution order.
     */
    val passes: List<Pass>

    /**
     * Creates a new EffectComposer.
     *
     * @param renderer The renderer to use
     * @param renderTarget Optional custom render target (otherwise created automatically)
     */
    constructor(renderer: Renderer, renderTarget: RenderTarget? = null)

    /**
     * Adds a pass to the end of the pipeline.
     *
     * @param pass The pass to add
     */
    fun addPass(pass: Pass)

    /**
     * Inserts a pass at a specific index.
     *
     * @param pass The pass to insert
     * @param index The index position
     */
    fun insertPass(pass: Pass, index: Int)

    /**
     * Removes a pass from the pipeline.
     *
     * @param pass The pass to remove
     */
    fun removePass(pass: Pass)

    /**
     * Renders all enabled passes in sequence.
     *
     * @param deltaTime Time since last frame in seconds
     */
    fun render(deltaTime: Float = 0.016f)

    /**
     * Resizes render targets.
     *
     * @param width New width in pixels
     * @param height New height in pixels
     */
    fun setSize(width: Int, height: Int)

    /**
     * Sets pixel ratio for high-DPI displays.
     *
     * @param pixelRatio The device pixel ratio
     */
    fun setPixelRatio(pixelRatio: Float)

    /**
     * Swaps read and write buffers.
     */
    fun swapBuffers()

    /**
     * Disposes of all resources.
     */
    fun dispose()
}

/**
 * Base class for all post-processing passes.
 *
 * FR-015: System MUST support chaining multiple post-processing passes in sequence
 */
abstract class Pass {
    /**
     * Human-readable name for debugging.
     */
    abstract val name: String

    /**
     * Whether this pass is currently enabled.
     */
    var enabled: Boolean = true

    /**
     * Whether this pass needs to swap read/write buffers.
     */
    var needsSwap: Boolean = true

    /**
     * Whether to clear the buffer before rendering.
     */
    var clear: Boolean = false

    /**
     * Whether this pass renders directly to screen.
     */
    var renderToScreen: Boolean = false

    /**
     * Renders this pass.
     *
     * @param composer The effect composer
     * @param writeBuffer Target buffer to write to
     * @param readBuffer Source buffer to read from
     * @param deltaTime Time since last frame
     */
    abstract fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    /**
     * Called when render target size changes.
     *
     * @param width New width
     * @param height New height
     */
    abstract fun setSize(width: Int, height: Int)

    /**
     * Disposes of pass resources.
     */
    open fun dispose() {}
}

/**
 * Renders a scene with a camera as the first pass.
 *
 * FR-006: System MUST provide RenderPass to render the scene as the first pass
 *
 * @sample
 * ```kotlin
 * val renderPass = RenderPass(scene, camera)
 * renderPass.clearColor = Color(0x000000)
 * renderPass.clearAlpha = 1.0f
 * composer.addPass(renderPass)
 * ```
 */
class RenderPass(
    /**
     * The scene to render.
     */
    val scene: Scene,

    /**
     * The camera to use for rendering.
     */
    val camera: Camera,

    /**
     * Optional material to override all scene materials.
     */
    val overrideMaterial: Material? = null,

    /**
     * Optional clear color (null = don't clear).
     */
    val clearColor: Color? = null,

    /**
     * Alpha value for clearing (0-1).
     */
    val clearAlpha: Float = 0.0f
) : Pass() {
    override val name = "RenderPass"

    /**
     * Whether to clear depth buffer before rendering.
     */
    var clearDepth: Boolean = false

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}

/**
 * Applies a custom shader as a full-screen effect.
 *
 * FR-007: System MUST provide ShaderPass for custom shader-based post-processing effects
 *
 * @sample
 * ```kotlin
 * val shader = Shader(
 *     vertexShader = fullscreenVertexShader,
 *     fragmentShader = customFragmentShader,
 *     uniforms = mutableMapOf(
 *         "tDiffuse" to Uniform(null),
 *         "intensity" to Uniform(1.0f)
 *     )
 * )
 * val shaderPass = ShaderPass(shader)
 * composer.addPass(shaderPass)
 * ```
 */
class ShaderPass(
    /**
     * The shader to apply.
     */
    val shader: Shader,

    /**
     * Name of the texture uniform for input (default "tDiffuse").
     */
    val textureID: String = "tDiffuse"
) : Pass() {
    override val name = "ShaderPass"

    /**
     * Direct access to shader uniforms.
     */
    val uniforms: MutableMap<String, Uniform>
        get() = shader.uniforms

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}

/**
 * Final pass that handles color space conversion and outputs to screen.
 *
 * FR-008: System MUST provide OutputPass for final output with color space conversion
 *
 * @sample
 * ```kotlin
 * val outputPass = OutputPass()
 * outputPass.toneMapping = ToneMapping.ACESFilmic
 * outputPass.toneMappingExposure = 1.0f
 * composer.addPass(outputPass)
 * ```
 */
class OutputPass : Pass() {
    override val name = "OutputPass"

    /**
     * Tone mapping mode to apply.
     */
    var toneMapping: ToneMapping = ToneMapping.Linear

    /**
     * Tone mapping exposure value.
     */
    var toneMappingExposure: Float = 1.0f

    init {
        needsSwap = false
        renderToScreen = true
    }

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}

enum class ToneMapping {
    Linear,
    Reinhard,
    Cineon,
    ACESFilmic,
    Custom
}

/**
 * Simple bloom effect using dual-filter downsampling/upsampling.
 *
 * FR-009: System MUST provide BloomPass and UnrealBloomPass for glow effects
 *
 * @sample
 * ```kotlin
 * val bloom = BloomPass(
 *     strength = 1.5f,
 *     radius = 0.4f,
 *     threshold = 0.85f
 * )
 * composer.addPass(bloom)
 * ```
 */
class BloomPass(
    /**
     * Bloom intensity (0-∞).
     */
    var strength: Float = 1.0f,

    /**
     * Bloom radius (0-1).
     */
    var radius: Float = 0.5f,

    /**
     * Luminance threshold for bloom (0-1).
     */
    var threshold: Float = 0.0f
) : Pass() {
    override val name = "BloomPass"

    /**
     * Number of mipmap levels for blur (2-10).
     */
    var nMips: Int = 5

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Advanced bloom with lens effects (dirt, starburst, etc.).
 *
 * FR-009: System MUST provide BloomPass and UnrealBloomPass for glow effects
 *
 * @sample
 * ```kotlin
 * val unrealBloom = UnrealBloomPass(
 *     strength = 2.0f,
 *     radius = 1.0f,
 *     threshold = 0.6f
 * )
 * unrealBloom.lensDirt = lensDirtTexture
 * composer.addPass(unrealBloom)
 * ```
 */
class UnrealBloomPass(
    var strength: Float = 1.5f,
    var radius: Float = 0.4f,
    var threshold: Float = 0.85f
) : Pass() {
    override val name = "UnrealBloomPass"

    /**
     * Lens dirt texture for bloom occlusion.
     */
    var lensDirt: io.kreekt.renderer.Texture? = null

    /**
     * Lens dirt intensity.
     */
    var lensDirtIntensity: Float = 1.0f

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Subpixel Morphological Antialiasing.
 *
 * FR-010: System MUST provide SMAA, FXAA, TAA, and SSAA anti-aliasing passes
 */
class SMAAPass : Pass() {
    override val name = "SMAAPass"

    /**
     * Edge detection threshold.
     */
    var edgeDetectionThreshold: Float = 0.1f

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Fast Approximate Antialiasing.
 *
 * FR-010: System MUST provide SMAA, FXAA, TAA, and SSAA anti-aliasing passes
 */
class FXAAPass : Pass() {
    override val name = "FXAAPass"

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}

/**
 * Temporal Antialiasing with jitter.
 *
 * FR-010: System MUST provide SMAA, FXAA, TAA, and SSAA anti-aliasing passes
 */
class TAAPass(
    val scene: Scene,
    val camera: Camera
) : Pass() {
    override val name = "TAAPass"

    /**
     * Sample count (higher = better quality, lower performance).
     */
    var sampleCount: Int = 8

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Super-Sampling Antialiasing.
 *
 * FR-010: System MUST provide SMAA, FXAA, TAA, and SSAA anti-aliasing passes
 */
class SSAAPass(
    val scene: Scene,
    val camera: Camera
) : Pass() {
    override val name = "SSAAPass"

    /**
     * Samples per dimension (2 = 4x SSAA, 3 = 9x SSAA).
     */
    var sampleLevel: Int = 2

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Screen-Space Ambient Occlusion.
 *
 * FR-011: System MUST provide SSAO (Screen-Space Ambient Occlusion) and SAO passes
 *
 * @sample
 * ```kotlin
 * val ssao = SSAOPass(scene, camera)
 * ssao.kernelRadius = 8.0f
 * ssao.kernelSize = 64
 * ssao.output = SSAOOutput.Default
 * composer.addPass(ssao)
 * ```
 */
class SSAOPass(
    val scene: Scene,
    val camera: Camera,
    var kernelRadius: Float = 8.0f,
    var minDistance: Float = 0.005f,
    var maxDistance: Float = 0.1f
) : Pass() {
    override val name = "SSAOPass"

    /**
     * Number of samples (16, 32, 64, 128).
     */
    var kernelSize: Int = 64

    /**
     * Output mode.
     */
    var output: SSAOOutput = SSAOOutput.Default

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

enum class SSAOOutput {
    Default,  // SSAO * scene
    SSAO,     // SSAO only
    Blur,     // Blurred SSAO
    Beauty,   // Scene without SSAO
    Depth,    // Depth buffer
    Normal    // Normal buffer
}

/**
 * Scalable Ambient Occlusion.
 *
 * FR-011: System MUST provide SSAO (Screen-Space Ambient Occlusion) and SAO passes
 */
class SAOPass(
    val scene: Scene,
    val camera: Camera
) : Pass() {
    override val name = "SAOPass"

    /**
     * Occlusion intensity.
     */
    var intensity: Float = 0.15f

    /**
     * Occlusion scale.
     */
    var scale: Float = 1.0f

    /**
     * Bias to avoid self-shadowing.
     */
    var bias: Float = 0.5f

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Depth-of-field with bokeh shapes.
 *
 * FR-012: System MUST provide BokehPass for depth-of-field effects
 *
 * @sample
 * ```kotlin
 * val bokeh = BokehPass(scene, camera)
 * bokeh.focus = 10.0f
 * bokeh.aperture = 0.025f
 * bokeh.maxblur = 0.01f
 * composer.addPass(bokeh)
 * ```
 */
class BokehPass(
    val scene: Scene,
    val camera: Camera
) : Pass() {
    override val name = "BokehPass"

    /**
     * Focus distance.
     */
    var focus: Float = 1.0f

    /**
     * Aperture size (larger = more blur).
     */
    var aperture: Float = 0.025f

    /**
     * Maximum blur amount.
     */
    var maxblur: Float = 0.01f

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Object highlighting with outlines.
 *
 * FR-013: System MUST provide OutlinePass for object highlighting
 *
 * @sample
 * ```kotlin
 * val outline = OutlinePass(scene, camera)
 * outline.selectedObjects = listOf(mesh1, mesh2)
 * outline.edgeStrength = 3.0f
 * outline.edgeGlow = 1.0f
 * outline.edgeThickness = 1.0f
 * outline.visibleEdgeColor = Color(1.0f, 1.0f, 1.0f)
 * composer.addPass(outline)
 * ```
 */
class OutlinePass(
    val scene: Scene,
    val camera: Camera
) : Pass() {
    override val name = "OutlinePass"

    /**
     * Objects to outline.
     */
    var selectedObjects: List<io.kreekt.core.scene.Object3D> = emptyList()

    /**
     * Edge detection strength.
     */
    var edgeStrength: Float = 3.0f

    /**
     * Edge glow intensity.
     */
    var edgeGlow: Float = 0.0f

    /**
     * Edge thickness.
     */
    var edgeThickness: Float = 1.0f

    /**
     * Color of visible edges.
     */
    var visibleEdgeColor: Color = Color(1.0f, 1.0f, 1.0f)

    /**
     * Color of hidden edges.
     */
    var hiddenEdgeColor: Color = Color(0.1f, 0.04f, 0.02f)

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
    override fun dispose()
}

/**
 * Digital glitch effect.
 *
 * FR-014: System MUST provide various artistic passes (GlitchPass, FilmPass, DotScreenPass, HalftonePass)
 */
class GlitchPass : Pass() {
    override val name = "GlitchPass"

    /**
     * Glitch intensity (0-1).
     */
    var amount: Float = 0.5f

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}

/**
 * Film grain and scanlines effect.
 *
 * FR-014: System MUST provide various artistic passes (GlitchPass, FilmPass, DotScreenPass, HalftonePass)
 */
class FilmPass : Pass() {
    override val name = "FilmPass"

    /**
     * Noise intensity (0-1).
     */
    var noiseIntensity: Float = 0.35f

    /**
     * Scanline intensity (0-1).
     */
    var scanlineIntensity: Float = 0.5f

    /**
     * Number of scanlines.
     */
    var scanlineCount: Int = 2048

    /**
     * Grayscale mode.
     */
    var grayscale: Boolean = false

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}

/**
 * Halftone dot screen effect.
 *
 * FR-014: System MUST provide various artistic passes (GlitchPass, FilmPass, DotScreenPass, HalftonePass)
 */
class DotScreenPass : Pass() {
    override val name = "DotScreenPass"

    /**
     * Dot center position.
     */
    var center: io.kreekt.core.math.Vector2 = io.kreekt.core.math.Vector2(0.5f, 0.5f)

    /**
     * Rotation angle in radians.
     */
    var angle: Float = 1.57f

    /**
     * Dot size.
     */
    var scale: Float = 1.0f

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}

/**
 * CMYK halftone effect.
 *
 * FR-014: System MUST provide various artistic passes (GlitchPass, FilmPass, DotScreenPass, HalftonePass)
 */
class HalftonePass : Pass() {
    override val name = "HalftonePass"

    /**
     * Shape of halftone dots (1=dot, 2=ellipse, 3=line, 4=square).
     */
    var shape: Int = 1

    /**
     * Radius of dots.
     */
    var radius: Float = 4.0f

    /**
     * Scatter amount.
     */
    var scatter: Float = 0.0f

    /**
     * Grayscale mode.
     */
    var grayscale: Boolean = false

    override fun render(
        composer: EffectComposer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float
    )

    override fun setSize(width: Int, height: Int)
}
