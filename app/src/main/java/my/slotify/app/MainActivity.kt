package my.slotify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import my.slotify.app.core.database.SlotifyDatabase
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.core.designsystem.theme.NeumorphicElevation
import my.slotify.app.core.designsystem.theme.SlotifyColors
import my.slotify.app.core.designsystem.theme.SlotifyTheme
import my.slotify.app.core.designsystem.theme.neumorphic
import my.slotify.app.core.designsystem.theme.neumorphicClickable
import my.slotify.app.feature.dashboard.TodayDashboardScreen
import my.slotify.app.feature.finance.FinanceScreen
import my.slotify.app.feature.logistics.LogisticsMapScreen
import my.slotify.app.feature.rewards.RewardVaultScreen
import my.slotify.app.feature.timetable.TimetableScreen

enum class Screen(val titleRes: Int, val icon: ImageVector) {
    DASHBOARD(R.string.nav_dashboard, Icons.Default.Home),
    TIMETABLE(R.string.nav_timetable, Icons.Default.CalendarMonth),
    LOGISTICS(R.string.nav_logistics, Icons.Default.DirectionsBus),
    REWARDS(R.string.nav_rewards, Icons.Default.Star),
    FINANCE(R.string.nav_finance, Icons.Default.AccountBalanceWallet)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = (application as SlotifyApplication).database
        val dao = database.slotifyDao()

        setContent {
            SlotifyTheme {
                val coroutineScope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                val students by dao.getAllStudents().collectAsState(initial = emptyList())
                var selectedStudent by remember { mutableStateOf<StudentEntity?>(null) }

                LaunchedEffect(students) {
                    if (selectedStudent == null && students.isNotEmpty()) {
                        selectedStudent = students.first()
                    }
                }

                val slots = remember { mutableStateListOf<TimetableSlotEntity>() }

                Scaffold(
                    bottomBar = {
                        NeumorphicBottomBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { currentScreen = it }
                        )
                    }
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
                                onToggleYearInversion = {
                                    // 1-Tap Year 4 vs Year 5 Inversion logic
                                }
                            )
                            Screen.LOGISTICS -> LogisticsMapScreen()
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
                                }
                            )
                            Screen.FINANCE -> FinanceScreen(
                                selectedStudent = selectedStudent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeumorphicBottomBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlotifyColors.BackgroundLight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .neumorphic(elevation = NeumorphicElevation.HIGH, cornerRadius = 24.dp)
            .background(SlotifyColors.SurfaceCard, RoundedCornerShape(24.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.values().forEach { screen ->
                val isSelected = currentScreen == screen
                Box(
                    modifier = Modifier
                        .neumorphicClickable(cornerRadius = 16.dp) { onScreenSelected(screen) }
                        .background(
                            if (isSelected) SlotifyColors.KpmBlue.copy(alpha = 0.15f) else SlotifyColors.SurfaceCard,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = stringResource(screen.titleRes),
                            tint = if (isSelected) SlotifyColors.KpmBlue else SlotifyColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(screen.titleRes),
                            fontSize = 10.sp,
                            color = if (isSelected) SlotifyColors.KpmBlue else SlotifyColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}
