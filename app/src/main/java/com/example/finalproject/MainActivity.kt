package com.example.finalproject

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.example.finalproject.ProjectItem
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.services.calendar.CalendarScopes
import com.google.android.gms.common.api.Scope

const val REQUEST_AUTHORIZATION = 1001

//  TODO:
//
//  Home - UI
//      Project card
//         Image
//         DONE -- Title
//         DONE -- Description
//         Two backgrounds / overlay
//      DONE -- FAB to make new task project
//      New task popup
//          “Add New Project”
//          Name field
//          Description field
//      “Add Project” confirmation button
//
//  Home - State
//      Project card
//          Image will be chosen randomly
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
                if (result.resultCode == Activity.RESULT_OK) {
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
                var projects by remember { mutableStateOf(sampleProjects()) }

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
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
                    composable(
                        "details/{title}/{description}"
                    ) { backStackEntry ->
                        val title = backStackEntry.arguments?.getString("title") ?: ""
                        val description = backStackEntry.arguments?.getString("description") ?: ""
                        ProjectDetailsScreen(title = title, description = description)
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

        if (authorizationResult != null) {
            if (authorizationResult.hasResolution()) {
                // Authorization still needs user interaction
                Log.d("MainActivity", "Authorization requires user interaction.")
            } else {
                // Check for granted scopes
                val grantedScopes = authorizationResult.grantedScopes
                if (grantedScopes != null && grantedScopes.isNotEmpty()) {
                    Log.d(
                        "MainActivity",
                        "Authorization successful! Granted scopes: $grantedScopes"
                    )
                    saveToCalendar()
                } else {
                    Log.e("MainActivity", "Authorization failed: No granted scopes.")
                }
            }
        } else {
            Log.e("MainActivity", "Authorization result is null or invalid.")
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
                Box(modifier = Modifier.padding(8.dp)) {
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
    fun HorizontalCard(title: String, description: String, modifier: Modifier = Modifier) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and description
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun HorizontalCardPreview() {
        FinalProjectTheme {
            HorizontalCard(
                title = "Card Title",
                description = "Card description"
            )
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
                HorizontalCard(
                    title = project.title,
                    description = project.description,
                    modifier = Modifier.clickable { onProjectClick(project) }
                )
            }
        }
    }

    @Composable
    fun AddNewProject(onClick: () -> Unit) {
        ExtendedFloatingActionButton(
            onClick = { onClick() },
            icon = { Icon(Icons.Filled.Add, "New project button") },
            text = { Text(text = "New project") },
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

    private fun sampleProjects(): List<ProjectItem> {
        return listOf(
            ProjectItem("Project 1", "Description 1"),
            ProjectItem("Project 2", "Description 2"),
            ProjectItem("Project 3", "Description 3")
        )
    }

    @Composable
    fun ProjectDetailsScreen(title: String, description: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Preview(showBackground = true)
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