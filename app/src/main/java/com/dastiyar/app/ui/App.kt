package com.dastiyar.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("today", "امروز", Icons.Outlined.AutoAwesome),
    TabItem("tasks", "کارها", Icons.Outlined.Checklist),
    TabItem("habits", "عادت", Icons.Outlined.FitnessCenter),
    TabItem("chat", "گفتگو", Icons.Outlined.Chat),
    TabItem("reports", "گزارش", Icons.Outlined.Assessment)
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            val inMainTab = tabs.any { it.route == currentRoute }
            if (inMainTab) {
                TopAppBar(
                    title = {
                        Text(
                            tabs.firstOrNull { it.route == currentRoute }?.label ?: "",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        IconButton(onClick = { nav.navigate("memory") }) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = "حافظه")
                        }
                        IconButton(onClick = { nav.navigate("settings") }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "تنظیمات")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateTo(nav, tab.route) },
                        icon = {
                            Icon(tab.icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "today",
            modifier = Modifier.fillMaxSize().padding(pad),
            enterTransition = { fadeIn(tween(180)) + slideInHorizontally(animationSpec = tween(220), initialOffsetX = { it / 6 }) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(180)) + slideInHorizontally(animationSpec = tween(220), initialOffsetX = { -it / 6 }) },
            popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(animationSpec = tween(220), targetOffsetX = { it / 6 }) }
        ) {
            composable("today") { TodayScreen(vm, onNavigate = { nav.navigate(it) }) }
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