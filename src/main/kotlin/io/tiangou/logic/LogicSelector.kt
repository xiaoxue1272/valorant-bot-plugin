package io.tiangou.logic

import io.ktor.util.date.*
import io.tiangou.EventHandleConfig
import io.tiangou.Global
import io.tiangou.reply
import io.tiangou.repository.LogicRepository
import io.tiangou.repository.UserCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.event.events.MessageEvent
import kotlin.coroutines.CoroutineContext

class LogicSelector : CoroutineScope {

    override val coroutineContext: CoroutineContext = Global.coroutineScope.coroutineContext

    private var logicList: List<LogicProcessor<MessageEvent>>? = null

    private var timeoutStamp: Long? = null

    var processJob: Job? = null

    private var timeoutCancelJob: Job? = null

    var isRunningError: Boolean = false

    private var index: Int = 0

    fun loadLogic(message: String): LogicProcessor<MessageEvent>? {
        logicList = logicList ?: LogicRepository.find(message)
        timeoutStamp = (getTimeMillis() + EventHandleConfig.config.waitTimeoutMinutes * 60 * 1000L)
        return logicList?.get(index++)
    }

    fun createTimeoutCancelJob(event: MessageEvent, userCache: UserCache) {
        if (timeoutCancelJob == null && isStatusNormal()) {
            timeoutCancelJob = launch {
                while (true) {
                    delay(timeoutStamp!! - getTimeMillis())
                    if (getTimeMillis() >= timeoutStamp!!) {
                        userCache.clean()
                        event.reply("等待输入超时,已自动退出,请重新发起")
                        break
                    }
                }
            }
        }
    }

    fun clean() {
        index = 0
        isRunningError = false
        processJob = null
        logicList = null
        timeoutStamp = null
        if (timeoutCancelJob != null && timeoutCancelJob!!.isActive) timeoutCancelJob!!.cancel()
        timeoutCancelJob = null
    }

    fun isStatusNormal(): Boolean {
        if (logicList?.size == index) {
            return false
        }
        return !isRunningError
    }


}