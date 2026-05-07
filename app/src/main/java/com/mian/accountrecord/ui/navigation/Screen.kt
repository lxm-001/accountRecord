package com.mian.accountrecord.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Report : Screen("report")
    object Budget : Screen("budget")
    object Profile : Screen("profile")
    object CategoryManage : Screen("category_manage")
    object LedgerManage : Screen("ledger_manage")
    object TransactionDetail : Screen("transaction/{id}") {
        fun createRoute(id: Long) = "transaction/$id"
    }
    object BillImport : Screen("bill_import")
    object UserSettings : Screen("user_settings")
    object Agreement : Screen("agreement")
    object Privacy : Screen("privacy")
    object Login : Screen("login")
    object Welcome : Screen("welcome/{nickname}/{avatarUrl}") {
        fun createRoute(nickname: String, avatarUrl: String?) =
            "welcome/$nickname/${avatarUrl ?: "none"}"
    }
}
