/**
 * Contract tests for MaterialFactory interface
 * These tests define the required behavior before implementation
 */
package io.kreekt.material

import io.kreekt.core.math.*
import kotlin.test.*

class MaterialFactoryTest {

    private lateinit var factory: MaterialFactory

    @BeforeTest
    fun setup() {
        // This will fail until we implement a concrete MaterialFactory
        factory = TODO("MaterialFactory implementation not available yet")
    }

    @Test
    fun testCreatePBRMaterial() {
        val options = PBRMaterialOptions(
            color = Color.RED,
            metalness = 0.8f,
            roughness = 0.2f,
            transparent = false,
            opacity = 1.0f
        )

        val material = factory.createPBRMaterial(options)

        assertNotNull(material, "PBR material should not be null")
        assertEquals(Color.RED, material.color, "Material color should match options")
        assertEquals(0.8f, material.metalness, "Material metalness should match options")
        assertEquals(0.2f, material.roughness, "Material roughness should match options")
        assertEquals(1.0f, material.opacity, "Material opacity should match options")
    }

    @Test
    fun testCreatePBRMaterialWithDefaults() {
        val material = factory.createPBRMaterial()

        assertNotNull(material, "Default PBR material should not be null")
        assertEquals(Color.WHITE, material.color, "Default color should be white")
        assertEquals(0f, material.metalness, "Default metalness should be 0")
        assertEquals(1f, material.roughness, "Default roughness should be 1")
        assertEquals(1f, material.opacity, "Default opacity should be 1")
    }

    @Test
    fun testCreateStandardMaterial() {
        val options = StandardMaterialOptions(
            color = Color.BLUE,
            transparent = true,
            opacity = 0.5f,
            wireframe = true
        )

        val material = factory.createStandardMaterial(options)

        assertNotNull(material, "Standard material should not be null")
        assertEquals(Color.BLUE, material.color, "Material color should match options")
        assertTrue(material.transparent, "Material should be transparent")
        assertEquals(0.5f, material.opacity, "Material opacity should match options")
        assertTrue(material.wireframe, "Material should be wireframe")
    }

    @Test
    fun testCreatePhysicalMaterial() {
        val options = PhysicalMaterialOptions(
            color = Color.GREEN,
            metalness = 0.5f,
            roughness = 0.3f,
            clearcoat = 0.8f,
            transmission = 0.9f
        )

        val material = factory.createPhysicalMaterial(options)

        assertNotNull(material, "Physical material should not be null")
        assertEquals(Color.GREEN, material.color, "Material color should match options")
        assertEquals(0.5f, material.metalness, "Material metalness should match options")
        assertEquals(0.3f, material.roughness, "Material roughness should match options")
        assertEquals(0.8f, material.clearcoat, "Material clearcoat should match options")
        assertEquals(0.9f, material.transmission, "Material transmission should match options")
    }

    @Test
    fun testCreateToonMaterial() {
        val gradientTexture = createTestTexture()
        val options = ToonMaterialOptions(
            color = Color.YELLOW,
            gradientMap = gradientTexture
        )

        val material = factory.createToonMaterial(options)

        assertNotNull(material, "Toon material should not be null")
        assertEquals(Color.YELLOW, material.color, "Material color should match options")
        assertEquals(gradientTexture, material.gradientMap, "Material gradient map should match options")
    }

    @Test
    fun testCreateMatcapMaterial() {
        val matcapTexture = createTestTexture()

        val material = factory.createMatcapMaterial(matcapTexture)

        assertNotNull(material, "Matcap material should not be null")
        assertEquals(matcapTexture, material.matcap, "Material matcap should match input texture")
    }

    @Test
    fun testCreateDepthMaterial() {
        val options = DepthMaterialOptions(
            depthPacking = DepthPacking.RGBA
        )

        val material = factory.createDepthMaterial(options)

        assertNotNull(material, "Depth material should not be null")
        assertEquals(DepthPacking.RGBA, material.depthPacking, "Material depth packing should match options")
    }

    @Test
    fun testCreateNormalMaterial() {
        val material = factory.createNormalMaterial()

        assertNotNull(material, "Normal material should not be null")
        // Normal material doesn't have specific configuration options to test
    }

