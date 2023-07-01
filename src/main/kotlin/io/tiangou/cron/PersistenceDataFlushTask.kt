package io.tiangou.cron

import io.tiangou.repository.PersistenceDataInitiator
import kotlinx.serialization.Serializable

@Serializable
class PersistenceDataFlushTask(
    override var cron: String,
    override var isEnable: Boolean
) : Task() {

    override suspend fun execute() {
        log.info("Valorant皮肤库数据刷新任务,开始")
        runCatching {
            PersistenceDataInitiator.init()
        }.onFailure {
            log.warning("Valorant皮肤库数据刷新任务,执行中发生异常,异常信息:", it)
        }
        log.info("Valorant皮肤库数据刷新任务,结束")
    }

}