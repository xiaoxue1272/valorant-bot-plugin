package io.tiangou.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.tiangou.ValorantPluginException
import io.tiangou.other.http.client
import io.tiangou.other.http.isRedirect

internal interface ApiInvoker<in T : Any, out R : Any> {

    fun prepareRequest(parameters: T?): HttpRequestBuilder

    suspend fun prepareResponse(response: HttpResponse): R

    suspend fun execute(
        requestBody: T? = null,
    ): R = with(this) {
        val request = prepareRequest(requestBody)
        prepareResponse(tryRequest(request, 2))
    }
}

internal suspend fun tryRequest(
    request: HttpRequestBuilder,
    times: Int,
    failedHandle: (suspend HttpResponse.() -> Unit)? = null
): HttpResponse {
    var response = client.request(request)
    if (response.status.isSuccess() || response.status.isRedirect()) {
        return response
    } else {
        repeat(times) {
            if (failedHandle != null) {
                failedHandle(response)
            }
            response = client.request(request)
            if (response.status.isSuccess() || response.status.isRedirect()) {
                return response
            }
        }
    }
    throw ValorantPluginException("API请求失败,且重试次数已达上限,请稍候再试")
}

class ApiException(
    message: String
) : ValorantPluginException(message) {

    constructor(errorEnum: ApiErrorEnum) : this(errorEnum.errorMessage)

}

enum class ApiErrorEnum(
    val errorMessage: String
) {

    API_REQUEST_FAILED_GET_ENTITLEMENTS_TOKEN("entitlements_token获取失败,请重新登录"),

}