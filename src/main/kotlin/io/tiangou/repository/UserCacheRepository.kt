@file:UseSerializers(FileSerializer::class)

package io.tiangou.repository

import io.tiangou.AutoFlushStorage
import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.api.RiotClientData
import io.tiangou.logic.LogicSelector
import io.tiangou.serializer.FileSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    var customBackgroundFile: File? = null,
    @Deprecated("由于设计问题, 自v0.7.0弃用") var dailyStorePushLocations: MutableMap<Long, Boolean> = mutableMapOf(),
    var dailyStorePushLocates: MutableMap<Long, ContactEnum> = mutableMapOf(),
    @Transient val logicSelector: LogicSelector = LogicSelector(),
) {

    enum class ContactEnum {
        USER,
        GROUP
    }

    inline fun <reified T> synchronous(crossinline block: suspend UserCache.() -> T): T =
        synchronized(this) { runBlocking { block(this@UserCache) } }

}

object UserCacheRepository : AutoFlushStorage<MutableMap<Long, UserCache>>(
    JsonStorage("user-cache", StoragePathEnum.DATA_PATH, serializer())
) {

    override var data: MutableMap<Long, UserCache> = runBlocking { load() ?: store(ConcurrentHashMap()) }

    init {
        registry()
    }

    operator fun get(qq: Long): UserCache = (data[qq] ?: UserCache().apply { data[qq] = this }).apply {
        if (dailyStorePushLocations.isEmpty()) {
            dailyStorePushLocations[qq] = true
        }
    }

    fun getAllUserCache(): Map<Long, UserCache> = data

}
