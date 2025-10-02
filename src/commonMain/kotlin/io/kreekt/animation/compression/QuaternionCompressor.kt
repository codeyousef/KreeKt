package io.kreekt.animation.compression

import io.kreekt.animation.AnimationCompressor.AnimationTrack
import io.kreekt.animation.AnimationCompressor.CompressionConfig
import io.kreekt.animation.AnimationCompressor.Keyframe
import io.kreekt.core.math.Quaternion
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Quaternion-specific compression using quantization
 */
object QuaternionCompressor {

    /**
     * Compressed quaternion representation
     */
    data class CompressedQuaternion(
        val data: Int, // Packed quaternion data
        val largestComponent: Int // Which component was dropped
    ) {
        fun decompress(): Quaternion {
            // Simplified decompression - real implementation would properly unpack
            return Quaternion()
        }
    }

    /**
     * Compress quaternion track using quantization
     */
    fun compress(
        track: AnimationTrack,
        config: CompressionConfig
    ): AnimationTrack {
        if (track.type != AnimationTrack.TrackType.ROTATION) return track

        val compressedKeyframes = track.keyframes.map { keyframe ->
            val quat = keyframe.quaternion

            // Ensure shortest path
            if (config.useQuaternionShortestPath) {
                quat.normalize()
            }

            // Quantize quaternion
            val compressed = quantize(quat, config.quaternionBits)
            val decompressed = compressed.decompress()

            keyframe.copy(
                value = floatArrayOf(decompressed.x, decompressed.y, decompressed.z, decompressed.w)
            )
        }

        return track.copy(keyframes = compressedKeyframes.toMutableList())
    }

    /**
     * Quantize quaternion to reduce precision
     */
    fun quantize(quaternion: Quaternion, bits: Int): CompressedQuaternion {
        // Find largest component to drop (for compression)
        val components = arrayOf(
            abs(quaternion.x),
            abs(quaternion.y),
            abs(quaternion.z),
            abs(quaternion.w)
        )
        val largestIndex = components.indices.maxByOrNull { components[it] } ?: 3

        // Quantize remaining components
        val maxValue = (1 shl bits) - 1
        val scale = maxValue / sqrt(1f - components[largestIndex] * components[largestIndex])

        // Simplified compression - real implementation would pack bits efficiently
        val packedData = when (largestIndex) {
            0 -> { // Drop X
                ((quaternion.y * scale).toInt() and maxValue) or
                (((quaternion.z * scale).toInt() and maxValue) shl bits) or
                (((quaternion.w * scale).toInt() and maxValue) shl (bits * 2))
            }
            1 -> { // Drop Y
                ((quaternion.x * scale).toInt() and maxValue) or
                (((quaternion.z * scale).toInt() and maxValue) shl bits) or
                (((quaternion.w * scale).toInt() and maxValue) shl (bits * 2))
            }
            2 -> { // Drop Z
                ((quaternion.x * scale).toInt() and maxValue) or
                (((quaternion.y * scale).toInt() and maxValue) shl bits) or
                (((quaternion.w * scale).toInt() and maxValue) shl (bits * 2))
            }
            else -> { // Drop W
                ((quaternion.x * scale).toInt() and maxValue) or
                (((quaternion.y * scale).toInt() and maxValue) shl bits) or
                (((quaternion.z * scale).toInt() and maxValue) shl (bits * 2))
            }
        }

        return CompressedQuaternion(packedData, largestIndex)
    }
}
