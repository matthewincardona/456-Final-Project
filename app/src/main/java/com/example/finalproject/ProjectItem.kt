package com.example.finalproject

data class TaskItem(
    val title: String,
    val description: String,
    val completed: Boolean = false
)

data class ProjectItem(
    val title: String,
    val description: String,
    val tasks: List<TaskItem> = mutableListOf(),
    val initialProgress: Float = 1.0f // Start fully filled
) {
    val progress: Float = initialProgress // Immutable progress field

    // Method to return a new ProjectItem with updated progress
    fun withProgress(newProgress: Float): ProjectItem {
        return this.copy(initialProgress = newProgress.coerceIn(0f, 1f)) // Ensure the progress stays between 0 and 1
    }
}





