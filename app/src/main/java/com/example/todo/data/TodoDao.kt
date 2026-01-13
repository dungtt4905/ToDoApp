package com.example.todo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos")
    fun observeAll(): Flow<List<TodoEntity>>

    @Insert
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoEntity?
    
    @Query("SELECT * FROM todos WHERE ivyDate = :date ORDER BY ivyRank ASC")
    suspend fun getIvyTasksForDate(date: String): List<TodoEntity>
    
    @Query("SELECT * FROM todos WHERE dueAt >= :startOfDay AND dueAt < :endOfDay ORDER BY dueAt ASC")
    suspend fun getTasksForDateRange(startOfDay: Long, endOfDay: Long): List<TodoEntity>
    
    @Query("SELECT DISTINCT date(dueAt / 1000, 'unixepoch', 'localtime') as date FROM todos WHERE dueAt IS NOT NULL")
    suspend fun getAllDatesWithTasks(): List<String>
}
