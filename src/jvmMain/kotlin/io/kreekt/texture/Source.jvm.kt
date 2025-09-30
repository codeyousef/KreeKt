package io.kreekt.texture

import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

/**
 * JVM implementation of ImageLoader
 */
actual class ImageLoader {
    private var crossOrigin: String = ""
    private var requestHeaders = mutableMapOf<String, String>()
    private var basePath: String = ""

    actual fun load(
        url: String,
        onLoad: ((ImageElement) -> Unit)?,
        onProgress: ((ProgressEvent) -> Unit)?,
        onError: ((ErrorEvent) -> Unit)?
    ): ImageElement {
        val fullUrl = if (basePath.isNotEmpty()) "$basePath/$url" else url
        val imageElement = ImageElement()

        try {
            val image = ImageIO.read(URL(fullUrl))
            imageElement.bufferedImage = image
            imageElement.src = fullUrl
            imageElement.width = image.width
            imageElement.height = image.height
            onLoad?.invoke(imageElement)
        } catch (e: Exception) {
            onError?.invoke(ErrorEvent("Failed to load image: ${e.message}"))
        }

        return imageElement
    }

    actual fun setCrossOrigin(crossOrigin: String): ImageLoader {
        this.crossOrigin = crossOrigin
        return this
    }

    actual fun setRequestHeader(headers: Map<String, String>): ImageLoader {
        this.requestHeaders.putAll(headers)
        return this
    }

    actual fun setPath(path: String): ImageLoader {
        this.basePath = path
        return this
    }
}

/**
 * JVM implementation of ImageElement using BufferedImage
 */
actual class ImageElement {
    internal var bufferedImage: BufferedImage? = null
    actual var src: String = ""
    actual var width: Int = 0
    actual var height: Int = 0

    actual val complete: Boolean
        get() = bufferedImage != null

    actual val naturalWidth: Int
        get() = bufferedImage?.width ?: 0

    actual val naturalHeight: Int
        get() = bufferedImage?.height ?: 0

    private val eventListeners = mutableMapOf<String, MutableList<() -> Unit>>()

    actual fun addEventListener(event: String, callback: () -> Unit) {
        eventListeners.getOrPut(event) { mutableListOf() }.add(callback)
    }

    actual fun removeEventListener(event: String, callback: () -> Unit) {
        eventListeners[event]?.remove(callback)
    }

    internal fun trigger(event: String) {
        eventListeners[event]?.forEach { it() }
    }
}

/**
 * JVM implementation of CanvasElement (stub - not fully functional on JVM)
 */
actual class CanvasElement {
    actual var width: Int = 0
    actual var height: Int = 0

    actual fun toDataURL(type: String, quality: Float): String {
        // TODO: Implement canvas to data URL conversion
        return ""
    }

    actual fun getContext(contextId: String): Any? {
        // TODO: Implement canvas context
        return null
    }
}

/**
 * JVM implementation of VideoElement (stub - not fully functional on JVM)
 */
actual class VideoElement {
    actual var src: String = ""
    actual var width: Int = 0
    actual var height: Int = 0
    actual var currentTime: Float = 0f
    actual var duration: Float = 0f
    actual val paused: Boolean = true
    actual val ended: Boolean = false

    actual fun play() {
        // TODO: Implement video playback
    }

    actual fun pause() {
        // TODO: Implement video pause
    }

    actual fun addEventListener(event: String, callback: () -> Unit) {
        // TODO: Implement event listeners
    }

    actual fun removeEventListener(event: String, callback: () -> Unit) {
        // TODO: Implement event listener removal
    }
}
