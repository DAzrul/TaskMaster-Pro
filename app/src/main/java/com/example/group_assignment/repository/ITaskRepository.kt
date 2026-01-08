package com.example.group_assignment.repository

import com.example.group_assignment.model.Task
import kotlinx.coroutines.flow.Flow

interface ITaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
}