package io.kreekt.core.math

import kotlin.math.pow

/**
 * Math utilities for JVM platform
 */
actual object MathUtils {
    actual fun pow(base: Float, exponent: Float): Float {
        return base.toDouble().pow(exponent.toDouble()).toFloat()
    }

    actual fun pow(base: Double, exponent: Double): Double {
        return base.pow(exponent)
    }
}