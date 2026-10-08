package my.slotify.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fix 1.2: Only schedule alarms if notification permission is already granted.
        // The runtime request is issued inside setContent via rememberLauncherForActivityResult.
        SlotifyNotificationManager.createNotificationChannels(this)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            SlotifyNotificationManager.scheduleDailyReminders(this)
        }

        val database = (application as SlotifyApplication).database
        val dao = database.slotifyDao()

        setContent {
            var appThemeMode by rememberSaveable { mutableStateOf(AppThemeMode.LIGHT) }
            val isDarkMode = appThemeMode != AppThemeMode.LIGHT
            var appLanguage by rememberSaveable { mutableStateOf(AppLanguage.BM) }

            // Fix 1.2: Runtime notification permission request (Android 13+ / API 33+)
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) SlotifyNotificationManager.scheduleDailyReminders(this@MainActivity)
            }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Fix 1.3: Runtime location permission request for LogisticsMapScreen
            var locationGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                )
            }
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> locationGranted = granted }

            val cycleTheme: () -> Unit = {
                appThemeMode = when (appThemeMode) {
                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                    AppThemeMode.DARK -> AppThemeMode.NORD
                    AppThemeMode.NORD -> AppThemeMode.LIGHT
                }
            }

            CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
                SlotifyTheme(themeMode = appThemeMode) {
                    val coroutineScope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
                    var fabExpanded by remember { mutableStateOf(false) }

                    val students by dao.getAllStudents().collectAsState(initial = emptyList())
                    var selectedStudent by remember { mutableStateOf<StudentEntity?>(null) }

                    LaunchedEffect(Unit) {
                        my.slotify.app.core.database.DatabaseSeeder.seedHomeworkAndExamsIfMissing(dao)
                    }

                    LaunchedEffect(students) {
                        if (selectedStudent == null && students.isNotEmpty()) {
                            selectedStudent = students.first()
                        } else if (selectedStudent != null) {
                            selectedStudent = students.find { it.id == selectedStudent?.id } ?: students.firstOrNull()
                        }
                    }

                    // Wire sessions + slots to real DB with dual-session cycling support
                    val studentSessions by remember(selectedStudent) {
                        if (selectedStudent != null)
                            dao.getSessionsForStudent(selectedStudent!!.id)
                        else
                            kotlinx.coroutines.flow.flowOf(emptyList())
                    }.collectAsState(initial = emptyList())

                    // Track which session is currently shown — reset to 0 when student changes
                    var activeSessionIndex by rememberSaveable(selectedStudent?.id) { mutableStateOf(0) }
                    val activeSession = studentSessions.getOrNull(
                        activeSessionIndex.coerceIn(0, (studentSessions.size - 1).coerceAtLeast(0))
                    )

                    val slots by remember(activeSession) {
                        if (activeSession != null)
                            dao.getAllSlotsForSession(activeSession.id)
                        else
                            kotlinx.coroutines.flow.flowOf(emptyList())
                    }.collectAsState(initial = emptyList())

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            SlotifyDrawerSheet(
                                currentScreen = currentScreen,
                                students = students,
                                selectedStudent = selectedStudent,
                                isDarkMode = isDarkMode,
                                appThemeMode = appThemeMode,
                                appLanguage = appLanguage,
                                onToggleTheme = cycleTheme,
                                onSelectTheme = { appThemeMode = it },
                                onSelectLanguage = { appLanguage = it },
                                onSelectStudent = { selectedStudent = it },
                                onSelectScreen = { screen ->
                                    currentScreen = screen
                                    coroutineScope.launch { drawerState.close() }
                                }
                            )
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                SlotifyTopBar(
                                    currentScreen = currentScreen,
                                    selectedStudent = selectedStudent,
                                    isDarkMode = isDarkMode,
                                    appThemeMode = appThemeMode,
                                    onToggleTheme = cycleTheme,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onOpenProfile = {
                                        currentScreen = Screen.PROFILES
                                    },
                                    onOpenRewards = {
                                        currentScreen = Screen.REWARDS
                                    }
                                )
                            },
                            floatingActionButton = {
                                SlotifySpeedDialFab(
                                    isExpanded = fabExpanded,
                                    onToggleExpand = { fabExpanded = !fabExpanded },
                                    currentScreen = currentScreen,
                                    onNavigate = { screen ->
                                        currentScreen = screen
                                        fabExpanded = false
                                    },
                                    onToggleTheme = cycleTheme,
                                    onToggleLanguage = {
                                        appLanguage = if (appLanguage == AppLanguage.BM) AppLanguage.EN else AppLanguage.BM
                                    },
                                    isDarkMode = isDarkMode,
                                    appThemeMode = appThemeMode,
                                    appLanguage = appLanguage
                                )
                            },
                            floatingActionButtonPosition = FabPosition.End
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentScreen) {
                                    Screen.DASHBOARD -> TodayDashboardScreen(
                                        students = students,
                                        selectedStudent = selectedStudent,
                                        dao = dao,
                                        onSelectStudent = { selectedStudent = it },
                                        onRewardEarned = { delta ->
                                            selectedStudent?.let { s ->
                                                coroutineScope.launch {
                                                    dao.updateStarCoins(s.id, delta)
                                                    selectedStudent = dao.getStudentById(s.id)
                                                }
                                            }
                                        }
                                    )
                                    Screen.TIMETABLE -> TimetableScreen(
                                        selectedStudent = selectedStudent,
                                        slots = slots,
                                        sessions = studentSessions,
                                        activeSessionIndex = activeSessionIndex,
                                        onToggleYearInversion = {
                                            if (studentSessions.size > 1) {
                                                activeSessionIndex = (activeSessionIndex + 1) % studentSessions.size
                                            }
                                        },
                                        dao = dao,
                                        session = activeSession
                                    )
                                    // Fix 1.3: Request location on first visit to the Logistics tab
                                    Screen.LOGISTICS -> {
                                        LaunchedEffect(Unit) {
                                            if (!locationGranted) {
                                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                            }
                                        }
                                        LogisticsMapScreen(locationGranted = locationGranted)
                                    }
                                    Screen.REWARDS -> RewardVaultScreen(
                                        selectedStudent = selectedStudent,
                                        onRedeemReward = { item ->
                                            selectedStudent?.let { s ->
                                                if (s.starCoinsBalance >= item.pointsCost) {
                                                    coroutineScope.launch {
                                                        dao.updateStarCoins(s.id, -item.pointsCost)
                                                        selectedStudent = dao.getStudentById(s.id)
                                                    }
                                                }
                                            }
                                        },
                                        onBonusStarsAwarded = { delta ->
                                            selectedStudent?.let { s ->
                                                coroutineScope.launch {
                                                    dao.updateStarCoins(s.id, delta)
                                                    selectedStudent = dao.getStudentById(s.id)
                                                }
                                            }
                                        }
                                    )
                                    Screen.FINANCE -> FinanceScreen(
                                        selectedStudent = selectedStudent,
                                        dao = dao
                                    )
                                    Screen.PROFILES -> StudentProfileScreen(
                                        students = students,
                                        selectedStudent = selectedStudent,
                                        onSelectStudent = { selectedStudent = it },
                                        onSaveProfile = { updated ->
                                            coroutineScope.launch {
                                                dao.updateStudent(updated)
                                                selectedStudent = dao.getStudentById(updated.id)
                                            }
                                        },
                                        dao = dao,
                                        onRewardStars = { delta ->
                                            selectedStudent?.let { s ->
                                                coroutineScope.launch {
                                                    dao.updateStarCoins(s.id, delta)
                                                    selectedStudent = dao.getStudentById(s.id)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
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
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Hamburger Menu Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 19.dp)
                    .background(SlotifyColors.SurfaceCard, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenDrawer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = strings.openMenu,
                    tint = SlotifyColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center: App / Screen Brand Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenProfile)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = SlotifyColors.KpmSkyBlue
                )
                selectedStudent?.let { s ->
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(SlotifyColors.SurfaceCard, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${s.avatarEmoji} ${s.nickname}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlotifyColors.TextPrimary
                        )
                    }
                }
            }

            // Right: Dark Mode Toggle, Star Balance & Profile Icon
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Theme Quick Button (Light -> Dark -> Nord -> Light)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 18.dp)
                        .background(SlotifyColors.SurfaceCard, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleTheme),
                    contentAlignment = Alignment.Center
                ) {
                    val (topBarIcon, topBarDesc, topBarTint) = when (appThemeMode) {
                        AppThemeMode.LIGHT -> Triple(Icons.Default.DarkMode, strings.darkMode, SlotifyColors.ActionAmber)
                        AppThemeMode.DARK -> Triple(Icons.Default.AcUnit, strings.nordMode, NordColors.Nord8)
                        AppThemeMode.NORD -> Triple(Icons.Default.LightMode, strings.lightMode, SlotifyColors.ActionAmber)
                    }
                    Icon(
                        imageVector = topBarIcon,
                        contentDescription = topBarDesc,
                        tint = topBarTint,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Star Coins Quick Button
                Box(
                    modifier = Modifier
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 16.dp)
                        .background(SlotifyColors.SurfaceCard, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenRewards)
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = strings.stars,
                            tint = SlotifyColors.StarGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(3.dp))
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
                        .size(36.dp)
                        .neumorphic(elevation = NeumorphicElevation.LOW, cornerRadius = 18.dp)
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
                        modifier = Modifier.size(19.dp)
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
                                    fontSize = 9.5.sp,
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
                    text = "Konfigurasi / Settings",
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
            }

            // Drawer Footer
            Column(modifier = Modifier.padding(top = 4.dp)) {
                HorizontalDivider(color = SlotifyColors.BorderSubtle, thickness = 0.8.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Slotify v1.0.0 • Xperia Edition",
                    fontSize = 9.5.sp,
                    color = SlotifyColors.TextTertiary
                )
                Text(
                    text = "Android 8.0 (API 26) Compatible",
                    fontSize = 8.5.sp,
                    color = SlotifyColors.TextTertiary
                )
            }
        }
    }
}
