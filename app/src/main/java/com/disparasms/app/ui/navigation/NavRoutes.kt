package com.disparasms.app.ui.navigation

sealed class NavRoutes(val route: String) {
    // Bottom nav destinations
    data object Home : NavRoutes("home")
    data object Groups : NavRoutes("groups")
    data object Messages : NavRoutes("messages")
    data object Tools : NavRoutes("tools")
    data object Settings : NavRoutes("settings")

    // Sub destinations
    data object Campaigns : NavRoutes("campaigns")
    data object CreateGroup : NavRoutes("groups/create")
    data object EditGroup : NavRoutes("groups/edit/{groupId}") {
        fun createRoute(groupId: Long) = "groups/edit/$groupId"
    }
    data object GroupDetail : NavRoutes("groups/{groupId}") {
        fun createRoute(groupId: Long) = "groups/$groupId"
    }
    data object CreateCampaign : NavRoutes("campaigns/create")
    data object CampaignDetail : NavRoutes("campaigns/{campaignId}") {
        fun createRoute(campaignId: Long) = "campaigns/$campaignId"
    }
    data object CampaignLogs : NavRoutes("campaigns/{campaignId}/logs") {
        fun createRoute(campaignId: Long) = "campaigns/$campaignId/logs"
    }
    data object ImportContacts : NavRoutes("import") {
        fun createRoute(groupId: Long?) = if (groupId != null) "import/$groupId" else "import"
    }
    data object AddContactsToGroup : NavRoutes("groups/{groupId}/add-contacts") {
        fun createRoute(groupId: Long) = "groups/$groupId/add-contacts"
    }
    data object BackupRestore : NavRoutes("settings/backup")
    data object SimManagement : NavRoutes("settings/sim")
    data object About : NavRoutes("settings/about")

    // Tools sub destinations
    data object ToolsNumberCleaner : NavRoutes("tools/number-cleaner")
    data object ToolsTemplates : NavRoutes("tools/templates")
    data object ToolsQuickTest : NavRoutes("tools/quick-test")
    data object ToolsReports : NavRoutes("tools/reports")
}
