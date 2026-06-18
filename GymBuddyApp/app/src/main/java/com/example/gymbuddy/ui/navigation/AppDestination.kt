package com.example.gymbuddy.ui.navigation

sealed class AppDestination(
    val route: String,
    val title: String,
    val isTopLevel: Boolean = false
) {
    data object Dashboard : AppDestination("dashboard", "Início", true)
    data object WorkoutPlans : AppDestination("workout_plans", "Plano de Treino", true)
    data object NewWorkoutPlan : AppDestination("new_workout_plan", "Novo Plano")
    data object History : AppDestination("history", "Progresso", true)
    data object AddProgress : AppDestination("add_progress", "Registar Peso")
    data object Profile : AppDestination("profile", "Perfil", true)
}

val topLevelDestinations = listOf(
    AppDestination.Dashboard,
    AppDestination.WorkoutPlans,
    AppDestination.History,
    AppDestination.Profile
)

fun destinationForRoute(route: String?): AppDestination = when (route) {
    AppDestination.WorkoutPlans.route -> AppDestination.WorkoutPlans
    AppDestination.NewWorkoutPlan.route -> AppDestination.NewWorkoutPlan
    AppDestination.History.route -> AppDestination.History
    AppDestination.AddProgress.route -> AppDestination.AddProgress
    AppDestination.Profile.route -> AppDestination.Profile
    else -> AppDestination.Dashboard
}
