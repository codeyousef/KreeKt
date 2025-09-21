# KreeKt - Industry Standard KMP Project Structure

```
kreekt/
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   ├── libs.versions.toml              # Version catalog (Gradle 7+ standard)
│   └── build-logic/                     # Convention plugins
│       ├── settings.gradle.kts
│       ├── build.gradle.kts
│       └── src/main/kotlin/
│           ├── kreekt.kmp.library.gradle.kts
│           ├── kreekt.kmp.application.gradle.kts
│           └── kreekt.publishing.gradle.kts
│
├── kreekt/                              # Main library module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/
│       │   │   └── io/kreekt/
│       │   │       ├── core/
│       │   │       │   ├── math/
│       │   │       │   │   ├── Vector2.kt
│       │   │       │   │   ├── Vector3.kt
│       │   │       │   │   ├── Vector4.kt
│       │   │       │   │   ├── Matrix3.kt
│       │   │       │   │   ├── Matrix4.kt
│       │   │       │   │   ├── Quaternion.kt
│       │   │       │   │   ├── Color.kt
│       │   │       │   │   └── MathUtils.kt
│       │   │       │   ├── scene/
│       │   │       │   │   ├── Object3D.kt
│       │   │       │   │   ├── Scene.kt
│       │   │       │   │   ├── Group.kt
│       │   │       │   │   └── Transform.kt
│       │   │       │   ├── geometry/
│       │   │       │   │   ├── BufferGeometry.kt
│       │   │       │   │   ├── BufferAttribute.kt
│       │   │       │   │   └── GeometryUtils.kt
│       │   │       │   └── utils/
│       │   │       │       ├── Disposable.kt
│       │   │       │       └── Pool.kt
│       │   │       ├── renderer/
│       │   │       │   ├── Renderer.kt
│       │   │       │   ├── RenderTarget.kt
│       │   │       │   └── gpu/
│       │   │       │       ├── GPUDevice.kt
│       │   │       │       ├── GPUBuffer.kt
│       │   │       │       └── GPUPipeline.kt
│       │   │       ├── geometry/
│       │   │       │   ├── BoxGeometry.kt
│       │   │       │   ├── SphereGeometry.kt
│       │   │       │   ├── PlaneGeometry.kt
│       │   │       │   └── CylinderGeometry.kt
│       │   │       ├── material/
│       │   │       │   ├── Material.kt
│       │   │       │   ├── StandardMaterial.kt
│       │   │       │   ├── PhysicalMaterial.kt
│       │   │       │   └── ShaderMaterial.kt
│       │   │       ├── light/
│       │   │       │   ├── Light.kt
│       │   │       │   ├── DirectionalLight.kt
│       │   │       │   ├── PointLight.kt
│       │   │       │   └── SpotLight.kt
│       │   │       ├── camera/
│       │   │       │   ├── Camera.kt
│       │   │       │   ├── PerspectiveCamera.kt
│       │   │       │   └── OrthographicCamera.kt
│       │   │       ├── mesh/
│       │   │       │   ├── Mesh.kt
│       │   │       │   ├── InstancedMesh.kt
│       │   │       │   └── SkinnedMesh.kt
│       │   │       ├── animation/
│       │   │       │   ├── AnimationClip.kt
│       │   │       │   ├── AnimationMixer.kt
│       │   │       │   └── KeyframeTrack.kt
│       │   │       ├── loader/
│       │   │       │   ├── Loader.kt
│       │   │       │   ├── GLTFLoader.kt
│       │   │       │   └── TextureLoader.kt
│       │   │       └── controls/
│       │   │           ├── OrbitControls.kt
│       │   │           └── FirstPersonControls.kt
│       │   └── resources/
│       │       └── shaders/
│       │           ├── common/
│       │           │   ├── uniforms.wgsl
│       │           │   └── functions.wgsl
│       │           ├── vertex/
│       │           │   └── standard.wgsl
│       │           └── fragment/
│       │               ├── standard.wgsl
│       │               └── pbr.wgsl
│       │
│       ├── commonTest/
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           ├── math/
│       │           │   └── Vector3Test.kt
│       │           └── scene/
│       │               └── SceneTest.kt
│       │
│       ├── jvmMain/                     # Desktop (Windows/Mac/Linux)
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               ├── VulkanRenderer.kt
│       │               ├── VulkanDevice.kt
│       │               └── DesktopWindow.kt
│       │
│       ├── androidMain/                 # Android specific
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               ├── AndroidVulkanRenderer.kt
│       │               └── AndroidSurface.kt
│       │
│       ├── iosMain/                     # iOS specific
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               ├── MetalRenderer.kt
│       │               └── IOSSurface.kt
│       │
│       ├── jsMain/                      # Web/Browser
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               ├── WebGPURenderer.kt
│       │               ├── WebGPUDevice.kt
│       │               └── CanvasElement.kt
│       │
│       ├── wasmJsMain/                  # WebAssembly (future)
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               └── WasmRenderer.kt
│       │
│       ├── nativeMain/                  # Shared native code
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               └── NativeMemory.kt
│       │
│       ├── appleMain/                   # Shared Apple (iOS/macOS/tvOS)
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               └── AppleCommon.kt
│       │
│       ├── desktopMain/                 # Shared desktop (JVM/Native)
│       │   └── kotlin/
│       │       └── io/kreekt/
│       │           └── platform/
│       │               └── DesktopCommon.kt
│       │
│       └── mobileMain/                  # Shared mobile (Android/iOS)
│           └── kotlin/
│               └── io/kreekt/
│                   └── platform/
│                       └── MobileCommon.kt
│
├── kreekt-physics/                      # Optional physics module
│   ├── build.gradle.kts
│   └── src/
│       └── commonMain/kotlin/
│           └── io/kreekt/physics/
│               ├── World.kt
│               ├── RigidBody.kt
│               └── CollisionShape.kt
│
├── kreekt-postprocessing/               # Optional post-processing module
│   ├── build.gradle.kts
│   └── src/
│       └── commonMain/kotlin/
│           └── io/kreekt/postprocessing/
│               ├── EffectComposer.kt
│               ├── BloomPass.kt
│               └── SSAOPass.kt
│
├── samples/                             # Sample applications
│   ├── desktop/
│   │   ├── build.gradle.kts
│   │   └── src/jvmMain/kotlin/
│   │       └── DesktopApp.kt
│   ├── android/
│   │   ├── build.gradle.kts
│   │   └── src/androidMain/kotlin/
│   │       └── AndroidApp.kt
│   ├── ios/
│   │   ├── build.gradle.kts
│   │   └── src/iosMain/kotlin/
│   │       └── IOSApp.kt
│   └── web/
│       ├── build.gradle.kts
│       └── src/jsMain/kotlin/
│           └── WebApp.kt
│
├── benchmark/                           # Performance benchmarks
│   ├── build.gradle.kts
│   └── src/
│       └── commonMain/kotlin/
│           └── Benchmarks.kt
│
├── docs/
│   ├── getting-started.md
│   └── api/
│
├── .github/
│   ├── workflows/
│   │   ├── build.yml
│   │   ├── publish.yml
│   │   └── benchmark.yml
│   └── dependabot.yml
│
├── .editorconfig
├── .gitignore
├── build.gradle.kts                     # Root build configuration
├── settings.gradle.kts                  # Include all modules
├── gradle.properties
├── local.properties                     # Local dev settings (git ignored)
├── README.md
├── LICENSE
└── CONTRIBUTING.md
```

