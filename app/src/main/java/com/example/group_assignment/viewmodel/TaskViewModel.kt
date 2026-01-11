package com.example.group_assignment.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.group_assignment.model.Task
import com.example.group_assignment.repository.FileTaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Enum for sorting types
enum class SortType { DATE, TITLE }

class TaskViewModel(private val repository: FileTaskRepository) : ViewModel() {

    // 1. Data State (The list of tasks)
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // 2. UI Event State (For error messages or success notifications)
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private var currentSort = SortType.DATE
    private var lastRawList: List<Task> = emptyList()

    init {
        observeTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            repository.getTasks().collect { taskList ->
                lastRawList = taskList
                sortAndEmit(taskList)
            }
        }
    }

    // --- VIEW MODEL LOGIC STARTS HERE ---

    // Logic: Validate input here, NOT in the Activity
    fun addTask(title: String, date: String) {
        if (title.isBlank() || date.isBlank()) {
            _uiMessage.value = "Error: Title and Date cannot be empty!" // Trigger error
            return
        }

        // Logic: Create ID and default status
        val newTask = Task(System.currentTimeMillis().toString(), title, date, false)

        viewModelScope.launch {
            repository.addTask(newTask)
            _uiMessage.value = "Task Added Successfully" // Trigger success message
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _uiMessage.value = "Task Deleted"
        }
    }

    // Logic: Handle sorting state
    fun updateSortOrder(sortType: SortType) {
        currentSort = sortType
        sortAndEmit(lastRawList)
    }

    // Helper to perform the actual sorting
    private fun sortAndEmit(list: List<Task>) {
        val sortedList = when (currentSort) {
            SortType.DATE -> list.sortedBy { it.dueDate }
            SortType.TITLE -> list.sortedBy { it.title }
        }
        _tasks.value = sortedList
    }

    // Call this after showing a Toast to prevent it from showing again on rotation
    fun clearMessage() {
        _uiMessage.value = null
    }
}

// ---------------------------------------------------------
// INI KILANG (FACTORY) YANG HILANG TU!
// Letak kat luar class TaskViewModel (paling bawah file)
// ---------------------------------------------------------
class TaskViewModelFactory(private val repository: FileTaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}