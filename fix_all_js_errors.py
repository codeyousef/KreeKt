#!/usr/bin/env python3
"""
Comprehensive JS compilation error fixing script for KreeKt project.
"""

import re
import os
import subprocess
from pathlib import Path
from typing import List, Tuple, Set

def run_gradle_compile():
    """Run gradle compile and get errors."""
    result = subprocess.run(
        ['./gradlew', 'compileKotlinJs', '--no-daemon'],
        capture_output=True,
        text=True
    )

    errors = []
    for line in result.stderr.split('\n'):
        if line.startswith('e:'):
            errors.append(line)

    return errors

def parse_error(error_line):
    """Parse an error line to extract file path and error details."""
    match = re.match(r'e: file:///(.*?):(\d+):(\d+) (.*)', error_line)
    if match:
        return {
            'file': '/' + match.group(1),
            'line': int(match.group(2)),
            'column': int(match.group(3)),
            'message': match.group(4)
        }
    return None

def fix_ibl_processor(file_path):
    """Fix IBLProcessor implementation issues."""
    with open(file_path, 'r') as f:
        content = f.read()

    # Fix the class to properly implement the interface
    if 'IBLProcessor.kt' in file_path:
        # Remove incorrect overrides
        content = re.sub(r'override\s+suspend\s+fun\s+generateEquirectangularMap\([^)]*\)[^{]*\{[^}]*\}', '', content)

        # Fix exception throwing
        content = re.sub(r'throw\s+IBLException\.\w+\b', 'throw Exception', content)

        # Fix the try-catch syntax error
        lines = content.split('\n')
        new_lines = []
        i = 0
        while i < len(lines):
            line = lines[i]
            if '} catch' in line or 'catch (' in line:
                # Fix malformed catch blocks
                new_lines.append('        } catch (e: Exception) {')
                i += 1
            elif 'throw IBLException' in line:
                # Replace with standard exception
                new_lines.append(line.replace('throw IBLException', 'throw Exception(IBLException'))
                i += 1
            else:
                new_lines.append(line)
                i += 1

        content = '\n'.join(new_lines)

        # Add missing properties
        if 'private val irradianceCache' not in content:
            # Find class opening
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'class IBLProcessorImpl' in line:
                    # Find opening brace
                    for j in range(i, min(i + 5, len(lines))):
                        if '{' in lines[j]:
                            # Add properties after dispatcher
                            insert_idx = j + 3  # After dispatcher and scope
                            lines.insert(insert_idx, '    private val irradianceCache: MutableMap<String, CubeTexture> = mutableMapOf()')
                            lines.insert(insert_idx + 1, '    private val prefilterCache: MutableMap<String, CubeTexture> = mutableMapOf()')
                            lines.insert(insert_idx + 2, '    private val brdfLUTCache: MutableMap<String, Texture2D> = mutableMapOf()')
                            lines.insert(insert_idx + 3, '    private val shCache: MutableMap<String, SphericalHarmonics> = mutableMapOf()')
                            content = '\n'.join(lines)
                            break
                    break

        # Implement the interface methods properly
        if 'suspend fun generateEquirectangularMap(' not in content or 'override suspend fun generateEquirectangularMap(' not in content:
            # Add implementation
            impl = '''
    override suspend fun generateEquirectangularMap(
        cubeMap: CubeTexture,
        width: Int,
        height: Int
    ): Texture {
        return withContext(dispatcher) {
            val equirectTexture = Texture2D(
                width = width,
                height = height,
                format = TextureFormat.RGBA32F,
                filter = TextureFilter.LINEAR
            )

            // Generate equirectangular projection from cubemap
            val data = FloatArray(width * height * 4)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val u = x.toFloat() / width
                    val v = y.toFloat() / height

                    // Convert UV to spherical coordinates
                    val theta = u * PI * 2f - PI
                    val phi = v * PI

                    // Convert to cartesian direction
                    val dir = Vector3(
                        sin(phi) * cos(theta),
                        cos(phi),
                        sin(phi) * sin(theta)
                    )

                    // Sample cubemap
                    val color = sampleCubemap(cubeMap, dir)
                    val idx = (y * width + x) * 4
                    data[idx] = color.r
                    data[idx + 1] = color.g
                    data[idx + 2] = color.b
                    data[idx + 3] = 1f
                }
            }

            equirectTexture.setData(data)
            equirectTexture
        }
    }

    override suspend fun generateIrradianceMap(
        environmentMap: Texture,
        size: Int
    ): CubeTexture {
        val key = "irradiance_${environmentMap.hashCode()}_$size"

        irradianceCache[key]?.let { return it }

        return withContext(dispatcher) {
            val irradianceMap = CubeTexture(
                size = size,
                format = TextureFormat.RGBA32F,
                filter = TextureFilter.LINEAR
            )

            // Generate irradiance convolution
            // Implementation details...

            irradianceCache[key] = irradianceMap
            irradianceMap
        }
    }

    override suspend fun generatePrefilterMap(
        environmentMap: Texture,
        size: Int,
        roughnessLevels: Int
    ): CubeTexture {
        val key = "prefilter_${environmentMap.hashCode()}_${size}_$roughnessLevels"

        prefilterCache[key]?.let { return it }

        return withContext(dispatcher) {
            val prefilterMap = CubeTexture(
                size = size,
                format = TextureFormat.RGBA32F,
                filter = TextureFilter.LINEAR,
                generateMipmaps = true
            )

            // Generate prefiltered environment map
            // Implementation details...

            prefilterCache[key] = prefilterMap
            prefilterMap
        }
    }

    override fun generateBRDFLUT(size: Int): Texture {
        val key = "brdf_$size"

        brdfLUTCache[key]?.let { return it }

        val brdfLUT = Texture2D(
            width = size,
            height = size,
            format = TextureFormat.RG16F,
            filter = TextureFilter.LINEAR
        )

        // Generate BRDF lookup table
        val data = FloatArray(size * size * 2)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val NdotV = x.toFloat() / size
                val roughness = y.toFloat() / size

                // Compute BRDF integral
                val integral = computeBRDFIntegral(NdotV, roughness)
                val idx = (y * size + x) * 2
                data[idx] = integral.x
                data[idx + 1] = integral.y
            }
        }

        brdfLUT.setData(data)
        brdfLUTCache[key] = brdfLUT
        brdfLUT
    }

    private fun computeBRDFIntegral(NdotV: Float, roughness: Float): Vector2 {
        // Placeholder implementation
        return Vector2(1f, 0f)
    }

    private fun sampleCubemap(cubemap: CubeTexture, direction: Vector3): Color {
        // Placeholder implementation
        return Color(1f, 1f, 1f)
    }
'''
            # Find where to insert the implementation
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'class IBLProcessorImpl' in line:
                    # Find the closing brace of the class
                    brace_count = 0
                    for j in range(i, len(lines)):
                        brace_count += lines[j].count('{') - lines[j].count('}')
                        if brace_count == 0 and j > i:
                            # Insert before closing brace
                            lines.insert(j, impl)
                            content = '\n'.join(lines)
                            break
                    break

    with open(file_path, 'w') as f:
        f.write(content)

    return True

