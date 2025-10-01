/**
 * Constants and Enumerations API Contract
 * Maps FR-CE001 through FR-CE010
 *
 * Constitutional Requirements:
 * - Type-safe constants and enums
 * - Comprehensive coverage of Three.js constants
 * - Cross-platform compatibility
 */

package io.kreekt.constants

/**
 * FR-CE001: Blending modes
 *
 * Test Contract:
 * - MUST match WebGPU/Vulkan blend factors
 * - MUST provide Three.js compatible names
 */
enum class Blending {
    NO_BLENDING,
    NORMAL_BLENDING,
    ADDITIVE_BLENDING,
    SUBTRACTIVE_BLENDING,
    MULTIPLY_BLENDING,
    CUSTOM_BLENDING
}

enum class BlendingEquation {
    ADD_EQUATION,
    SUBTRACT_EQUATION,
    REVERSE_SUBTRACT_EQUATION,
    MIN_EQUATION,
    MAX_EQUATION
}

enum class BlendingFactor {
    ZERO_FACTOR,
    ONE_FACTOR,
    SRC_COLOR_FACTOR,
    ONE_MINUS_SRC_COLOR_FACTOR,
    SRC_ALPHA_FACTOR,
    ONE_MINUS_SRC_ALPHA_FACTOR,
    DST_ALPHA_FACTOR,
    ONE_MINUS_DST_ALPHA_FACTOR,
    DST_COLOR_FACTOR,
    ONE_MINUS_DST_COLOR_FACTOR,
    SRC_ALPHA_SATURATE_FACTOR
}

/**
 * FR-CE002: Depth modes
 */
enum class DepthMode {
    NEVER_DEPTH,
    ALWAYS_DEPTH,
    LESS_DEPTH,
    LESS_EQUAL_DEPTH,
    EQUAL_DEPTH,
    GREATER_EQUAL_DEPTH,
    GREATER_DEPTH,
    NOT_EQUAL_DEPTH
}

/**
 * FR-CE003: Cull face modes
 */
enum class CullFaceMode {
    CULL_FACE_NONE,
    CULL_FACE_BACK,
    CULL_FACE_FRONT,
    CULL_FACE_FRONT_AND_BACK
}

enum class FrontFaceDirection {
    FRONT_FACE_CW,   // Clockwise
    FRONT_FACE_CCW   // Counter-clockwise (default)
}

/**
 * FR-CE004: Side rendering modes
 */
enum class Side {
    FRONT_SIDE,
    BACK_SIDE,
    DOUBLE_SIDE
}

/**
 * FR-CE005: Shading modes
 */
enum class Shading {
    FLAT_SHADING,
    SMOOTH_SHADING
}

/**
 * FR-CE006: Texture mapping modes
 */
enum class Mapping {
    UV_MAPPING,
    CUBE_REFLECTION_MAPPING,
    CUBE_REFRACTION_MAPPING,
    EQUIRECTANGULAR_REFLECTION_MAPPING,
    EQUIRECTANGULAR_REFRACTION_MAPPING,
    CUBE_UV_MAPPING
}

/**
 * FR-CE007: Texture wrapping modes
 */
enum class Wrapping {
    REPEAT_WRAPPING,
    CLAMP_TO_EDGE_WRAPPING,
    MIRRORED_REPEAT_WRAPPING
}

/**
 * FR-CE008: Texture filtering modes
 */
enum class MagnificationFilter {
    NEAREST_FILTER,
    LINEAR_FILTER
}

enum class MinificationFilter {
    NEAREST_FILTER,
    NEAREST_MIPMAP_NEAREST_FILTER,
    NEAREST_MIPMAP_LINEAR_FILTER,
    LINEAR_FILTER,
    LINEAR_MIPMAP_NEAREST_FILTER,
    LINEAR_MIPMAP_LINEAR_FILTER
}

/**
 * FR-CE009: Data types
 */
enum class DataType {
    UNSIGNED_BYTE,
    BYTE,
    SHORT,
    UNSIGNED_SHORT,
    INT,
    UNSIGNED_INT,
    FLOAT,
    HALF_FLOAT,
    UNSIGNED_SHORT_4_4_4_4,
    UNSIGNED_SHORT_5_5_5_1,
    UNSIGNED_SHORT_5_6_5,
    UNSIGNED_INT_24_8
}

/**
 * Pixel formats
 */
