package io.tiangou.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.tiangou.api.data.ContentTiersResponse
import io.tiangou.api.data.ThemeResponse
import io.tiangou.api.data.ThirdPartyValorantApiResponse
import io.tiangou.api.data.WeaponSkinResponse

sealed class ThirdPartyValorantApi<T : Any, R> : ApiInvoker<T, ThirdPartyValorantApiResponse<R>> {

    object AllWeaponSkin : ThirdPartyValorantApi<Unit, List<WeaponSkinResponse>>() {

        override fun prepareRequest(parameters: Unit?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/weapons/skins?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<WeaponSkinResponse>> =
            response.body()
    }

    object AllContentTier : ThirdPartyValorantApi<String, List<ContentTiersResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/contenttiers?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<ContentTiersResponse>> =
            response.body()
    }

    object AllTheme : ThirdPartyValorantApi<String, List<ThemeResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/themes?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<ThemeResponse>> =
            response.body()
    }

}
