package com.example.automationapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.automationapp.ui.screens.*
import com.example.automationapp.ui.viewmodel.OnboardingViewModel

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object RuleList : Screen("rule_list")
    object CreateRule : Screen("create_rule?ruleId={ruleId}") {
        fun createRoute(ruleId: Long? = null) = if (ruleId != null) "create_rule?ruleId=$ruleId" else "create_rule"
    }
    object RuleDetails : Screen("rule_details/{ruleId}") {
        fun createRoute(ruleId: Long) = "rule_details/$ruleId"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val isSetupComplete by onboardingViewModel.isSetupComplete.collectAsState()

    // Determine start destination based on setup status
    val startDestination = if (isSetupComplete) {
        Screen.RuleList.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.RuleList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RuleList.route) {
            RuleListScreen(
                onNavigateToCreateRule = { navController.navigate(Screen.CreateRule.createRoute()) },
                onNavigateToRuleDetails = { ruleId ->
                    navController.navigate(Screen.RuleDetails.createRoute(ruleId))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(
            route = Screen.CreateRule.route,
            arguments = listOf(
                navArgument("ruleId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getLong("ruleId") ?: -1L
            CreateRuleScreen(
                ruleId = if (ruleId > 0) ruleId else null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.RuleDetails.route,
            arguments = listOf(navArgument("ruleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getLong("ruleId")
            if (ruleId != null) {
                RuleDetailsScreen(
                    ruleId = ruleId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditRule = { id ->
                        navController.navigate(Screen.CreateRule.createRoute(id))
                    }
                )
            }
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
