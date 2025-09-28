package io.kreekt.texture

import io.kreekt.core.math.Matrix3
import io.kreekt.core.math.Vector2
import io.kreekt.renderer.TextureFilter
import io.kreekt.renderer.TextureFormat
import io.kreekt.renderer.TextureWrap

/**
 * Base texture class following Three.js Texture API
 * T104 - Comprehensive texture system with filtering, wrapping, and transformation
 */
abstract class Texture(
    override val id: Int = generateId(),
    var name: String = ""
) : io.kreekt.renderer.Texture {

    // Image data properties
    var image: Any? = null
    var mipmaps: Array<Any>? = null
    var mapping: TextureMapping = TextureMapping.UV

    // Texture transformation
    val offset: Vector2 = Vector2(0f, 0f)
    val repeat: Vector2 = Vector2(1f, 1f)
    val center: Vector2 = Vector2(0f, 0f)
    var rotation: Float = 0f

    // Texture matrix (computed from offset, repeat, center, rotation)
    val matrix: Matrix3 = Matrix3()
    var matrixAutoUpdate: Boolean = true

    // Filtering
    var magFilter: TextureFilter = TextureFilter.LINEAR
    var minFilter: TextureFilter = TextureFilter.LINEAR_MIPMAP_LINEAR

    // Wrapping
    var wrapS: TextureWrap = TextureWrap.REPEAT
    var wrapT: TextureWrap = TextureWrap.REPEAT

    // Anisotropic filtering
    var anisotropy: Float = 1f

    // Texture format and type
    var format: TextureFormat = TextureFormat.RGBA8
    var internalFormat: TextureFormat? = null
    var type: TextureType = TextureType.UNSIGNED_BYTE

    // Pixel settings
    var unpackAlignment: Int = 4
    var flipY: Boolean = true
    var premultiplyAlpha: Boolean = false

    // Generate mipmaps
    var generateMipmaps: Boolean = true

    // Update flags
    override var needsUpdate: Boolean = true
    var isRenderTarget: Boolean = false

    // Version for invalidation
    var version: Int = 0

    // User data
    val userData: MutableMap<String, Any> = mutableMapOf()

    // Events
    var onUpdate: (() -> Unit)? = null

    companion object {
        private var nextId = 1
        private fun generateId(): Int = nextId++

        // Texture constants matching Three.js
        const val DEFAULT_IMAGE = 1
        const val DEFAULT_MAPPING = 300
    }

    /**
     * Update the texture matrix from transformation properties
     */
    fun updateMatrix() {
        matrix.setUvTransform(offset.x, offset.y, repeat.x, repeat.y, rotation, center.x, center.y)
    }

    /**
     * Clone this texture (abstract - implemented by subclasses)
     */
    abstract fun clone(): Texture

    /**
     * Copy properties from another texture
     */
    open fun copy(source: Texture): Texture {
        name = source.name
        image = source.image
        mipmaps = source.mipmaps?.let { Array(it.size) { i -> it[i] } }
        mapping = source.mapping

        offset.copy(source.offset)
        repeat.copy(source.repeat)
        center.copy(source.center)
        rotation = source.rotation

        wrapS = source.wrapS
        wrapT = source.wrapT

        magFilter = source.magFilter
        minFilter = source.minFilter

        anisotropy = source.anisotropy

        format = source.format
        internalFormat = source.internalFormat
        type = source.type

        unpackAlignment = source.unpackAlignment
        flipY = source.flipY
        premultiplyAlpha = source.premultiplyAlpha
        generateMipmaps = source.generateMipmaps

        userData.clear()
        userData.putAll(source.userData)

        needsUpdate = true

        return this
    }

    /**
     * Dispose of this texture
     */
    override fun dispose() {
        version++
        needsUpdate = true
    }

    /**
     * Transform UV coordinates using this texture's matrix
     */
    fun transformUv(uv: Vector2): Vector2 {
        if (matrixAutoUpdate) updateMatrix()
        return uv.applyMatrix3(matrix)
    }

    override fun toString(): String = "Texture(id=$id, name='$name', ${width}x$height)"
}

/**
 * Texture mapping modes
 */
enum class TextureMapping {
    UV,
    CUBE_REFLECTION,
    CUBE_REFRACTION,
    EQUIRECTANGULAR_REFLECTION,
    EQUIRECTANGULAR_REFRACTION,
    SPHERICAL_REFLECTION,
    CYLINDRICAL_REFLECTION
}

/**
 * Texture data types
 */
enum class TextureType {
    UNSIGNED_BYTE,
    BYTE,
    SHORT,
    UNSIGNED_SHORT,
    INT,
    UNSIGNED_INT,
    FLOAT,
    HALF_FLOAT,
    UNSIGNED_SHORT_4_4_4_4,
    UNSIGNED_SHORT_5_5_5_1,
    UNSIGNED_SHORT_5_6_5,
    UNSIGNED_INT_24_8
}

/**
 * Matrix3 extension for UV transformation
 */
private fun Matrix3.setUvTransform(
    tx: Float, ty: Float,
    sx: Float, sy: Float,
    rotation: Float,
    cx: Float, cy: Float
) {
    val c = kotlin.math.cos(rotation)
    val s = kotlin.math.sin(rotation)

    set(
        sx * c, sx * s, -sx * (c * cx + s * cy) + cx + tx,
        -sy * s, sy * c, -sy * (-s * cx + c * cy) + cy + ty,
        0f, 0f, 1f
    )
}

/**
 * Vector2 extension for matrix application
 */
private fun Vector2.applyMatrix3(matrix: Matrix3): Vector2 {
    val x = this.x
    val y = this.y
    val e = matrix.elements

    this.x = e[0] * x + e[3] * y + e[6]
    this.y = e[1] * x + e[4] * y + e[7]

    return this
}