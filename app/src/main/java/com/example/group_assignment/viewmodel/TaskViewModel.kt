// State management logic
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

class TaskViewModel(private val repository: ITaskRepository) : ViewModel() {

    // Kita guna MutableStateFlow untuk pegang cara sorting (Default: Date)
    private val _sortOrder = MutableStateFlow(SortType.DATE)

    // UI akan observe _tasks yang dah siap disusun
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    init {
        viewModelScope.launch {
            // Gabungkan data dari Repository + SortOrder
            // Bila salah satu berubah, dia akan run logic sort ni balik
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

    // Fungsi untuk UI panggil bila user pilih menu
    fun updateSortOrder(sortType: SortType) {
        _sortOrder.value = sortType
    }

    // Fungsi CRUD biasa
    fun addTask(title: String, date: String) {
        viewModelScope.launch { repository.addTask(Task(title = title, dueDate = date)) }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }
}

// Enum untuk jenis sorting (Senang nak manage)
enum class SortType {
    TITLE, DATE
}

class TaskViewModelFactory(private val repository: ITaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}