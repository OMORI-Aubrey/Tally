package com.jaeyun.tally.probe

import com.jaeyun.tally.AppContainer
import com.jaeyun.tally.data.room.countsInStats
import com.jaeyun.tally.util.WeekRange
import com.jaeyun.tally.util.weekRangeOf
import java.time.ZoneId

/** 이번 주 체감 질문 상태 (#14). 실기기에서 주 1회 트리거를 확인하고, 지워서 다시 시험한다 */
data class PerceivedWeekInfo(
    val week: WeekRange,
    /** 마지막으로 물은 세션의 시작 시각 */
    val lastAskedAt: Long?,
    /** 통계 포함·타임라인 있는 이번 주 세션 수. 다음 세션이 후보라면 (이 값 + 1)번째다 */
    val candidates: Int,
    val answered: Int,
) {
    val askedThisWeek: Boolean get() = (lastAskedAt != null && lastAskedAt in week) || answered > 0
}

/** DB·DataStore 조회라 IO 디스패처에서 부른다 */
internal suspend fun loadPerceivedWeek(container: AppContainer, now: Long): PerceivedWeekInfo {
    val week = weekRangeOf(now, ZoneId.systemDefault())
    val repository = container.perceivedFocusRepository
    val sessions = repository.finishedStartedBetween(week.startAt, week.endAt)
    return PerceivedWeekInfo(
        week = week,
        lastAskedAt = repository.lastAskedAt(),
        candidates = sessions.count { it.countsInStats },
        answered = sessions.count { it.perceivedFocusMin != null },
    )
}

/** 이번 주 세션의 체감 답과 물은 기록을 지운다. 다음 세션부터 다시 후보가 된다 */
internal suspend fun clearPerceivedWeek(container: AppContainer, now: Long) {
    val week = weekRangeOf(now, ZoneId.systemDefault())
    val dao = container.database.studySessionDao()
    dao.getFinishedStartedBetween(week.startAt, week.endAt).forEach { dao.setPerceivedFocus(it.id, null) }
    container.settingsStore.setPerceivedAskedAt(null)
}
