package com.example.group_assignment.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.group_assignment.model.Task
import com.example.group_assignment.repository.ITaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Enhanced Task ViewModel with Category Support
 * Role B: Main Task List UI, Task Data Class, Internal Storage Repository
 *
 * Improvements:
 * - Enhanced addTask method to support category
 * - Maintains sorting functionality
 * - Better state management
 */
class TaskViewModel(private val repository: ITaskRepository) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortType.DATE)
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    init {
        viewModelScope.launch {
            combine(repository.getTasks(), _sortOrder) { tasks, sortOrder ->
                when (sortOrder) {
                    SortType.TITLE -> tasks.sortedBy { it.title.lowercase() }
                    SortType.DATE -> tasks.sortedBy { it.dueDate.lowercase() }
                }
            }.collect { sortedList ->
                _tasks.value = sortedList
            }
        }
    }

    /**
     * Update sorting order
     */
    fun updateSortOrder(sortType: SortType) {
        _sortOrder.value = sortType
    }

    /**
     * Add a new task (legacy method for backward compatibility)
     */
    fun addTask(title: String, date: String) {
        viewModelScope.launch {
            repository.addTask(Task(title = title, dueDate = date))
        }
    }

    /**
     * Add a new task with category (enhanced method)
     */
    fun addTask(
        title: String,
        date: String,
        category: String = "General"
    ) {
        viewModelScope.launch {
            repository.addTask(
                Task(
                    title = title,
                    dueDate = date,
                    category = category
                )
            )
        }
    }

    /**
     * Update an existing task
     */
    fun updateTask(task: Task) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    /**
     * Delete a task
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }
}

/**
 * Enum for sorting types
 */
enum class SortType {
    TITLE, DATE
}

/**
 * Factory for creating TaskViewModel with dependency injection
 */
class TaskViewModelFactory(private val repository: ITaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}