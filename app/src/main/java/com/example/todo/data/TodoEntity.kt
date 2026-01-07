package com.example.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val note: String = "",
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,               // epoch millis
    val priority: Priority = Priority.MEDIUM,
    val tag: EisenhowerTag = EisenhowerTag.DO_NOW,
    
    // Repeat Config
    val isRepeat: Boolean = false,
    val repeatType: RepeatType? = null,
    val repeatInterval: Int = 1,

    // Ivy Lee Method
    val ivyDate: String? = null, // yyyy-MM-dd
    val ivyRank: Int? = null     // 1-6
)