enum class PixelFormat {
    ALPHA,
    RGB,
    RGBA,
    LUMINANCE,
    LUMINANCE_ALPHA,
    DEPTH_COMPONENT,
    DEPTH_STENCIL,
    RED,
    RED_INTEGER,
    RG,
    RG_INTEGER,
    RGB_INTEGER,
    RGBA_INTEGER
}

/**
 * Internal texture formats
 */
enum class InternalFormat {
    // 8-bit
    R8,
    R8_SNORM,
    R8I,
    R8UI,
    RG8,
    RG8_SNORM,
    RG8I,
    RG8UI,
    RGB8,
    RGB8_SNORM,
    RGB8I,
    RGB8UI,
    RGBA8,
    RGBA8_SNORM,
    RGBA8I,
    RGBA8UI,

    // 16-bit
    R16I,
    R16UI,
    R16F,
    RG16I,
    RG16UI,
    RG16F,
    RGB16I,
    RGB16UI,
    RGB16F,
    RGBA16I,
    RGBA16UI,
    RGBA16F,

    // 32-bit
    R32I,
    R32UI,
    R32F,
    RG32I,
    RG32UI,
    RG32F,
    RGB32I,
    RGB32UI,
    RGB32F,
    RGBA32I,
    RGBA32UI,
    RGBA32F,

    // Depth/stencil
    DEPTH_COMPONENT16,
    DEPTH_COMPONENT24,
    DEPTH_COMPONENT32F,
    DEPTH24_STENCIL8,
    DEPTH32F_STENCIL8,

    // Compressed
    COMPRESSED_RGB_S3TC_DXT1_EXT,
    COMPRESSED_RGBA_S3TC_DXT1_EXT,
    COMPRESSED_RGBA_S3TC_DXT3_EXT,
    COMPRESSED_RGBA_S3TC_DXT5_EXT
}

/**
 * FR-CE010: Encoding types
 */
enum class TextureEncoding {
    LINEAR_ENCODING,
    SRGB_ENCODING
}

enum class ColorSpace {
    NO_COLOR_SPACE,
    SRGB_COLOR_SPACE,
    LINEAR_SRGB_COLOR_SPACE
}

/**
 * Tone mapping modes
 */
enum class ToneMapping {
    NO_TONE_MAPPING,
    LINEAR_TONE_MAPPING,
    REINHARD_TONE_MAPPING,
    CINEON_TONE_MAPPING,
    ACES_FILMIC_TONE_MAPPING,
    CUSTOM_TONE_MAPPING
}

/**
 * Normal map types
 */
enum class NormalMapType {
    TANGENT_SPACE_NORMAL_MAP,
    OBJECT_SPACE_NORMAL_MAP
}

/**
 * Combine operations
 */
enum class Combine {
    MULTIPLY_OPERATION,
    MIX_OPERATION,
    ADD_OPERATION
}

/**
 * Stencil operations
 */
enum class StencilOp {
    ZERO_OP,
    KEEP_OP,
    REPLACE_OP,
    INCREMENT_OP,
    DECREMENT_OP,
    INCREMENT_WRAP_OP,
    DECREMENT_WRAP_OP,
    INVERT_OP
}

/**
 * Stencil functions
 */
enum class StencilFunc {
    NEVER_STENCIL_FUNC,
    LESS_STENCIL_FUNC,
    EQUAL_STENCIL_FUNC,
    LESS_EQUAL_STENCIL_FUNC,
    GREATER_STENCIL_FUNC,
    NOT_EQUAL_STENCIL_FUNC,
    GREATER_EQUAL_STENCIL_FUNC,
    ALWAYS_STENCIL_FUNC
}

/**
 * Buffer usage hints
 */
enum class Usage {
    STATIC_DRAW,
    DYNAMIC_DRAW,
    STREAM_DRAW,
    STATIC_READ,
    DYNAMIC_READ,
    STREAM_READ,
    STATIC_COPY,
    DYNAMIC_COPY,
    STREAM_COPY
}

/**
 * Primitive types
 */
enum class DrawMode {
    TRIANGLES,
    TRIANGLE_STRIP,
    TRIANGLE_FAN,
    LINES,
    LINE_STRIP,
    LINE_LOOP,
    POINTS
}

/**
 * Interpolation modes
 */
enum class InterpolationMode {
    DISCRETE_INTERPOLATION,
    LINEAR_INTERPOLATION,
    CUBIC_INTERPOLATION
}

