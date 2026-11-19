package com.fatflow.app.data.local

import androidx.room.TypeConverter
import com.fatflow.app.domain.DayType
import com.fatflow.app.domain.SessionKind
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromDayType(value: DayType?): String? = value?.name

    @TypeConverter
    fun toDayType(value: String?): DayType? = value?.let { DayType.valueOf(it) }

    @TypeConverter
    fun fromSessionKind(value: SessionKind?): String? = value?.name

    @TypeConverter
    fun toSessionKind(value: String?): SessionKind? = value?.let { SessionKind.valueOf(it) }
}