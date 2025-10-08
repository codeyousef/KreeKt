#!/bin/bash
# Debug script to run VoxelCraft JVM and capture full output

echo "Running VoxelCraft JVM with full error output..."
../../gradlew :examples:voxelcraft:runJvm --info --stacktrace 2>&1 | tee voxelcraft-jvm-error.log
