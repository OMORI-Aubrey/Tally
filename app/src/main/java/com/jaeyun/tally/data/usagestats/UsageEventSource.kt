package com.jaeyun.tally.data.usagestats

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.jaeyun.tally.domain.model.RawEvent
import com.jaeyun.tally.domain.model.RawEventType

/** [UsageEventSource.query] 결과. 권한이 없어도 예외를 던지지 않는다 */
sealed interface UsageQueryResult {
    /** 사용 기록 접근이 허용되지 않았다. 세션은 착석만 저장한다(`hasTimeline = false`, §6.3 #1) */
    data object NoPermission : UsageQueryResult

    /** 시각 오름차순. 복원에 쓰는 4종만 들어 있다 */
    data class Events(val events: List<RawEvent>) : UsageQueryResult
}

/** 세션 구간의 사용 기록 조회. 세션 종료 파이프라인(#12)이 쓰고, 테스트에서는 가짜로 바꾼다 */
interface UsageEventQuery {
    /** `[beginAt, endAt]` 구간의 이벤트. 블로킹 호출이다 */
    fun query(beginAt: Long, endAt: Long): UsageQueryResult
}

/** `UsageEvents.Event` 타입 → 복원에 쓰는 4종 (§6.1, PLAN.md §4). 나머지는 null */
internal fun rawEventTypeOf(eventType: Int): RawEventType? = when (eventType) {
    UsageEvents.Event.ACTIVITY_RESUMED -> RawEventType.ACTIVITY_RESUMED
    UsageEvents.Event.SCREEN_INTERACTIVE -> RawEventType.SCREEN_INTERACTIVE
    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> RawEventType.SCREEN_NON_INTERACTIVE
    UsageEvents.Event.KEYGUARD_HIDDEN -> RawEventType.KEYGUARD_HIDDEN
    else -> null
}

/**
 * `UsageStatsManager` 래퍼 (§6.4). 세션 종료 시 한 번 일괄 조회한다(§3.3). 도메인에는 [RawEvent]만 넘긴다.
 *
 * [query]는 기기의 사용 기록을 읽는 블로킹 호출이라 IO 디스패처에서 부른다.
 */
class UsageEventSource(private val context: Context) : UsageEventQuery {

    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)

    /**
     * 특수 권한이라 결과 콜백이 없다. 화면에 돌아올 때마다 다시 확인한다 (§6.4).
     * 기획서 예시의 `unsafeCheckOpNoThrow`는 deprecated라 `checkOpNoThrow`를 쓴다.
     */
    fun hasPermission(): Boolean = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
    ) == AppOpsManager.MODE_ALLOWED

    /** `ACTIVITY_RESUMED`만 패키지 이름을 담는다 */
    override fun query(beginAt: Long, endAt: Long): UsageQueryResult {
        if (!hasPermission()) return UsageQueryResult.NoPermission
        val usageEvents = try {
            usageStatsManager.queryEvents(beginAt, endAt)
        } catch (_: SecurityException) {
            // 확인과 조회 사이에 권한이 꺼진 경우
            return UsageQueryResult.NoPermission
        } ?: return UsageQueryResult.Events(emptyList())

        val event = UsageEvents.Event()
        val events = mutableListOf<RawEvent>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = rawEventTypeOf(event.eventType) ?: continue
            events += RawEvent(
                timestamp = event.timeStamp,
                type = type,
                packageName = if (type == RawEventType.ACTIVITY_RESUMED) event.packageName else null,
            )
        }
        return UsageQueryResult.Events(events.sortedBy { it.timestamp })
    }
}
