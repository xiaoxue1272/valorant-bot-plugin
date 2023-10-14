package io.tiangou

import io.tiangou.config.PluginConfig
import java.io.File
import java.util.*


sealed interface ByteArrayCache {

    fun put(value: ByteArray?)

    fun get(): ByteArray?

    fun getOrFail(): ByteArray = get() ?: throw NoSuchElementException("缓存为空")

    fun clean()

}

class FileByteArrayCache : ByteArrayCache {

    private val cacheElement: File =
        PluginGlobal.pluginCacheFolder.resolve(UUID.randomUUID().toString()).apply { deleteOnExit() }

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

class MemoryByteArrayCache : ByteArrayCache {

    private var cacheElement: ByteArray? = null

    override fun get(): ByteArray? = cacheElement

    override fun put(value: ByteArray?) {
        cacheElement = value
    }

    override fun clean() {
        cacheElement = null
    }
}

internal fun ByteArray.cache() = CacheFactory.create().apply { put(this@cache) }

object CacheFactory {

    fun create() = when (PluginConfig.drawImageConfig.cache) {
        PluginConfig.CacheType.MEMORY -> MemoryByteArrayCache()
        PluginConfig.CacheType.FILE -> FileByteArrayCache()
    }

}

