package com.example.todo.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val dao: TodoDao) {
    fun observeAll(): Flow<List<TodoEntity>> = dao.observeAll()

    suspend fun add(
        title: String,
        note: String,
        dueAt: Long?,
        priority: Priority,
        tag: EisenhowerTag
    ) {
        dao.insert(
            TodoEntity(
                title = title,
                note = note,
                dueAt = dueAt,
                priority = priority,
                tag = tag
            )
        )
    }
    
    suspend fun addEntity(todo: TodoEntity) {
        dao.insert(todo)
    }

    suspend fun update(todo: TodoEntity) = dao.update(todo)
    suspend fun delete(todo: TodoEntity) = dao.delete(todo)
    suspend fun getById(id: Long): TodoEntity? = dao.getById(id)
    
    // Ivy Lee queries
    suspend fun getIvyTasksForDate(date: String): List<TodoEntity> {
        return dao.getIvyTasksForDate(date)
    }
    
    // Calendar queries
    suspend fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): List<TodoEntity> {
        return dao.getTasksForDateRange(startOfDay, endOfDay)
    }
    
    suspend fun getAllDatesWithTasks(): List<String> {
        return dao.getAllDatesWithTasks()
    }
}
