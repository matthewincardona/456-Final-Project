package com.example.finalproject

data class TaskItem(
    val title: String,
    val description: String,
    var completed: Boolean = false,
    var timerRunning: Boolean = false,  // Flag to track if the timer is running
    var startTime: Long = 0L,           // Start time to track elapsed time
    var elapsedTime: Long = 0L          // Elapsed time
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





