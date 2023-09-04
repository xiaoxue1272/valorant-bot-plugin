package io.tiangou.cron

import io.tiangou.repository.persistnce.PersistenceDataInitiator
import kotlinx.serialization.Serializable

@Serializable
class PersistenceDataFlushCronTask(
    override var cron: String,
    override var isEnable: Boolean
) : CronTask() {

    override val description: String = "Valorant数据更新"

    override suspend fun execute() = PersistenceDataInitiator.init()

}