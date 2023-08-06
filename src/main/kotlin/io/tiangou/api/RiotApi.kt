@file:JvmName("RiotApiKt")
@file:UseSerializers(AtomicReferenceSerializer::class)

package io.tiangou.api

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.tiangou.AtomicReferenceSerializer
import io.tiangou.api.data.*
import io.tiangou.other.http.ClientData
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import net.mamoe.mirai.console.util.safeCast
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

sealed class RiotApi<in T : Any, out R : Any> : ApiInvoker<T, R> {


    override suspend fun execute(requestBody: T?): R {
        val riotClientData = coroutineContext[ClientData]?.safeCast<RiotClientData>()
        val request = prepareRequest(requestBody)
        riotClientData?.onRequest(request)
        val response = tryRequest(request, 1) {
            riotClientData?.onResponse(this)
            riotClientData?.onRequest(request)
        }
        return prepareResponse(response)
    }


    /**
     * 登录前必须的接口
     * 获取并保存安全认证Cookies
     * 返回是否成功
     */
    object AuthCookies : RiotApi<AuthCookiesRequest, AuthResponse>() {

        override fun prepareRequest(parameters: AuthCookiesRequest?): HttpRequestBuilder = request {
            url.takeFrom("https://auth.riotgames.com/api/v1/authorization")
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
            setBody(parameters)
        }

        override suspend fun prepareResponse(response: HttpResponse): AuthResponse = response.body()

    }

    /**
     * 登录接口
     * 如果 type = RESPONSE
     * 则登录成功
     * 如果 type = MULTI_FACTOR
     * 则需要调用二部验证接口
     * 登录成功后安全 token自动保存
     */
    object Auth : RiotApi<AuthRequest, AuthResponse>() {

        override fun prepareRequest(parameters: AuthRequest?): HttpRequestBuilder = request {
            url.takeFrom("https://auth.riotgames.com/api/v1/authorization")
            method = HttpMethod.Put
            setBody(parameters)
            contentType(ContentType.Application.Json)
        }

        override suspend fun prepareResponse(response: HttpResponse): AuthResponse = response.body()

    }

    /**
     * 二部验证接口
     * 返回结果同登录接口
     * 返回是否成功
     */
    object MultiFactorAuth : RiotApi<MultiFactorAuthRequest, AuthResponse>() {

        override fun prepareRequest(parameters: MultiFactorAuthRequest?): HttpRequestBuilder = request {
            url.takeFrom("https://auth.riotgames.com/api/v1/authorization")
            method = HttpMethod.Put
            setBody(parameters)
            contentType(ContentType.Application.Json)
        }

        override suspend fun prepareResponse(response: HttpResponse): AuthResponse = response.body()

    }

    /**
     * 通过现有Cookies重新获取安全Token
     * 安全 token自动保存
     * 返回是否成功
     */
    object CookieReAuth : RiotApi<Unit, Url>() {

        override fun prepareRequest(parameters: Unit?): HttpRequestBuilder = request {
            url.takeFrom("https://auth.riotgames.com/authorize?redirect_uri=https%3A%2F%2Fplayvalorant.com%2Fopt_in&client_id=play-valorant-web-prod&response_type=token%20id_token&nonce=1")
            method = HttpMethod.Get
        }

        override suspend fun prepareResponse(response: HttpResponse) =
            Url(response.headers["location"]!!)

    }

    object EntitlementsAuth : RiotApi<Unit, EntitlementAuthResponse>() {

        const val ENTITLEMENTS_AUTH_URL: String = "https://entitlements.auth.riotgames.com/api/token/v1"

        override fun prepareRequest(parameters: Unit?): HttpRequestBuilder = request {
            url.takeFrom(ENTITLEMENTS_AUTH_URL)
            method = HttpMethod.Post
            contentType(ContentType.Application.Json)
        }

        override suspend fun prepareResponse(response: HttpResponse): EntitlementAuthResponse = response.body()

    }

    object PlayerInfo : RiotApi<Unit, PlayerInfoResponse>() {

        override fun prepareRequest(parameters: Unit?): HttpRequestBuilder = request {
            url.takeFrom("https://auth.riotgames.com/userinfo")
            method = HttpMethod.Get
        }

        override suspend fun prepareResponse(response: HttpResponse): PlayerInfoResponse = response.body()

    }

    object Storefront : RiotApi<StorefrontRequest, StoreFrontResponse>() {

        override fun prepareRequest(parameters: StorefrontRequest?): HttpRequestBuilder = request {
            url.takeFrom("https://pd.${parameters!!.shard}.a.pvp.net/store/v2/storefront/${parameters.puuid}")
            method = HttpMethod.Get
        }

        override suspend fun prepareResponse(response: HttpResponse): StoreFrontResponse = response.body()
    }

}

@Serializable
class RiotClientData(
    private val authToken: AtomicReference<String?> = AtomicReference(),
    private val xRiotEntitlementsJwt: AtomicReference<String?> = AtomicReference(),
    var puuid: String? = null,
    var region: String? = null,
    var shard: String? = null,
) : ClientData() {

    fun flushAccessToken(url: Url) {
        parseQueryString(url.fragment).let { parameters ->
            parameters["access_token"]?.takeIf { it.isNotEmpty() }?.apply { authToken.set(this) }
        }
    }

    fun flushAccessToken(url: String) {
        parseQueryString(Url(url).fragment).let { parameters ->
            parameters["access_token"]?.takeIf { it.isNotEmpty() }?.apply { authToken.set(this) }
        }
    }

    fun flushXRiotEntitlementsJwt(xRiotEntitlementsJwt: String) {
        xRiotEntitlementsJwt.takeIf { it.isNotEmpty() }?.apply {
            this@RiotClientData.xRiotEntitlementsJwt.set(this)
        }
    }

    fun onRequest(request: HttpRequestBuilder) {
        if (request.host.endsWith("auth.riotgames.com")) {
            request.headers[HttpHeaders.Authorization] = "Bearer $authToken"
        }
        if (request.host.endsWith(".a.pvp.net")) {
            request.headers[HttpHeaders.Authorization] = "Bearer $authToken"
            request.headers["X-Riot-Entitlements-JWT"] = "$xRiotEntitlementsJwt"
        }
    }

    suspend fun onResponse(response: HttpResponse) {
        if (response.status.value in 400..499) {
            if (response.request.url.toString() == RiotApi.EntitlementsAuth.ENTITLEMENTS_AUTH_URL) {
                throw ApiException(ApiErrorEnum.API_REQUEST_FAILED_GET_ENTITLEMENTS_TOKEN)
            }
            flushAccessToken(RiotApi.CookieReAuth.execute())
            flushXRiotEntitlementsJwt(RiotApi.EntitlementsAuth.execute().entitlementsToken)
        }
    }

}