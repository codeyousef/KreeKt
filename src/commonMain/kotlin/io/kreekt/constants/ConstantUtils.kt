package io.kreekt.constants

/**
 * ConstantUtils - Maps KreeKt constants to WebGPU/Vulkan values
 * Provides conversion utilities for cross-platform rendering
 */
object ConstantUtils {

    /**
     * Map blending mode to WebGPU blend operation
     */
    fun blendingToWebGPU(blending: BlendingMode): String {
        return when (blending) {
            BlendingMode.NO_BLENDING -> "none"
            BlendingMode.NORMAL_BLENDING -> "normal"
            BlendingMode.ADDITIVE_BLENDING -> "additive"
            BlendingMode.SUBTRACTIVE_BLENDING -> "subtractive"
            BlendingMode.MULTIPLY_BLENDING -> "multiply"
            BlendingMode.CUSTOM_BLENDING -> "custom"
        }
    }

    /**
     * Map depth function to WebGPU compare function
     */
    fun depthFuncToWebGPU(depthFunc: DepthMode): String {
        return when (depthFunc) {
            DepthMode.NEVER_DEPTH -> "never"
            DepthMode.ALWAYS_DEPTH -> "always"
            DepthMode.LESS_DEPTH -> "less"
            DepthMode.LESS_EQUAL_DEPTH -> "less-equal"
            DepthMode.EQUAL_DEPTH -> "equal"
            DepthMode.GREATER_EQUAL_DEPTH -> "greater-equal"
            DepthMode.GREATER_DEPTH -> "greater"
            DepthMode.NOT_EQUAL_DEPTH -> "not-equal"
        }
    }

    /**
     * Map cull face to WebGPU cull mode
     */
    fun cullFaceToWebGPU(cullFace: CullFace): String {
        return when (cullFace) {
            CullFace.NONE -> "none"
            CullFace.BACK -> "back"
            CullFace.FRONT -> "front"
            CullFace.FRONT_AND_BACK -> "both"
        }
    }

    /**
     * Map texture format to WebGPU texture format
     */
    fun textureFormatToWebGPU(format: TextureFormat): String {
        return when (format) {
            TextureFormat.RGBA -> "rgba8unorm"
            TextureFormat.RGB -> "rgb8unorm"
            TextureFormat.RED -> "r8unorm"
            TextureFormat.RG -> "rg8unorm"
            TextureFormat.DEPTH_COMPONENT -> "depth24plus"
            TextureFormat.DEPTH_STENCIL -> "depth24plus-stencil8"
            else -> "rgba8unorm"
        }
    }

    /**
     * Map texture wrap mode to WebGPU address mode
     */
    fun wrapModeToWebGPU(wrap: TextureWrapping): String {
        return when (wrap) {
            TextureWrapping.REPEAT_WRAPPING -> "repeat"
            TextureWrapping.CLAMP_TO_EDGE_WRAPPING -> "clamp-to-edge"
            TextureWrapping.MIRRORED_REPEAT_WRAPPING -> "mirror-repeat"
        }
    }

    /**
     * Map texture filter to WebGPU filter mode
     */
    fun filterToWebGPU(filter: TextureFilter): String {
        return when (filter) {
            TextureFilter.NEAREST, TextureFilter.NEAREST_MIPMAP_NEAREST -> "nearest"
            TextureFilter.LINEAR, TextureFilter.LINEAR_MIPMAP_LINEAR -> "linear"
            else -> "linear"
        }
    }

    /**
     * Map data type to WebGPU vertex format
     */
    fun dataTypeToWebGPU(type: TextureDataType, itemSize: Int): String {
        return when (type) {
            TextureDataType.FLOAT -> when (itemSize) {
                1 -> "float32"
                2 -> "float32x2"
                3 -> "float32x3"
                4 -> "float32x4"
                else -> "float32"
            }

            TextureDataType.INT -> when (itemSize) {
                1 -> "sint32"
                2 -> "sint32x2"
                3 -> "sint32x3"
                4 -> "sint32x4"
                else -> "sint32"
            }

            TextureDataType.UNSIGNED_INT -> when (itemSize) {
                1 -> "uint32"
                2 -> "uint32x2"
                3 -> "uint32x3"
                4 -> "uint32x4"
                else -> "uint32"
            }

            else -> "float32"
        }
    }

    /**
     * Map encoding to color space
     */
    fun encodingToColorSpace(encoding: TextureEncoding): String {
        return when (encoding) {
            TextureEncoding.LINEAR_ENCODING -> "linear"
            TextureEncoding.SRGB_ENCODING -> "srgb"
        }
    }
}
