/**
 * Post-Processing API Contract
 *
 * This file defines the complete API surface for the post-processing effects subsystem,
 * ensuring Three.js r180 compatibility with Kotlin idioms.
 *
 * Contract Design Principles:
 * - Type-safe method signatures
 * - Data classes for immutable configuration
 * - Default parameters for common use cases
 * - Sealed classes for type hierarchies
 * - Extension functions for DSL builders
 */

package io.kreekt.contracts.postfx

import io.kreekt.core.math.*
import io.kreekt.postfx.*
import io.kreekt.scene.*
import io.kreekt.camera.*
import io.kreekt.material.*
import io.kreekt.texture.*

// ============================================================================
// Core Post-Processing API
// ============================================================================

/**
 * EffectComposer: Manages post-processing pass chain
 * Three.js equivalent: THREE.EffectComposer
 */
interface EffectComposerAPI {
    val renderer: Renderer
    val renderTarget1: RenderTarget
    val renderTarget2: RenderTarget
    val writeBuffer: RenderTarget
    val readBuffer: RenderTarget
    val passes: MutableList<Pass>
    var renderToScreen: Boolean

    fun swapBuffers()
    fun addPass(pass: Pass)
    fun insertPass(pass: Pass, index: Int)
    fun removePass(pass: Pass)
    fun isLastEnabledPass(passIndex: Int): Boolean
    fun render(deltaTime: Float = 0f)
    fun reset(renderTarget: RenderTarget? = null)
    fun setSize(width: Int, height: Int)
    fun setPixelRatio(pixelRatio: Float)
    fun dispose()
}

/**
 * Pass: Base class for all post-processing passes
 * Three.js equivalent: THREE.Pass
 */
interface PassAPI {
    var enabled: Boolean
    var needsSwap: Boolean
    var clear: Boolean
    var renderToScreen: Boolean

    fun setSize(width: Int, height: Int)
    fun render(
        renderer: Renderer,
        writeBuffer: RenderTarget,
        readBuffer: RenderTarget,
        deltaTime: Float,
        maskActive: Boolean
    )

    fun dispose()
}

/**
 * RenderPass: Renders scene with camera
 * Three.js equivalent: THREE.RenderPass
 */
interface RenderPassAPI : PassAPI {
    var scene: Scene
    var camera: Camera
    var overrideMaterial: Material?
    var clearColor: Color?
    var clearAlpha: Float
    var clearDepth: Boolean
}

/**
 * ShaderPass: Generic shader pass
 * Three.js equivalent: THREE.ShaderPass
 */
interface ShaderPassAPI : PassAPI {
    var material: ShaderMaterial
    val fsQuad: FullScreenQuad
    var uniforms: MutableMap<String, Uniform>

    fun setUniform(name: String, value: Any)
}

/**
 * FullScreenQuad: Utility for full-screen rendering
 * Three.js equivalent: THREE.FullScreenQuad
 */
interface FullScreenQuad {
    var material: Material
    fun render(renderer: Renderer)
    fun dispose()
}

// ============================================================================
// Bloom Effects
// ============================================================================

/**
 * BloomPass: Simple bloom effect
 * Three.js equivalent: THREE.BloomPass
 */
interface BloomPassAPI : PassAPI {
    var strength: Float
    var kernelSize: Int
    var sigma: Float
    var resolution: Int
}

/**
 * UnrealBloomPass: Advanced bloom with threshold
 * Three.js equivalent: THREE.UnrealBloomPass
 */
interface UnrealBloomPassAPI : PassAPI {
    var resolution: Vector2
    var strength: Float
    var radius: Float
    var threshold: Float

    val renderTargetsHorizontal: List<RenderTarget>
    val renderTargetsVertical: List<RenderTarget>
    val nMips: Int

    fun getCompositeMaterial(nMips: Int): ShaderMaterial
}

/**
 * SelectiveBloomPass: Bloom on selected objects only
 */
interface SelectiveBloomPassAPI : PassAPI {
    var selectedObjects: List<Object3D>
    var strength: Float
    var radius: Float
    var threshold: Float

    fun addObject(obj: Object3D)
    fun removeObject(obj: Object3D)
    fun clearObjects()
}

// ============================================================================
// Anti-Aliasing
// ============================================================================

/**
 * SMAAPass: Subpixel Morphological Anti-Aliasing
 * Three.js equivalent: THREE.SMAAPass
 */
interface SMAAPassAPI : PassAPI {
    var edgeDetectionMaterial: ShaderMaterial
    var blendingWeightsMaterial: ShaderMaterial
    var neighborhoodBlendingMaterial: ShaderMaterial

    val renderTargetEdgeDetection: RenderTarget
    val renderTargetBlendingWeights: RenderTarget