    @Test
    fun testCreateShaderMaterial() {
        val vertexShader = """
            attribute vec3 position;
            uniform mat4 projectionMatrix;
            uniform mat4 modelViewMatrix;

            void main() {
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
        """.trimIndent()

        val fragmentShader = """
            uniform vec3 color;

            void main() {
                gl_FragColor = vec4(color, 1.0);
            }
        """.trimIndent()

        val material = factory.createShaderMaterial(vertexShader, fragmentShader)

        assertNotNull(material, "Shader material should not be null")
        assertEquals(vertexShader, material.vertexShader, "Vertex shader should match input")
        assertEquals(fragmentShader, material.fragmentShader, "Fragment shader should match input")
        assertNotNull(material.uniforms, "Uniforms map should not be null")
        assertNotNull(material.defines, "Defines map should not be null")
    }

    @Test
    fun testCreateRawShaderMaterial() {
        val vertexShader = """
            #version 300 es
            in vec3 position;
            uniform mat4 projectionMatrix;
            uniform mat4 modelViewMatrix;

            void main() {
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
        """.trimIndent()

        val fragmentShader = """
            #version 300 es
            precision highp float;
            uniform vec3 color;
            out vec4 fragColor;

            void main() {
                fragColor = vec4(color, 1.0);
            }
        """.trimIndent()

        val material = factory.createRawShaderMaterial(vertexShader, fragmentShader)

        assertNotNull(material, "Raw shader material should not be null")
        assertEquals(vertexShader, material.vertexShader, "Vertex shader should match input")
        assertEquals(fragmentShader, material.fragmentShader, "Fragment shader should match input")
    }

    @Test
    fun testCreatePointsMaterial() {
        val options = PointsMaterialOptions(
            color = Color.MAGENTA,
            size = 5f,
            sizeAttenuation = false
        )

        val material = factory.createPointsMaterial(options)

        assertNotNull(material, "Points material should not be null")
        assertEquals(Color.MAGENTA, material.color, "Material color should match options")
        assertEquals(5f, material.size, "Material size should match options")
        assertFalse(material.sizeAttenuation, "Material size attenuation should match options")
    }

    @Test
    fun testCreateLineMaterial() {
        val options = LineMaterialOptions(
            color = Color.CYAN,
            linewidth = 3f
        )

        val material = factory.createLineMaterial(options)

        assertNotNull(material, "Line material should not be null")
        assertEquals(Color.CYAN, material.color, "Material color should match options")
        assertEquals(3f, material.linewidth, "Material linewidth should match options")
    }

    @Test
    fun testCreateSpriteMaterial() {
        val spriteTexture = createTestTexture()
        val options = SpriteMaterialOptions(
            color = Color.ORANGE,
            map = spriteTexture,
            rotation = 45f
        )

        val material = factory.createSpriteMaterial(options)

        assertNotNull(material, "Sprite material should not be null")
        assertEquals(Color.ORANGE, material.color, "Material color should match options")
        assertEquals(spriteTexture, material.map, "Material map should match options")
        assertEquals(45f, material.rotation, "Material rotation should match options")
    }

    @Test
    fun testAdvancedPBRFeatures() {
        val material = factory.createPBRMaterial()

        // Test clearcoat properties
        material.clearcoat = 0.8f
        material.clearcoatRoughness = 0.1f
        assertEquals(0.8f, material.clearcoat, "Clearcoat should be settable")
        assertEquals(0.1f, material.clearcoatRoughness, "Clearcoat roughness should be settable")

        // Test transmission properties
        material.transmission = 0.9f
        material.thickness = 0.5f
        assertEquals(0.9f, material.transmission, "Transmission should be settable")
        assertEquals(0.5f, material.thickness, "Thickness should be settable")

        // Test iridescence properties
        material.iridescence = 0.7f
        material.iridescenceIOR = 1.3f
        assertEquals(0.7f, material.iridescence, "Iridescence should be settable")
        assertEquals(1.3f, material.iridescenceIOR, "Iridescence IOR should be settable")

        // Test sheen properties
        material.sheen = 0.6f
        material.sheenColor = Color.RED
        assertEquals(0.6f, material.sheen, "Sheen should be settable")
        assertEquals(Color.RED, material.sheenColor, "Sheen color should be settable")

        // Test anisotropy properties
        material.anisotropy = 0.4f
        material.anisotropyRotation = 30f
        assertEquals(0.4f, material.anisotropy, "Anisotropy should be settable")
        assertEquals(30f, material.anisotropyRotation, "Anisotropy rotation should be settable")
    }

