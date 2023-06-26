package io.tiangou

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import net.mamoe.mirai.console.util.cast
import net.mamoe.mirai.utils.MiraiLogger
import java.io.File
import java.nio.file.Path
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * 持久化存储接口
 *
 * 其实很想实现 [net.mamoe.mirai.console.data.PluginData] 但是我要以 JSON形式保存 而且我也没太整明白怎么实现PluginData
 *
 */
interface Storage<T> {

    /**
     * 数据存储文件
     */
    val storeFile: File

    /**
     * 从文件中加载数据
     */
    suspend fun load(): T?

    /**
     * 存储数据到文件中
     */
    suspend fun store(data: T): T

}

enum class StoragePathEnum(
    val path: Path
) {
    CONFIG_PATH(ValorantBotPlugin.configFolderPath),
    DATA_PATH(ValorantBotPlugin.dataFolderPath),
    ;
}

class StringStorage(
    fileName: String,
    storagePath: StoragePathEnum
) : Storage<String> {

    private val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    override val storeFile: File =
        storagePath.path.resolve(fileName).toFile()

    init {
        log.info("storage file path:${storeFile.absolutePath}")
        storeFile.createNewFile()
        if (!storeFile.isFile) {
            log.warning("${storeFile.name} is not a file!")
        }
    }

    override suspend fun load(): String? {
        if (!storeFile.isFile) {
            return null
        }
        return withContext(Dispatchers.IO) {
            storeFile.readText().takeIf { it.isNotEmpty() }
        }
    }

    override suspend fun store(data: String): String = data.apply {
        if (storeFile.isFile) {
            withContext(Dispatchers.IO) {
                storeFile.writeText(data)
            }
        }
    }
}

open class JsonStorage<T>(
    fileName: String,
    storagePath: StoragePathEnum,
    private val serializer: KSerializer<T>
) : Storage<T> {

    private val stringStorage: Storage<String> = StringStorage("$fileName.json", storagePath)

    override val storeFile: File
        get() = stringStorage.storeFile

    override suspend fun load(): T? =
        stringStorage.load()?.takeIf { it.isNotEmpty() }?.let { Global.json.decodeFromString(serializer, it) }

    override suspend fun store(data: T): T = data.apply {
        stringStorage.store(Global.json.encodeToString(serializer, data))
    }

}

abstract class AutoFlushStorage<T : Any>(
    storage: Storage<T>
) : Storage<T> by storage {

    protected abstract var data: T

    fun registry() {
        autoFlushList.find { it == this } ?: (autoFlushList.add(this.cast()))
    }

    companion object {

        private val autoFlushList: MutableList<AutoFlushStorage<Any>> = mutableListOf()

        private val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

        var data: Any? = null

        init {
            Global.coroutineScope.launch {
                while (true) {
                    delay(10.toDuration(DurationUnit.MINUTES))
                    autoFlushList.forEach { autoFlushStore ->
                        autoFlushStore.apply {
                            runCatching {
                                log.debug("auto flush data to file, path: ${storeFile.absolutePath}")
                                store(data)
                            }.onFailure {
                                log.warning("auto flush error", it)
                            }
                        }
                    }
                }
            }
        }

    }

}


