package com.wuyuhui.wordmemo.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wuyuhui.wordmemo.data.repository.WordRepository
import com.wuyuhui.wordmemo.ui.library.LibraryDetailScreen
import com.wuyuhui.wordmemo.ui.library.LibraryListScreen
import com.wuyuhui.wordmemo.ui.placeholder.QuizScreen
import com.wuyuhui.wordmemo.ui.placeholder.StatsScreen
import com.wuyuhui.wordmemo.ui.placeholder.TodayScreen
import com.wuyuhui.wordmemo.ui.word.WordDetailScreen

object Routes {
    const val TODAY = "today"
    const val LIBRARIES = "libraries"
    const val QUIZ = "quiz"
    const val STATS = "stats"

    const val LIBRARY_DETAIL_PATTERN = "library/{libraryId}"
    const val WORD_DETAIL_PATTERN = "word/{wordId}"

    fun libraryDetail(id: Long) = "library/$id"
    fun wordDetail(id: Long) = "word/$id"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.TODAY, "今日", Icons.Filled.Home),
    TopLevelDestination(Routes.LIBRARIES, "词库", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Routes.QUIZ, "测试", Icons.Filled.Edit),
    TopLevelDestination(Routes.STATS, "统计", Icons.Filled.Info)
)

@Composable
fun WordMemoApp(repository: WordRepository, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute == destination.route) return@NavigationBarItem
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARIES,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.TODAY) {
                TodayScreen()
            }
            composable(Routes.LIBRARIES) {
                LibraryListScreen(
                    repository = repository,
                    onOpenLibrary = { libraryId ->
                        navController.navigate(Routes.libraryDetail(libraryId))
                    }
                )
            }
            composable(
                route = Routes.LIBRARY_DETAIL_PATTERN,
                arguments = listOf(navArgument("libraryId") { type = NavType.LongType })
            ) { entry ->
                val libraryId = entry.arguments?.getLong("libraryId") ?: 0L
                LibraryDetailScreen(
                    repository = repository,
                    libraryId = libraryId,
                    onBack = { navController.popBackStack() },
                    onOpenWord = { wordId ->
                        navController.navigate(Routes.wordDetail(wordId))
                    }
                )
            }
            composable(
                route = Routes.WORD_DETAIL_PATTERN,
                arguments = listOf(navArgument("wordId") { type = NavType.LongType })
            ) { entry ->
                val wordId = entry.arguments?.getLong("wordId") ?: 0L
                WordDetailScreen(
                    repository = repository,
                    wordId = wordId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.QUIZ) {
                QuizScreen()
            }
            composable(Routes.STATS) {
                StatsScreen()
            }
        }
    }
}
