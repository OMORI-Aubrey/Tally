package com.jaeyun.tally.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jaeyun.tally.ui.components.control.IndexTab
import com.jaeyun.tally.ui.components.control.IndexTabBar
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.screens.appclass.AppClassificationScreen
import com.jaeyun.tally.ui.screens.onboarding.OnboardingScreen
import com.jaeyun.tally.ui.screens.perceived.PerceivedInputScreen
import com.jaeyun.tally.ui.screens.records.RecordsScreen
import com.jaeyun.tally.ui.screens.report.SessionReportScreen
import com.jaeyun.tally.ui.screens.settings.SettingsScreen
import com.jaeyun.tally.ui.screens.timer.TimerScreen
import com.jaeyun.tally.ui.theme.TapeBlue
import com.jaeyun.tally.ui.theme.TapeMint
import com.jaeyun.tally.ui.theme.TapePink

/**
 * 앱 루트. 하단 탭 3개(타이머·기록·설정)와 라우트 7종의 NavHost (§8.1, 와이어프레임 §9).
 *
 * 하단 탭은 탭 목적지에서만 보인다. 앱 분류·체감 입력·세션 리포트·온보딩은 탭 없이 전체 화면으로 연다.
 * 타이머가 진행 중일 때도 숨긴다. 공부 중에 기록을 보러 갈 이유가 없다(와이어프레임 §2-B).
 *
 * 온보딩을 마치기 전에는 온보딩이 시작 목적지다(§8.7). 설정을 읽는 동안에는 빈 종이만 그린다.
 */
@Composable
fun TallyApp(viewModel: TallyAppViewModel = viewModel(factory = TallyAppViewModel.Factory)) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = TopLevelTab.entries.firstOrNull { tab ->
        backStackEntry?.destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }
    // 확인 전에는 IDLE로 본다. 앱을 다시 열어 RUNNING이 복원되면 탭이 내려가 숨는다
    var timerRunning by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedTab != null) {
                IndexTabBar(
                    tabs = TopLevelTab.entries.map { IndexTab(stringResource(it.label), it.color) },
                    selectedIndex = selectedTab.ordinal,
                    onSelect = { navController.navigateToTab(TopLevelTab.entries[it]) },
                    // 진행 중에는 탭만 내려 숨기고 자리는 남긴다. 탭 바를 빼면 화면 아래 여백이 바뀌어 종료 버튼이 튄다
                    tabsVisible = !(selectedTab == TopLevelTab.TIMER && timerRunning),
                )
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().notebookPaper().padding(innerPadding)
        when (val start = startDestination) {
            null -> Box(contentModifier)
            else -> TallyNavHost(
                navController = navController,
                startDestination = start,
                onTimerRunningChange = { timerRunning = it },
                modifier = contentModifier,
            )
        }
    }
}

/** 선택되지 않은 탭 색. 실제 색인 탭처럼 탭마다 고정한다 */
private val TopLevelTab.color
    get() = when (this) {
        TopLevelTab.TIMER -> TapeBlue
        TopLevelTab.RECORDS -> TapePink
        TopLevelTab.SETTINGS -> TapeMint
    }

@Composable
private fun TallyNavHost(
    navController: NavHostController,
    startDestination: Any,
    onTimerRunningChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<Onboarding> {
            OnboardingScreen(onDone = { navController.finishOnboarding() })
        }
        composable<Timer> {
            TimerScreen(
                // 주 1회는 실측을 보기 전에 체감을 먼저 묻는다(§3.4)
                onSessionFinished = { done ->
                    if (done.askPerceived) {
                        navController.navigate(PerceivedInput(done.sessionId))
                    } else {
                        navController.navigate(SessionReport(done.sessionId))
                    }
                },
                onRunningChange = onTimerRunningChange,
                onOpenRecords = { navController.navigateToTab(TopLevelTab.RECORDS) },
            )
        }
        composable<Records> {
            RecordsScreen(onOpenSession = { navController.navigate(SessionReport(it)) })
        }
        composable<Settings> {
            SettingsScreen(onOpenAppClassification = { navController.navigate(AppClassification) })
        }
        composable<AppClassification> {
            AppClassificationScreen(onBack = { navController.popBackStack() })
        }
        composable<PerceivedInput> { entry ->
            val sessionId = entry.toRoute<PerceivedInput>().sessionId
            PerceivedInputScreen(sessionId = sessionId, onDone = { navController.showReportAfterPerceivedInput(sessionId) })
        }
        composable<SessionReport> { entry ->
            val sessionId = entry.toRoute<SessionReport>().sessionId
            SessionReportScreen(sessionId = sessionId, onClose = { navController.popBackStack() })
        }
    }
}

/**
 * 탭마다 백스택과 화면 상태를 따로 보존한다. 타이머가 아닌 탭에서 뒤로가기를 누르면 타이머로 돌아온다.
 *
 * 그래프의 시작 목적지가 아니라 타이머까지 꺼낸다. 온보딩으로 시작한 날은 시작 목적지(온보딩)가 백스택에 없어서
 * 거기까지 꺼내려 하면 아무것도 꺼내지 않고 탭이 쌓인다. 온보딩을 마치면 타이머가 늘 백스택 맨 아래에 있다.
 */
private fun NavController.navigateToTab(tab: TopLevelTab) = navigate(tab.route) {
    popUpTo<Timer> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** 체감 입력을 마치면 그 자리를 리포트로 바꾼다. 리포트에서 뒤로가면 체감 입력이 아니라 타이머로 돌아온다 */
private fun NavController.showReportAfterPerceivedInput(sessionId: Long) = navigate(SessionReport(sessionId)) {
    popUpTo<PerceivedInput> { inclusive = true }
}

/** 온보딩을 마치면 백스택에서 지운다. 타이머에서 뒤로가면 앱을 나간다 */
private fun NavController.finishOnboarding() = navigate(Timer) {
    popUpTo<Onboarding> { inclusive = true }
    launchSingleTop = true
}
