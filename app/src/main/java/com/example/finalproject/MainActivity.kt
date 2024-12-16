package com.example.finalproject

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finalproject.ui.theme.FinalProjectTheme
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.delay

const val REQUEST_AUTHORIZATION = 1001

//  TODO:
//
//  Home - UI
//      Project card
//         Image
//         Title
//         Description
//         Two backgrounds / overlay
//      FAB to make new task project
//      New task popup
//          “Add New Project”
//          Name field
//          Description field
//      “Add Project” confirmation button
//
//  Home - State
//      Project card
//          Needs to be clickable and take you to the task page
//      State for progress ticking down (change background width)
//      FAB
//          Clicking opens Add New Project popup
//      Add New Project popup
//          Check for valid name and description when clicking “Add Project”
//          Add way to delete projects
//      Save to local storage
//      Load from local storage

class MainActivity : ComponentActivity() {

    // ActivityResultLauncher for handling the authorization result
    private lateinit var authorizationLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ActivityResultLauncher
        authorizationLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    Log.d("MainActivity", "Authorization successful!")
                    // Handle success, e.g., fetch data from Google Calendar API
                } else {
                    Log.e("MainActivity", "Authorization failed or canceled.")
                }
            }

        enableEdgeToEdge()
        setContent {
            FinalProjectTheme {
                val navController = rememberNavController()
                var projects by remember {
                    mutableStateOf(StorageHelper.loadProjects(this)) // Load saved projects on startup
                }

                // Observe changes and save to local storage
                LaunchedEffect(projects) {
                    StorageHelper.saveProjects(this@MainActivity, projects)
                }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            projects = projects,
                            onProjectClick = { project ->
                                navController.navigate("details/${project.title}/${project.description}")
                            },
                            onAddProject = { name, description ->
                                if (name.isNotBlank() && description.isNotBlank()) {
                                    projects = projects + ProjectItem(name, description)
                                }
                            },
                            onAuthorize = { requestGoogleCalendarAuthorization() }
                        )
                    }
                    composable("details/{title}/{description}") { backStackEntry ->
                        val title = backStackEntry.arguments?.getString("title") ?: ""
                        val description = backStackEntry.arguments?.getString("description") ?: ""
                        val project = projects.find { it.title == title && it.description == description }

                        if (project != null) {
                            ProjectDetailsScreen(
                                project = project,
                                onUpdateProject = { updatedProject ->
                                    projects = projects.map {
                                        if (it.title == updatedProject.title && it.description == updatedProject.description) updatedProject else it
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Function to request Google Calendar authorization
    private fun requestGoogleCalendarAuthorization() {
        val requestedScopes = listOf(Scope(CalendarScopes.CALENDAR_READONLY))

        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(requestedScopes)
            .build()

        Identity.getAuthorizationClient(this)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    val intentSenderRequest =
                        pendingIntent?.let { IntentSenderRequest.Builder(it).build() }
                    if (intentSenderRequest != null) {
                        authorizationLauncher.launch(intentSenderRequest)
                    }
                } else {
                    // Access already granted
                    Log.d("MainActivity", "Authorization already granted.")
                    saveToCalendar()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Authorization failed: ${e.localizedMessage}")
            }
    }

    // Handle Authorization Result
    private fun handleAuthorizationResult(data: Intent) {
        val authorizationResult = Identity.getAuthorizationClient(this)
            .getAuthorizationResultFromIntent(data)

        if (authorizationResult.hasResolution()) {
            // Authorization still needs user interaction
            Log.d("MainActivity", "Authorization requires user interaction.")
        } else {
            // Check for granted scopes
            val grantedScopes = authorizationResult.grantedScopes
            if (grantedScopes.isNotEmpty()) {
                Log.d(
                    "MainActivity",
                    "Authorization successful! Granted scopes: $grantedScopes"
                )
                saveToCalendar()
            } else {
                Log.e("MainActivity", "Authorization failed: No granted scopes.")
            }
        }
    }

    private fun saveToCalendar() {
        Log.d("MainActivity", "Ready to access Google Calendar API.")
        // Call your Google Calendar API logic here
    }

    @Composable
    fun HomeScreen(
        projects: List<ProjectItem>,
        onProjectClick: (ProjectItem) -> Unit,
        onAddProject: (String, String) -> Unit,
        onAuthorize: () -> Unit
    ) {
        var isDialogOpen by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                // Button to trigger Google Calendar Authorization
                Box(modifier = Modifier.padding(top = 48.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                    TextButton(
                        onClick = { onAuthorize() },  // Trigger authorization
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Connect Google Calendar")
                    }
                }
            },
            floatingActionButton = {
                AddNewProject(onClick = { isDialogOpen = true })
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                ProjectList(
                    projects = projects,
                    onProjectClick = onProjectClick
                )
                if (isDialogOpen) {
                    AddProjectDialog(
                        onDismiss = { isDialogOpen = false },
                        onAddProject = { name, description ->
                            onAddProject(name, description)
                            isDialogOpen = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun HorizontalCard(
        title: String,
        description: String,
        progress: Float,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF333333) // Set the background to #333333
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White // Make the text white for better contrast
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray // Use light gray for secondary text
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (progress <= 0.2f) Color.Red else Color.Green, // Red when progress is low
                )
            }
        }
    }


    @Composable
    fun ProjectList(
        projects: List<ProjectItem>,
        onProjectClick: (ProjectItem) -> Unit,
        modifier: Modifier = Modifier
    ) {
        LazyColumn(modifier = modifier) {
            items(projects) { project ->
                // Simulate the progress ticking down every second
                var progress by remember { mutableFloatStateOf(project.progress) }

                // Reduce progress over time
                LaunchedEffect(Unit) {
                    while (progress > 0f) {
                        delay(1000L) // Decrease progress every second
                        progress -= 0.01f // Decrease by a small amount
                    }
                }

                HorizontalCard(
                    title = project.title,
                    description = project.description,
                    progress = progress, // Pass progress value
                    modifier = Modifier.clickable { onProjectClick(project) }
                )
            }
        }
    }


    @Composable
    fun AddNewProject(onClick: () -> Unit) {
        ExtendedFloatingActionButton(
            onClick = { onClick() },
            icon = { Icon(Icons.Filled.Add, "New Project", tint = Color.White) },
            text = { Text(text = "New Project", color = Color.White) },
            containerColor = Color.Blue, // Accent color for FAB
        )
    }

    @Composable
    fun AddProjectDialog(
        onDismiss: () -> Unit,
        onAddProject: (String, String) -> Unit
    ) {
        var name by remember { mutableStateOf(TextFieldValue("")) }
        var description by remember { mutableStateOf(TextFieldValue("")) }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text(text = "Add New Project") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = "Name") },
                        trailingIcon = {
                            IconButton(onClick = { name = TextFieldValue("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear name")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(text = "Description") },
                        trailingIcon = {
                            IconButton(onClick = { description = TextFieldValue("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear description")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddProject(name.text, description.text)
                }) {
                    Text(text = "Add Project")
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(text = "Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    @SuppressLint("MutableCollectionMutableState")
    @Composable
    fun ProjectDetailsScreen(
        project: ProjectItem,
        onUpdateProject: (ProjectItem) -> Unit
    ) {
        val context = LocalContext.current
        var tasks by remember { mutableStateOf(StorageHelper.loadTasks(context, project.title)) }
        var progress by remember { mutableFloatStateOf(project.progress) }
        var isDialogOpen by remember { mutableStateOf(false) }

        // Track the timer state and elapsed time
        LaunchedEffect(tasks) {
            // Update elapsed time for tasks that are running the timer
            while (tasks.any { it.timerRunning }) {
                delay(1000L) // Update every second
                tasks = tasks.map { task ->
                    if (task.timerRunning) {
                        val elapsedTime = System.currentTimeMillis() - task.startTime + task.elapsedTime
                        task.copy(elapsedTime = elapsedTime)
                    } else {
                        task
                    }
                }.toMutableList()
                StorageHelper.saveTasks(context, project.title, tasks) // Save updated tasks
                onUpdateProject(project.copy(tasks = tasks)) // Update the project with the new task state
            }
        }

        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { isDialogOpen = true },
                    icon = { Icon(Icons.Filled.Add, "Add Task", tint = Color.White) },
                    text = { Text(text = "Add Task", color = Color.White) },
                    containerColor = Color.Blue, // Accent color for FAB
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Project Title and Description
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (progress <= 0.2f) Color.Red else Color.Green, // Red when progress is low
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tasks Section
                Text(
                    text = "Tasks:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Tasks Section
                if (tasks.isEmpty()) {
                    Text("No tasks added yet.")
                } else {
                    LazyColumn {
                        items(tasks) { task ->
                            TaskCard(
                                task = task,
                                onToggleCompletion = {
                                    val updatedTask = task.copy(completed = !task.completed)
                                    tasks = tasks.map {
                                        if (it == task) updatedTask else it
                                    }
                                    StorageHelper.saveTasks(context, project.title, tasks) // Save updated tasks
                                    if (updatedTask.completed) {
                                        progress = minOf(1f, progress + 0.1f)
                                    }
                                    onUpdateProject(project.withProgress(progress))
                                },
                                onToggleTimer = { task ->
                                    // Toggle the timer for the task
                                    tasks = tasks.map { currentTask ->
                                        if (currentTask == task) {
                                            if (currentTask.timerRunning) {
                                                // Stop the timer
                                                val elapsedTime = System.currentTimeMillis() - currentTask.startTime + currentTask.elapsedTime
                                                currentTask.copy(timerRunning = false, elapsedTime = elapsedTime)
                                            } else {
                                                // Start the timer
                                                currentTask.copy(timerRunning = true, startTime = System.currentTimeMillis())
                                            }
                                        } else {
                                            currentTask
                                        }
                                    }.toMutableList()
                                    StorageHelper.saveTasks(context, project.title, tasks) // Save updated tasks
                                    onUpdateProject(project.copy(tasks = tasks)) // Update project with new tasks
                                }
                            )
                        }
                    }
                }
            }

            // Handling Timer Toggle
            fun toggleTimer(task: TaskItem) {
                tasks = tasks.map { currentTask ->
                    if (currentTask == task) {
                        if (currentTask.timerRunning) {
                            val elapsedTime = System.currentTimeMillis() - currentTask.startTime + currentTask.elapsedTime
                            currentTask.copy(timerRunning = false, elapsedTime = elapsedTime)
                        } else {
                            currentTask.copy(timerRunning = true, startTime = System.currentTimeMillis())
                        }
                    } else {
                        currentTask
                    }
                }.toMutableList()
                StorageHelper.saveTasks(context, project.title, tasks) // Save updated tasks
                onUpdateProject(project.copy(tasks = tasks)) // Update project with new tasks
            }

            // Add Task Dialog
            if (isDialogOpen) {
                AddTaskDialog(
                    onDismiss = { isDialogOpen = false },
                    onAddTask = { taskTitle, taskDescription ->
                        if (taskTitle.isNotBlank() && taskDescription.isNotBlank()) {
                            val newTask = TaskItem(taskTitle, taskDescription)
                            tasks = tasks + newTask
                            StorageHelper.saveTasks(context, project.title, tasks) // Save tasks after addition
                            onUpdateProject(project.copy(tasks = tasks))
                        }
                        isDialogOpen = false
                    }
                )
            }
        }

        // Simulate the progress ticking down every second
        LaunchedEffect(Unit) {
            while (progress > 0f) {
                delay(1000L) // Decrease progress every second
                progress -= 0.01f // Decrease by a small amount
                val updatedProject = project.withProgress(progress) // Update project with new progress
                onUpdateProject(updatedProject)
            }
        }
    }


    @Composable
    fun AddTaskDialog(
        onDismiss: () -> Unit,
        onAddTask: (String, String) -> Unit
    ) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add New Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Task Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddTask(title, description)
                }) {
                    Text("Add Task")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    fun TaskCard(
        task: TaskItem,
        onToggleCompletion: () -> Unit,
        onToggleTimer: (TaskItem) -> Unit
    ) {
        // Format the elapsed time into hours, minutes, and seconds
        val hours = (task.elapsedTime / 3600).toInt()
        val minutes = ((task.elapsedTime % 3600) / 60).toInt()
        val seconds = (task.elapsedTime % 60).toInt()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onToggleCompletion() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggleCompletion() }
            )
            Text(
                text = task.title,
                style = if (task.completed) {
                    MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = TextDecoration.LineThrough
                    )
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))

            // Display elapsed time in the format HH:MM:SS
            Text(
                text = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Play/Stop button to toggle the timer
            IconButton(onClick = { onToggleTimer(task) }) {
                Icon(
                    imageVector = if (task.timerRunning) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = if (task.timerRunning) "Stop Timer" else "Start Timer"
                )
            }
        }
    }


    //    @Preview(showBackground = true)
        @Composable
        fun ProjectListPreview() {
            FinalProjectTheme {
                Scaffold(
                    floatingActionButton = {
                        AddNewProject(onClick = {})
                    }
                ) { innerPadding ->
                    ProjectList(
                        projects = listOf(
                            ProjectItem("Project 1", "Description 1"),
                            ProjectItem("Project 2", "Description 2"),
                            ProjectItem("Project 3", "Description 3")
                        ),
                        modifier = Modifier.padding(innerPadding),
                        onProjectClick = {} // Pass an empty lambda
                    )
                }
            }
        }
    }

//@Composable
//fun ProjectList(
//    projects: List<ProjectItem>,
//    onProjectClick: (ProjectItem) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    LazyColumn(modifier = modifier) {
//        items(projects) { project ->
//            HorizontalCard(
//                title = project.title,
//                description = project.description,
//                modifier = Modifier.clickable { onProjectClick(project) }
//            )
//        }
//    }
//}

