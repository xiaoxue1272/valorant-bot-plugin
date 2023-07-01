package io.tiangou.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.tiangou.api.data.*
import kotlinx.coroutines.runBlocking

sealed class ThirdPartyValorantApi<T : Any, R> : ApiInvoker<T, ThirdPartyValorantApiResponse<R>> {

    object AllWeaponSkins : ThirdPartyValorantApi<Unit, List<WeaponSkinResponse>>() {

        override fun prepareRequest(parameters: Unit?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/weapons/skins?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<WeaponSkinResponse>> =
            response.body()
    }

    object AllContentTiers : ThirdPartyValorantApi<String, List<ContentTiersResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/contenttiers?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<ContentTiersResponse>> =
            response.body()
    }

    object AllThemes : ThirdPartyValorantApi<String, List<ThemeResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/themes?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<ThemeResponse>> =
            response.body()
    }

    object AllContracts : ThirdPartyValorantApi<String, List<ContractResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/contracts?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<ContractResponse>> =
            response.body()
    }

    object AllBuddies : ThirdPartyValorantApi<String, List<BuddieResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/buddies?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<BuddieResponse>> =
            response.body()
    }

    object AllCurrencies : ThirdPartyValorantApi<String, List<CurrencyResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/currencies?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<CurrencyResponse>> =
            response.body()
    }

    object AllPlayerCards : ThirdPartyValorantApi<String, List<PlayCardResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/playercards?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<PlayCardResponse>> =
            response.body()
    }

    object AllPlayerTitles : ThirdPartyValorantApi<String, List<PlayTitleResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/playertitles?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<PlayTitleResponse>> =
            response.body()
    }

    object AllSprays : ThirdPartyValorantApi<String, List<SprayResponse>>() {

        override fun prepareRequest(parameters: String?): HttpRequestBuilder = request {
            url.takeFrom("https://valorant-api.com/v1/sprays?language=zh-TW")
        }

        override suspend fun prepareResponse(response: HttpResponse): ThirdPartyValorantApiResponse<List<SprayResponse>> =
            response.body()
    }

}

fun main() {
    runBlocking { println(ThirdPartyValorantApi.AllSprays.execute()) }
}

