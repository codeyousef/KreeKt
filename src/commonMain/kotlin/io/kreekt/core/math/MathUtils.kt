package io.kreekt.core.math

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