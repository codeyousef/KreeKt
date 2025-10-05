// VoxelCraft Web Worker entry point
// This file is processed by webpack to create the worker bundle

// Import the Kotlin/JS worker module
// The actual path will be resolved by webpack
importScripts('./voxelcraft.js');

// Initialize the worker from the Kotlin code
if (typeof io !== 'undefined' &&
    io.kreekt &&
    io.kreekt.examples &&
    io.kreekt.examples.voxelcraft &&
    io.kreekt.examples.voxelcraft.initMeshWorker) {

    io.kreekt.examples.voxelcraft.initMeshWorker();
} else {
    console.error('Worker: Failed to find initMeshWorker function');
}