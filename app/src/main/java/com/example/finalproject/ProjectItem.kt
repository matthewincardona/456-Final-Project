package com.example.finalproject

data class TaskItem(
    val title: String,
    val description: String,
    val completed: Boolean = false
)

data class ProjectItem(
    val title: String,
    val description: String,
    val tasks: List<TaskItem> = mutableListOf()
)