    val areaTexture: Texture
    val searchTexture: Texture
}

/**
 * FXAAPass: Fast Approximate Anti-Aliasing
 * Three.js equivalent: THREE.FXAAPass
 */
interface FXAAPassAPI : ShaderPassAPI {
    // Uses shader uniforms for configuration
}

/**
 * TAAPass: Temporal Anti-Aliasing
 * Three.js equivalent: THREE.TAAPass
 */
interface TAAPassAPI : PassAPI {
    var sampleLevel: Int
    var unbiased: Boolean
    val sampleRenderTarget: RenderTarget?
    val holdRenderTarget: RenderTarget?
    var accumulate: Boolean

    fun reset()
}

/**
 * SSAAPass: Super-Sample Anti-Aliasing
 * Three.js equivalent: THREE.SSAAPass
 */
interface SSAAPassAPI : PassAPI {
    var sampleLevel: Int
    var unbiased: Boolean
    val sampleRenderTarget: RenderTarget
    val copyUniforms: MutableMap<String, Uniform>
}

// ============================================================================
// Ambient Occlusion
// ============================================================================

/**
 * SSAOPass: Screen-Space Ambient Occlusion
 * Three.js equivalent: THREE.SSAOPass
 */
interface SSAOPassAPI : PassAPI {
    var scene: Scene
    var camera: Camera
    var kernelRadius: Float
    var kernelSize: Int
    var noiseTexture: DataTexture
    var output: SSAOOutput

    val ssaoMaterial: ShaderMaterial
    val normalRenderTarget: RenderTarget
    val ssaoRenderTarget: RenderTarget
    val blurRenderTarget: RenderTarget

    fun generateSampleKernel()
    fun generateRandomKernelRotations()
}

enum class SSAOOutput {
    Default,
    SSAO,
    Blur,
    Beauty,
    Depth,
    Normal
}

/**
 * SAOPass: Scalable Ambient Occlusion
 * Three.js equivalent: THREE.SAOPass
 */
interface SAOPassAPI : PassAPI {
    var scene: Scene
    var camera: Camera
    var params: SAOParams

    val saoMaterial: ShaderMaterial
    val vBlurMaterial: ShaderMaterial
    val hBlurMaterial: ShaderMaterial
    val materialCopy: ShaderMaterial

    val depthRenderTarget: RenderTarget
    val saoRenderTarget: RenderTarget
    val blurRenderTarget: RenderTarget

    fun renderPass(
        renderer: Renderer,
        passMaterial: Material,
        renderTarget: RenderTarget,
        clearColor: Color?,
        clearAlpha: Float
    )

    fun renderOverride(
        renderer: Renderer,
        overrideMaterial: Material,
        renderTarget: RenderTarget,
        clearColor: Color?,
        clearAlpha: Float
    )
}

data class SAOParams(
    var output: SAOOutput = SAOOutput.Default,
    var saoBias: Float = 0.5f,
    var saoIntensity: Float = 0.18f,
    var saoScale: Float = 1f,
    var saoKernelRadius: Int = 100,
    var saoMinResolution: Float = 0f,
    var saoBlur: Boolean = true,
    var saoBlurRadius: Int = 8,
    var saoBlurStdDev: Float = 4f,
    var saoBlurDepthCutoff: Float = 0.01f
)

enum class SAOOutput {
    Default,
    SAO,
    Normal
}

// ============================================================================
// Depth of Field
// ============================================================================

/**
 * BokehPass: Bokeh depth of field
 * Three.js equivalent: THREE.BokehPass
 */
interface BokehPassAPI : PassAPI {
    var scene: Scene
    var camera: Camera
    var params: BokehParams

    val materialDepth: ShaderMaterial
    val materialBokeh: ShaderMaterial

    fun setSize(width: Int, height: Int)
}

data class BokehParams(
    var focus: Float = 1f,
    var aperture: Float = 0.025f,
    var maxblur: Float = 1f,
    var width: Int = 512,
    var height: Int = 512
)

// ============================================================================
// Edge Detection and Outlining
// ============================================================================

/**
 * OutlinePass: Object outlining/selection highlighting
 * Three.js equivalent: THREE.OutlinePass
 */
interface OutlinePassAPI : PassAPI {
    var renderScene: Scene
    var renderCamera: Camera
    var selectedObjects: List<Object3D>
    var visibleEdgeColor: Color
    var hiddenEdgeColor: Color
    var edgeGlow: Float
    var edgeThickness: Float
    var edgeStrength: Float
    var downSampleRatio: Float
    var pulsePeriod: Float

