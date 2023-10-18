package io.tiangou.cron

import io.tiangou.*
import io.tiangou.repository.UserCache
import io.tiangou.repository.UserCacheRepository
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.MessageChainBuilder

abstract class AbstractSubscribeCronTask : CronTask() {

    protected data class UserSubscribeLocation(val user: User, val contacts: List<Contact>)

    abstract val messageText: String

    abstract val subscribeType: SubscribeType

    abstract suspend fun getUserSubscribeImage(userQQ: Long, userCache: UserCache) : ByteArray

    override suspend fun execute() {
        val currentDateTime = now(timeZone.toZoneId()).format()
        UserCacheRepository.getAllUserCache()
            .filterValues {
                it.isRiotAccountLogin
            }.forEach { (userQQ, userCache) ->
                val userSubscribePushDataList = userCache.mapSubscribeByBot(userQQ, getOnlineBots())
                userSubscribePushDataList
                    .takeIf { it.isNotEmpty() }
                    ?.forEach { userSubscribePushData ->
                        runCatching {
                            userSubscribePushData.contacts.forEach { contact ->
                                val imageBytes = getUserSubscribeImage(userQQ, userCache)
                                val image = uploadImageOrFail(imageBytes, contact)
                                val message = MessageChainBuilder()
                                    .append("$currentDateTime\n $messageText:\n")
                                    .append(image)
                                    .build()
                                contact.reply(message, userQQ)
                            }
                        }.onFailure { throwable ->
                            log.warning("QQ:[${userQQ}],推送每日商店时异常,异常信息:", throwable)
                            userSubscribePushData.user.sendMessage("推送每日商店时出错,错误信息:[$throwable]")
                        }
                    }
            }
    }


    private fun UserCache.mapSubscribeByBot(userQQ: Long, onlineBots: List<Bot>): List<UserSubscribeLocation> {
        val contactQQSet = subscribes
            .filterValues { it.contains(subscribeType) }
            .keys
            .toMutableSet()
        val result = mutableListOf<UserSubscribeLocation>()
        onlineBots.forEach { bot ->
            val user = bot.getUser(userQQ)
            if (user != null) {
                val contactList = mutableListOf<Contact>()
                for (contactQQ in contactQQSet.toList()) {
                    val contact = bot.getContact(contactQQ)
                    if (contact != null) {
                        contactList.add(contact)
                        contactQQSet.remove(contactQQ)
                    }
                }
                if (contactList.isNotEmpty()) result.add(UserSubscribeLocation(user, contactList))
            }
        }
        return result
    }

}