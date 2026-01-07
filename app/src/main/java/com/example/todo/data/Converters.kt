package com.example.todo.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromPriority(p: Priority): String = p.name
    @TypeConverter fun toPriority(s: String): Priority = Priority.valueOf(s)
    
    @TypeConverter fun fromTag(v: EisenhowerTag): String = v.name
    @TypeConverter fun toTag(v: String): EisenhowerTag = EisenhowerTag.valueOf(v)
    
    @TypeConverter fun fromRepeatType(r: RepeatType?): String? = r?.name
    @TypeConverter fun toRepeatType(s: String?): RepeatType? = s?.let { RepeatType.valueOf(it) }
}
