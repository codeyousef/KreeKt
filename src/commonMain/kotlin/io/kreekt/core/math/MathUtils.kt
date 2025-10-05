package io.kreekt.core.math

import kotlin.math.abs

/**
 * Math utilities for common mathematical operations
 */
expect object MathUtils {
    /**
     * Power function for Float values
     */
    fun pow(base: Float, exponent: Float): Float

    /**
     * Power function for Double values
     */
    fun pow(base: Double, exponent: Double): Double
}

/**
 * Epsilon value for floating-point comparisons
 * Use 1e-6 for float precision (approximately 6 decimal places)
 */
const val EPSILON = 1e-6f

/**
 * Compare two floats for near-equality using epsilon
 * @param a First float value
 * @param b Second float value
 * @param epsilon Tolerance for comparison (default: EPSILON)
 * @return true if values are within epsilon of each other
 */
fun floatEquals(a: Float, b: Float, epsilon: Float = EPSILON): Boolean {
    return abs(a - b) < epsilon
}

/**
 * Check if a float is approximately zero
 * @param value Float value to check
 * @param epsilon Tolerance for comparison (default: EPSILON)
 * @return true if value is within epsilon of zero
 */
fun floatIsZero(value: Float, epsilon: Float = EPSILON): Boolean {
    return abs(value) < epsilon
}