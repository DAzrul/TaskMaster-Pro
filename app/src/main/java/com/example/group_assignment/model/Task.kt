package com.example.group_assignment.model

import java.util.UUID

/**
 * Enhanced Task Data Model with Category Support
 * Role B: Main Task List UI, Task Data Class, Internal Storage Repository
 *
 * Improvements:
 * - Added category field for task organization
 * - Added creation timestamp for better tracking
 * - Enhanced documentation
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var dueDate: String,
    var isCompleted: Boolean = false,
    var category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)