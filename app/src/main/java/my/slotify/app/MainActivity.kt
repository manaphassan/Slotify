package my.slotify.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import my.slotify.app.core.di.SlotifyViewModelFactory
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import kotlinx.coroutines.launch
import my.slotify.app.core.widget.SlotifyAppWidgetProvider
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.component.SlotifySpeedDialFab
import my.slotify.app.core.designsystem.theme.AppLanguage
import my.slotify.app.core.designsystem.theme.AppThemeMode
import my.slotify.app.core.designsystem.theme.LocalAppLanguage
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.NordColors
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyTheme
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.strings
import my.slotify.app.core.notification.SlotifyNotificationManager
import my.slotify.app.feature.dashboard.TodayDashboardScreen
import my.slotify.app.feature.finance.FinanceScreen
import my.slotify.app.feature.logistics.LogisticsMapScreen
import my.slotify.app.feature.profile.StudentProfileScreen
import my.slotify.app.feature.rewards.RewardVaultScreen
import my.slotify.app.feature.timetable.TimetableScreen

enum class Screen(val icon: ImageVector) {
    DASHBOARD(Icons.Default.Home),
    TIMETABLE(Icons.Default.CalendarToday),
    LOGISTICS(Icons.Default.DirectionsBus),
    REWARDS(Icons.Default.Star),
    FINANCE(Icons.Default.AccountBalanceWallet),
    PROFILES(Icons.Default.Person)
}

@Composable
fun Screen.localizedTitle(): String = when (this) {
    Screen.DASHBOARD -> strings.today
    Screen.TIMETABLE -> strings.timetable
    Screen.LOGISTICS -> strings.logisticsMap
    Screen.REWARDS -> strings.rewards
    Screen.FINANCE -> strings.finance
    Screen.PROFILES -> strings.profile
}

class MainActivity : ComponentActivity() {

