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
import java.io.File

@Serializable
data class UserData(
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

    suspend inline fun <reified T> synchronous(crossinline block: suspend UserData.() -> T): T {
        return try {
            lock.lock(this)
            block(this@UserData)
        } finally {
            lock.takeIf { it.holdsLock(this) }?.unlock(this)
        }
    }

    inline fun <reified T> synchronized(crossinline block: suspend UserData.() -> T): T {
        return runBlocking { synchronous(block) }
    }

}

object UserDataRepository : AutoFlushStorage<MutableMap<Long, UserData>>(
    JsonStorage("user-cache", StoragePathEnum.DATA_PATH, serializer())
) {

    override var data: MutableMap<Long, UserData> = runBlocking { load() ?: store(mutableMapOf()) }

    init {
        registry()
    }

    operator fun get(qq: Long): UserData = data[qq] ?: UserData().apply {
        data[qq] = this
        dailyStorePushLocates.takeIf { it.isEmpty() }?.let { dailyStorePushLocates[qq] = UserData.ContactEnum.USER }
    }

    fun getAllUserCache(): Map<Long, UserData> = data

}
