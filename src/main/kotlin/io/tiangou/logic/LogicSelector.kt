package io.tiangou.logic

import io.tiangou.repository.LogicRepository
import kotlinx.coroutines.Job
import net.mamoe.mirai.event.events.MessageEvent
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext


class LogicSelector : AbstractCoroutineContextElement(LogicSelector) {

    private var logicList: List<LogicProcessor<MessageEvent>>? = null

    private var logicTimestamp: Long? = null

    var isRunningError: Boolean = false

    var processJob: Job? = null

    private var index: Int = 0

    fun loadLogic(message: String): LogicProcessor<MessageEvent> {
        logicList = logicList ?: LogicRepository.find(message).apply { index = 0 }
        return logicList!![index++]
    }

    fun clear() {
        isRunningError = false
        processJob = null
        logicList = null
        logicTimestamp = null
    }

    fun isStatusNormal(): Boolean = logicList?.run {
        if (isRunningError) {
            return@run false
        }
        if (size >= index + 1) {
            return@run true
        }
        return@run logicTimestamp != null && (System.currentTimeMillis() - logicTimestamp!!) > 5 * 60 * 1000
    } ?: false

    companion object Key : CoroutineContext.Key<LogicSelector>

}