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
import kotlinx.serialization.serializer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    var customBackgroundFile: @Serializable(FileSerializer::class) File? = null,
    @Transient val logicSelector: LogicSelector = LogicSelector(),
) {

    @Transient
    val logicTransferData: LogicTransferData = LogicTransferData()

    /**
     * 判断当前指令是否全部执行完成
     * 若全部执行完成,则清空指令相关信息
     * 包括责任链及责任链传递参数
     */
    fun cleanWhenCompleted() =
        logicSelector.takeIf { !it.isStatusNormal() }?.apply {
            logicSelector.clean()
            logicTransferData.clean()
        }

    fun clean() = apply {
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

    fun getAllUserCache(): Map<Long, UserCache> = data

}
