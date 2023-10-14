@file:UseSerializers(FileSerializer::class)

package io.tiangou.repository

import io.tiangou.*
import io.tiangou.api.RiotClientData
import io.tiangou.logic.LogicController
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import java.io.File

@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var logoutDay: Int = 0,
    var customBackgroundFile: File? = null,
    val subscribes: MutableMap<Long, MutableList<SubscribeType>> = mutableMapOf(),
) {

    @Transient
    private val lock = Mutex()

    @Transient
    val logicController: LogicController = LogicController()

    @Transient
    private val imageCaches: MutableMap<GenerateImageType, ByteArrayCache> = mutableMapOf()

    fun cacheImage(type: GenerateImageType, byteArray: ByteArray) {
        imageCaches.put(type, byteArray.cache())?.clean()
    }

    suspend fun cacheImage(type: GenerateImageType, block: suspend (GenerateImageType) -> ByteArray) =
        imageCaches.put(type, block(type).cache())?.clean()

    fun getOrCacheImage(type: GenerateImageType, byteArray: ByteArray) =
        imageCaches.getOrPut(type) { byteArray.cache() }

    suspend fun getOrCacheImage(type: GenerateImageType, block: suspend (GenerateImageType) -> ByteArray) =
        imageCaches.getOrPut(type) { block(type).cache() }

    fun clearCacheImages() = imageCaches.apply { values.forEach { it.clean() } }.clear()

    fun removeCacheImage(type: GenerateImageType) = imageCaches.remove(type)?.clean()


    suspend fun <T> synchronous(block: suspend UserCache.() -> T): T {
        return try {
            lock.lock(this)
            block(this@UserCache)
        } finally {
            lock.takeIf { it.holdsLock(this) }?.unlock(this)
        }
    }

    fun <T> synchronized(block: suspend UserCache.() -> T): T {
        return runBlocking { synchronous(block) }
    }

}

object UserCacheRepository : AutoFlushStorage<MutableMap<Long, UserCache>>(
    JsonStorage("user-cache", StoragePathEnum.DATA_PATH, serializer())
) {

    override var data: MutableMap<Long, UserCache> = runBlocking { load() ?: store(mutableMapOf()) }

    override suspend fun load(): MutableMap<Long, UserCache>? = try {
        super.load()
    } catch (e: Exception) {
        OldUserCacheDataStructureAdapter().adapt()
    }

    init {
        registry()
    }

    operator fun get(qq: Long): UserCache = data.getOrPut(qq) {
        UserCache().apply {
            subscribes[qq] = SubscribeType.all()
        }
    }

    fun remove(qq: Long) {
        data.remove(qq)?.apply {
            customBackgroundFile?.delete()
            clearCacheImages()
        }
    }

    fun getAllUserCache(): Map<Long, UserCache> = data

}

// 现在是v0.8.0 需要写一个旧的 v0.7.0的 userCache数据结构适配器 completed

@Serializable
data class OldUserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    var subscribeList: List<SubscribeType> = listOf(),
    var customBackgroundFile: File? = null,
    val dailyStorePushLocates: MutableMap<Long, ContactEnum> = mutableMapOf(),
) {

    @Transient
    val lock = Mutex()

    @Transient
    val logicController: LogicController = LogicController()

    @Serializable
    enum class ContactEnum {
        USER,
        GROUP
    }

}

class OldUserCacheDataStructureAdapter :
    JsonStorage<MutableMap<Long, OldUserCache>>("user-cache", StoragePathEnum.DATA_PATH, serializer()) {

    suspend fun adapt(): MutableMap<Long, UserCache>? {
        return load()?.mapValues {
            val userCache = UserCache(
                it.value.riotClientData,
                it.value.isRiotAccountLogin,
                0,
                it.value.customBackgroundFile,
                subscribes = it.value.dailyStorePushLocates.mapValues {
                    mutableListOf(SubscribeType.DAILY_STORE)
                }.toMutableMap()
            )
            userCache
        }?.toMutableMap()

    }

}