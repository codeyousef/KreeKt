/**
 * Simple launcher for the KreeKt Basic Scene Example
 * This version demonstrates the core library functionality without complex LWJGL setup
 */

import kotlinx.coroutines.*
import io.kreekt.core.math.*
import io.kreekt.core.scene.*

fun main(args: Array<String>) = runBlocking {
    println("🚀 KreeKt Basic Scene Example - Simple Launcher")
    println("=" .repeat(50))

    // Demonstrate core KreeKt functionality
    demonstrateCoreMath()
    demonstrateSceneGraph()

    println("\n✅ Core KreeKt functionality working!")
    println("\nTo run the full interactive example:")
    println("./gradlew :examples:basic-scene:run")
    println("\nFor web version:")
    println("./gradlew :examples:basic-scene:jsBrowserDevelopmentRun")
}

fun demonstrateCoreMath() {
    println("\n📐 Testing Core Math Library:")

    // Vector operations
    val v1 = Vector3(1.0f, 2.0f, 3.0f)
    val v2 = Vector3(4.0f, 5.0f, 6.0f)
    val v3 = v1 + v2

    println("  Vector3 addition: $v1 + $v2 = $v3")
    println("  Vector3 length: |$v1| = ${v1.length()}")
    println("  Vector3 dot product: $v1 · $v2 = ${v1.dot(v2)}")

    // Matrix operations
    val m1 = Matrix4.identity()
    m1.setPosition(Vector3(10.0f, 20.0f, 30.0f))
    println("  Matrix4 translation: $m1")

    // Quaternion operations
    val q1 = Quaternion.fromAxisAngle(Vector3.UP, Math.PI.toFloat() / 4)
    println("  Quaternion from axis-angle: $q1")

    // Color operations
    val color1 = Color.RED
    val color2 = Color.BLUE
    val blended = Color.lerp(color1, color2, 0.5f)
    println("  Color blending: RED + BLUE = $blended")
}

fun demonstrateSceneGraph() {
    println("\n🏗️ Testing Scene Graph:")

    // Create scene
    val scene = Scene()
    println("  Created scene: ${scene::class.simpleName}")

    // Create objects
    val cube = Object3D().apply {
        position.set(1.0f, 0.0f, 0.0f)
        rotation.y = Math.PI.toFloat() / 4
        scale.set(2.0f, 2.0f, 2.0f)
    }

    val sphere = Object3D().apply {
        position.set(-1.0f, 1.0f, 0.0f)
    }

    // Build hierarchy
    scene.add(cube)
    scene.add(sphere)

    // Add child to cube
    val childObject = Object3D().apply {
        position.set(0.0f, 2.0f, 0.0f)
    }
    cube.add(childObject)

    println("  Scene objects: ${scene.children.size}")
    println("  Cube children: ${cube.children.size}")

    // Test world matrix calculation
    childObject.updateMatrixWorld()
    val worldPos = Vector3()
    childObject.matrixWorld.decompose(worldPos, null, null)
    println("  Child world position: $worldPos")

    // Test scene traversal
    var objectCount = 0
    scene.traverse {
        objectCount++
    }
    println("  Total objects in scene: $objectCount")
}