package com.dastiyar.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("today", "امروز", Icons.Filled.AutoAwesome),
    TabItem("tasks", "کارها", Icons.Filled.Checklist),
    TabItem("habits", "عادت", Icons.Filled.FitnessCenter),
    TabItem("chat", "چت", Icons.Filled.Chat),
    TabItem("reports", "گزارش", Icons.Filled.Assessment)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DastiyarApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.events.collectLatest { snackbar.showSnackbar(it) }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            val inMainTab = tabs.any { it.route == currentRoute }
            if (inMainTab) {
                TopAppBar(
                    title = { Text(tabs.firstOrNull { it.route == currentRoute }?.label ?: "") },
                    actions = {
                        IconButton(onClick = { nav.navigate("memory") }) {
                            Icon(Icons.Filled.MenuBook, contentDescription = "حافظه")
                        }
                        IconButton(onClick = { nav.navigate("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navigateTo(nav, tab.route) },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "today",
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            composable("today") { TodayScreen(vm) }
            composable("tasks") { TasksScreen(vm) }
            composable("habits") { HabitsScreen(vm) }
            composable("chat") { ChatScreen(vm) }
            composable("reports") { ReportsScreen(vm) }
            composable("memory") { MemoryScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}

private fun navigateTo(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}