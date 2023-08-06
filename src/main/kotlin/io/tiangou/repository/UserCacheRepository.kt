@file:UseSerializers(FileSerializer::class)

package io.tiangou.repository

import io.tiangou.AutoFlushStorage
import io.tiangou.FileSerializer
import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.api.RiotClientData
import io.tiangou.logic.LogicSelector
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
    var customBackgroundFile: File? = null,
//    @Deprecated("由于设计问题, 自v0.7.0弃用") var dailyStorePushLocations: MutableMap<Long, Boolean> = mutableMapOf(),
    var dailyStorePushLocates: MutableMap<Long, ContactEnum> = mutableMapOf(),
) {

    @Transient
    val lock = Mutex()

    @Transient
    val logicSelector: LogicSelector = LogicSelector()

    @Serializable
    enum class ContactEnum {
        USER,
        GROUP
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

    init {
        registry()
    }

    operator fun get(qq: Long): UserCache = data[qq] ?: UserCache().apply {
        data[qq] = this
        dailyStorePushLocates.takeIf { it.isEmpty() }?.let { dailyStorePushLocates[qq] = UserCache.ContactEnum.USER }
    }

    fun getAllUserCache(): Map<Long, UserCache> = data

}
