import kotlinx.browser.window

fun main() {
    println("KreeKt JavaScript example loaded")

    // Initialize FPS counter display
    window.setInterval({
        val fpsElement = kotlinx.browser.document.getElementById("fps")
        if (fpsElement != null) {
            // This is just a placeholder - real FPS would come from the renderer
            fpsElement.textContent = "60"
        }
    }, 1000)
}