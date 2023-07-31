package io.tiangou.utils

import io.tiangou.api.RiotApi
import io.tiangou.api.data.StoreFrontResponse
import io.tiangou.api.data.StorefrontRequest
import io.tiangou.other.http.actions
import io.tiangou.repository.UserCache
import java.util.concurrent.ConcurrentHashMap

object StoreApiHelper {

    private suspend fun invokeRiotApi(userCache: UserCache) =
        userCache.riotClientData.actions {
            flushAccessToken(RiotApi.CookieReAuth.execute())
            flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
            RiotApi.Storefront.execute(StorefrontRequest(shard!!, puuid!!))
        }.apply { storeFronts[userCache.riotClientData.puuid!!] = this }

    internal suspend fun queryStoreFront(userCache: UserCache): StoreFrontResponse =
        storeFronts[userCache.riotClientData.puuid!!]
            ?: invokeRiotApi(userCache)

    fun clean(userCache: UserCache) {
        userCache.riotClientData.puuid?.let {
            userCache.synchronous {
                storeFronts.remove(it)
            }
        }
    }

    internal val storeFronts: ConcurrentHashMap<String, StoreFrontResponse> by lazy { ConcurrentHashMap() }


}