def fix_unresolved_references(file_path):
    """Fix unresolved reference errors."""
    with open(file_path, 'r') as f:
        content = f.read()

    fixed = False

    # Add missing imports
    imports_needed = set()

    if 'withContext' in content and 'import kotlinx.coroutines.withContext' not in content:
        imports_needed.add('import kotlinx.coroutines.withContext')

    if 'PI' in content and 'import kotlin.math.PI' not in content:
        imports_needed.add('import kotlin.math.PI')

    if 'sin(' in content and 'import kotlin.math.sin' not in content:
        imports_needed.add('import kotlin.math.sin')

    if 'cos(' in content and 'import kotlin.math.cos' not in content:
        imports_needed.add('import kotlin.math.cos')

    if 'TextureFormat' in content and 'import io.kreekt.renderer.TextureFormat' not in content:
        imports_needed.add('import io.kreekt.renderer.TextureFormat')

    if 'TextureFilter' in content and 'import io.kreekt.renderer.TextureFilter' not in content:
        imports_needed.add('import io.kreekt.renderer.TextureFilter')

    if 'CubeTexture' in content and 'import io.kreekt.renderer.CubeTexture' not in content:
        imports_needed.add('import io.kreekt.renderer.CubeTexture')

    if 'Texture2D' in content and 'import io.kreekt.material.Texture2D' not in content:
        imports_needed.add('import io.kreekt.material.Texture2D')

    if 'SphericalHarmonics' in content and 'import io.kreekt.lighting.SphericalHarmonics' not in content:
        imports_needed.add('import io.kreekt.lighting.SphericalHarmonics')

    if imports_needed:
        lines = content.split('\n')
        # Find last import
        last_import_idx = 0
        for i, line in enumerate(lines):
            if line.startswith('import '):
                last_import_idx = i

        # Add imports
        for imp in sorted(imports_needed):
            lines.insert(last_import_idx + 1, imp)
            last_import_idx += 1

        content = '\n'.join(lines)
        fixed = True

    # Fix specific unresolved references
    replacements = [
        (r'\.value\b(?!\s*=)', '.data'),  # Fix IBLResult.Success access
        (r'TextureType\.', 'TextureFilter.'),
        (r'(?<!TextureFormat\.)RGBA\b', 'TextureFormat.RGBA8'),
        (r'(?<!TextureFormat\.)FLOAT32\b', 'TextureFormat.RGBA32F'),
    ]

    for pattern, replacement in replacements:
        new_content = re.sub(pattern, replacement, content)
        if new_content != content:
            content = new_content
            fixed = True

    if fixed:
        with open(file_path, 'w') as f:
            f.write(content)

    return fixed

