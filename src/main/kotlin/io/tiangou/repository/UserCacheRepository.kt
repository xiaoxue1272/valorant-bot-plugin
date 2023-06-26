package io.tiangou.repository

import io.tiangou.AutoFlushStorage
import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.api.RiotClientData
import io.tiangou.logic.LogicSelector
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.serializer
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap


@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    @Transient val logicSelector: LogicSelector = LogicSelector(),
) {

    @Transient val logicTransferData: LogicTransferData = LogicTransferData()

    data class LogicTransferData(
        var account: WeakReference<String>? = null,
        var password: WeakReference<String>? = null,
        var needLoginVerify: WeakReference<Boolean>? = null
    ) {

        fun clear() {
            account?.clear()
            password?.clear()
            needLoginVerify?.clear()
        }

    }

}

inline fun <reified T> WeakReference<T>.getAndClear() = get().apply { clear() }

object UserCacheRepository :
    AutoFlushStorage<MutableMap<Long, UserCache>>(JsonStorage("user-cache", StoragePathEnum.DATA_PATH, serializer())) {

    override var data: MutableMap<Long, UserCache> = runBlocking { load() ?: store(ConcurrentHashMap()) }

    init {
        registry()
    }

    operator fun get(qq: Long): UserCache = data[qq] ?: UserCache().apply { data[qq] = this }

    fun getAllUserCache() = data.toMap()

}
