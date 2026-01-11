package com.example.group_assignment.repository

import android.content.Context
import android.util.Log
import com.example.group_assignment.model.Task
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Enhanced File-based Task Repository Implementation
 * Role B: Main Task List UI, Task Data Class, Internal Storage Repository
 *
 * Improvements:
 * - Added comprehensive error handling with logging
 * - Implemented search functionality by title
 * - Better file validation and backup mechanism
 * - Added method to get tasks by category
 * - Task statistics methods (total, completed, pending)
 */
class FileTaskRepository(private val context: Context) : ITaskRepository {

    companion object {
        private const val TAG = "FileTaskRepository"
        private const val FILE_NAME = "tasks.json"
        private const val BACKUP_FILE_NAME = "tasks_backup.json"
    }

    private val gson = Gson()
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())

    init {
        loadTasks()
    }

    /**
     * Loads tasks from internal storage with error handling
     * Creates backup before loading
     */
    private fun loadTasks() {
        val file = File(context.filesDir, FILE_NAME)

        if (file.exists()) {
            try {
                val jsonString = file.readText()
                val type = object : TypeToken<List<Task>>() {}.type
                val loadedTasks: List<Task> = gson.fromJson(jsonString, type) ?: emptyList()
                _tasks.value = loadedTasks
                Log.d(TAG, "Successfully loaded ${loadedTasks.size} tasks")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tasks: ${e.message}", e)
                _tasks.value = emptyList()
                // Try to restore from backup if available
                tryRestoreFromBackup()
            }
        } else {
            Log.d(TAG, "No existing tasks file found. Starting fresh.")
            _tasks.value = emptyList()
        }
    }

    /**
     * Attempts to restore tasks from backup file
     */
    private fun tryRestoreFromBackup() {
        val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
        if (backupFile.exists()) {
            try {
                val jsonString = backupFile.readText()
                val type = object : TypeToken<List<Task>>() {}.type
                _tasks.value = gson.fromJson(jsonString, type) ?: emptyList()
                Log.d(TAG, "Restored tasks from backup")
            } catch (e: Exception) {
                Log.e(TAG, "Backup restore failed: ${e.message}", e)
            }
        }
    }

    /**
     * Saves tasks to internal storage with backup
     * Creates backup of existing file before overwriting
     */
    private suspend fun saveTasks() {
        withContext(Dispatchers.IO) {
            try {
                // Create backup of existing file
                val mainFile = File(context.filesDir, FILE_NAME)
                if (mainFile.exists()) {
                    val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
                    mainFile.copyTo(backupFile, overwrite = true)
                }

                // Save new data
                val jsonString = gson.toJson(_tasks.value)
                context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
                    it.write(jsonString.toByteArray())
                }
                Log.d(TAG, "Successfully saved ${_tasks.value.size} tasks")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving tasks: ${e.message}", e)
                throw e // Re-throw to notify caller
            }
        }
    }

    // ========== BASIC CRUD OPERATIONS ==========

    override fun getTasks(): Flow<List<Task>> = _tasks.asStateFlow()

    override suspend fun addTask(task: Task) {
        try {
            val current = _tasks.value.toMutableList()
            current.add(task)
            _tasks.value = current
            saveTasks()
            Log.d(TAG, "Added task: ${task.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add task: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateTask(task: Task) {
        try {
            val current = _tasks.value.toMutableList()
            val index = current.indexOfFirst { it.id == task.id }
            if (index != -1) {
                current[index] = task
                _tasks.value = current
                saveTasks()
                Log.d(TAG, "Updated task: ${task.title}")
            } else {
                Log.w(TAG, "Task not found for update: ${task.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update task: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteTask(task: Task) {
        try {
            val current = _tasks.value.toMutableList()
            val removed = current.remove(task)
            if (removed) {
                _tasks.value = current
                saveTasks()
                Log.d(TAG, "Deleted task: ${task.title}")
            } else {
                Log.w(TAG, "Task not found for deletion: ${task.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete task: ${e.message}", e)
            throw e
        }
    }

    // ========== ENHANCED FEATURES ==========

    /**
     * Search tasks by title (case-insensitive)
     * Returns tasks that contain the search query in their title
     */
    override fun searchTasks(query: String): List<Task> {
        if (query.isBlank()) return _tasks.value

        val searchQuery = query.trim().lowercase()
        return _tasks.value.filter { task ->
            task.title.lowercase().contains(searchQuery)
        }.also {
            Log.d(TAG, "Search '$query' returned ${it.size} results")
        }
    }

    /**
     * Get tasks by category
     */
    override fun getTasksByCategory(category: String): List<Task> {
        return _tasks.value.filter { it.category.equals(category, ignoreCase = true) }
    }

    /**
     * Get all unique categories from existing tasks
     */
    override fun getAllCategories(): List<String> {
        return _tasks.value
            .map { it.category }
            .distinct()
            .sorted()
    }

    // ========== STATISTICS METHODS ==========

    /**
     * Get total number of tasks
     */
    override fun getTotalTaskCount(): Int {
        return _tasks.value.size
    }

    /**
     * Get number of completed tasks
     */
    override fun getCompletedTaskCount(): Int {
        return _tasks.value.count { it.isCompleted }
    }

    /**
     * Get number of pending (incomplete) tasks
     */
    override fun getPendingTaskCount(): Int {
        return _tasks.value.count { !it.isCompleted }
    }

    /**
     * Get completion percentage
     */
    override fun getCompletionPercentage(): Int {
        val total = getTotalTaskCount()
        if (total == 0) return 0
        return ((getCompletedTaskCount().toDouble() / total) * 100).toInt()
    }
}