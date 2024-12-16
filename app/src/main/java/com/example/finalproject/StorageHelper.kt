package com.example.finalproject

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object StorageHelper {
    private const val PREF_NAME = "project_prefs"
    private const val KEY_PROJECTS = "projects"
    private const val KEY_TASKS_PREFIX = "tasks_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveProjects(context: Context, projects: List<ProjectItem>) {
        val json = Gson().toJson(projects)
        getPrefs(context).edit().putString(KEY_PROJECTS, json).apply()
    }

    fun loadProjects(context: Context): List<ProjectItem> {
        val json = getPrefs(context).getString(KEY_PROJECTS, null)
        val type = object : TypeToken<List<ProjectItem>>() {}.type
        return json?.let { Gson().fromJson(it, type) } ?: emptyList()
    }

    // Save tasks for a specific project
    fun saveTasks(context: Context, projectTitle: String, tasks: List<TaskItem>) {
        val json = Gson().toJson(tasks)
        getPrefs(context).edit().putString(KEY_TASKS_PREFIX + projectTitle, json).apply()
    }

    // Load tasks for a specific project
    fun loadTasks(context: Context, projectTitle: String): List<TaskItem> {
        val json = getPrefs(context).getString(KEY_TASKS_PREFIX + projectTitle, null)
        val type = object : TypeToken<List<TaskItem>>() {}.type
        return json?.let { Gson().fromJson(it, type) } ?: emptyList()
    }
}