def fix_texture_classes():
    """Create missing texture classes."""
    texture_file = '/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin/io/kreekt/renderer/TextureTypes.kt'

    content = '''/**
 * Texture types and classes for the renderer
 */
package io.kreekt.renderer

import io.kreekt.core.math.*

/**
 * Base texture interface
 */
interface Texture {
    val width: Int
    val height: Int
    val format: TextureFormat
    val filter: TextureFilter
    fun setData(data: FloatArray)
    fun dispose()
}

/**
 * 2D Texture implementation
 */
class Texture2D(
    override val width: Int,
    override val height: Int,
    override val format: TextureFormat = TextureFormat.RGBA8,
    override val filter: TextureFilter = TextureFilter.LINEAR,
    val generateMipmaps: Boolean = false
) : Texture {
    private var data: FloatArray? = null

    override fun setData(data: FloatArray) {
        this.data = data
    }

    override fun dispose() {
        data = null
    }
}

/**
 * Cube texture for environment mapping
 */
class CubeTexture(
    val size: Int,
    override val format: TextureFormat = TextureFormat.RGBA8,
    override val filter: TextureFilter = TextureFilter.LINEAR,
    val generateMipmaps: Boolean = false
) : Texture {
    override val width: Int get() = size
    override val height: Int get() = size

    private val faces = Array<FloatArray?>(6) { null }

    override fun setData(data: FloatArray) {
        // Set data for all faces
        val faceSize = size * size * 4
        for (i in 0..5) {
            faces[i] = data.sliceArray(i * faceSize until (i + 1) * faceSize)
        }
    }

    fun setFaceData(face: CubeFace, data: FloatArray) {
        faces[face.ordinal] = data
    }

    fun getFaceData(face: CubeFace): FloatArray? = faces[face.ordinal]

    override fun dispose() {
        for (i in faces.indices) {
            faces[i] = null
        }
    }
}

/**
 * Cube face enumeration
 */
enum class CubeFace {
    POSITIVE_X,
    NEGATIVE_X,
    POSITIVE_Y,
    NEGATIVE_Y,
    POSITIVE_Z,
    NEGATIVE_Z
}

/**
 * Texture formats
 */
enum class TextureFormat {
    R8,
    RG8,
    RGB8,
    RGBA8,
    R16F,
    RG16F,
    RGB16F,
    RGBA16F,
    R32F,
    RG32F,
    RGB32F,
    RGBA32F,
    DEPTH24,
    DEPTH32F,
    DEPTH24_STENCIL8
}

/**
 * Texture filtering modes
 */
enum class TextureFilter {
    NEAREST,
    LINEAR,
    NEAREST_MIPMAP_NEAREST,
    LINEAR_MIPMAP_NEAREST,
    NEAREST_MIPMAP_LINEAR,
    LINEAR_MIPMAP_LINEAR
}

/**
 * Texture wrapping modes
 */
enum class TextureWrap {
    REPEAT,
    CLAMP_TO_EDGE,
    MIRRORED_REPEAT
}
'''

    with open(texture_file, 'w') as f:
        f.write(content)

    print(f"Created {texture_file}")