    @Test
    fun testShaderMaterialUniforms() {
        val material = factory.createShaderMaterial("", "")

        // Test setting different uniform types
        material.setUniform("floatValue", 3.14f)
        material.setUniform("vec3Value", Vector3(1f, 2f, 3f))
        material.setUniform("colorValue", Color.RED)

        val floatUniform = material.getUniform("floatValue")
        assertNotNull(floatUniform, "Float uniform should exist")
        assertEquals(3.14f, floatUniform.value, "Float uniform value should match")

        val vec3Uniform = material.getUniform("vec3Value")
        assertNotNull(vec3Uniform, "Vec3 uniform should exist")
        assertEquals(Vector3(1f, 2f, 3f), vec3Uniform.value, "Vec3 uniform value should match")

        val colorUniform = material.getUniform("colorValue")
        assertNotNull(colorUniform, "Color uniform should exist")
        assertEquals(Color.RED, colorUniform.value, "Color uniform value should match")
    }

    @Test
    fun testShaderMaterialDefines() {
        val material = factory.createShaderMaterial("", "")

        // Test adding and removing defines
        material.addDefine("USE_UV", "")
        material.addDefine("MAX_LIGHTS", 8)
        material.addDefine("USE_SKINNING", true)

        assertTrue(material.defines.containsKey("USE_UV"), "USE_UV define should be added")
        assertEquals(8, material.defines["MAX_LIGHTS"], "MAX_LIGHTS define should have correct value")
        assertEquals(true, material.defines["USE_SKINNING"], "USE_SKINNING define should have correct value")

        material.removeDefine("USE_UV")
        assertFalse(material.defines.containsKey("USE_UV"), "USE_UV define should be removed")
    }

    @Test
    fun testMaterialFeatureFlags() {
        val material = factory.createShaderMaterial("", "")

        // Test feature flags
        material.wireframe = true
        material.morphTargets = true
        material.morphNormals = true
        material.skinning = true
        material.numSkinningBones = 64

        assertTrue(material.wireframe, "Wireframe should be enabled")
        assertTrue(material.morphTargets, "Morph targets should be enabled")
        assertTrue(material.morphNormals, "Morph normals should be enabled")
        assertTrue(material.skinning, "Skinning should be enabled")
        assertEquals(64, material.numSkinningBones, "Number of skinning bones should match")
    }

    @Test
    fun testInvalidParametersThrowExceptions() {
        assertFailsWith<IllegalArgumentException> {
            factory.createPBRMaterial(PBRMaterialOptions(metalness = -0.1f)) // Negative metalness
        }

        assertFailsWith<IllegalArgumentException> {
            factory.createPBRMaterial(PBRMaterialOptions(roughness = 1.1f)) // Roughness > 1
        }

        assertFailsWith<IllegalArgumentException> {
            factory.createStandardMaterial(StandardMaterialOptions(opacity = -0.1f)) // Negative opacity
        }

        assertFailsWith<IllegalArgumentException> {
            factory.createPointsMaterial(PointsMaterialOptions(size = 0f)) // Zero size
        }
    }

    @Test
    fun testTextureBinding() {
        val material = factory.createPBRMaterial()
        val texture = createTestTexture()

        // Test texture assignment
        material.map = texture
        material.normalMap = texture
        material.metalnessMap = texture
        material.roughnessMap = texture

        assertEquals(texture, material.map, "Albedo map should be assigned")
        assertEquals(texture, material.normalMap, "Normal map should be assigned")
        assertEquals(texture, material.metalnessMap, "Metalness map should be assigned")
        assertEquals(texture, material.roughnessMap, "Roughness map should be assigned")

        // Test texture clearing
        material.map = null
        assertNull(material.map, "Albedo map should be cleared")
    }

    // Helper methods to create test objects (these will also fail until implemented)
    private fun createTestTexture(): Texture {
        return TODO("Texture implementation not available yet")
    }
}

// Mock interfaces and classes for testing (these will be replaced with real implementations)
private interface Material {
    var color: Color
    var transparent: Boolean
    var opacity: Float
    var wireframe: Boolean
}

private interface StandardMaterial : Material

private interface PhysicalMaterial : PBRMaterial

private interface ToonMaterial : Material {
    var gradientMap: Texture?
}

private interface MatcapMaterial : Material {
    var matcap: Texture
}

private interface DepthMaterial : Material {
    var depthPacking: DepthPacking
}

private interface NormalMaterial : Material

private interface RawShaderMaterial : ShaderMaterial

private interface PointsMaterial : Material {
    var size: Float
    var sizeAttenuation: Boolean
}

private interface LineMaterial : Material {
    var linewidth: Float
}

private interface SpriteMaterial : Material {
    var map: Texture?
    var rotation: Float
}

private interface Texture

private interface CompiledShader

private interface ShaderProgram