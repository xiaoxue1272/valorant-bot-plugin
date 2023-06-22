package io.tiangou.serializer

import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

object CookieSerializer : KSerializer<Cookie> {

    override fun deserialize(decoder: Decoder): Cookie {
        return parseServerSetCookieHeader(decoder.decodeString())
    }

    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Cookie) {
        encoder.encodeString(renderSetCookieHeader(value))
    }
}

@OptIn(ExperimentalSerializationApi::class)
class AtomicReferenceSerializer<T>(
    private val element: KSerializer<T>
) : KSerializer<AtomicReference<T>> {


    override fun deserialize(decoder: Decoder): AtomicReference<T> {
        return AtomicReference(decoder.decodeNullableSerializableValue(element))
    }

    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: AtomicReference<T>) {
        encoder.encodeNullableSerializableValue(element, value.get())
    }
}

class AtomicLongSerializer : KSerializer<AtomicLong> {

    override fun deserialize(decoder: Decoder): AtomicLong {
        return AtomicLong(decoder.decodeLong())
    }

    override val descriptor: SerialDescriptor = Long.serializer().descriptor

    override fun serialize(encoder: Encoder, value: AtomicLong) {
        encoder.encodeLong(value.get())
    }
}