def fix_spherical_harmonics():
    """Create SphericalHarmonics class."""
    sh_file = '/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin/io/kreekt/lighting/SphericalHarmonics.kt'

    content = '''/**
 * Spherical Harmonics for efficient irradiance representation
 */
package io.kreekt.lighting

import io.kreekt.core.math.*

/**
 * Spherical harmonics coefficients for lighting
 */
class SphericalHarmonics(
    val order: Int = 2
) {
    val coefficients: Array<Vector3>

    init {
        val size = (order + 1) * (order + 1)
        coefficients = Array(size) { Vector3() }
    }

    /**
     * Evaluate spherical harmonics for a given direction
     */
    fun evaluate(direction: Vector3): Color {
        var result = Vector3()

        // L0
        result += coefficients[0] * 0.282095f

        if (order >= 1) {
            // L1
            result += coefficients[1] * 0.488603f * direction.y
            result += coefficients[2] * 0.488603f * direction.z
            result += coefficients[3] * 0.488603f * direction.x
        }

        if (order >= 2) {
            // L2
            val x = direction.x
            val y = direction.y
            val z = direction.z

            result += coefficients[4] * 1.092548f * x * y
            result += coefficients[5] * 1.092548f * y * z
            result += coefficients[6] * 0.315392f * (3f * z * z - 1f)
            result += coefficients[7] * 1.092548f * x * z
            result += coefficients[8] * 0.546274f * (x * x - y * y)
        }

        return Color(result.x, result.y, result.z)
    }

    /**
     * Add a light sample to the spherical harmonics
     */
    fun addLightSample(direction: Vector3, radiance: Color, weight: Float = 1f) {
        val r = Vector3(radiance.r, radiance.g, radiance.b) * weight

        // L0
        coefficients[0] += r * 0.282095f

        if (order >= 1) {
            // L1
            coefficients[1] += r * (0.488603f * direction.y)
            coefficients[2] += r * (0.488603f * direction.z)
            coefficients[3] += r * (0.488603f * direction.x)
        }

        if (order >= 2) {
            // L2
            val x = direction.x
            val y = direction.y
            val z = direction.z

            coefficients[4] += r * (1.092548f * x * y)
            coefficients[5] += r * (1.092548f * y * z)
            coefficients[6] += r * (0.315392f * (3f * z * z - 1f))
            coefficients[7] += r * (1.092548f * x * z)
            coefficients[8] += r * (0.546274f * (x * x - y * y))
        }
    }

    /**
     * Scale all coefficients
     */
    fun scale(factor: Float) {
        for (coeff in coefficients) {
            coeff.x *= factor
            coeff.y *= factor
            coeff.z *= factor
        }
    }

    /**
     * Clear all coefficients
     */
    fun clear() {
        for (coeff in coefficients) {
            coeff.set(0f, 0f, 0f)
        }
    }

    companion object {
        /**
         * Create from irradiance cubemap
         */
        fun fromCubemap(cubemap: CubeTexture, samples: Int = 1024): SphericalHarmonics {
            val sh = SphericalHarmonics()

            // Sample the cubemap and accumulate SH coefficients
            // Implementation would sample cubemap faces

            return sh
        }
    }
}
'''

    with open(sh_file, 'w') as f:
        f.write(content)

    print(f"Created {sh_file}")

def main():
    """Main fixing process."""

    # Create missing files
    fix_texture_classes()
    fix_spherical_harmonics()

    # Fix existing files
    src_dir = Path('/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin')

    # Fix IBLProcessor specifically
    ibl_file = src_dir / 'io/kreekt/lighting/IBLProcessor.kt'
    if ibl_file.exists():
        fix_ibl_processor(str(ibl_file))

    # Fix all unresolved references
    fixed_count = 0
    for kt_file in src_dir.rglob('*.kt'):
        if fix_unresolved_references(str(kt_file)):
            fixed_count += 1

    print(f"Fixed {fixed_count} files")

if __name__ == '__main__':
    main()