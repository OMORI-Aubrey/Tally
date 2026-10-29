package com.jaeyun.tally.probe

import android.app.usage.UsageEvents

data class ProbeEvent(
    val timestamp: Long,
    val type: Int,
    val packageName: String,
)

/** 화면이 켜진 뒤 가장 먼저 일어난 일 (§6.2 원안 기준) */
enum class ScreenOnOutcome {
    /** 앱이 포그라운드로 왔다 */
    APP_RESUMED,

    /** 앱 없이 화면이 다시 꺼졌다. 잠금화면·알림만 본 경우 (§6.2.1) */
    SCREEN_OFF,

    /** 조회 범위가 끝날 때까지 다음 이벤트가 없다 */
    NO_EVENT,
}

data class ScreenOnSample(
    val screenOnAt: Long,
    val outcome: ScreenOnOutcome,
    /** §6.2 원안의 `__SCREEN_ON_UNKNOWN__` 길이. 다음 RESUMED / NON_INTERACTIVE까지 */
    val durationMs: Long,
    /** 화면 켜짐부터 KEYGUARD_HIDDEN까지. [durationMs] 안에 잠금 해제가 없었으면 null */
    val keyguardHiddenMs: Long?,
    val resumedPackage: String?,
) {
    /** 확정 규칙의 `__SCREEN_ON_UNKNOWN__` 길이. KEYGUARD_HIDDEN도 구간을 끝낸다 */
    val keyguardAwareMs: Long get() = keyguardHiddenMs ?: durationMs

    /** 잠금 해제로 끝난 샘플. 알림만 보고 끈 경우는 제외된다 */
    val isUnlock: Boolean get() = outcome == ScreenOnOutcome.APP_RESUMED || keyguardHiddenMs != null
}

data class DurationStats(
    val count: Int,
    val medianMs: Long?,
    val maxMs: Long?,
    val atLeast5sCount: Int,
    val atLeast10sCount: Int,
)

data class ScreenOnStats(
    /** §6.2 원안. 앱 진입 샘플의 [ScreenOnSample.durationMs] */
    val current: DurationStats,
    /** 확정 규칙. 잠금 해제 샘플의 [ScreenOnSample.keyguardAwareMs]. 흡수 임계값은 이 값으로 정한다 */
    val keyguardAware: DurationStats,
    val screenOffCount: Int,
)

/**
 * §6.2.1 `__SCREEN_ON_UNKNOWN__` 구간 길이를 실측한다. 흡수 임계값(5초) 확정의 근거가 된다.
 *
 * 실기기(삼성 One UI)에서 런처는 SCREEN_INTERACTIVE **직전**에 RESUMED되어, §6.2 원안으로는
 * 잠금 해제 후 홈 화면에 머문 시간이 구간에 포함된다. 그래서 KEYGUARD_HIDDEN도 구간을 끝내는
 * 규칙을 확정했고(PLAN.md §4), 두 규칙의 길이를 함께 잰다.
 */
object ScreenOnAnalyzer {

    /** 확정 규칙의 입력 이벤트 4종 */
    val TARGET_TYPES = setOf(
        UsageEvents.Event.ACTIVITY_RESUMED,
        UsageEvents.Event.SCREEN_INTERACTIVE,
        UsageEvents.Event.SCREEN_NON_INTERACTIVE,
        UsageEvents.Event.KEYGUARD_HIDDEN,
    )

    fun analyze(events: List<ProbeEvent>, windowEnd: Long): List<ScreenOnSample> {
        val sorted = events.sortedBy { it.timestamp }
        val samples = mutableListOf<ScreenOnSample>()
        sorted.forEachIndexed { index, event ->
            if (event.type != UsageEvents.Event.SCREEN_INTERACTIVE) return@forEachIndexed

            var keyguardHiddenAt: Long? = null
            var sample: ScreenOnSample? = null
            for (next in sorted.subList(index + 1, sorted.size)) {
                when (next.type) {
                    UsageEvents.Event.KEYGUARD_HIDDEN ->
                        if (keyguardHiddenAt == null) keyguardHiddenAt = next.timestamp

                    UsageEvents.Event.ACTIVITY_RESUMED ->
                        sample = event.sampleOf(ScreenOnOutcome.APP_RESUMED, next.timestamp, keyguardHiddenAt, next.packageName)

                    UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                        sample = event.sampleOf(ScreenOnOutcome.SCREEN_OFF, next.timestamp, keyguardHiddenAt, null)

                    // 끝을 못 찾은 채 다시 화면이 켜졌다. 그 시점까지로 끊는다
                    UsageEvents.Event.SCREEN_INTERACTIVE ->
                        sample = event.sampleOf(ScreenOnOutcome.NO_EVENT, next.timestamp, keyguardHiddenAt, null)
                }
                if (sample != null) break
            }
            samples += sample ?: event.sampleOf(ScreenOnOutcome.NO_EVENT, windowEnd, keyguardHiddenAt, null)
        }
        return samples
    }

