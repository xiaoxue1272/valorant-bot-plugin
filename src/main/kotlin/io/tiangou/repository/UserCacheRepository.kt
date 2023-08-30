@file:UseSerializers(FileSerializer::class)

package io.tiangou.repository

import io.tiangou.AutoFlushStorage
import io.tiangou.FileSerializer
import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.api.RiotClientData
import io.tiangou.logic.LogicController
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import net.mamoe.mirai.Bot
import java.io.File

@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    var subscribeList: List<SubscribeType> = listOf(),
    var customBackgroundFile: File? = null,
//    @Deprecated("由于设计问题, 自v0.7.0弃用") var dailyStorePushLocations: MutableMap<Long, Boolean> = mutableMapOf(),
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

    @Serializable
    enum class SubscribeType {
        DAILY_STORE,
        DAILY_MATCH_SUMMARY_DATA
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

    operator fun get(qq: Long): UserCache = data[qq] ?: UserCache().apply {
        data[qq] = this
        dailyStorePushLocates.takeIf { it.isEmpty() }?.let { dailyStorePushLocates[qq] = UserCache.ContactEnum.USER }
    }

    fun getAllUserCache(): Map<Long, UserCache> = data

}


@Serializable
data class OldUserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    var customBackgroundFile: File? = null,
    var dailyStorePushLocations: MutableMap<Long, Boolean> = mutableMapOf()
)

class OldUserCacheDataStructureAdapter: JsonStorage<MutableMap<Long, OldUserCache>>("user-cache", StoragePathEnum.DATA_PATH, serializer()) {


    suspend fun adapt(): MutableMap<Long, UserCache>? {
        fun convertDailyStorePushLocates(map: MutableMap<Long, Boolean>): MutableMap<Long, UserCache.ContactEnum> =
            map.mapValues {
                var isGroup = false
                for (bot in Bot.instances) {
                    if (bot.getGroup(it.key) != null) {
                        isGroup = true
                        break
                    }
                }
                return@mapValues if (isGroup) {
                    UserCache.ContactEnum.GROUP
                } else {
                    UserCache.ContactEnum.USER
                }
            }.toMutableMap()

        return load()?.mapValues {
            UserCache(
                it.value.riotClientData,
                it.value.isRiotAccountLogin,
                it.value.subscribeDailyStore,
                customBackgroundFile = it.value.customBackgroundFile,
                dailyStorePushLocates = convertDailyStorePushLocates(it.value.dailyStorePushLocations)
            )
        }?.toMutableMap()

    }

}