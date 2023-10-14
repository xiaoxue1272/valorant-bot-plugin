package io.tiangou

import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
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
 *
 * ------------------------------------
 *
 * 2023.07.15
 * mirai 2.15更新了插件可以以JSON格式保存 但是对于Map对象的属性委托 是否能检测到Map.Entry.value的内部属性变更, 暂时还存疑(看了一圈我觉得不能)
 * 所以不出意外的情况下,后续不会考虑迁移至PluginData
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
    CONFIG_PATH(PluginGlobal.configFolderPath),
    DATA_PATH(PluginGlobal.dataFolderPath),
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

private val json = Json {
    encodeDefaults = true
    useAlternativeNames = false
    isLenient = true
    prettyPrint = true
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
        stringStorage.load()?.takeIf { it.isNotEmpty() }?.let { json.decodeFromString(serializer, it) }

    override suspend fun store(data: T): T = data.apply {
        stringStorage.store(json.encodeToString(serializer, data))
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

        private suspend fun saveData() {
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

        init {
            PluginGlobal.coroutineScope.launch {
                while (true) {
                    delay(10.toDuration(DurationUnit.MINUTES))
                    saveData()
                }
            }.invokeOnCompletion {
                runBlocking(Dispatchers.IO) { saveData() }
            }
        }

    }

}


