package io.tiangou

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.mamoe.mirai.utils.MiraiLogger
import kotlin.coroutines.CoroutineContext

sealed interface Task {

    val description: String

    suspend fun execute()

}

abstract class AbstractTask: Task, CoroutineScope {

    protected val log: MiraiLogger = MiraiLogger.Factory.create(Task::class)

    var job: Job? = null

    override val coroutineContext: CoroutineContext =
        Global.coroutineScope.coroutineContext + CoroutineName("GlobalTaskContext :${this::class::simpleName}")


    suspend fun run() =
        runCatching {
            log.info("$description, 开始")
            execute()
            log.info("$description, 结束")
        }.onFailure {
            log.warning("$description, 执行时发生异常:", it)
        }

    abstract fun enable()

    abstract fun disable()

    protected fun start(block: suspend () -> Unit) {
        job = launch {
            block()
        }
    }

    protected fun stop() {
        runCatching { job?.cancel() }
    }

}