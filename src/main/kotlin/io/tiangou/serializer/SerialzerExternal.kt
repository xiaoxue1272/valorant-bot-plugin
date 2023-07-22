package io.tiangou.serializer

import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.nio.file.Path
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path
import kotlin.io.path.pathString

class CookieSerializer : KSerializer<Cookie> {

    override fun deserialize(decoder: Decoder): Cookie {
        return parseServerSetCookieHeader(decoder.decodeString())
    }

    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Cookie) {
        encoder.encodeString(renderSetCookieHeader(value))
    }
}

class AtomicReferenceSerializer<T>(
    private val element: KSerializer<T>
) : KSerializer<AtomicReference<T>> {


    override fun deserialize(decoder: Decoder): AtomicReference<T> {
        return AtomicReference(element.deserialize(decoder))
    }

    override val descriptor: SerialDescriptor = element.descriptor

    override fun serialize(encoder: Encoder, value: AtomicReference<T>) {
        element.serialize(encoder, value.get())
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

class FileSerializer : KSerializer<File> {

    override fun deserialize(decoder: Decoder): File {
        return Path(decoder.decodeString()).toFile()
    }

    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.path)
    }
}

class PathSerializer : KSerializer<Path> {

    override fun deserialize(decoder: Decoder): Path {
        return Path(decoder.decodeString())
    }

    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.pathString)
    }
}

class TimeZoneSerializer : KSerializer<TimeZone> {

    override fun deserialize(decoder: Decoder): TimeZone {
        return TimeZone.getTimeZone(ZoneId.of(decoder.decodeString()))
    }

    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun serialize(encoder: Encoder, value: TimeZone) {
        encoder.encodeString(value.toZoneId().id)
    }
}