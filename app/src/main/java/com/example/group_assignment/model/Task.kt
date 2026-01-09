package com.example.group_assignment.model

import java.util.UUID

data class Task(
    val taskId: String = UUID.randomUUID().toString(),
    var taskTitle: String,
    var taskDueDate: String,
    var isTaskCompleted: Boolean = false
)

