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
    var subscribeDailyStore: Boolean = false,
    var subscribeList: List<SubscribeType> = listOf(),
    var customBackgroundFile: File? = null,
    var subscribePushLocates: MutableMap<Long, SubscribeData> = mutableMapOf(),
) {

    @Transient
    val lock = Mutex()

    @Transient
    val logicController: LogicController = LogicController()

    @Transient
    val generateImages: MutableMap<GenerateImageType, ByteArrayCache> = mutableMapOf()

    @Serializable
    enum class ContactEnum {
        USER,
        GROUP
    }

    @Serializable
    enum class SubscribeType(
        val value : String
    ) {
        DAILY_STORE("每日商店"),
        WEEKLY_ACCESSORY_STORE("每周配件商店"),
        DAILY_MATCH_SUMMARY_DATA("每日比赛数据统计");


        companion object {

            fun findByValue(value: String): SubscribeType = SubscribeType.values().firstOrNull { it.value == value } ?: throw ValorantPluginException("无效的订阅类型")


        }


    }


    @Serializable
    data class SubscribeData(
        val contactEnum: ContactEnum,
        val subscribeTypeList: MutableList<SubscribeType> = mutableListOf()
    ) {

        fun subscribeAll(): SubscribeData = apply { subscribeTypeList.addAll(SubscribeType.values()) }

        fun subscribe(type: SubscribeType): SubscribeData = apply { subscribeTypeList.add(type) }

        fun subscribe(value: String): SubscribeData = apply { subscribeTypeList.add(SubscribeType.findByValue(value)) }

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
        null
    }

    init {
        registry()
    }

    operator fun get(qq: Long): UserCache = data.getOrDefault(qq, UserCache().apply {
        subscribePushLocates.takeIf { it.isEmpty() }?.put(qq, UserCache.SubscribeData(UserCache.ContactEnum.USER).subscribeAll())
    })

    fun getAllUserCache(): Map<Long, UserCache> = data

}


//@Serializable
//data class OldUserCache(
//    val riotClientData: RiotClientData = RiotClientData(),
//    var isRiotAccountLogin: Boolean = false,
//    var subscribeDailyStore: Boolean = false,
//    var customBackgroundFile: File? = null,
//    var dailyStorePushLocations: MutableMap<Long, Boolean> = mutableMapOf()
//)
//
//class OldUserCacheDataStructureAdapter :
//    JsonStorage<MutableMap<Long, OldUserCache>>("user-cache", StoragePathEnum.DATA_PATH, serializer()) {
//
//
//    suspend fun adapt(): MutableMap<Long, UserCache>? {
//        fun convertDailyStorePushLocates(map: MutableMap<Long, Boolean>): MutableMap<Long, UserCache.ContactEnum> =
//            map.mapValues {
//                var isGroup = false
//                for (bot in getOnlineBots()) {
//                    if (bot.getGroup(it.key) != null) {
//                        isGroup = true
//                        break
//                    }
//                }
//                return@mapValues if (isGroup) {
//                    UserCache.ContactEnum.GROUP
//                } else {
//                    UserCache.ContactEnum.USER
//                }
//            }.toMutableMap()
//
//        return load()?.mapValues {
//            UserCache(
//                it.value.riotClientData,
//                it.value.isRiotAccountLogin,
//                it.value.subscribeDailyStore,
//                customBackgroundFile = it.value.customBackgroundFile,
//                subscribePushLocates = convertDailyStorePushLocates(it.value.dailyStorePushLocations)
//            )
//        }?.toMutableMap()
//
//    }
//
//}