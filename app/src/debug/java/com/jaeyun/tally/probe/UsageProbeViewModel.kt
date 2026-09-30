package com.jaeyun.tally.probe

import android.app.Application
import android.app.usage.UsageEvents
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
)

private const val RESUME_QUERY_DELAY_MILLIS = 1_000L

class UsageProbeViewModel(application: Application) : AndroidViewModel(application) {

    private val reader = UsageEventReader(application)
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

    /** 결과 기록용 텍스트. 이벤트는 필터 없이 전부, 시각 오름차순으로 담는다 */
    fun dumpText(): String {
        val result = _uiState.value.result ?: return ""
        return ProbeText.dump(getApplication<Application>().resources, result)
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
        return ProbeResult(
            beginAt = beginAt,
            endAt = endAt,
            rows = rows,
            labels = labels,
            typeCounts = events.groupingBy { it.type }.eachCount(),
            samples = samples,
            stats = ScreenOnAnalyzer.stats(samples),
        )
    }
}
