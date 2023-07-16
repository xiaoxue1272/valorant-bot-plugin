package io.tiangou.other.http

import io.ktor.http.*
import io.tiangou.serializer.AtomicLongSerializer
import io.tiangou.serializer.CookieSerializer
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

@Serializable
open class ClientData(
    val cookies: MutableList<@Serializable(CookieSerializer::class) Cookie> = mutableListOf(),
    val oldestCookieTimestamp: @Serializable(AtomicLongSerializer::class) AtomicLong = AtomicLong()
) : CoroutineContext.Element {

    @Transient
    override val key: CoroutineContext.Key<*> = ClientData

    companion object Key : CoroutineContext.Key<ClientData>

}

suspend inline fun <reified T : ClientData, reified R> T.actions(crossinline block: suspend T.() -> R) =
    withContext(this) { block() }