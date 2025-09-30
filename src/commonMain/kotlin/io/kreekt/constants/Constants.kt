package io.kreekt.constants

// Blending modes
enum class BlendingMode {
    NO_BLENDING,
    NORMAL_BLENDING,
    ADDITIVE_BLENDING,
    SUBTRACTIVE_BLENDING,
    MULTIPLY_BLENDING,
    CUSTOM_BLENDING
}

// Texture wrapping
enum class TextureWrapping {
    REPEAT_WRAPPING,
    CLAMP_TO_EDGE_WRAPPING,
    MIRRORED_REPEAT_WRAPPING
}

// Texture filtering
enum class TextureFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR
}

// Texture format
enum class TextureFormat {
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

// Texture data type
enum class TextureDataType {
    UNSIGNED_BYTE,
    BYTE,
    SHORT,
    UNSIGNED_SHORT,
    INT,
    UNSIGNED_INT,
    FLOAT,
    HALF_FLOAT
}

// Side rendering
enum class Side {
    FRONT_SIDE,
    BACK_SIDE,
    DOUBLE_SIDE
}

// Depth modes
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

// Cull face
enum class CullFace {
    NONE,
    BACK,
    FRONT,
    FRONT_AND_BACK
}

// Texture encoding
enum class TextureEncoding {
    LINEAR_ENCODING,
    SRGB_ENCODING
}

// Texture mapping
enum class TextureMapping {
    UV_MAPPING,
    CUBE_REFLECTION_MAPPING,
    CUBE_REFRACTION_MAPPING,
    EQUIRECTANGULAR_REFLECTION_MAPPING,
    EQUIRECTANGULAR_REFRACTION_MAPPING,
    CUBE_UV_REFLECTION_MAPPING
}