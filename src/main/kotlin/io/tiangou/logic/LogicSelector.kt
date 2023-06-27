package io.tiangou.logic

import io.tiangou.repository.LogicRepository
import kotlinx.coroutines.Job
import net.mamoe.mirai.event.events.MessageEvent


class LogicSelector {

    private var logicList: List<LogicProcessor<MessageEvent>>? = null

    private var logicTimestamp: Long? = null

    var processJob: Job? = null
        set(value) {
            field = value
            field?.invokeOnCompletion {
                if (it != null) {
                    clean()
                }
                processJob = null
            }
        }

    var isRunningError: Boolean = false

    private var index: Int = 0

    fun loadLogic(message: String): LogicProcessor<MessageEvent>? {
        logicList = logicList ?: LogicRepository.find(message).apply { index = 0 }
        return logicList?.run {
            logicTimestamp = System.currentTimeMillis()
            logicList!![index++]
        }
    }

    fun isLast(): Boolean = logicList?.run { size == index } ?: false

    fun clean() {
        isRunningError = false
        processJob = null
        logicList = null
        logicTimestamp = null
    }

    fun isStatusNormal(): Boolean {
        if (isLast()) {
            return false
        }
        if (isRunningError) {
            return false
        }
        // TODO : 逻辑块等待超时时取消执行(默认5分钟)
        return logicTimestamp != null && 5 * 60 * 1000 > (System.currentTimeMillis() - logicTimestamp!!)
    }

}