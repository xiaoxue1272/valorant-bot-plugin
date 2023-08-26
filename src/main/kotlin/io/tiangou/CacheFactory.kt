package io.tiangou

import java.io.File
import java.util.UUID


sealed interface ByteArrayCache {

    fun put(value: ByteArray?)

    fun get(): ByteArray?

    fun clean()

}

class FileByteArrayCache: ByteArrayCache {

    private val cacheElement: File = Global.pluginCacheFolder.resolve(UUID.randomUUID().toString()).apply { deleteOnExit() }

    override fun get(): ByteArray? = cacheElement.takeIf { it.exists() }?.readBytes()

    override fun put(value: ByteArray?) {
        value?.let {
            cacheElement.createNewFile()
            cacheElement.writeBytes(it)
        }
    }

    override fun clean() {
        cacheElement.delete()
    }
}

class MemoryByteArrayCache: ByteArrayCache {

    private var cacheElement: ByteArray? = null

    override fun get(): ByteArray? = cacheElement

    override fun put(value: ByteArray?) {
        cacheElement = value
    }

    override fun clean() {
        cacheElement = null
    }
}



object CacheFactory {

    fun create() = when(Global.drawImageConfig.cache) {
        CacheType.MEMORY -> MemoryByteArrayCache()
        CacheType.FILE -> FileByteArrayCache()
    }

}

