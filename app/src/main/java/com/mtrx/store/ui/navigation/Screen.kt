package com.mtrx.store.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Updates : Screen("updates")
    object MyApps : Screen("my_apps")
    object Settings : Screen("settings")
    object AppDetail : Screen("app_detail/{packageName}") {
        fun createRoute(packageName: String): String = "app_detail/$packageName"
    }
}