    val resolution: Vector2
    val renderTargetMaskBuffer: RenderTarget
    val renderTargetDepthBuffer: RenderTarget
    val renderTargetMaskDownSampleBuffer: RenderTarget
    val renderTargetEdgeBuffer1: RenderTarget
    val renderTargetEdgeBuffer2: RenderTarget

    fun changeVisibilityOfSelectedObjects(bVisible: Boolean)
    fun changeVisibilityOfNonSelectedObjects(bVisible: Boolean)
    fun updateTextureMatrix()
}

/**
 * SobelOperatorShader: Edge detection using Sobel operator
 */
interface SobelOperatorShaderAPI {
    val uniforms: Map<String, Uniform>
    val vertexShader: String
    val fragmentShader: String
}

// ============================================================================
// Color Grading and Tone Mapping
// ============================================================================

/**
 * AdaptiveToneMappingPass: HDR adaptive tone mapping
 * Three.js equivalent: THREE.AdaptiveToneMappingPass
 */
interface AdaptiveToneMappingPassAPI : PassAPI {
    var adaptive: Boolean
    var resolution: Int
    var middleGrey: Float
    var maxLuminance: Float
    var minLuminance: Float

    val luminanceRT: RenderTarget?
    val previousLuminanceRT: RenderTarget?
    val currentLuminanceRT: RenderTarget?

    fun reset()
    fun setAdaptive(adaptive: Boolean)
    fun setAdaptionRate(rate: Float)
    fun setMinLuminance(minLum: Float)
    fun setMaxLuminance(maxLum: Float)
    fun setMiddleGrey(middleGrey: Float)
}

/**
 * LUTPass: Look-Up Table color grading
 */
interface LUTPassAPI : ShaderPassAPI {
    var lut: Texture
    var intensity: Float

    fun setLUT(lut: Texture)
}

// ============================================================================
// Screen Effects
// ============================================================================

/**
 * GlitchPass: Digital glitch effect
 * Three.js equivalent: THREE.GlitchPass
 */
interface GlitchPassAPI : PassAPI {
    var goWild: Boolean
    val curF: Int
    val randX: Int

    fun generateTrigger()
    fun generateHeightmap(dt_size: Int): DataTexture
}

/**
 * FilmPass: Film grain and scanlines
 * Three.js equivalent: THREE.FilmPass
 */
interface FilmPassAPI : PassAPI {
    var uniforms: MutableMap<String, Uniform>
    val material: ShaderMaterial

    // Uniforms
    var noiseIntensity: Float
    var scanlinesIntensity: Float
    var scanlinesCount: Int
    var grayscale: Boolean
}

/**
 * DotScreenPass: Halftone dot screen effect
 * Three.js equivalent: THREE.DotScreenPass
 */
interface DotScreenPassAPI : ShaderPassAPI {
    var center: Vector2
    var angle: Float
    var scale: Float
}

/**
 * HalftonePass: CMYK halftone printing effect
 * Three.js equivalent: THREE.HalftonePass
 */
interface HalftonePassAPI : PassAPI {
    var params: HalftoneParams
    val uniforms: MutableMap<String, Uniform>

    fun setSize(width: Int, height: Int)
}

data class HalftoneParams(
    var shape: Int = 1,  // 1: dot, 2: ellipse, 3: line, 4: square
    var radius: Float = 4f,
    var rotateR: Float = Math.PI.toFloat() / 12f * 1f,
    var rotateG: Float = Math.PI.toFloat() / 12f * 2f,
    var rotateB: Float = Math.PI.toFloat() / 12f * 3f,
    var scatter: Float = 0f,
    var blending: Float = 1f,
    var blendingMode: Int = 1,  // 1: linear, 2: multiply, 3: add, 4: lighter, 5: darker
    var greyscale: Boolean = false,
    var disable: Boolean = false
)

/**
 * AfterimagePass: Motion blur / afterimage effect
 * Three.js equivalent: THREE.AfterimagePass
 */
interface AfterimagePassAPI : PassAPI {
    var damp: Float
    val textureComp: RenderTarget
    val textureOld: RenderTarget
    val compFsMaterial: ShaderMaterial
    val copyFsMaterial: ShaderMaterial
}

// ============================================================================
// Utility Passes
// ============================================================================

/**
 * MaskPass: Mask rendering for stencil operations
 * Three.js equivalent: THREE.MaskPass
 */
interface MaskPassAPI : PassAPI {
    var scene: Scene
    var camera: Camera
    var inverse: Boolean
}

/**
 * ClearMaskPass: Clear stencil mask
 * Three.js equivalent: THREE.ClearMaskPass
 */
interface ClearMaskPassAPI : PassAPI {
    // No additional properties
}

/**
 * CopyShader: Simple texture copy
 * Three.js equivalent: THREE.CopyShader
 */
