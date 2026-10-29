package com.jaeyun.tally.probe

import com.jaeyun.tally.AppContainer
import com.jaeyun.tally.data.room.AppSegment
import com.jaeyun.tally.data.room.StudySession

/** 마지막으로 끝난 세션의 저장 결과 (#12). 세션 종료 파이프라인이 실기기에서 맞게 저장했는지 본다 */
data class LastSessionInfo(
    val session: StudySession,
    /** 시각 오름차순 */
    val segments: List<AppSegment>,
    /** 패키지 → 앱 이름 (분류표에 있는 앱만) */
    val labels: Map<String, String>,
) {
    val sessionMillis: Long get() = (session.endAt ?: session.startAt) - session.startAt
    val coveredMillis: Long get() = segments.sumOf { it.endAt - it.startAt }

    /** §6.2 불변식: 빈틈·겹침 없이 세션 전체를 덮는다 */
    val covers: Boolean
        get() = segments.isNotEmpty() &&
            segments.first().startAt == session.startAt &&
            segments.last().endAt == session.endAt &&
            segments.zipWithNext().all { (a, b) -> a.endAt == b.startAt } &&
            coveredMillis == sessionMillis
}

/** DB 조회라 IO 디스패처에서 부른다 */
internal suspend fun loadLastSession(container: AppContainer): LastSessionInfo? {
    val db = container.database
    val session = db.studySessionDao().getLatestFinished() ?: return null
    val segments = db.appSegmentDao().getBySession(session.id)
    val labels = db.appClassificationDao()
        .getByPackages(segments.map { it.packageName }.distinct())
        .associate { it.packageName to it.appLabel }
    return LastSessionInfo(session, segments, labels)
}
