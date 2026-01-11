package com.example.group_assignment.repository

import com.example.group_assignment.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * Enhanced Task Repository Interface
 * Role B: Main Task List UI, Task Data Class, Internal Storage Repository
 *
 * Improvements:
 * - Added search functionality
 * - Added filter by category
 * - Added statistics methods
 * - Better documentation
 */
interface ITaskRepository {

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * Returns a Flow of all tasks for reactive updates
     */
    fun getTasks(): Flow<List<Task>>

    /**
     * Adds a new task to the repository
     */
    suspend fun addTask(task: Task)

    /**
     * Updates an existing task
     */
    suspend fun updateTask(task: Task)

    /**
     * Deletes a task from the repository
     */
    suspend fun deleteTask(task: Task)

    // ========== SEARCH & FILTER OPERATIONS ==========

    /**
     * Search tasks by title (case-insensitive)
     * @param query Search query string
     * @return List of tasks matching the query
     */
    fun searchTasks(query: String): List<Task>

    /**
     * Get tasks by category
     * @param category The category to filter by
     * @return List of tasks in the specified category
     */
    fun getTasksByCategory(category: String): List<Task>

    /**
     * Get all unique categories
     * @return List of all category names
     */
    fun getAllCategories(): List<String>

    // ========== STATISTICS METHODS ==========

    /**
     * Get total number of tasks
     * @return Total task count
     */
    fun getTotalTaskCount(): Int

    /**
     * Get number of completed tasks
     * @return Completed task count
     */
    fun getCompletedTaskCount(): Int

    /**
     * Get number of pending (incomplete) tasks
     * @return Pending task count
     */
    fun getPendingTaskCount(): Int

    /**
     * Get completion percentage
     * @return Percentage of completed tasks (0-100)
     */
    fun getCompletionPercentage(): Int
}