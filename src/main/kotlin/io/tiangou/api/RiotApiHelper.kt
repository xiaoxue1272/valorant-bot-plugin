package io.tiangou.api

import io.tiangou.api.data.StorefrontRequest
import io.tiangou.other.http.actions
import io.tiangou.repository.UserCache

object RiotApiHelper {

    internal suspend fun queryStoreFrontApi(userCache: UserCache) =
        userCache.riotClientData.actions {
            flushAccessToken(RiotApi.CookieReAuth.execute())
            flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
            RiotApi.Storefront.execute(StorefrontRequest(shard!!, puuid!!))
        }

}