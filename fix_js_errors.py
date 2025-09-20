#!/usr/bin/env python3
"""
Automated JS compilation error fixing script for KreeKt project.
Fixes common compilation errors in Kotlin/JS.
"""

import re
import os
from pathlib import Path

def fix_file(file_path, fixes):
    """Apply fixes to a single file."""
    try:
        with open(file_path, 'r') as f:
            content = f.read()

        original_content = content

        for fix in fixes:
            content = fix(content, file_path)

        if content != original_content:
            with open(file_path, 'w') as f:
                f.write(content)
            print(f"Fixed: {file_path}")
            return True
    except Exception as e:
        print(f"Error fixing {file_path}: {e}")
    return False

def fix_system_references(content, file_path):
    """Replace System.currentTimeMillis() with currentTimeMillis()"""
    if 'System.' in content:
        # Add import if not present
        if 'import io.kreekt.core.platform.currentTimeMillis' not in content:
            # Find the last import line
            import_lines = []
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if line.startswith('import '):
                    import_lines.append(i)

            if import_lines:
                last_import = import_lines[-1]
                lines.insert(last_import + 1, 'import io.kreekt.core.platform.currentTimeMillis')
                content = '\n'.join(lines)

        # Replace System.currentTimeMillis() with currentTimeMillis()
        content = re.sub(r'System\.currentTimeMillis\(\)', 'currentTimeMillis()', content)

        # Replace System.arraycopy with custom function
        content = re.sub(r'System\.arraycopy', 'platformArrayCopy', content)

        # Add import for platformArrayCopy if needed
        if 'platformArrayCopy' in content and 'import io.kreekt.core.platform.platformArrayCopy' not in content:
            lines = content.split('\n')
            import_lines = []
            for i, line in enumerate(lines):
                if line.startswith('import '):
                    import_lines.append(i)
            if import_lines:
                last_import = import_lines[-1]
                lines.insert(last_import + 1, 'import io.kreekt.core.platform.platformArrayCopy')
                content = '\n'.join(lines)

    return content

def fix_morphtargets_property(content, file_path):
    """Add morphTargets property to BufferGeometry"""
    if 'BufferGeometry.kt' in str(file_path) and 'class BufferGeometry' in content:
        if 'val morphTargets:' not in content:
            # Find where to add the property
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'val morphAttributes:' in line:
                    # Add morphTargets property after morphAttributes
                    new_property = """
    /**
     * Morph targets for backward compatibility
     * Returns the position morph attributes if they exist
     */
    val morphTargets: Array<BufferAttribute>?
        get() = _morphAttributes["position"]"""
                    lines.insert(i + 1, new_property)
                    content = '\n'.join(lines)
                    break
    return content

def fix_not_operator(content, file_path):
    """Fix 'not' operator issues"""
    # Fix patterns like "if (!something" to "if (something != null && !something"
    content = re.sub(r'if\s*\(\s*!\s*([a-zA-Z_][a-zA-Z0-9_]*)\.(isNullOrEmpty\(\))', r'if (\1?.isNullOrEmpty() != false', content)
    return content

def add_missing_properties(content, file_path):
    """Add missing properties to classes"""

    # Add dispatcher property to IBLProcessorImpl
    if 'IBLProcessor.kt' in str(file_path) and 'class IBLProcessorImpl' in content:
        if 'private val dispatcher' not in content:
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'class IBLProcessorImpl' in line:
                    # Find the class opening brace
                    for j in range(i, min(i+10, len(lines))):
                        if '{' in lines[j]:
                            # Add dispatcher property
                            lines.insert(j+1, '    private val dispatcher: CoroutineDispatcher = Dispatchers.Default')
                            content = '\n'.join(lines)
                            break
                    break

    # Add cache properties to IBLProcessorImpl
    if 'IBLProcessor.kt' in str(file_path) and 'class IBLProcessorImpl' in content:
        cache_props = [
            ('irradianceCache', 'MutableMap<String, CubeTexture>'),
            ('prefilterCache', 'MutableMap<String, CubeTexture>'),
            ('brdfLUTCache', 'MutableMap<String, Texture2D>'),
            ('shCache', 'MutableMap<String, SphericalHarmonics>')
        ]

        for prop_name, prop_type in cache_props:
            if f'private val {prop_name}' not in content:
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if 'private val dispatcher' in line:
                        lines.insert(i+1, f'    private val {prop_name}: {prop_type} = mutableMapOf()')
                        content = '\n'.join(lines)
                        break

    return content

def fix_ibl_result_issues(content, file_path):
    """Fix IBLResult type issues"""
    if 'IBLProcessor.kt' in str(file_path):
        # Fix Result.Success references
        content = re.sub(r'IBLResult\.Success<[^>]+>\(([^)]+)\)', r'IBLResult.Success(\1)', content)

        # Fix value property access
        content = re.sub(r'(\w+)\.value\b', r'(\1 as? IBLResult.Success<*>)?.data ?: throw IllegalStateException("Not a success result")', content)

    return content

def add_missing_imports(content, file_path):
    """Add missing imports"""
    imports_to_add = []

    # Check what needs to be imported
    if 'CoroutineDispatcher' in content and 'import kotlinx.coroutines.CoroutineDispatcher' not in content:
        imports_to_add.append('import kotlinx.coroutines.CoroutineDispatcher')

    if 'Dispatchers' in content and 'import kotlinx.coroutines.Dispatchers' not in content:
        imports_to_add.append('import kotlinx.coroutines.Dispatchers')

    if 'coroutineScope' in content and 'import kotlinx.coroutines.coroutineScope' not in content:
        imports_to_add.append('import kotlinx.coroutines.coroutineScope')

    if imports_to_add:
        lines = content.split('\n')
        import_lines = []
        for i, line in enumerate(lines):
            if line.startswith('import '):
                import_lines.append(i)

        if import_lines:
            last_import = import_lines[-1]
            for imp in imports_to_add:
                lines.insert(last_import + 1, imp)
                last_import += 1
            content = '\n'.join(lines)

    return content

def fix_texture_types(content, file_path):
    """Fix texture type references"""
    if 'TextureType' in content:
        content = re.sub(r'TextureType\.', 'TextureFilter.', content)

    if 'FLOAT32' in content and 'TextureFormat.' not in content:
        content = re.sub(r'(?<!TextureFormat\.)FLOAT32', 'TextureFormat.RGBA32F', content)

    if 'RGBA' in content and 'TextureFormat.' not in content:
        content = re.sub(r'(?<!TextureFormat\.)RGBA(?!\w)', 'TextureFormat.RGBA8', content)

    return content

def main():
    """Main function to fix all JS compilation errors."""

    src_dir = Path('/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin')

    fixes = [
        fix_system_references,
        fix_morphtargets_property,
        fix_not_operator,
        add_missing_properties,
        fix_ibl_result_issues,
        add_missing_imports,
        fix_texture_types,
    ]

    fixed_count = 0

    # Process all Kotlin files
    for kt_file in src_dir.rglob('*.kt'):
        if fix_file(kt_file, fixes):
            fixed_count += 1

    print(f"\nFixed {fixed_count} files")

if __name__ == '__main__':
    main()