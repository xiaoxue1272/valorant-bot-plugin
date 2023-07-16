package io.tiangou.other.http

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.math.min

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