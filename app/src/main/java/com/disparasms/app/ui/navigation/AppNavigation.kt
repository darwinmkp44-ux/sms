package com.disparasms.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.House
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.disparasms.app.ui.screen.home.HomeScreen
import com.disparasms.app.ui.screen.groups.GroupsScreen
import com.disparasms.app.ui.screen.groups.CreateGroupScreen
import com.disparasms.app.ui.screen.groups.AddContactsToGroupScreen
import com.disparasms.app.ui.screen.groups.EditGroupScreen
import com.disparasms.app.ui.screen.groups.GroupDetailScreen
import com.disparasms.app.ui.screen.campaign.CreateCampaignScreen
import com.disparasms.app.ui.screen.campaign.CampaignDetailScreen
import com.disparasms.app.ui.screen.campaign.CampaignLogsScreen
import com.disparasms.app.ui.screen.history.CampaignHistoryScreen
import com.disparasms.app.ui.screen.settings.SettingsScreen
import com.disparasms.app.ui.screen.settings.AboutScreen
import com.disparasms.app.ui.screen.settings.BackupRestoreScreen
import com.disparasms.app.ui.screen.settings.SimManagementScreen
import com.disparasms.app.ui.screen.import.ImportScreen
import com.disparasms.app.ui.screen.messages.MessagesScreen
import com.disparasms.app.ui.screen.tools.ToolsScreen
import com.disparasms.app.ui.screen.tools.NumberCleanerScreen
import com.disparasms.app.ui.screen.tools.TemplatesScreen
import com.disparasms.app.ui.screen.tools.QuickTestScreen
import com.disparasms.app.ui.screen.tools.ReportsScreen

private const val ANIM_DURATION = 250

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Início", NavRoutes.Home.route, Icons.Filled.House, Icons.Outlined.House),
    BottomNavItem("Grupos", NavRoutes.Groups.route, Icons.Filled.Campaign, Icons.Outlined.Campaign),
    BottomNavItem("Mensagens", NavRoutes.Messages.route, Icons.Filled.Sms, Icons.Outlined.Sms),
    BottomNavItem("Ferramentas", NavRoutes.Tools.route, Icons.Filled.Build, Icons.Outlined.Build),
    BottomNavItem("Definições", NavRoutes.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Home.route,
            modifier = Modifier.padding(padding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(ANIM_DURATION)) + fadeIn(tween(ANIM_DURATION))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(ANIM_DURATION)) + fadeOut(tween(ANIM_DURATION))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(ANIM_DURATION)) + fadeIn(tween(ANIM_DURATION))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(ANIM_DURATION)) + fadeOut(tween(ANIM_DURATION))
            }
        ) {
            // Bottom nav
            composable(NavRoutes.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(NavRoutes.Groups.route) {
                GroupsScreen(navController = navController)
            }
            composable(NavRoutes.Messages.route) {
                MessagesScreen(navController = navController)
            }
            composable(NavRoutes.Tools.route) {
                ToolsScreen(navController = navController)
            }
            composable(NavRoutes.Settings.route) {
                SettingsScreen(navController = navController)
            }

            // Campaigns sub-routes
            composable(NavRoutes.Campaigns.route) {
                CampaignHistoryScreen(navController = navController)
            }
            composable(NavRoutes.CreateCampaign.route) {
                CreateCampaignScreen(navController = navController)
            }
            composable(
                route = NavRoutes.CampaignDetail.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.LongType })
            ) {
                CampaignDetailScreen(navController = navController)
            }
            composable(
                route = NavRoutes.CampaignLogs.route,
                arguments = listOf(navArgument("campaignId") { type = NavType.LongType })
            ) {
                CampaignLogsScreen(navController = navController)
            }

            // Groups sub-routes
            composable(NavRoutes.CreateGroup.route) {
                CreateGroupScreen(navController = navController)
            }
            composable(
                route = NavRoutes.EditGroup.route,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) {
                EditGroupScreen(navController = navController)
            }
            composable(
                route = NavRoutes.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) {
                GroupDetailScreen(navController = navController)
            }

            // Settings sub-routes
            composable(NavRoutes.SimManagement.route) {
                SimManagementScreen(navController = navController)
            }
            composable(NavRoutes.BackupRestore.route) {
                BackupRestoreScreen(navController = navController)
            }
            composable(NavRoutes.About.route) {
                AboutScreen(navController = navController)
            }

            // Import
            composable(
                route = NavRoutes.AddContactsToGroup.route,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) {
                AddContactsToGroupScreen(navController = navController)
            }
            composable(NavRoutes.ImportContacts.route) {
                ImportScreen(navController = navController, groupId = null)
            }
            composable(
                route = "import/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId", -1L)
                    ?.takeIf { it != -1L }
                ImportScreen(navController = navController, groupId = groupId)
            }

            // Tools sub-routes
            composable(NavRoutes.ToolsNumberCleaner.route) {
                NumberCleanerScreen(navController = navController)
            }
            composable(NavRoutes.ToolsTemplates.route) {
                TemplatesScreen(navController = navController)
            }
            composable(NavRoutes.ToolsQuickTest.route) {
                QuickTestScreen(navController = navController)
            }
            composable(NavRoutes.ToolsReports.route) {
                ReportsScreen(navController = navController)
            }
        }
    }
}
