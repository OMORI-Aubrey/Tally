package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.AppSegment
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.domain.metrics.MetricsCalculator
import com.jaeyun.tally.domain.metrics.MetricsVersion
import com.jaeyun.tally.domain.metrics.SessionValidity
import com.jaeyun.tally.domain.reconstructor.ReconstructedTimeline

/** 종료된 세션과 저장할 구간 */
internal data class CompletedSession(val session: StudySession, val segments: List<AppSegment>)

/**
 * 세션 종료 시 저장할 값을 만든다 (§9). DB·사용 기록 조회는 [SessionRepository]가 하고, 여기서는 계산만 한다.
 *
 * - [timeline]이 null(사용 기록 권한 없음)이면 타임라인 없이 착석만 저장한다(`hasTimeline = false`, §6.3 #1).
 *   지표는 계산하지 않았으므로 `metricsVersion = NOT_COMPUTED`로 둔다
 * - 타임라인이 있으면 지표 6개를 계산해 `metricsVersion = CURRENT`로 저장하고 구간을 함께 돌려준다
 * - 복원 불변식이 깨졌으면 [strictInvariant](debug 빌드)일 때 예외를 던진다. 아니면 [onViolation]으로 알리고 통계에서 뺀다.
 *   깨진 타임라인으로 계산한 `LFS`와 스트립은 틀리기 때문이다(§6.2.2)
 * - 통계 포함 여부는 세션 길이(종료 − 시작)로 정한다(§6.3 #3·#4)
 */
internal fun completeSession(
    session: StudySession,
    now: Long,
    timeline: ReconstructedTimeline?,
    strictInvariant: Boolean,
    onViolation: (String) -> Unit,
): CompletedSession {
    val sessionSec = ((now - session.startAt) / 1000).coerceAtLeast(0).toInt()
    val withinLength = SessionValidity.isValidForStats(sessionSec)

    if (timeline == null) {
        return CompletedSession(
            session = session.copy(
                endAt = now,
                tTotalSec = sessionSec,
                isValidForStats = withinLength,
                hasTimeline = false,
                metricsVersion = MetricsVersion.NOT_COMPUTED,
            ),
            segments = emptyList(),
        )
    }

    val violation = timeline.invariantViolation
    if (violation != null) {
        val message = "세션 ${session.id} 타임라인 불변식 위반: $violation"
        check(!strictInvariant) { message }
        onViolation(message)
    }

    val metrics = MetricsCalculator.calculate(timeline.segments, session.startAt, now)
    return CompletedSession(
        session = session.copy(
            endAt = now,
            tTotalSec = metrics.tTotalSec,
            tFocusSec = metrics.tFocusSec,
            tDistSec = metrics.tDistSec,
            lfsSec = metrics.lfsSec,
            densityPct = metrics.densityPct,
            interruptionCount = metrics.interruptionCount,
            metricsVersion = MetricsVersion.CURRENT,
            isValidForStats = withinLength && violation == null,
            hasTimeline = true,
        ),
        segments = timeline.segments.map {
            AppSegment(
                sessionId = session.id,
                packageName = it.packageName,
                category = it.category,
                startAt = it.startAt,
                endAt = it.endAt,
            )
        },
    )
}