/**
 * Animation loop modes
 */
enum class AnimationLoopMode {
    ONCE,
    REPEAT,
    PING_PONG
}

/**
 * Animation blending modes
 */
enum class AnimationBlendMode {
    NORMAL_ANIMATION_BLEND_MODE,
    ADDITIVE_ANIMATION_BLEND_MODE
}

/**
 * Skinning modes
 */
enum class SkinningMode {
    LINEAR_SKINNING,
    DUAL_QUATERNION_SKINNING
}

/**
 * Shadow types
 */
enum class ShadowMapType {
    BASIC_SHADOW_MAP,
    PCF_SHADOW_MAP,
    PCF_SOFT_SHADOW_MAP,
    VSM_SHADOW_MAP
}

/**
 * Material precision
 */
enum class Precision {
    HIGH_P,
    MEDIUM_P,
    LOW_P
}

/**
 * Vertex colors mode
 */
enum class VertexColors {
    NO_COLORS,
    FACE_COLORS,
    VERTEX_COLORS
}

/**
 * Combine operations for materials
 */
enum class CombineOperation {
    MULTIPLY,
    MIX,
    ADD
}

/**
 * Constants object for backward compatibility with Three.js
 */
object Constants {
    // Revision
    const val REVISION = "r180"

    // Mouse buttons
    const val MOUSE_LEFT = 0
    const val MOUSE_MIDDLE = 1
    const val MOUSE_RIGHT = 2
    const val MOUSE_ROTATE = 0
    const val MOUSE_DOLLY = 1
    const val MOUSE_PAN = 2

    // Touch actions
    const val TOUCH_ROTATE = 0
    const val TOUCH_PAN = 1
    const val TOUCH_DOLLY_PAN = 2
    const val TOUCH_DOLLY_ROTATE = 3

    // Color space
    const val LINEAR_SRGB = "linear-srgb"
    const val SRGB = "srgb"

    // Texture encoding (legacy)
    const val LINEAR_ENCODING = 3000
    const val SRGB_ENCODING = 3001

    // Tone mapping
    const val NO_TONE_MAPPING = 0
    const val LINEAR_TONE_MAPPING = 1
    const val REINHARD_TONE_MAPPING = 2
    const val CINEON_TONE_MAPPING = 3
    const val ACES_FILMIC_TONE_MAPPING = 4

    // Limits
    const val MAX_TEXTURE_SIZE = 16384
    const val MAX_CUBE_MAP_SIZE = 16384
    const val MAX_ARRAY_TEXTURE_LAYERS = 2048
    const val MAX_3D_TEXTURE_SIZE = 2048
    const val MAX_VERTEX_ATTRIBUTES = 16
    const val MAX_VARYING_VECTORS = 15
    const val MAX_VERTEX_UNIFORM_VECTORS = 1024
    const val MAX_FRAGMENT_UNIFORM_VECTORS = 1024
    const val MAX_SAMPLES = 4
}

/**
 * Utility functions for constants
 */
object ConstantUtils {
    /**
     * Get WebGPU blend factor from Three.js blending mode
     */
    fun getWebGPUBlendFactor(factor: BlendingFactor): String

    /**
     * Get WebGPU compare function from depth mode
     */
    fun getWebGPUCompareFunction(depthMode: DepthMode): String

    /**
     * Get WebGPU cull mode from cull face mode
     */
    fun getWebGPUCullMode(cullFace: CullFaceMode): String

    /**
     * Get WebGPU filter from texture filter
     */
    fun getWebGPUFilter(filter: MinificationFilter): String
    fun getWebGPUFilter(filter: MagnificationFilter): String

    /**
     * Get WebGPU address mode from wrapping
     */
    fun getWebGPUAddressMode(wrapping: Wrapping): String

    /**
     * Get WebGPU texture format from pixel format and type
     */
    fun getWebGPUTextureFormat(format: PixelFormat, type: DataType): String

    /**
     * Check if format is compressed
     */
    fun isCompressedFormat(format: InternalFormat): Boolean

    /**
     * Check if format has depth
     */
    fun isDepthFormat(format: InternalFormat): Boolean

    /**
     * Check if format has stencil
     */
    fun isStencilFormat(format: InternalFormat): Boolean

    /**
     * Get bytes per pixel for format
     */
    fun getBytesPerPixel(format: PixelFormat, type: DataType): Int
}