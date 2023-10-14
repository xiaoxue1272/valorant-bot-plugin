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
    var subscribes: MutableMap<Long, MutableList<SubscribeType>> = mutableMapOf(),
) {

    @Transient
    val lock = Mutex()

    @Transient
    val logicController: LogicController = LogicController()

    @Transient
    private val imageCaches: MutableMap<GenerateImageType, ByteArrayCache> = mutableMapOf()

    fun cacheImage(type: GenerateImageType, byteArray: ByteArray) = imageCaches.put(type, byteArray.cache())

    suspend fun cacheImage(type: GenerateImageType, block: suspend (GenerateImageType) -> ByteArray) =
        imageCaches.put(type, block(type).cache())

    fun getOrCacheImage(type: GenerateImageType, byteArray: ByteArray) =
        imageCaches.getOrPut(type) { byteArray.cache() }

    suspend fun getOrCacheImage(type: GenerateImageType, block: suspend (GenerateImageType) -> ByteArray) =
        imageCaches.getOrPut(type) { block(type).cache() }

    fun clearCacheImages() = imageCaches.clear()

    fun removeCacheImage(type: GenerateImageType) = imageCaches.remove(type)

    @Serializable
    enum class SubscribeType(
        val value: String
    ) {
        DAILY_STORE("每日商店"),
        WEEKLY_ACCESSORY_STORE("每周配件商店"),
        DAILY_MATCH_SUMMARY_DATA("每日比赛数据统计");


        companion object {

            fun findByValue(value: String): SubscribeType? = SubscribeType.values().firstOrNull { it.value == value }

            fun findByValueNotNull(value: String): SubscribeType =
                findByValue(value) ?: throw ValorantPluginException("无效的订阅类型")

            fun findByName(name: String): SubscribeType? = SubscribeType.values().firstOrNull { it.name == name }

            fun findByNameNotNull(name: String): SubscribeType =
                findByName(name) ?: throw ValorantPluginException("无效的订阅类型")

            fun find(keywords: String): SubscribeType? =
                SubscribeType.values().firstOrNull { it.name == keywords || it.value == keywords }

            fun findNotNull(keywords: String): SubscribeType =
                find(keywords) ?: throw ValorantPluginException("无效的订阅类型")

            fun all() = values().toMutableList()

        }


    }


    suspend inline fun <reified T> synchronous(crossinline block: suspend UserCache.() -> T): T {
        return try {
            lock.lock(this)
            block(this@UserCache)
        } finally {
            lock.takeIf { it.holdsLock(this) }?.unlock(this)
        }
    }

    inline fun <reified T> synchronized(crossinline block: suspend UserCache.() -> T): T {
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
            subscribes[qq] = UserCache.SubscribeType.all()
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
    var customBackgroundFile: File? = null,
    var dailyStorePushLocates: MutableMap<Long, ContactEnum> = mutableMapOf(),
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
            UserCache(
                it.value.riotClientData,
                it.value.isRiotAccountLogin,
                0,
                it.value.customBackgroundFile,
                it.value.dailyStorePushLocates.mapValues { mutableListOf(UserCache.SubscribeType.DAILY_STORE) }
                    .toMutableMap()
            )
        }?.toMutableMap()

    }

}