## gradle/libs.versions.toml
```toml
[versions]
kotlin = "1.9.22"
coroutines = "1.8.0"
serialization = "1.6.2"
ktor = "2.3.7"
compose = "1.5.11"
dokka = "1.9.10"
lwjgl = "3.3.3"
atomicfu = "0.23.1"

[libraries]
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-atomicfu = { module = "org.jetbrains.kotlinx:atomicfu", version.ref = "atomicfu" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
lwjgl-core = { module = "org.lwjgl:lwjgl", version.ref = "lwjgl" }
lwjgl-vulkan = { module = "org.lwjgl:lwjgl-vulkan", version.ref = "lwjgl" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
dokka = { id = "org.jetbrains.dokka", version.ref = "dokka" }
compose = { id = "org.jetbrains.compose", version.ref = "compose" }
```

## Root build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.dokka)
}

allprojects {
    group = "io.kreekt"
    version = "0.1.0-alpha01"
    
    repositories {
        mavenCentral()
        google()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

## kreekt/build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

kotlin {
    // JVM Target
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    
    // JS Target
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    
    // WASM Target (experimental)
    wasmJs {
        browser()
    }
    
    // Android Target
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    
    // iOS Targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    // macOS Targets
    macosX64()
    macosArm64()
    
    // Windows Target
    mingwX64()
    
    // Linux Target
    linuxX64()
    
    // Configure source sets
    sourceSets {
        // Common source set
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.atomicfu)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        // Desktop shared code (JVM)
        val jvmMain by getting {
            dependencies {
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.vulkan)
            }
        }
        
        // Web/Browser
        val jsMain by getting {
            dependencies {
                implementation(npm("@webgpu/types", "0.1.40"))
            }
        }
        
        // Native shared code
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        
        // Apple shared code
        val appleMain by creating {
            dependsOn(nativeMain)
        }
        
        val iosX64Main by getting {
            dependsOn(appleMain)
        }
        val iosArm64Main by getting {
            dependsOn(appleMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(appleMain)
        }
        val macosX64Main by getting {
            dependsOn(appleMain)
        }
        val macosArm64Main by getting {
            dependsOn(appleMain)
        }
        
        // Mobile shared code
        val mobileMain by creating {
            dependsOn(commonMain)
        }
        
        val androidMain by getting {
            dependsOn(mobileMain)
        }
    }
}

android {
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
    }
    namespace = "io.kreekt"
}
```

## Key Differences from Previous Structure

### Industry Standard Features:
1. **Single main module** with optional feature modules
2. **Gradle Version Catalogs** (libs.versions.toml)
3. **Convention plugins** in gradle/build-logic
4. **Hierarchical source sets** (appleMain, nativeMain, etc.)
5. **Proper platform targets** including WASM
6. **Standard KMP directory layout**

### Modern KMP Patterns:
- Shared source sets for platform groups
- NPM dependencies for JS
- Proper Android configuration
- iOS simulator support
- Version catalog for dependency management

### Simplified Structure:
- Core functionality in single module
- Optional modules only for major features
- Sample apps separate from main library
- Benchmarks in dedicated module

This is how modern KMP projects like Ktor, SQLDelight, and Compose Multiplatform are structured.
