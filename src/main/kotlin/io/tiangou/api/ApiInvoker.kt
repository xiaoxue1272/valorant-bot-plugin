package io.tiangou.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.tiangou.ValorantRuntimeException
import io.tiangou.other.http.client
import io.tiangou.other.http.isRedirect

internal interface ApiInvoker<in T : Any, out R : Any> {

    fun prepareRequest(parameters: T?): HttpRequestBuilder

    suspend fun prepareResponse(response: HttpResponse): R

    suspend fun execute(
        requestBody: T? = null,
    ): R = with(this) {
        val request = prepareRequest(requestBody)
        prepareResponse(tryRequest(request, 3))
    }
}

internal suspend fun tryRequest(
    request: HttpRequestBuilder,
    times: Int,
    failedHandle: (suspend HttpResponse.() -> Unit)? = null
): HttpResponse {
    repeat(times) {
        val httpResponse = client.request(request)
        if (httpResponse.status.isSuccess() || httpResponse.status.isRedirect()) {
            return httpResponse
        }
        if (failedHandle != null) {
            failedHandle(httpResponse)
        }
    }
    throw ApiException(ApiErrorEnum.API_REQUEST_FAILED_AND_RETRY_OVER)
}

class ApiException(
    message: String
) : ValorantRuntimeException(message) {

    constructor(errorEnum: ApiErrorEnum) : this(errorEnum.errorMessage)

}

enum class ApiErrorEnum(
    val errorMessage: String
) {

    API_REQUEST_FAILED_AND_RETRY_OVER("API请求失败,且重试次数已达三次,请稍候再试"),

    API_REQUEST_FAILED_GET_ENTITLEMENTS_TOKEN("entitlements_token获取失败"),

}