    fun stats(samples: List<ScreenOnSample>): ScreenOnStats = ScreenOnStats(
        current = durationStats(
            samples.filter { it.outcome == ScreenOnOutcome.APP_RESUMED }.map { it.durationMs },
        ),
        keyguardAware = durationStats(
            samples.filter { it.isUnlock }.map { it.keyguardAwareMs },
        ),
        screenOffCount = samples.count { it.outcome == ScreenOnOutcome.SCREEN_OFF && it.keyguardHiddenMs == null },
    )

    private fun durationStats(durations: List<Long>): DurationStats {
        val sorted = durations.sorted()
        val median = if (sorted.isEmpty()) {
            null
        } else {
            (sorted[(sorted.size - 1) / 2] + sorted[sorted.size / 2]) / 2
        }
        return DurationStats(
            count = sorted.size,
            medianMs = median,
            maxMs = sorted.lastOrNull(),
            atLeast5sCount = sorted.count { it >= 5_000 },
            atLeast10sCount = sorted.count { it >= 10_000 },
        )
    }

    private fun ProbeEvent.sampleOf(
        outcome: ScreenOnOutcome,
        endAt: Long,
        keyguardHiddenAt: Long?,
        resumedPackage: String?,
    ) = ScreenOnSample(
        screenOnAt = timestamp,
        outcome = outcome,
        durationMs = endAt - timestamp,
        keyguardHiddenMs = keyguardHiddenAt?.let { it - timestamp },
        resumedPackage = resumedPackage,
    )
}

/** 공개 상수가 없는 타입(10, 12)은 AOSP `UsageEvents.Event`의 숨김 상수 이름을 쓴다 */
fun eventTypeName(type: Int): String = when (type) {
    UsageEvents.Event.ACTIVITY_RESUMED -> "ACTIVITY_RESUMED"
    UsageEvents.Event.ACTIVITY_PAUSED -> "ACTIVITY_PAUSED"
    UsageEvents.Event.ACTIVITY_STOPPED -> "ACTIVITY_STOPPED"
    UsageEvents.Event.SCREEN_INTERACTIVE -> "SCREEN_INTERACTIVE"
    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> "SCREEN_NON_INTERACTIVE"
    UsageEvents.Event.KEYGUARD_SHOWN -> "KEYGUARD_SHOWN"
    UsageEvents.Event.KEYGUARD_HIDDEN -> "KEYGUARD_HIDDEN"
    UsageEvents.Event.CONFIGURATION_CHANGE -> "CONFIGURATION_CHANGE"
    UsageEvents.Event.USER_INTERACTION -> "USER_INTERACTION"
    UsageEvents.Event.SHORTCUT_INVOCATION -> "SHORTCUT_INVOCATION"
    UsageEvents.Event.STANDBY_BUCKET_CHANGED -> "STANDBY_BUCKET_CHANGED"
    UsageEvents.Event.FOREGROUND_SERVICE_START -> "FOREGROUND_SERVICE_START"
    UsageEvents.Event.FOREGROUND_SERVICE_STOP -> "FOREGROUND_SERVICE_STOP"
    UsageEvents.Event.DEVICE_SHUTDOWN -> "DEVICE_SHUTDOWN"
    UsageEvents.Event.DEVICE_STARTUP -> "DEVICE_STARTUP"
    10 -> "NOTIFICATION_SEEN"
    12 -> "NOTIFICATION_INTERRUPTION"
    else -> "TYPE_$type"
}
