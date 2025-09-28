import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("🚀 KreeKt Basic Scene Example")
    println("============================")

    try {
        val example = BasicSceneExample()

        println("Initializing example...")
        example.initialize()

        println("Scene created successfully!")
        example.printSceneInfo()

        // Simulate a few animation frames
        println("\nRunning animation frames...")
        repeat(5) { frame ->
            val deltaTime = 0.016f // ~60 FPS
            example.render(deltaTime * frame)
            println("Frame ${frame + 1} rendered")
            delay(100) // Small delay to see progress
        }

        println("\n✅ Example completed successfully!")

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}