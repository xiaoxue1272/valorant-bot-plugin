@file:UseSerializers(FileSerializer::class)

package io.tiangou.repository

import io.tiangou.AutoFlushStorage
import io.tiangou.JsonStorage
import io.tiangou.StoragePathEnum
import io.tiangou.api.RiotClientData
import io.tiangou.logic.LogicSelector
import io.tiangou.reply
import io.tiangou.serializer.FileSerializer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import net.mamoe.mirai.event.events.MessageEvent
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    var customBackgroundFile: File? = null,
    var dailyStorePushLocations: MutableMap<Long, Boolean> = mutableMapOf(),
    @Transient val logicSelector: LogicSelector = LogicSelector(),
) {

    @Transient
    private var isExiting: Boolean = false

    @Transient
    val logicTransferData: LogicTransferData = LogicTransferData()

    inline fun <reified T> synchronous(crossinline block: suspend UserCache.() -> T): T =
        synchronized(this) { runBlocking { block(this@UserCache) } }

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

    suspend fun MessageEvent.exitLogic() {
        if (isExiting) {
            reply("请勿重复操作")
            return
        }
        if (logicSelector.processJob?.isActive == true) {
            logicSelector.processJob!!.invokeOnCompletion {
                runBlocking {
                    clean()
                    reply("已退出指令")
                    isExiting = false
                }
            }
            reply("当前指令正在执行中,请等待执行结束后自动退出")
            isExiting = true
        } else {
            clean()
            reply("已退出指令")
        }
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
