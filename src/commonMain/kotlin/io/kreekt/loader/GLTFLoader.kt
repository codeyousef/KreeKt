package io.kreekt.loader

class GLTFLoader : AssetLoader<Any> {

    override suspend fun load(path: String): Any {
        // GLTF loading implementation
        return loadAsset(path)
    }

    private suspend fun loadAsset(path: String): Any {
        // Platform-specific loading
        return Any()
    }
}

