package com.example.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.R
import com.example.data.local.SyncPreferences
import com.example.data.repository.CadenceRepository
import com.example.data.sync.DeltaSyncWorker
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.effectiveLanguageCode
import com.example.ui.components.withLocaleTag
import com.example.ui.screens.CalorieCalculatorScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.viewmodels.CalorieViewModel
import com.example.ui.viewmodels.FocusViewModel
import com.example.ui.viewmodels.GoalsViewModel
import com.example.ui.viewmodels.JournalViewModel
import com.example.ui.viewmodels.TodayViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data object TodayRoute

@Serializable
data class FocusRoute(val taskId: String? = null)

@Serializable
data object GoalsRoute

@Serializable
data object CalorieRoute

@Serializable
data object JournalRoute

private data class NavTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val routeObj: Any
)

@Composable
fun CadenceApp(auth: FirebaseAuth = Firebase.auth) {
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val syncPrefs = remember(baseContext) { SyncPreferences(baseContext) }
    val storedLanguageTag by syncPrefs.observeLanguageTag()
        .collectAsStateWithLifecycle(initialValue = "system")

    val currentLocale = remember(baseConfig) {
        baseConfig.locales.get(0) ?: Locale.getDefault()
    }
    val effectiveCode = remember(storedLanguageTag, currentLocale) {
        effectiveLanguageCode(storedLanguageTag, currentLocale)
    }

    val localizedContext = remember(baseContext, storedLanguageTag) {
        baseContext.withLocaleTag(storedLanguageTag)
    }
    val localizedConfig = remember(baseConfig, storedLanguageTag) {
        if (storedLanguageTag == "system") {
            baseConfig
        } else {
            Configuration(baseConfig).apply {
                setLocale(Locale.forLanguageTag(storedLanguageTag))
            }
        }
    }

    val toggleLanguage: () -> Unit = {
        val next = if (effectiveCode == "mn") "en" else "mn"
        scope.launch { syncPrefs.setLanguageTag(next) }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfig
    ) {
        key(effectiveCode) {
            val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
            val user = currentUser

            if (user == null) {
                AuthScreen(
                    currentLanguageCode = effectiveCode,
                    onToggleLanguage = toggleLanguage,
                    onAuthSuccess = {}
                )
            } else {
                key(user.uid) {
                    AuthenticatedShell(
                        currentUserId = user.uid,
                        userEmail = user.email,
                        currentLanguageCode = effectiveCode,
                        onToggleLanguage = toggleLanguage
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(
    currentUserId: String,
    userEmail: String?,
    currentLanguageCode: String,
    onToggleLanguage: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val navController = rememberNavController()

    LaunchedEffect(currentUserId) {
        DeltaSyncWorker.schedulePeriodicSync(context.applicationContext)
    }

    val todayViewModel: TodayViewModel = viewModel(
        key = "today_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                TodayViewModel(CadenceRepository(app), currentUserId)
            }
        }
    )
    val focusViewModel: FocusViewModel = viewModel(
        key = "focus_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                FocusViewModel(CadenceRepository(app), currentUserId)
            }
        }
    )
    val goalsViewModel: GoalsViewModel = viewModel(
        key = "goals_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                GoalsViewModel(CadenceRepository(app), currentUserId)
            }
        }
    )
    val calorieViewModel: CalorieViewModel = viewModel(
        key = "calorie_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                CalorieViewModel(CadenceRepository(app), SyncPreferences(app), currentUserId)
            }
        }
    )
    val journalViewModel: JournalViewModel = viewModel(
        key = "journal_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                JournalViewModel(CadenceRepository(app), currentUserId)
            }
        }
    )

    val tabs = remember {
        listOf(
            NavTab(R.string.nav_today, Icons.Rounded.Today, Icons.Outlined.Today, "tab_today", TodayRoute),
            NavTab(R.string.nav_focus, Icons.Rounded.Timer, Icons.Outlined.Timer, "tab_focus", FocusRoute()),
            NavTab(R.string.nav_goals, Icons.Rounded.Flag, Icons.Outlined.Flag, "tab_goals", GoalsRoute),
            NavTab(R.string.nav_kcal, Icons.Rounded.LocalFireDepartment, Icons.Outlined.LocalFireDepartment, "tab_kcal", CalorieRoute),
            NavTab(R.string.nav_journal, Icons.Rounded.AutoStories, Icons.Outlined.AutoStories, "tab_journal", JournalRoute)
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    fun isTabSelected(tab: NavTab): Boolean = when (tab.routeObj) {
        is TodayRoute -> currentDest?.hasRoute<TodayRoute>() == true
        is FocusRoute -> currentDest?.hasRoute<FocusRoute>() == true
        is GoalsRoute -> currentDest?.hasRoute<GoalsRoute>() == true
        is CalorieRoute -> currentDest?.hasRoute<CalorieRoute>() == true
        is JournalRoute -> currentDest?.hasRoute<JournalRoute>() == true
        else -> false
    }

    fun navigateToTab(routeObj: Any) {
        navController.navigate(routeObj) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected = isTabSelected(tab)
                            val label = stringResource(tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.routeObj) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected = isTabSelected(tab)
                            val label = stringResource(tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.routeObj) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = TodayRoute,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<TodayRoute> {
                        TodayScreen(
                            viewModel = todayViewModel,
                            userEmail = userEmail,
                            currentLanguageCode = currentLanguageCode,
                            onToggleLanguage = onToggleLanguage,
                            onStartFocusForTask = { task ->
                                navController.navigate(FocusRoute(taskId = task.id)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                }
                            },
                            onSignOutClick = {
                                signOutUser(context, credentialManager, scope)
                            }
                        )
                    }
                    composable<FocusRoute> { entry ->
                        val args = entry.toRoute<FocusRoute>()
                        FocusScreen(
                            viewModel = focusViewModel,
                            preselectedTaskId = args.taskId
                        )
                    }
                    composable<GoalsRoute> {
                        GoalsScreen(viewModel = goalsViewModel)
                    }
                    composable<CalorieRoute> {
                        CalorieCalculatorScreen(viewModel = calorieViewModel)
                    }
                    composable<JournalRoute> {
                        JournalScreen(viewModel = journalViewModel)
                    }
                }
            }
        }
    }
}
