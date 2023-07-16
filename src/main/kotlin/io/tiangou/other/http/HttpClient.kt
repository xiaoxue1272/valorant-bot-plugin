package io.tiangou.other.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.tiangou.Global
import okhttp3.ConnectionPool
import java.time.Duration
import java.util.concurrent.TimeUnit


internal val client: HttpClient = HttpClient(OkHttp) {
    followRedirects = false
    engine {
        config {
            connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
            readTimeout(Duration.ofSeconds(15))
            writeTimeout(Duration.ofSeconds(15))
            pingInterval(Duration.ofMinutes(5))
            retryOnConnectionFailure(true)
        }
    }
    Charsets {
        register(Charsets.UTF_8)
    }
//    install(Logging) {
//        level = LogLevel.ALL
//    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 3)
        exponentialDelay()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30 * 1000L
        connectTimeoutMillis = 10 * 1000L
        socketTimeoutMillis = 60 * 1000L
    }
    install(ContentEncoding) {
        deflate(1.0F)
        gzip(0.9F)
    }
    install(HttpCookies) {
        storage = UserCookiesStorage
    }
    install(UserAgent) {
        agent = "Apache-HttpClient/4.5.14 (Java/17.0.6)"
    }
    install(ContentNegotiation) {
        json(Global.json)
    }
}

internal fun HttpStatusCode.isRedirect(): Boolean = when (value) {
    HttpStatusCode.MovedPermanently.value,
    HttpStatusCode.Found.value,
    HttpStatusCode.TemporaryRedirect.value,
    HttpStatusCode.PermanentRedirect.value,
    HttpStatusCode.SeeOther.value -> true

    else -> false
}