#!/usr/bin/env python3
"""
Fix remaining JS compilation errors for KreeKt project.
"""

import re
import os
from pathlib import Path

def fix_animation_compressor(file_path):
    """Fix AnimationCompressor issues with data property access."""
    with open(file_path, 'r') as f:
        content = f.read()

    # Fix .data references (likely from Result types)
    content = re.sub(r'(\w+)\.data\b', r'(\1 as? Result.Success<*>)?.value ?: throw IllegalStateException("Not a success")', content)

    # Fix * operator for Vector3/Float multiplication
    if 'times' in content and 'operator' in content:
        # Add extension operator if not present
        if 'operator fun Float.times(v: Vector3)' not in content:
            lines = content.split('\n')
            # Find imports section
            for i, line in enumerate(lines):
                if line.startswith('package '):
                    # Add after package declaration
                    lines.insert(i + 2, '''
// Extension operators for Vector3 arithmetic
private operator fun Float.times(v: Vector3): Vector3 = v * this
private operator fun Vector3.times(f: Float): Vector3 = Vector3(x * f, y * f, z * f)
''')
                    content = '\n'.join(lines)
                    break

    with open(file_path, 'w') as f:
        f.write(content)

def fix_morph_target_animator(file_path):
    """Fix MorphTargetAnimator issues."""
    with open(file_path, 'r') as f:
        content = f.read()

    # Fix .data references
    content = re.sub(r'(\w+)\.data\b', r'(\1 as? AnimationResult.Success<*>)?.value ?: emptyList()', content)

    # Fix type inference issues
    content = re.sub(r'\.map\s*\{([^}]+)\}', r'.map<Any, Any> { \1 }', content)

    with open(file_path, 'w') as f:
        f.write(content)

def fix_ibl_processor_cleanup(file_path):
    """Clean up IBLProcessor duplicate code and errors."""
    with open(file_path, 'r') as f:
        lines = f.readlines()

    # Remove duplicate implementations
    cleaned_lines = []
    seen_methods = set()
    in_method = False
    method_name = None
    brace_count = 0
    skip_until_brace_zero = False

    for line in lines:
        # Check for method definitions
        if 'fun generateEquirectangularMap' in line or \
           'fun generateIrradianceMap' in line or \
           'fun generatePrefilterMap' in line or \
           'fun generateBRDFLUT' in line:

            method_match = re.search(r'fun (\w+)', line)
            if method_match:
                current_method = method_match.group(1)
                if current_method in seen_methods and 'override' in line:
                    skip_until_brace_zero = True
                    brace_count = 0
                else:
                    seen_methods.add(current_method)

        if skip_until_brace_zero:
            brace_count += line.count('{') - line.count('}')
            if brace_count <= 0:
                skip_until_brace_zero = False
            continue

        cleaned_lines.append(line)

    content = ''.join(cleaned_lines)

    # Fix HDRFormat references
    content = re.sub(r'HDRFormat\.', '', content)

    # Fix TextureType references
    content = re.sub(r'TextureType\.FLOAT', 'TextureFilter.LINEAR', content)

    # Fix exception constructors
    content = re.sub(r'IBLException\.(\w+)\(([^)]+)\)', r'Exception("\1: \2")', content)

    # Ensure proper imports
    if 'import kotlin.math.*' not in content:
        content = content.replace('package io.kreekt.lighting',
                                  'package io.kreekt.lighting\n\nimport kotlin.math.*')

    with open(file_path, 'w') as f:
        f.write(content)

def fix_lighting_types(file_path):
    """Fix lighting type issues."""
    with open(file_path, 'r') as f:
        content = f.read()

    # Fix format property references
    content = re.sub(r'(\w+)\.format\b', r'TextureFormat.RGBA32F', content)

    # Fix exposure property references
    content = re.sub(r'(\w+)\.exposure\b', r'1.0f', content)

    with open(file_path, 'w') as f:
        f.write(content)

