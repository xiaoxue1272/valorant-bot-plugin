package io.tiangou

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

fun now(zoneId: ZoneId? = null): LocalDateTime =
    LocalDateTime.now(zoneId?.let { Clock.system(it) } ?: Clock.systemDefaultZone())

fun TemporalAccessor.format(formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE): String =
    formatter.format(this)
