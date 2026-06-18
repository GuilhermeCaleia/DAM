package com.example.gymbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gymbuddy.ui.GymBuddyTheme
import com.example.gymbuddy.ui.navigation.AppDestination
import com.example.gymbuddy.ui.navigation.destinationForRoute
import com.example.gymbuddy.ui.navigation.topLevelDestinations
import com.example.gymbuddy.ui.screens.AddProgressScreen
import com.example.gymbuddy.ui.screens.DashboardScreen
import com.example.gymbuddy.ui.screens.HistoryScreen
import com.example.gymbuddy.ui.screens.NewWorkoutPlanScreen
import com.example.gymbuddy.ui.screens.ProfileScreen
import com.example.gymbuddy.ui.screens.WorkoutPlansScreen
import com.example.gymbuddy.ui.screens.calculateTargetDate
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val viewModel: GymViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.workoutPlans.observe(this) { plans ->
            AlarmHelper(this).scheduleWorkoutAlarms(plans)
        }

        askNotificationPermission()
        scheduleNotifications()

        setContent {
            GymBuddyTheme {
                GymBuddyApp(
                    viewModel = viewModel,
                    onLogout = {
                        startActivity(Intent(this, AuthActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            Firebase.auth.signOut()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleNotifications() {
        val workRequest = PeriodicWorkRequestBuilder<GymNotificationWorker>(15, TimeUnit.MINUTES)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag("gym_notif_task")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "gym_notifications",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GymBuddyApp(
    viewModel: GymViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val appDestination = destinationForRoute(currentRoute)

    val workoutPlans by viewModel.workoutPlans.observeAsState(emptyList())
    val trainingLogs by viewModel.trainingLogs.observeAsState(emptyList())
    val progressEntries by viewModel.progressEntries.observeAsState(emptyList())
    val fullName = Firebase.auth.currentUser?.displayName ?: "Utilizador"
    val firstName = fullName.split(" ").firstOrNull() ?: fullName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appDestination.title) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (appDestination.isTopLevel) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = iconForDestination(destination)),
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            when (appDestination) {
                AppDestination.WorkoutPlans -> {
                    FloatingActionButton(onClick = { navController.navigate(AppDestination.NewWorkoutPlan.route) }) {
                        Icon(painterResource(android.R.drawable.ic_input_add), contentDescription = "Adicionar plano")
                    }
                }
                AppDestination.History -> {
                    FloatingActionButton(onClick = { navController.navigate(AppDestination.AddProgress.route) }) {
                        Icon(painterResource(android.R.drawable.ic_input_add), contentDescription = "Adicionar progresso")
                    }
                }
                else -> Unit
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Dashboard.route) {
                DashboardScreen(
                    userFirstName = firstName,
                    workoutPlans = workoutPlans,
                    trainingLogs = trainingLogs
                )
            }
            composable(AppDestination.WorkoutPlans.route) {
                WorkoutPlansScreen(
                    plans = workoutPlans,
                    logs = trainingLogs,
                    onMarkDone = { plan, isRegular ->
                        if (isRegular) {
                            viewModel.markAttendance(plan, calculateTargetDate(plan))
                        } else {
                            viewModel.markAttendance(plan)
                        }
                    },
                    onDeletePlan = { plan -> viewModel.deleteWorkoutPlan(plan) }
                )
            }
            composable(AppDestination.NewWorkoutPlan.route) {
                NewWorkoutPlanScreen(
                    onSave = { plan ->
                        viewModel.insertWorkoutPlan(plan)
                        navController.navigateUp()
                    },
                    onInvalidForm = {
                        Toast.makeText(navController.context, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            composable(AppDestination.History.route) {
                HistoryScreen(
                    entries = progressEntries,
                    onDeleteEntry = { entry -> viewModel.deleteProgressEntry(entry) }
                )
            }
            composable(AppDestination.AddProgress.route) {
                AddProgressScreen(
                    onSave = { entry ->
                        viewModel.insertProgressEntry(entry)
                        navController.navigateUp()
                    },
                    onInvalidWeight = {
                        Toast.makeText(navController.context, "Por favor insira um peso válido", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            composable(AppDestination.Profile.route) {
                ProfileScreen(viewModel = viewModel, onLogout = onLogout)
            }
        }
    }
}

private fun iconForDestination(destination: AppDestination): Int = when (destination) {
    AppDestination.Dashboard -> android.R.drawable.ic_menu_today
    AppDestination.WorkoutPlans -> android.R.drawable.ic_menu_agenda
    AppDestination.History -> android.R.drawable.ic_menu_recent_history
    AppDestination.Profile -> android.R.drawable.ic_menu_myplaces
    else -> android.R.drawable.ic_menu_help
}
