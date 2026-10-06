package com.jaeyun.tally.probe

import android.app.Application
import android.app.usage.UsageEvents
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jaeyun.tally.TallyApplication
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.usagestats.AppClassifier
import com.jaeyun.tally.data.usagestats.AutoClassification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProbeRow(
    val event: ProbeEvent,
    val label: String,
    val isLauncher: Boolean,
    val isSelf: Boolean,
)

data class ProbeResult(
    val beginAt: Long,
    val endAt: Long,
    /** 시각 오름차순 */
    val rows: List<ProbeRow>,
    val labels: Map<String, String>,
    val typeCounts: Map<Int, Int>,
    val samples: List<ScreenOnSample>,
    val stats: ScreenOnStats,
    /** 조회 범위에서 RESUMED된 앱의 자동 분류(#11). 최근에 RESUMED된 순서 */
    val classifications: List<AutoClassification>,
) {
    val latest: ProbeRow? get() = rows.lastOrNull()

    /**
     * 다른 앱에서 돌아온 직후라면 마지막 RESUMED가 본 앱이어야 한다. 아니면 조회 지연을 의심한다.
     * 직전 앱의 STOPPED는 본 앱 RESUMED 뒤에 오므로 마지막 이벤트가 아니라 마지막 RESUMED로 판단한다.
     */
    val selfResumedIsLatest: Boolean
        get() = rows.lastOrNull { it.event.type == UsageEvents.Event.ACTIVITY_RESUMED }?.isSelf == true
}

data class ProbeUiState(
    val permissionGranted: Boolean = false,
    val windowMinutes: Int = 15,
    val targetOnly: Boolean = true,
    val result: ProbeResult? = null,
    /** 사용 기록 권한과 관계없이 DB에서 읽는다 */
    val lastSession: LastSessionInfo? = null,
    val perceivedWeek: PerceivedWeekInfo? = null,
    /** 임시(#14): 시간 더하기 대상 */
    val runningSession: StudySession? = null,
    val onboarding: OnboardingInfo? = null,
    /** 임시(#21): ① 화면 꺼짐 확인 기준을 1분으로 낮췄는지 */
    val quickScreenOffCheck: Boolean = false,
)

private const val RESUME_QUERY_DELAY_MILLIS = 1_000L

class UsageProbeViewModel(application: Application) : AndroidViewModel(application) {

    private val reader = UsageEventReader(application)
    private val classifier = AppClassifier(application)
    private val selfPackage = application.packageName

    private val _uiState = MutableStateFlow(ProbeUiState())
    val uiState: StateFlow<ProbeUiState> = _uiState.asStateFlow()

    private var queryJob: Job? = null

    /**
     * 화면에 돌아올 때마다 호출. 다른 앱에서 복귀한 직후의 이벤트 수신 여부를 확인할 수 있다.
     * onResume 도중에 바로 조회하면 본 앱의 RESUMED가 아직 기록되기 전이라 조회를 조금 늦춘다.
     */
    fun refresh() {
        val granted = reader.hasPermission()
        _uiState.update { it.copy(permissionGranted = granted) }
        loadLastSession()
        if (granted) query(delayMillis = RESUME_QUERY_DELAY_MILLIS)
    }

    fun selectWindow(minutes: Int) {
        _uiState.update { it.copy(windowMinutes = minutes) }
        query()
    }

    fun setTargetOnly(targetOnly: Boolean) {
        _uiState.update { it.copy(targetOnly = targetOnly) }
    }

    fun query(delayMillis: Long = 0) {
        val state = _uiState.value
        if (!state.permissionGranted) return
        queryJob?.cancel()
        queryJob = viewModelScope.launch {
            delay(delayMillis)
            val result = withContext(Dispatchers.IO) { load(state.windowMinutes) }
            _uiState.update { it.copy(result = result) }
        }
    }