interface CopyShaderAPI {
    val uniforms: Map<String, Uniform>
    val vertexShader: String
    val fragmentShader: String
}

/**
 * SavePass: Save render buffer to texture
 * Three.js equivalent: THREE.SavePass
 */
interface SavePassAPI : PassAPI {
    val renderTarget: RenderTarget
    val material: ShaderMaterial
}

/**
 * TexturePass: Render texture to screen
 * Three.js equivalent: THREE.TexturePass
 */
interface TexturePassAPI : PassAPI {
    var map: Texture?
    var opacity: Float
}

// ============================================================================
// Factory Functions (Kotlin Idiom)
// ============================================================================

/**
 * DSL builder for EffectComposer
 */
fun effectComposer(
    renderer: Renderer,
    renderTarget: RenderTarget? = null,
    init: EffectComposer.() -> Unit = {}
): EffectComposer {
    val composer = EffectComposer(renderer, renderTarget)
    composer.init()
    return composer
}

/**
 * Extension function to add multiple passes
 */
fun EffectComposer.addPasses(vararg passes: Pass) {
    passes.forEach { addPass(it) }
}

/**
 * Extension function for standard post-processing setup
 */
fun EffectComposer.setupStandardPipeline(
    scene: Scene,
    camera: Camera,
    enableBloom: Boolean = true,
    enableAA: Boolean = true
) {
    // Render pass
    addPass(RenderPass(scene, camera))

    // Bloom
    if (enableBloom) {
        addPass(UnrealBloomPass(
            resolution = Vector2(256f, 256f),
            strength = 1.5f,
            radius = 0.4f,
            threshold = 0.85f
        ))
    }

    // Anti-aliasing
    if (enableAA) {
        val smaaPass = SMAAPass(
            width = renderer.getSize().x.toInt(),
            height = renderer.getSize().y.toInt()
        )
        addPass(smaaPass)
    }
}

// ============================================================================
// Usage Examples
// ============================================================================

/**
 * Example: Basic post-processing setup
 */
fun exampleBasicPostProcessing(
    renderer: Renderer,
    scene: Scene,
    camera: Camera
): EffectComposer {
    return effectComposer(renderer) {
        // Scene render
        addPass(RenderPass(scene, camera))

        // Bloom
        addPass(UnrealBloomPass(
            resolution = Vector2(256f, 256f),
            strength = 1.5f,
            radius = 0.4f,
            threshold = 0.85f
        ))

        // FXAA
        val fxaaPass = FXAAPass()
        fxaaPass.renderToScreen = true
        addPass(fxaaPass)
    }
}

/**
 * Example: Advanced effects pipeline
 */
fun exampleAdvancedPipeline(
    renderer: Renderer,
    scene: Scene,
    camera: Camera
): EffectComposer {
    return effectComposer(renderer) {
        // Render scene
        addPass(RenderPass(scene, camera))

        // SSAO
        addPass(SSAOPass(
            scene = scene,
            camera = camera,
            kernelRadius = 8f,
            kernelSize = 32
        ))

        // Depth of field
        addPass(BokehPass(
            scene = scene,
            camera = camera,
            params = BokehParams(
                focus = 1f,
                aperture = 0.025f,
                maxblur = 1f
            )
        ))

        // Bloom
        addPass(UnrealBloomPass(
            resolution = Vector2(256f, 256f),
            strength = 1.0f,
            radius = 0.5f,
            threshold = 0.8f
        ))

        // TAA
        val taaPass = TAAPass(
            scene = scene,
            camera = camera
        )
        taaPass.renderToScreen = true
        addPass(taaPass)
    }
}

/**
 * Example: Outline selected objects
 */
fun exampleOutlineEffect(
    scene: Scene,
    camera: Camera,
    selectedObjects: List<Object3D>
): OutlinePass {
    return OutlinePass(
        resolution = Vector2(1024f, 1024f),
        scene = scene,
        camera = camera
    ).apply {
        this.selectedObjects = selectedObjects
        visibleEdgeColor = Color(1f, 1f, 0f)  // Yellow
        hiddenEdgeColor = Color(0.1f, 0.04f, 0.02f)
        edgeGlow = 0f
        edgeThickness = 1f
        edgeStrength = 3f
        pulsePeriod = 0f
    }
}

/**
 * Example: Film grain effect
 */
fun exampleFilmGrain(): FilmPass {
    return FilmPass(
        noiseIntensity = 0.35f,
        scanlinesIntensity = 0.5f,
        scanlinesCount = 2048,
        grayscale = false
    )
}

/**
 * Example: Glitch effect
 */
fun exampleGlitchEffect(): GlitchPass {
    return GlitchPass().apply {
        goWild = false  // Controlled glitching
    }
}
