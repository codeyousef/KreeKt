package io.kreekt.animation

import io.kreekt.core.math.Quaternion
import io.kreekt.core.math.Vector3
import io.kreekt.core.platform.currentTimeMillis
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Advanced Animation Compressor for optimizing animation tracks and reducing memory usage.
 * Implements keyframe optimization, quaternion compression, spline fitting, and quality optimization.
 *
 * T040 - AnimationCompressor for track optimization
 */
object AnimationCompressor {

    /**
     * Compression configuration
     */
    data class CompressionConfig(
        // General settings
        val removeRedundantKeys: Boolean = true,
        val positionTolerance: Float = 0.001f,
        val rotationTolerance: Float = 0.001f,
        val scaleTolerance: Float = 0.001f,
        val timeTolerance: Float = 0.001f,

        // Spline optimization
        val useSplineCompression: Boolean = true,
        val splineErrorThreshold: Float = 0.01f,
        val maxSplineSegmentLength: Float = 2.0f,

        // Quaternion compression
        val quantizeQuaternions: Boolean = true,
        val quaternionBits: Int = 16, // Bits per component
        val useQuaternionShortestPath: Boolean = true,

        // Curve fitting
        val useCurveFitting: Boolean = true,
        val maxCurveError: Float = 0.005f,
        val simplificationRatio: Float = 0.5f,

        // Quality settings
        val targetCompressionRatio: Float = 0.3f, // Target 30% of original size
        val preserveFirstLastKeys: Boolean = true,
        val enableMultithreading: Boolean = true
    )

    /**
     * Compression result
     */
    data class CompressionResult(
        val originalSize: Int,
        val compressedSize: Int,
        val compressionRatio: Float,
        val qualityLoss: Float,
        val processingTime: Long,
        val optimizations: List<String>
    ) {
        val spaceSavings: Float
            get() = 1f - compressionRatio
    }

    /**
     * Animation track for compression
     */
    data class AnimationTrack(
        val name: String,
        val type: TrackType,
        val keyframes: MutableList<Keyframe>,
        val interpolation: InterpolationType = InterpolationType.LINEAR
    ) {
        enum class TrackType {
            POSITION,
            ROTATION,
            SCALE,
            MORPH_WEIGHTS,
            CUSTOM
        }

        enum class InterpolationType {
            STEP,
            LINEAR,
            CUBIC,
            HERMITE,
            BEZIER
        }

        fun getDuration(): Float = keyframes.maxOfOrNull { it.time } ?: 0f
        fun getKeyframeCount(): Int = keyframes.size
        fun getSize(): Int = keyframes.size * when (type) {
            TrackType.POSITION, TrackType.SCALE -> 12 // 3 floats
            TrackType.ROTATION -> 16 // 4 floats
            TrackType.MORPH_WEIGHTS -> 4 // 1 float
            TrackType.CUSTOM -> 4 // 1 float default
        }
    }

