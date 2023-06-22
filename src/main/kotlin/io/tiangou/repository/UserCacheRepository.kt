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
import net.mamoe.mirai.console.util.cast
import java.util.concurrent.ConcurrentHashMap


@Serializable
data class UserCache(
    val riotClientData: RiotClientData = RiotClientData(),
    var isRiotAccountLogin: Boolean = false,
    var subscribeDailyStore: Boolean = false,
    @Transient val logicSelector: LogicSelector = LogicSelector(),
    @Transient val cacheAnythingMap: MutableMap<Any, Any> = mutableMapOf(),
) {


    inline fun <reified T : Any> removeCache(key: Any): T? = cacheAnythingMap.remove(key)?.cast<T>()

    fun setCache(key: Any, value: Any) = cacheAnythingMap.put(key, value)

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
