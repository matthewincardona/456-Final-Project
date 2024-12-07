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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.finalproject.ui.theme.FinalProjectTheme
import com.example.finalproject.ProjectItem

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            }
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
}

@Composable
fun HomeScreen(
    projects: List<ProjectItem>,
    onProjectClick: (ProjectItem) -> Unit,
    onAddProject: (String, String) -> Unit
) {
    var isDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
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

fun sampleProjects(): List<ProjectItem> {
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