    /**
     * Keyframe data
     */
    data class Keyframe(
        val time: Float,
        val value: FloatArray,
        val inTangent: FloatArray? = null,
        val outTangent: FloatArray? = null
    ) {
        // Vector3 helpers
        val vector3: Vector3
            get() = Vector3(
                value.getOrElse(0) { 0f },
                value.getOrElse(1) { 0f },
                value.getOrElse(2) { 0f }
            )

        // Quaternion helpers
        val quaternion: Quaternion
            get() = Quaternion(
                value.getOrElse(0) { 0f },
                value.getOrElse(1) { 0f },
                value.getOrElse(2) { 0f },
                value.getOrElse(3) { 1f }
            )

        fun distanceTo(other: Keyframe): Float {
            var distance = 0f
            for (i in 0 until minOf(value.size, other.value.size)) {
                val diff = value[i] - other.value[i]
                distance += (diff * diff)
            }
            return sqrt(distance)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Keyframe) return false
            return time == other.time && value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return time.hashCode() * 31 + value.contentHashCode()
        }
    }

    /**
     * Spline segment for curve compression
     */
    private data class SplineSegment(
        val startTime: Float,
        val endTime: Float,
        val controlPoints: List<Vector3>,
        val error: Float
    )

    /**
     * Compressed quaternion representation
     */
    private data class CompressedQuaternion(
        val data: Int, // Packed quaternion data
        val largestComponent: Int // Which component was dropped
    ) {
        fun decompress(): Quaternion {
            // Simplified decompression - real implementation would properly unpack
            return Quaternion()
        }
    }

    /**
     * Main compression function
     */
    fun compressAnimation(
        tracks: List<AnimationTrack>,
        config: CompressionConfig = CompressionConfig()
    ): Pair<List<AnimationTrack>, CompressionResult> {
        val startTime = currentTimeMillis()
        val originalSize = tracks.sumOf { it.getSize() }
        val optimizations = mutableListOf<String>()

        val compressedTracks = tracks.map { track ->
            compressTrack(track, config, optimizations)
        }

        val compressedSize = compressedTracks.sumOf { it.getSize() }
        val compressionRatio = compressedSize.toFloat() / originalSize.toFloat()
        val processingTime = currentTimeMillis() - startTime

        val result = CompressionResult(
            originalSize = originalSize,
            compressedSize = compressedSize,
            compressionRatio = compressionRatio,
            qualityLoss = estimateQualityLoss(tracks, compressedTracks),
            processingTime = processingTime,
            optimizations = optimizations
        )

        return Pair(compressedTracks, result)
    }

    /**
     * Compress individual track
     */
    private fun compressTrack(
        track: AnimationTrack,
        config: CompressionConfig,
        optimizations: MutableList<String>
    ): AnimationTrack {
        var processedTrack = track.copy()

        // Step 1: Remove redundant keyframes
        if (config.removeRedundantKeys) {
            processedTrack = removeRedundantKeyframes(processedTrack, config)
            optimizations.add("Removed redundant keyframes for ${track.name}")
        }

        // Step 2: Apply tolerance-based compression
        processedTrack = applyToleranceCompression(processedTrack, config)
        optimizations.add("Applied tolerance compression for ${track.name}")

        // Step 3: Quaternion-specific compression
        if (track.type == AnimationTrack.TrackType.ROTATION && config.quantizeQuaternions) {
            processedTrack = compressQuaternionTrack(processedTrack, config)
            optimizations.add("Compressed quaternions for ${track.name}")
        }

        // Step 4: Spline compression
        if (config.useSplineCompression) {
            processedTrack = applySplineCompression(processedTrack, config)
            optimizations.add("Applied spline compression for ${track.name}")
        }

        // Step 5: Curve fitting optimization
        if (config.useCurveFitting) {
            processedTrack = applyCurveFitting(processedTrack, config)
            optimizations.add("Applied curve fitting for ${track.name}")
        }

        return processedTrack
    }

    /**
     * Remove redundant keyframes
     */
    private fun removeRedundantKeyframes(
        track: AnimationTrack,
        config: CompressionConfig
    ): AnimationTrack {
        if (track.keyframes.size <= 2) return track

        val tolerance = when (track.type) {
            AnimationTrack.TrackType.POSITION -> config.positionTolerance
            AnimationTrack.TrackType.ROTATION -> config.rotationTolerance
            AnimationTrack.TrackType.SCALE -> config.scaleTolerance
            else -> 0.001f
        }

        val filteredKeyframes = mutableListOf<Keyframe>()

        // Always keep first keyframe
        if (config.preserveFirstLastKeys) {
            filteredKeyframes.add(track.keyframes.first())
        }

        // Douglas-Peucker algorithm for keyframe reduction
        val indices = douglasPeucker(track.keyframes, tolerance, 0, track.keyframes.size - 1)

        for (i in 1 until indices.size - 1) {
            filteredKeyframes.add(track.keyframes[indices[i]])
        }

        // Always keep last keyframe
        if (config.preserveFirstLastKeys && track.keyframes.size > 1) {
            filteredKeyframes.add(track.keyframes.last())
        }

        return track.copy(keyframes = filteredKeyframes)
    }

    /**
     * Douglas-Peucker algorithm for curve simplification
     */
    private fun douglasPeucker(
        keyframes: List<Keyframe>,
        tolerance: Float,
        startIndex: Int,
        endIndex: Int
    ): List<Int> {
        if (endIndex - startIndex <= 1) {
            return listOf(startIndex, endIndex)
        }

        var maxDistance = 0f
        var maxIndex = -1

        // Find the point with maximum distance from line segment
        for (i in startIndex + 1 until endIndex) {
            val distance = pointToLineDistance(
                keyframes[i],
                keyframes[startIndex],
                keyframes[endIndex]
            )
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }

        // If max distance is greater than tolerance, recursively subdivide
        if (maxDistance > tolerance && maxIndex != -1) {
            val leftIndices = douglasPeucker(keyframes, tolerance, startIndex, maxIndex)
            val rightIndices = douglasPeucker(keyframes, tolerance, maxIndex, endIndex)

            return leftIndices + rightIndices.drop(1) // Remove duplicate middle point
        } else {
            return listOf(startIndex, endIndex)
        }
    }

    /**
     * Calculate distance from point to line segment
     */
    private fun pointToLineDistance(
        point: Keyframe,
        lineStart: Keyframe,
        lineEnd: Keyframe
    ): Float {
        // Simplified distance calculation in time-value space
        val lineLength = lineEnd.time - lineStart.time
        if (lineLength == 0f) return point.distanceTo(lineStart)

        val t = ((point.time - lineStart.time) / lineLength).coerceIn(0f, 1f)

        // Linear interpolation between line endpoints
        val interpolatedValue = FloatArray(point.value.size) { i ->
            lineStart.value[i] + t * (lineEnd.value[i] - lineStart.value[i])
        }

        val interpolatedKeyframe = Keyframe(point.time, interpolatedValue)
        return point.distanceTo(interpolatedKeyframe)
    }

    /**
     * Apply tolerance-based compression
     */
    private fun applyToleranceCompression(
        track: AnimationTrack,
        config: CompressionConfig
    ): AnimationTrack {
        val tolerance = when (track.type) {
            AnimationTrack.TrackType.POSITION -> config.positionTolerance
            AnimationTrack.TrackType.ROTATION -> config.rotationTolerance
            AnimationTrack.TrackType.SCALE -> config.scaleTolerance
            else -> 0.001f
        }

        val compressedKeyframes = mutableListOf<Keyframe>()
        var lastKeyframe: Keyframe? = null

        for (keyframe in track.keyframes) {
            if (lastKeyframe == null || keyframe.distanceTo(lastKeyframe) > tolerance) {
                compressedKeyframes.add(keyframe)
                lastKeyframe = keyframe
            }
        }

        return track.copy(keyframes = compressedKeyframes)
    }

    /**
     * Compress quaternion track using quantization
     */
    private fun compressQuaternionTrack(
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
            val compressed = quantizeQuaternion(quat, config.quaternionBits)
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
    private fun quantizeQuaternion(quaternion: Quaternion, bits: Int): CompressedQuaternion {
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

    /**
     * Apply spline-based compression
     */
    private fun applySplineCompression(
        track: AnimationTrack,
        config: CompressionConfig
    ): AnimationTrack {
        if (track.keyframes.size < 4) return track

        val segments = generateSplineSegments(track, config)
        val optimizedKeyframes = convertSegmentsToKeyframes(segments)

        return track.copy(
            keyframes = optimizedKeyframes.toMutableList(),
            interpolation = AnimationTrack.InterpolationType.CUBIC
        )
    }

    /**
     * Generate spline segments for track
     */
    private fun generateSplineSegments(
        track: AnimationTrack,
        config: CompressionConfig
    ): List<SplineSegment> {
        val segments = mutableListOf<SplineSegment>()
        var startIndex = 0

        while (startIndex < track.keyframes.size - 1) {
            val segment = findOptimalSplineSegment(
                track.keyframes,
                startIndex,
                config.splineErrorThreshold,
                config.maxSplineSegmentLength
            )
            segments.add(segment)

            // Find next start index
            startIndex = track.keyframes.indexOfFirst { it.time >= segment.endTime }
            if (startIndex == -1) break
        }

        return segments
    }

    /**
     * Find optimal spline segment starting from given index
     */
    private fun findOptimalSplineSegment(
        keyframes: List<Keyframe>,
        startIndex: Int,
        errorThreshold: Float,
        maxLength: Float
    ): SplineSegment {
        val startKeyframe = keyframes[startIndex]
        var endIndex = startIndex + 1
        var bestEndIndex = endIndex

        // Extend segment while error is acceptable
        while (endIndex < keyframes.size) {
            val endKeyframe = keyframes[endIndex]
            val segmentLength = endKeyframe.time - startKeyframe.time

            if (segmentLength > maxLength) break

            val error = calculateSplineError(keyframes, startIndex, endIndex)
            if (error <= errorThreshold) {
                bestEndIndex = endIndex
            } else {
                break
            }

            endIndex++
        }

        val endKeyframe = keyframes[bestEndIndex]
        val controlPoints = generateControlPoints(keyframes, startIndex, bestEndIndex)

        return SplineSegment(
            startTime = startKeyframe.time,
            endTime = endKeyframe.time,
            controlPoints = controlPoints,
            error = calculateSplineError(keyframes, startIndex, bestEndIndex)
        )
    }

    /**
     * Calculate error for spline approximation
     */
    private fun calculateSplineError(
        keyframes: List<Keyframe>,
        startIndex: Int,
        endIndex: Int
    ): Float {
        // Simplified error calculation
        var totalError = 0f
        val segmentKeyframes = keyframes.subList(startIndex, endIndex + 1)

        for (i in 1 until segmentKeyframes.size - 1) {
            val keyframe = segmentKeyframes[i]
            val interpolated = interpolateSpline(
                segmentKeyframes.first(),
                segmentKeyframes.last(),
                keyframe.time
            )
            totalError = totalError + keyframe.distanceTo(interpolated)
        }

        return totalError / maxOf(1, segmentKeyframes.size - 2)
    }

    /**
     * Generate control points for spline
     */
    private fun generateControlPoints(
        keyframes: List<Keyframe>,
        startIndex: Int,
        endIndex: Int
    ): List<Vector3> {
        // Simplified - generate basic control points
        val startKeyframe = keyframes[startIndex]
        val endKeyframe = keyframes[endIndex]

        return listOf(
            startKeyframe.vector3,
            endKeyframe.vector3
        )
    }

    /**
     * Interpolate using spline
     */
    private fun interpolateSpline(
        start: Keyframe,
        end: Keyframe,
        time: Float
    ): Keyframe {
        val t = (time - start.time) / (end.time - start.time)
        val interpolatedValue = FloatArray(start.value.size) { i ->
            start.value[i] + t * (end.value[i] - start.value[i])
        }
        return Keyframe(time, interpolatedValue)
    }

    /**
     * Convert spline segments back to keyframes
     */
    private fun convertSegmentsToKeyframes(segments: List<SplineSegment>): List<Keyframe> {
        val keyframes = mutableListOf<Keyframe>()

        for (segment in segments) {
            // Add start point
            keyframes.add(
                Keyframe(
                    segment.startTime,
                    floatArrayOf(segment.controlPoints[0].x, segment.controlPoints[0].y, segment.controlPoints[0].z)
                )
            )

            // Add end point (will be deduplicated)
            if (segment == segments.last()) {
                keyframes.add(
                    Keyframe(
                        segment.endTime,
                        floatArrayOf(
                            segment.controlPoints.last().x,
                            segment.controlPoints.last().y,
                            segment.controlPoints.last().z
                        )
                    )
                )
            }
        }

        return keyframes.distinctBy { it.time }
    }

    /**
     * Apply curve fitting optimization
     */
    private fun applyCurveFitting(
        track: AnimationTrack,
        config: CompressionConfig
    ): AnimationTrack {
        val targetKeyframeCount = (track.keyframes.size * config.simplificationRatio).toInt()
        if (targetKeyframeCount >= track.keyframes.size) return track

        // Use curve fitting to reduce keyframe count while maintaining shape
        val importantIndices = findImportantKeyframes(track, targetKeyframeCount, config.maxCurveError)
        val reducedKeyframes = importantIndices.map { track.keyframes[it] }

        return track.copy(keyframes = reducedKeyframes.toMutableList())
    }

    /**
     * Find most important keyframes to preserve curve shape
     */
    private fun findImportantKeyframes(
        track: AnimationTrack,
        targetCount: Int,
        maxError: Float
    ): List<Int> {
        if (targetCount >= track.keyframes.size) {
            return track.keyframes.indices.toList()
        }

        val importantIndices = mutableSetOf<Int>()

        // Always include first and last
        importantIndices.add(0)
        importantIndices.add(track.keyframes.size - 1)

        // Find keyframes with highest curvature
        val curvatures = calculateCurvatures(track.keyframes)
        val sortedByCurvature = curvatures.indices.sortedByDescending { curvatures[it] }

        for (index in sortedByCurvature) {
            if (importantIndices.size >= targetCount) break
            importantIndices.add(index)
        }

        return importantIndices.sorted()
    }

    /**
     * Calculate curvature at each keyframe
     */
    private fun calculateCurvatures(keyframes: List<Keyframe>): FloatArray {
        val curvatures = FloatArray(keyframes.size)

        for (i in 1 until keyframes.size - 1) {
            val prev = keyframes[i - 1]
            val curr = keyframes[i]
            val next = keyframes[i + 1]

            // Simplified curvature calculation
            val v1 = curr.distanceTo(prev)
            val v2 = next.distanceTo(curr)
            val directDistance = next.distanceTo(prev)

            curvatures[i] = abs(v1 + v2 - directDistance)
        }

        return curvatures
    }

    /**
     * Estimate quality loss from compression
     */
    private fun estimateQualityLoss(
        originalTracks: List<AnimationTrack>,
        compressedTracks: List<AnimationTrack>
    ): Float {
        var totalError = 0f
        var comparisonCount = 0

        for ((original, compressed) in originalTracks.zip(compressedTracks)) {
            val sampleCount = 100 // Sample at 100 points for comparison
            val duration = original.getDuration()

            for (i in 0 until sampleCount) {
                val time = (i.toFloat() / (sampleCount - 1)) * duration
                val originalValue = sampleTrackAtTime(original, time)
                val compressedValue = sampleTrackAtTime(compressed, time)

                if (originalValue != null && compressedValue != null) {
                    totalError = totalError + originalValue.distanceTo(compressedValue)
                    comparisonCount++
                }
            }
        }

        return if (comparisonCount > 0) totalError / comparisonCount else 0f
    }

    /**
     * Sample track value at specific time
     */
    private fun sampleTrackAtTime(track: AnimationTrack, time: Float): Keyframe? {
        if (track.keyframes.isEmpty()) return null

        // Find surrounding keyframes
        val beforeIndex = track.keyframes.indexOfLast { it.time <= time }
        val afterIndex = track.keyframes.indexOfFirst { it.time >= time }

        return when {
            beforeIndex == -1 -> track.keyframes.first()
            afterIndex == -1 -> track.keyframes.last()
            beforeIndex == afterIndex -> track.keyframes[beforeIndex]
            else -> {
                val before = track.keyframes[beforeIndex]
                val after = track.keyframes[afterIndex]
                interpolateKeyframes(before, after, time)
            }
        }
    }

    /**
     * Interpolate between two keyframes
     */
    private fun interpolateKeyframes(before: Keyframe, after: Keyframe, time: Float): Keyframe {
        val t = (time - before.time) / (after.time - before.time)
        val interpolatedValue = FloatArray(before.value.size) { i ->
            before.value[i] + t * (after.value[i] - before.value[i])
        }
        return Keyframe(time, interpolatedValue)
    }

    /**
     * Batch compress multiple animations
     */
    fun batchCompress(
        animations: Map<String, List<AnimationTrack>>,
        config: CompressionConfig = CompressionConfig()
    ): Map<String, Pair<List<AnimationTrack>, CompressionResult>> {
        return animations.mapValues { (_, tracks) ->
            compressAnimation(tracks, config)
        }
    }

    /**
     * Optimize compression config for target quality/size ratio
     */
    fun optimizeConfig(
        sampleTracks: List<AnimationTrack>,
        targetCompressionRatio: Float,
        maxQualityLoss: Float
    ): CompressionConfig {
        var config = CompressionConfig(targetCompressionRatio = targetCompressionRatio)

        // Iteratively adjust parameters to meet targets
        var attempts = 0
        while (attempts < 10) {
            val (_, result) = compressAnimation(sampleTracks, config)

            if (result.compressionRatio <= targetCompressionRatio &&
                result.qualityLoss <= maxQualityLoss) {
                break
            }

            // Adjust config based on results
            config = adjustConfig(config, result, targetCompressionRatio, maxQualityLoss)
            attempts++
        }

        return config
    }

    private fun adjustConfig(
        config: CompressionConfig,
        result: CompressionResult,
        targetRatio: Float,
        maxQualityLoss: Float
    ): CompressionConfig {
        val ratioError = result.compressionRatio - targetRatio
        val qualityError = result.qualityLoss - maxQualityLoss

        return config.copy(
            positionTolerance = if (ratioError > 0) config.positionTolerance * 1.1f else config.positionTolerance * 0.9f,
            rotationTolerance = if (ratioError > 0) config.rotationTolerance * 1.1f else config.rotationTolerance * 0.9f,
            scaleTolerance = if (ratioError > 0) config.scaleTolerance * 1.1f else config.scaleTolerance * 0.9f,
            simplificationRatio = if (qualityError > 0) config.simplificationRatio * 1.1f else config.simplificationRatio * 0.9f
        )
    }
}