package io.tiangou.other.http

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.tiangou.Global
import io.tiangou.serializer.AtomicLongSerializer
import io.tiangou.serializer.CookieSerializer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import okhttp3.ConnectionPool
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.math.min


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

internal object UserCookiesStorage : CookiesStorage {

    private val mutex = Mutex()

    private val defaultClientData = ClientData()

    override suspend fun get(requestUrl: Url): List<Cookie> {
        return (coroutineContext[ClientData] ?: defaultClientData).run {
            val oldestCookie: AtomicLong = oldestCookieTimestamp
            cookies.let { cookies ->
                mutex.withLock(cookies) {
                    val date = GMTDate()

                    if (date.timestamp >= oldestCookie.get()) cleanup(this, date.timestamp)
                    cookies.filter { it.matches(requestUrl) }
                }
            }
        }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        (coroutineContext[ClientData] ?: defaultClientData).apply {
            mutex.withLock(this) {
                with(cookie) {
                    if (name.isBlank()) return@withLock
                }
                val cookies: MutableList<Cookie> = cookies
                val oldestCookie: AtomicLong = oldestCookieTimestamp
                cookies.removeAll { it.name == cookie.name && it.matches(requestUrl) }
                cookies.add(cookie.fillDefaults(requestUrl))
                cookie.expires?.timestamp?.let { expires ->
                    if (expires > 0L && oldestCookie.get() > expires) {
                        oldestCookie.set(expires)
                    }
                }
            }
        }

    }

    override fun close() {
    }

    private fun cleanup(clientData: ClientData, timestamp: Long) {
        clientData.apply {
            val cookies: MutableList<Cookie> = cookies
            cookies.removeAll { cookie ->
                val expires = cookie.expires?.timestamp ?: return@removeAll false
                expires < timestamp
            }

            oldestCookieTimestamp.set(
                cookies.fold(Long.MAX_VALUE) { acc, cookie ->
                    cookie.expires?.timestamp?.let { min(acc, it) } ?: acc
                }
            )
        }

    }

    private fun Cookie.matches(requestUrl: Url): Boolean {
        val domain = domain?.toLowerCasePreservingASCIIRules()?.trimStart('.')
            ?: error("Domain field should have the default value")

        val path = with(path) {
            val current = path ?: error("Path field should have the default value")
            if (current.endsWith('/')) current else "$path/"
        }

        val host = requestUrl.host.toLowerCasePreservingASCIIRules()
        val requestPath = let {
            val pathInRequest = requestUrl.encodedPath
            if (pathInRequest.endsWith('/')) pathInRequest else "$pathInRequest/"
        }

        if (host != domain && (hostIsIp(host) || !host.endsWith(".$domain"))) {
            return false
        }

        if (path != "/" &&
            requestPath != path &&
            !requestPath.startsWith(path)
        ) return false

        return !(secure && !requestUrl.protocol.isSecure())
    }

    private fun Cookie.fillDefaults(requestUrl: Url): Cookie {
        var result = this

        if (result.path?.startsWith("/") != true) {
            result = result.copy(path = requestUrl.encodedPath)
        }

        if (result.domain.isNullOrBlank()) {
            result = result.copy(domain = requestUrl.host)
        }

        return result
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


@Serializable
open class ClientData(
    val cookies: MutableList<@Serializable(with = CookieSerializer::class) Cookie> = mutableListOf(),
    val oldestCookieTimestamp: @Serializable(with = AtomicLongSerializer::class) AtomicLong = AtomicLong()
) : CoroutineContext.Element {

    @Transient
    override val key: CoroutineContext.Key<*> = ClientData

    companion object Key : CoroutineContext.Key<ClientData>

}

suspend inline fun <reified T : ClientData, reified R> T.actions(crossinline block: suspend T.() -> R) = withContext(this) { block() }