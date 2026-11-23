package com.mtrx.store.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mtrx.store.data.local.preferences.AppTheme
import com.mtrx.store.data.local.preferences.SettingsDataStore
import com.mtrx.store.data.repository.StoreRepository
import com.mtrx.store.ui.navigation.BottomNavBar
import com.mtrx.store.ui.navigation.Screen
import com.mtrx.store.ui.screens.detail.AppDetailScreen
import com.mtrx.store.ui.screens.home.HomeScreen
import com.mtrx.store.ui.screens.myapps.MyAppsScreen
import com.mtrx.store.ui.screens.settings.SettingsScreen
import com.mtrx.store.ui.screens.updates.UpdatesScreen
import com.mtrx.store.ui.theme.MTRXStoreTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var storeRepository: StoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsStateWithLifecycle(initialValue = AppTheme.DARK)
            val updatesCount by storeRepository.getUpdatesCount().collectAsStateWithLifecycle(initialValue = 0)

            MTRXStoreTheme(themeSetting = themeMode) {
                MainAppScaffold(
                    updatesCount = updatesCount,
                    initialIntent = intent
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            storeRepository.refreshInstalledStatus()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainAppScaffold(
    updatesCount: Int,
    initialIntent: Intent?
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Handle deep navigation from notification (e.g., "navigate_to" -> "updates")
    LaunchedEffect(initialIntent) {
        val target = initialIntent?.getStringExtra("navigate_to")
        if (target == "updates") {
            navController.navigate(Screen.Updates.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Updates.route,
        Screen.MyApps.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF090A0D),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    updatesCount = updatesCount,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAppClick = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToUpdates = {
                        navController.navigate(Screen.Updates.route)
                    }
                )
            }

            composable(Screen.Updates.route) {
                UpdatesScreen(
                    onAppClick = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            composable(Screen.MyApps.route) {
                MyAppsScreen(
                    onAppClick = { packageName ->
                        navController.navigate(Screen.AppDetail.createRoute(packageName))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(
                    navArgument("packageName") { type = NavType.StringType }
                )
            ) {
                AppDetailScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
