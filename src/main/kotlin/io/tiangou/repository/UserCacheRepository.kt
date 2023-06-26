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
import java.util.concurrent.ConcurrentHashMap


@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    @Transient val logicSelector: LogicSelector = LogicSelector(),
) {

    @Transient val logicTransferData: LogicTransferData = LogicTransferData()

    fun autoClean() =
        logicSelector.takeIf { !it.isStatusNormal() }?.apply {
            logicSelector.clean()
            logicTransferData.clean()
    }

    data class LogicTransferData(
        var account: String? = null,
        var password: String? = null,
        var needLoginVerify: Boolean? = null
    ) {

        fun clean() {
            account = null
            password = null
            needLoginVerify = null
        }

    }

}

object UserCacheRepository :
    AutoFlushStorage<MutableMap<Long, UserCache>>(JsonStorage("user-cache", StoragePathEnum.DATA_PATH, serializer())) {

    override var data: MutableMap<Long, UserCache> = runBlocking { load() ?: store(ConcurrentHashMap()) }

    init {
        registry()
    }

    operator fun get(qq: Long): UserCache = data[qq] ?: UserCache().apply { data[qq] = this }

    fun getAllUserCache() = data.toMap()

}
