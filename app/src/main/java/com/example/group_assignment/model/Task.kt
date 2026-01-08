package com.example.group_assignment.model

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var dueDate: String,
    var isCompleted: Boolean = false
)