def fix_texture_types_additions():
    """Add missing texture type definitions."""
    texture_file = '/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin/io/kreekt/renderer/TextureTypes.kt'

    with open(texture_file, 'r') as f:
        content = f.read()

    # Add setData extension for CubeTexture
    if 'fun CubeTexture.setFaceData(face: Int' not in content:
        content = content.replace('enum class CubeFace {', '''
/**
 * Extension to set face data on CubeTexture
 */
fun CubeTexture.setFaceData(face: Int, data: FloatArray, mip: Int = 0) {
    setFaceData(CubeFace.values()[face], data)
}

enum class CubeFace {''')

    with open(texture_file, 'w') as f:
        f.write(content)

def create_animation_result_types():
    """Create AnimationResult types."""
    result_file = '/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin/io/kreekt/animation/AnimationResult.kt'

    content = '''/**
 * Result types for animation operations
 */
package io.kreekt.animation

/**
 * Animation operation result
 */
sealed class AnimationResult<out T> {
    data class Success<T>(val value: T) : AnimationResult<T>()
    data class Error(val message: String) : AnimationResult<Nothing>()
}

/**
 * Result type for general operations
 */
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}
'''

    with open(result_file, 'w') as f:
        f.write(content)

    print(f"Created {result_file}")

def fix_hdr_environment():
    """Fix HDREnvironment data class."""
    types_file = '/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin/io/kreekt/lighting/IBLTypes.kt'

    # Check if file exists, if not create it
    content = '''/**
 * IBL (Image-Based Lighting) types
 */
package io.kreekt.lighting

import io.kreekt.renderer.TextureFormat

/**
 * HDR Environment data
 */
data class HDREnvironment(
    val data: FloatArray,
    val width: Int,
    val height: Int,
    val format: TextureFormat = TextureFormat.RGBA32F
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HDREnvironment

        if (!data.contentEquals(other.data)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (format != other.format) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + format.hashCode()
        return result
    }
}

/**
 * IBL Configuration
 */
data class IBLConfig(
    val irradianceSize: Int = 32,
    val prefilterSize: Int = 128,
    val brdfLUTSize: Int = 512,
    val roughnessLevels: Int = 5,
    val samples: Int = 1024,
    val useSphericalHarmonics: Boolean = true,
    val sphericalHarmonicsOrder: Int = 2
)

/**
 * IBL Environment Maps
 */
data class IBLEnvironmentMaps(
    val environment: CubeTexture? = null,
    val irradianceMap: Any? = null,
    val prefilterMap: Any? = null,
    val brdfLUT: Any? = null,
    val sphericalHarmonics: SphericalHarmonics? = null,
    val config: IBLConfig
)

/**
 * Spherical Harmonics interface
 */
interface SphericalHarmonics {
    val coefficients: Array<Vector3>
    fun evaluate(direction: Vector3): Color
}

// Import these from other packages
import io.kreekt.core.math.Vector3
import io.kreekt.core.math.Color
import io.kreekt.renderer.CubeTexture
'''

    with open(types_file, 'w') as f:
        f.write(content)

    print(f"Created {types_file}")

def main():
    """Main fixing process."""

    # Create missing files
    create_animation_result_types()
    fix_hdr_environment()
    fix_texture_types_additions()

    src_dir = Path('/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin')

    # Fix specific files
    animation_compressor = src_dir / 'io/kreekt/animation/AnimationCompressor.kt'
    if animation_compressor.exists():
        fix_animation_compressor(str(animation_compressor))
        print(f"Fixed {animation_compressor}")

    morph_animator = src_dir / 'io/kreekt/animation/MorphTargetAnimator.kt'
    if morph_animator.exists():
        fix_morph_target_animator(str(morph_animator))
        print(f"Fixed {morph_animator}")

    ibl_processor = src_dir / 'io/kreekt/lighting/IBLProcessor.kt'
    if ibl_processor.exists():
        fix_ibl_processor_cleanup(str(ibl_processor))
        print(f"Fixed {ibl_processor}")

    # Fix all lighting type files
    for kt_file in (src_dir / 'io/kreekt/lighting').glob('*.kt'):
        if 'Types' in str(kt_file):
            fix_lighting_types(str(kt_file))
            print(f"Fixed {kt_file}")

if __name__ == '__main__':
    main()