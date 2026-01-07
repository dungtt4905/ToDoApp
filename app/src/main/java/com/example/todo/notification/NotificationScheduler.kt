package com.example.todo.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todo.data.TodoEntity

object NotificationScheduler {

    fun schedule(context: Context, todo: TodoEntity) {
        val dueAt = todo.dueAt ?: return
        if (todo.isDone) {
            cancel(context, todo)
            return
        }
        val now = System.currentTimeMillis()
        if (dueAt < now) return

        // Schedule for 1 day before, 12 hours before, 1 hour before
        // But only if that time is in the future
        val triggers = listOf(
            24 * 60 * 60 * 1000L, // 1 day
            12 * 60 * 60 * 1000L, // 12 hours
            1 * 60 * 60 * 1000L   // 1 hour
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        triggers.forEachIndexed { index, offset ->
            val triggerTime = dueAt - offset
            if (triggerTime > now) {
                val intent = Intent(context, TodoNotificationReceiver::class.java).apply {
                    putExtra("TASK_ID", todo.id)
                    putExtra("TASK_TITLE", "Reminder: ${todo.title}")
                    val timeDesc = when(index) {
                        0 -> "1 day"
                        1 -> "12 hours"
                        2 -> "1 hour"
                        else -> "soon"
                    }
                    putExtra("TASK_MESSAGE", "Task is due in $timeDesc")
                }
                
                // Use a unique ID for each trigger of the same task: (taskId * 10 + index)
                val pendingIntentId = (todo.id * 10 + index).toInt()
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    pendingIntentId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                             alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        } else {
                            // Fallback or request permission logic could be here
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun cancel(context: Context, todo: TodoEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Cancel all potential 3 triggers
        for (i in 0..2) {
            val pendingIntentId = (todo.id * 10 + i).toInt()
            val intent = Intent(context, TodoNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                pendingIntentId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
