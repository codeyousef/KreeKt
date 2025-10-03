package io.kreekt.fog

import io.kreekt.core.math.Color

/**
 * Base interface for all fog types
 */
sealed interface FogBase {
    val color: Color
    val name: String
}
