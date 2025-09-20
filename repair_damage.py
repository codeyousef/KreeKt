#!/usr/bin/env python3
"""
Repair damage from incorrect fixes
"""

import re
import os
from pathlib import Path

def repair_files():
    """Repair files damaged by bad fixes."""
    src_dir = Path('/mnt/d/Projects/KMP/KreeKt/src/commonMain/kotlin')

    repairs_made = 0

    for kt_file in src_dir.rglob('*.kt'):
        try:
            with open(kt_file, 'r') as f:
                content = f.read()

            original = content

            # Fix incorrect parentheses patterns
            # Fix patterns like: array[(index * itemSize)] -> array[index * itemSize]
            content = re.sub(r'\[(\([^)]+\))\]', r'[\1]', content)
            content = re.sub(r'\[\(([^()]+)\)\]', r'[\1]', content)

            # Fix patterns like: 0.(488603f * y) -> 0.488603f * y
            content = re.sub(r'(\d+)\.(\()', r'\1 * (', content)
            content = re.sub(r'0\.\((\d+f)', r'0.\1', content)

            # Fix quaternion.(x * scale) -> quaternion.x * scale
            content = re.sub(r'quaternion\.\(([xyzw]) \*', r'quaternion.\1 *', content)

            # Fix vertices[i].(z * weight) -> vertices[i].z * weight
            content = re.sub(r'(\w+\[i\])\.\(([xyzw]) \*', r'\1.\2 *', content)

            # Fix bad SHOrder const
            content = content.replace('const val SHOrder.SH_L2', 'const val SH_L2')

            # Fix FloatArray size calculations
            content = re.sub(r'FloatArray\(\(([^)]+) \* ([^)]+)\)\)', r'FloatArray(\1 * \2)', content)

            # Fix function name issue in BufferGeometry
            content = re.sub(r'private fun "geometry-\$\{currentTimeMillis\(\)\}[^:]+: String \{[^}]+\}', '', content)

            # Fix arithmetic operations with extra parentheses
            content = re.sub(r' \+ \(([^()]+)\);', r' + \1;', content)
            content = re.sub(r' = \1 \+ ', r' += ', content)

            # Fix double multiplication
            content = re.sub(r'size \* \(size \* (\d+)\)', r'size * size * \1', content)
            content = re.sub(r'width \* \(height \* (\d+)\)', r'width * height * \1', content)

            # Fix division operations
            content = re.sub(r'/ \(\(([^)]+) \* ([^)]+)\)\)', r'/ (\1 * \2)', content)

            # Fix specific known issues in various files
            if 'IBLProcessor.kt' in str(kt_file):
                # Fix bad arithmetic patterns in IBL
                content = re.sub(r'(\w+) = (\1) \+ ([^;]+)', r'\1 += \3', content)

            if content != original:
                with open(kt_file, 'w') as f:
                    f.write(content)
                repairs_made += 1
                print(f"Repaired: {kt_file.name}")

        except Exception as e:
            print(f"Error repairing {kt_file}: {e}")

    return repairs_made

def main():
    """Main repair process."""
    print("Repairing damaged files...")
    repairs = repair_files()
    print(f"Repaired {repairs} files")

if __name__ == '__main__':
    main()