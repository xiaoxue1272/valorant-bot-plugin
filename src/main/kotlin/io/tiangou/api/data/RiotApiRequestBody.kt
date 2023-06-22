@file:JvmName("RiotApiRequestBodyKt")

package io.tiangou.api.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    var username: String? = null,
    var password: String? = null,
    val type: String = "auth",
    val remember: Boolean = true,
    val language: String = "zh_CN"
)

@Serializable
data class AuthCookiesRequest(
    @SerialName("client_id") val clientId: String = "play-valorant-web-prod",
    val nonce: String = "1",
    @SerialName("redirect_uri") val redirectUri: String = "https://playvalorant.com/opt_in",
    @SerialName("response_type") val responseType: String = "token id_token",
    val scope: String = "account openid"
)

@Serializable
data class MultiFactorAuthRequest(
    val code: String,
    val type: String = "multifactor",
    val rememberDevice: Boolean = true,
)

data class StorefrontRequest(
    val shard: String,
    val puuid: String
)
