package io.tiangou.delay

import io.tiangou.repository.UserCacheRepository
import kotlin.time.Duration


/**
 * 指定在当前任务
 * [创建成功 -> 等待执行 -> 执行结束]
 * 在创建成功 到执行结束这段期间内, 有且只能有一个待运行或正在运行的任务. (不可并行,且不可创建)
 * 可能有点抽象
 * 举个例子:
 * 假如第一天 有1个人查询了每日商店, 且每日商店是第二天早上北京时间8点准时刷新,那么我们创建了一个延时任务,在第二天清理上一天生成的图片缓存
 * OK,没有问题,相信这段很好理解
 * 那么第二天 有两个或者三个人查询了每日商店, 首先整体思路不变, 我们还是要清理上一天生成的图片缓存, ok, 那么我们还是创建一个延时任务来清理.
 * 这时候问题来了, 我不管几个人, 1个, 2个, 3个还是多少个, 我们的商店都是每天早上8点准时刷新, 如果我按人头来算, 每个查询过商店的人, 我都给他创建一个任务
 * 是否有点多此一举了呢? 对吧, 所以我们就限定一个任务来清理所有人的缓存就OK.
 * 而且在这个任务创建之后, 我们也没必要在去创建新的同一类型的任务. 直到这个任务执行完了, 用户重新查询商店, 我们这时候才会去重新创建任务.
 * OK,这个字段就是干这个用的
 * @author xiaoxue1272
 * @see DelayTask
 * @since v0.8.0
 */
abstract class UserCacheImageCleanDelayTask(delay: Duration): DelayTask(delay) {

    override val description: String = "用户生成缓存图片清理任务"

    override suspend fun execute() {
        UserCacheRepository.getAllUserCache()
            .forEach {
                it.value.synchronous {
                    generateImages.clear()
                }
            }
    }
}