package io.tiangou.api

import io.tiangou.api.data.StoreFrontResponse
import io.tiangou.api.data.StorefrontRequest
import io.tiangou.other.http.actions
import io.tiangou.repository.UserData
import java.util.concurrent.ConcurrentHashMap

object StoreApiHelper {

    private suspend fun invokeRiotApi(userData: UserData) =
        userData.riotClientData.actions {
            flushAccessToken(RiotApi.CookieReAuth.execute())
            flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
            RiotApi.Storefront.execute(StorefrontRequest(shard!!, puuid!!))
        }.apply { storeFronts[userData.riotClientData.puuid!!] = this }

    internal suspend fun queryStoreFront(userData: UserData): StoreFrontResponse =
        storeFronts[userData.riotClientData.puuid!!]
            ?: invokeRiotApi(userData)

    fun clean(userData: UserData) {
        userData.riotClientData.puuid?.let {
            storeFronts.remove(it)
        }
    }

    private val storeFronts: ConcurrentHashMap<String, StoreFrontResponse> by lazy { ConcurrentHashMap() }


}