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
    private val jsonParser = Gson()
    private val dataFileName = "tasks.json"
    private val taskFlow = MutableStateFlow<List<Task>>(emptyList())

    init {
        initializeData()
    }

    private fun initializeData() {
        val storageFile = File(context.filesDir, dataFileName)
        if (!storageFile.exists()) {
            taskFlow.value = emptyList()
            return
        }
            try {
                val jsonContent = storageFile.readText()
                val listType = object : TypeToken<List<Task>>() {}.type

                val loadedList : List<Task>? = jsonParser.fromJson(jsonContent, listType)
                taskFlow.value = loadedList ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                taskFlow.value = emptyList()
            }

    }

    private suspend fun persistData(tasks: List<Task>) {
        withContext(Dispatchers.IO) {
            try{
                val jsonString = jsonParser.toJson(tasks)
                File(context.filesDir, dataFileName).writeText(jsonString)
            }
            catch (e: Exception){
                e.printStackTrace()
            }




        }
    }

    override fun getTasks(): Flow<List<Task>> = taskFlow.asStateFlow()

    override suspend fun addTask(task: Task) {
        val updatedList = taskFlow.value + task
       updatedLocalState(updatedList)
    }

    override suspend fun updateTask(task: Task) {
        val updatedList = taskFlow.value.map { currentTask ->
            if (currentTask.taskId == task.taskId) task else currentTask
        }
        updatedLocalState(updatedList)
    }



    override suspend fun deleteTask(task: Task) {
        val updatedList = taskFlow.value.filter {
            it.taskId != task.taskId
        }
        updatedLocalState(updatedList)
    }
    private suspend fun updatedLocalState(newList: List<Task>){
        taskFlow.value = newList
        persistData(newList)
    }
}