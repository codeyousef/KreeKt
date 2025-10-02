/**
 * Spherical Harmonics Computation
 * Fast irradiance approximation using SH coefficients
 */
package io.kreekt.lighting.ibl

import io.kreekt.core.math.Color
import io.kreekt.core.math.Vector2
import io.kreekt.core.math.Vector3
import io.kreekt.lighting.IBLResult
import io.kreekt.lighting.SphericalHarmonics
import io.kreekt.renderer.CubeTexture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.PI
import kotlin.math.sin

/**
 * Compute spherical harmonics coefficients from cubemap
 */
suspend fun computeSphericalHarmonics(
    environmentMap: CubeTexture,
    order: Int,
    sampleCubemap: (CubeTexture, Vector3) -> Color,
    dispatcher: CoroutineContext
): IBLResult<SphericalHarmonics> = withContext(dispatcher) {
    try {
        val coefficients = Array(9) { Vector3.ZERO }

        // Sample the irradiance map to generate SH coefficients
        val sampleCount = 64 * 64 // Samples per face
        val deltaPhi = 2f * PI.toFloat() / 64f
        val deltaTheta = PI.toFloat() / 64f

        for (face in 0 until 6) {
            for (i in 0 until 64) {
                for (j in 0 until 64) {
                    val theta = (i + 0.5f) * deltaTheta
                    val phi = (j + 0.5f) * deltaPhi

                    val direction = cubeFaceToDirection(face, theta, phi)
                    val color = sampleCubemap(environmentMap, direction)

                    // Compute solid angle weight
                    val weight = sin(theta) * deltaTheta * deltaPhi

                    // Compute SH basis functions and accumulate coefficients
                    val sh = evaluateSphericalHarmonics(direction)
                    for (k in 0 until 9) {
                        coefficients[k] = coefficients[k].add(color.toVector3().multiplyScalar(sh[k] * weight))
                    }
                }
            }
        }

        // Normalize coefficients
        val normalizer = 4f * PI.toFloat() / sampleCount
        for (k in 0 until 9) {
            coefficients[k] = coefficients[k] * normalizer
        }

        val sh = IBLSphericalHarmonics(coefficients)
        IBLResult.Success(sh)
    } catch (e: Exception) {
        IBLResult.Error("Failed to generate spherical harmonics: ${e.message}")
    }
}

/**
 * Apply SH lighting to a normal
 */
fun applySHLighting(sh: SphericalHarmonics, normal: Vector3): Color {
    val shBasis = evaluateSphericalHarmonics(normal)
    var result = Vector3.ZERO

    for (i in 0 until 9) {
        result = result.add(sh.coefficients[i].clone().multiplyScalar(shBasis[i]))
    }

    return Color(result.x.coerceAtLeast(0f), result.y.coerceAtLeast(0f), result.z.coerceAtLeast(0f))
}

/**
 * Evaluate spherical harmonics basis functions
 */
fun evaluateSphericalHarmonics(direction: Vector3): FloatArray {
    val x = direction.x
    val y = direction.y
    val z = direction.z

    // SH basis functions (order 2)
    return floatArrayOf(
        0.282095f,                           // Y₀⁰
        0.488603f * y,                       // Y₁⁻¹
        0.488603f * z,                       // Y₁⁰
        0.488603f * x,                       // Y₁¹
        1.092548f * (x * y),                   // Y₂⁻²
        1.092548f * (y * z),                   // Y₂⁻¹
        0.315392f * (3f * z * z - 1f),       // Y₂⁰
        1.092548f * (x * z),                   // Y₂¹
        0.546274f * (x * x - (y * y))          // Y₂²
    )
}

/**
 * Spherical harmonics implementation
 */
internal data class IBLSphericalHarmonics(
    override val coefficients: Array<Vector3>
) : SphericalHarmonics {

    override fun evaluate(direction: Vector3): Vector3 {
        val sh = evaluateSphericalHarmonics(direction)
        var result = Vector3.ZERO

        for (i in 0 until 9) {
            result = result + coefficients[i] * sh[i]
        }

        return result
    }
}

/**
 * Convert cube face and spherical coordinates to direction
 */
private fun cubeFaceToDirection(face: Int, theta: Float, phi: Float): Vector3 {
    val x = sin(theta) * kotlin.math.cos(phi)
    val y = kotlin.math.cos(theta)
    val z = sin(theta) * sin(phi)

    return when (face) {
        0 -> Vector3(1f, -y, -x)     // +X
        1 -> Vector3(-1f, -y, x)     // -X
        2 -> Vector3(x, 1f, y)       // +Y
        3 -> Vector3(x, -1f, -y)     // -Y
        4 -> Vector3(x, -y, 1f)      // +Z
        5 -> Vector3(-x, -y, -1f)    // -Z
        else -> Vector3(0f, 0f, 1f)
    }.normalized
}

/**
 * Extension functions for Color
 */
private fun Color.toVector3(): Vector3 = Vector3(r, g, b)
private fun Color.Companion.fromVector3(v: Vector3): Color = Color(v.x, v.y, v.z)