    /** Deep-link target from the widget / notifications (EXTRA_SCREEN), consumed by the NavHost. */
    private val pendingScreenRoute = mutableStateOf<Screen?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        screenFromIntent(intent)?.let { pendingScreenRoute.value = it }
    }

    private fun screenFromIntent(intent: Intent?): Screen? {
        val target = intent?.getStringExtra("EXTRA_SCREEN") ?: intent?.getStringExtra("TARGET_SCREEN") ?: return null
        return Screen.entries.firstOrNull { it.name.equals(target, ignoreCase = true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Only schedule alarms if notification permission is already granted; the runtime
        // request is issued inside setContent.
        SlotifyNotificationManager.createNotificationChannels(this)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            SlotifyNotificationManager.scheduleDailyReminders(this)
        }
        if (savedInstanceState == null) pendingScreenRoute.value = screenFromIntent(intent)

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = SlotifyViewModelFactory)
            val ui by mainViewModel.uiState.collectAsStateWithLifecycle()

            val notifPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) SlotifyNotificationManager.scheduleDailyReminders(this@MainActivity)
            }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // FINE must be requested together with COARSE on Android 12+; either grant is enough.
            fun hasLocationPermission(): Boolean =
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

            var locationGranted by remember { mutableStateOf(hasLocationPermission()) }
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result -> locationGranted = result.values.any { it } || hasLocationPermission() }
            // Permission may be granted/revoked from system Settings while we are paused.
            LifecycleResumeEffect(Unit) {
                locationGranted = hasLocationPermission()
                onPauseOrDispose { }
            }

            CompositionLocalProvider(LocalAppLanguage provides ui.language) {
                SlotifyTheme(themeMode = ui.themeMode) {
                    val coroutineScope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val navController = rememberNavController()
                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentScreen = backStackEntry?.destination?.route
                        ?.let { route -> Screen.entries.firstOrNull { it.name == route } } ?: Screen.DASHBOARD
                    var fabExpanded by rememberSaveable { mutableStateOf(false) }

                    // Top-level navigation: one entry per tab on top of Dashboard, so Back from any
                    // tab returns to Dashboard and Back on Dashboard exits.
                    val navigateTo: (Screen) -> Unit = { screen ->
                        fabExpanded = false
                        if (screen != currentScreen) {
                            navController.navigate(screen.name) {
                                popUpTo(Screen.DASHBOARD.name) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                    LaunchedEffect(pendingScreenRoute.value) {
                        pendingScreenRoute.value?.let { screen ->
                            navigateTo(screen)
                            pendingScreenRoute.value = null
                        }
                    }

                    val isDarkMode = ui.themeMode != AppThemeMode.LIGHT
                    // targetSdk 35+ is always edge-to-edge: keep system-bar icons legible for the
                    // in-app theme (which can differ from the system dark-mode setting).
                    DisposableEffect(isDarkMode) {
                        val style = if (isDarkMode) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                        }
                        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                        onDispose { }
                    }
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            SlotifyDrawerSheet(
                                currentScreen = currentScreen,
                                students = ui.students,
                                selectedStudent = ui.selectedStudent,
                                isDarkMode = isDarkMode,
                                appThemeMode = ui.themeMode,
                                appLanguage = ui.language,
                                onToggleTheme = mainViewModel::cycleTheme,
                                onSelectTheme = mainViewModel::setTheme,
                                onSelectLanguage = mainViewModel::setLanguage,
                                onSelectStudent = mainViewModel::selectStudent,
                                onSelectScreen = { screen ->
                                    navigateTo(screen)
                                    coroutineScope.launch { drawerState.close() }
                                }
                            )
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                SlotifyTopBar(
                                    currentScreen = currentScreen,
                                    selectedStudent = ui.selectedStudent,
                                    isDarkMode = isDarkMode,
                                    appThemeMode = ui.themeMode,
                                    onToggleTheme = mainViewModel::cycleTheme,
                                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                    onOpenProfile = { navigateTo(Screen.PROFILES) },
                                    onOpenRewards = { navigateTo(Screen.REWARDS) }
                                )
                            },
                            floatingActionButton = {
                                SlotifySpeedDialFab(
                                    isExpanded = fabExpanded,
                                    onToggleExpand = { fabExpanded = !fabExpanded },
                                    currentScreen = currentScreen,
                                    onNavigate = navigateTo,
                                    onToggleTheme = mainViewModel::cycleTheme,
                                    onToggleLanguage = mainViewModel::toggleLanguage,
                                    isDarkMode = isDarkMode,
                                    appThemeMode = ui.themeMode,
                                    appLanguage = ui.language
                                )
                            },
                            floatingActionButtonPosition = FabPosition.End
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = Screen.DASHBOARD.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                composable(Screen.DASHBOARD.name) {
                                    TodayDashboardScreen(
                                        students = ui.students,
                                        onSelectStudent = mainViewModel::selectStudent
                                    )
                                }
                                composable(Screen.TIMETABLE.name) { TimetableScreen() }
                                composable(Screen.LOGISTICS.name) {
                                    // Request location on first visit to the Logistics tab
                                    LaunchedEffect(Unit) {
                                        if (!locationGranted) {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    }
                                    LogisticsMapScreen(locationGranted = locationGranted)
                                }
                                composable(Screen.REWARDS.name) { RewardVaultScreen() }
                                composable(Screen.FINANCE.name) { FinanceScreen() }
                                composable(Screen.PROFILES.name) {
                                    StudentProfileScreen(
                                        students = ui.students,
                                        onSelectStudent = mainViewModel::selectStudent
                                    )
                                }
                            }

                            // Registered after NavHost so they take priority over its back
                            // handling: Back closes the drawer / speed-dial before popping a screen.
                            BackHandler(enabled = drawerState.isOpen) { coroutineScope.launch { drawerState.close() } }
                            BackHandler(enabled = fabExpanded) { fabExpanded = false }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Neumorphic Top Bar with Left Hamburger Menu, Center Title/Avatar, Dark Mode Toggle & Profile Shortcut
 */
@Composable
fun SlotifyTopBar(
    currentScreen: Screen,
    selectedStudent: StudentEntity?,
    isDarkMode: Boolean = false,
    appThemeMode: AppThemeMode = if (isDarkMode) AppThemeMode.DARK else AppThemeMode.LIGHT,
    onToggleTheme: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenRewards: () -> Unit
) {
    Surface(
        color = SlotifyColors.CanvasBase,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Hamburger Menu Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 18.dp)
                    .background(SlotifyColors.SurfaceCard, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenDrawer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = strings.openMenu,
                    tint = SlotifyColors.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center: App / Screen Brand Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(onClick = onOpenProfile)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = SlotifyColors.KpmSkyBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                selectedStudent?.let { s ->
                    Spacer(Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .background(SlotifyColors.SurfaceCard, RoundedCornerShape(10.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${s.avatarEmoji} ${s.nickname}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlotifyColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Right: Dark Mode Toggle, Star Balance & Profile Icon
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Theme Quick Button (Light -> Dark -> Nord -> Light)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 17.dp)
                        .background(SlotifyColors.SurfaceCard, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center
                ) {
                    val (topBarIcon, topBarDesc, topBarTint) = when (appThemeMode) {
                        AppThemeMode.LIGHT -> Triple(
                            Icons.Default.DarkMode,
                            strings.darkMode,
                            SlotifyColors.ActionAmber
                        )

                        AppThemeMode.DARK -> Triple(Icons.Default.AcUnit, strings.nordMode, NordColors.Nord8)
                        AppThemeMode.NORD -> Triple(
                            Icons.Default.LightMode,
                            strings.lightMode,
                            SlotifyColors.ActionAmber
                        )
                    }
                    Icon(
                        imageVector = topBarIcon,
                        contentDescription = topBarDesc,
                        tint = topBarTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Star Coins Quick Button
                Box(
                    modifier = Modifier
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 16.dp)
                        .background(SlotifyColors.SurfaceCard, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenRewards)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = strings.stars,
                            tint = SlotifyColors.StarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${selectedStudent?.starCoinsBalance ?: 0}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                }

                // Profile Shortcut Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 17.dp)
                        .background(
                            if (currentScreen == Screen.PROFILES) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard,
                            CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = strings.profile,
                        tint = if (currentScreen == Screen.PROFILES) Color.White else SlotifyColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Slide-out Left Navigation Drawer Sheet with Settings & Tactile Neumorphic Styling
 */
@Composable
fun SlotifyDrawerSheet(
    currentScreen: Screen,
    students: List<StudentEntity>,
    selectedStudent: StudentEntity?,
    isDarkMode: Boolean = false,
    appThemeMode: AppThemeMode = if (isDarkMode) AppThemeMode.DARK else AppThemeMode.LIGHT,
    appLanguage: AppLanguage,
    onToggleTheme: () -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectStudent: (StudentEntity) -> Unit,
    onSelectScreen: (Screen) -> Unit
) {
    val scrollState = rememberScrollState()

    ModalDrawerSheet(
        drawerContainerColor = SlotifyColors.CanvasBase,
        drawerContentColor = SlotifyColors.TextPrimary,
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drawer Brand Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 14.dp)
                        .background(SlotifyColors.SurfaceCard, RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 18.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Slotify",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlotifyColors.KpmSkyBlue
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = strings.appTagline,
                            fontSize = 10.sp,
                            color = SlotifyColors.TextSecondary
                        )
                    }
                }

                // Child Quick Switcher in Drawer
                Text(
                    text = strings.activeChild,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlotifyColors.TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    students.forEach { s ->
                        val isSelected = s.id == selectedStudent?.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard
                                )
                                .clickable { onSelectStudent(s) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(s.avatarEmoji, fontSize = 14.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = s.nickname,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else SlotifyColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)

                // Menu Navigation Items
                Text(
                    text = strings.navMenu,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlotifyColors.TextSecondary
                )

                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f) else Color.Transparent
                            )
                            .clickable { onSelectScreen(screen) }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = null,
                                tint = if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = screen.localizedTitle(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SlotifyColors.KpmSkyBlue else SlotifyColors.TextPrimary
                            )
                        }
                    }
                }

                HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)

                // App Settings: Dark Mode & Language Toggles
                Text(
                    text = strings.settingsTitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlotifyColors.TextSecondary
                )

                // Language Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (appLanguage == AppLanguage.BM) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard)
                            .clickable { onSelectLanguage(AppLanguage.BM) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🇲🇾 Melayu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (appLanguage == AppLanguage.BM) Color.White else SlotifyColors.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (appLanguage == AppLanguage.EN) SlotifyColors.KpmSkyBlue else SlotifyColors.SurfaceCard)
                            .clickable { onSelectLanguage(AppLanguage.EN) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🇬🇧 English",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (appLanguage == AppLanguage.EN) Color.White else SlotifyColors.TextPrimary
                        )
                    }
                }

                // Theme Selector Row (Cerah / Gelap / Nord)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val themeOptions = listOf(
                        Triple(AppThemeMode.LIGHT, "☀️", if (appLanguage == AppLanguage.BM) "Cerah" else "Light"),
                        Triple(AppThemeMode.DARK, "🌙", if (appLanguage == AppLanguage.BM) "Gelap" else "Dark"),
                        Triple(AppThemeMode.NORD, "❄️", "Nord")
                    )

                    themeOptions.forEach { (mode, emoji, label) ->
                        val isSelected = appThemeMode == mode
                        val activeBgColor = when (mode) {
                            AppThemeMode.NORD -> NordColors.Nord8
                            AppThemeMode.DARK -> SlotifyColors.ActionAmber
                            AppThemeMode.LIGHT -> SlotifyColors.KpmSkyBlue
                        }
                        val activeTextColor = when (mode) {
                            AppThemeMode.NORD -> NordColors.Nord0
                            AppThemeMode.DARK -> Color.Black
                            AppThemeMode.LIGHT -> Color.White
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) activeBgColor else SlotifyColors.SurfaceCard)
                                .clickable { onSelectTheme(mode) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = emoji, fontSize = 11.sp)
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeTextColor else SlotifyColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)

                // Quick Widget Installation Button
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlotifyColors.KpmSkyBlue.copy(alpha = 0.15f))
                        .clickable {
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val myProvider = ComponentName(context, SlotifyAppWidgetProvider::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                Toast.makeText(context, "Membuka pengesahan widget skrin utama...", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Sila tekan lama pada skrin utama dan pilih Widgets ➔ Slotify",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📱", fontSize = 13.sp)
                        Text(
                            text = strings.installWidget,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlotifyColors.KpmSkyBlue
                        )
                    }
                }
            }

            // Drawer Footer
            Column(modifier = Modifier.padding(top = 4.dp)) {
                HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Slotify v${BuildConfig.VERSION_NAME}",
                    fontSize = 10.sp,
                    color = SlotifyColors.TextTertiary
                )
                Text(
                    text = strings.compatibilityNote,
                    fontSize = 10.sp,
                    color = SlotifyColors.TextTertiary
                )
            }
        }
    }
}
