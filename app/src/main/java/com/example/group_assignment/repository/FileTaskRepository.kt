// JSON handling for storage
package com.example.group_assignment.repository

import android.content.Context
import com.example.group_assignment.model.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class FileTaskRepository(private val context: Context) : ITaskRepository {
    private val gson = Gson()
    private val fileName = "tasks.json"
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    init {
        loadTasks()
    }

    private fun loadTasks() {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            try {
                val jsonString = file.readText()
                val type = object : TypeToken<List<Task>>() {}.type
                _tasks.value = gson.fromJson(jsonString, type) ?: emptyList()
            } catch (e: Exception) {
                _tasks.value = emptyList()
            }
        }
    }

    private suspend fun saveTasks() {
        withContext(Dispatchers.IO) {
            val jsonString = gson.toJson(_tasks.value)
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
        }
    }

    override fun getTasks(): Flow<List<Task>> = _tasks.asStateFlow()

    override suspend fun addTask(task: Task) {
        val current = _tasks.value.toMutableList()
        current.add(task)
        _tasks.value = current
        saveTasks()
    }

    override suspend fun updateTask(task: Task) {
        val current = _tasks.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index != -1) {
            current[index] = task
            _tasks.value = current
            saveTasks()
        }
    }

    override suspend fun deleteTask(task: Task) {
        val current = _tasks.value.toMutableList()
        current.remove(task)
        _tasks.value = current
        saveTasks()
    }
}