    private fun loadLastSession() {
        viewModelScope.launch {
            val container = getApplication<TallyApplication>().container
            val info = withContext(Dispatchers.IO) { loadLastSession(container) }
            val perceived = withContext(Dispatchers.IO) { loadPerceivedWeek(container, System.currentTimeMillis()) }
            val running = withContext(Dispatchers.IO) { loadRunningSession(container) }
            val onboarding = withContext(Dispatchers.IO) { loadOnboarding(container) }
            val quickScreenOff = withContext(Dispatchers.IO) { container.settingsStore.settings.first().quickScreenOffCheck }
            _uiState.update {
                it.copy(
                    lastSession = info,
                    perceivedWeek = perceived,
                    runningSession = running,
                    onboarding = onboarding,
                    quickScreenOffCheck = quickScreenOff,
                )
            }
        }
    }

    /** 임시(#21): ① 화면 꺼짐 확인 기준을 60분 ↔ 1분으로 바꾼다. 다음에 끝내는 세션부터 적용된다 */
    fun setQuickScreenOffCheck(enabled: Boolean) {
        viewModelScope.launch {
            val container = getApplication<TallyApplication>().container
            withContext(Dispatchers.IO) { container.settingsStore.setQuickScreenOffCheck(enabled) }
            loadLastSession()
        }
    }

    /** 온보딩 완료를 되돌린다. 저장이 끝난 뒤에 돌아오므로 이어서 본 앱을 열면 온보딩부터 나온다 */
    suspend fun resetOnboarding() {
        val container = getApplication<TallyApplication>().container
        withContext(Dispatchers.IO) { resetOnboarding(container) }
    }

    /** 임시(#14): 진행 중 세션을 [minutes]분 늘린다 */
    fun extendRunningSession(minutes: Int) {
        viewModelScope.launch {
            val container = getApplication<TallyApplication>().container
            withContext(Dispatchers.IO) { extendRunningSession(container, minutes) }
            loadLastSession()
        }
    }

    /** 체감 질문을 다시 시험할 수 있게 이번 주 기록을 지운다 */
    fun clearPerceivedWeek() {
        viewModelScope.launch {
            val container = getApplication<TallyApplication>().container
            withContext(Dispatchers.IO) { clearPerceivedWeek(container, System.currentTimeMillis()) }
            loadLastSession()
        }
    }

    /** 결과 기록용 텍스트. 이벤트는 필터 없이 전부, 시각 오름차순으로 담는다 */
    fun dumpText(): String {
        val state = _uiState.value
        val res = getApplication<Application>().resources
        val lastSession = state.lastSession?.let { ProbeText.lastSessionDump(res, it) }.orEmpty()
        val perceived = state.perceivedWeek?.let { ProbeText.perceivedDump(res, it) }.orEmpty()
        val result = state.result?.let { ProbeText.dump(res, it) }.orEmpty()
        return listOf(lastSession, perceived, result).filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun load(windowMinutes: Int): ProbeResult {
        val endAt = System.currentTimeMillis()
        val beginAt = endAt - windowMinutes * 60_000L
        val events = reader.read(beginAt, endAt).sortedBy { it.timestamp }
        val launchers = reader.launcherPackages()
        val labels = events.map { it.packageName }
            .distinct()
            .associateWith { reader.appLabel(it) ?: it }
        val rows = events.map { event ->
            ProbeRow(
                event = event,
                label = labels.getValue(event.packageName),
                isLauncher = event.packageName in launchers,
                isSelf = event.packageName == selfPackage,
            )
        }
        val samples = ScreenOnAnalyzer.analyze(events, endAt)
        val resumedPackages = events
            .filter { it.type == UsageEvents.Event.ACTIVITY_RESUMED }
            .map { it.packageName }
            .asReversed()
            .distinct()
        return ProbeResult(
            beginAt = beginAt,
            endAt = endAt,
            rows = rows,
            labels = labels,
            typeCounts = events.groupingBy { it.type }.eachCount(),
            samples = samples,
            stats = ScreenOnAnalyzer.stats(samples),
            classifications = classifier.classifyAll(resumedPackages),
        )
    }
}
