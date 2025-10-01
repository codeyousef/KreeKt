package io.kreekt.helper

import io.kreekt.core.scene.Object3D
import io.kreekt.core.math.Color

/**
 * Base class for visual debugging helpers
 */
abstract class Helper : Object3D() {
    var color: Color = Color(0xffffff)
}