package com.jaeyun.tally.data.repository

import androidx.room.withTransaction
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.data.room.WeeklyGoal
import com.jaeyun.tally.data.room.toSessionStats
import com.jaeyun.tally.domain.baseline.BaselineCalculator
import com.jaeyun.tally.domain.model.WeekStats
import com.jaeyun.tally.util.weekRangeOf
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/** 기록 탭 판정 섹션이 보여 줄 목표 (§8.4) */
data class WeeklyGoals(
    /** 이번 주 목표. 기준선이 미활성이었거나 지난주에 통계 세션이 없었으면 null */
    val current: WeeklyGoal?,
    /** 지난주 목표를 달성했으면 그 목표(배지). 미달성·목표 없음은 null */
    val achievedLastWeek: WeeklyGoal?,
)

/**
 * 주간 목표 (§7.3, §9). 서버·백그라운드 작업 없이 기록 탭에 들어올 때 주 경계를 처리한다(PLAN.md §4).
 *
 * @param debugBuild debug 빌드인지. 기준선 완화 플래그는 debug 빌드에서만 효과가 있다 (§5.3)
 */
class WeeklyGoalRepository(
    private val database: TallyDatabase,
    private val settings: AppSettingsStore,
    private val debugBuild: Boolean,
) {
    private val goalDao = database.weeklyGoalDao()
    private val sessionDao = database.studySessionDao()

    /**
     * 1. 달성 여부를 아직 채우지 않은 지난 목표(지난주, 그리고 기록 탭을 열지 않아 밀린 주)를 그 주 값으로 채운다
     * 2. 이번 주 목표가 없으면 끝난 지난주 값과 지난주를 뺀 직전 4주 기준선으로 만들어 저장한다. 한 번 저장한 목표는 바꾸지 않는다
     */
    suspend fun refresh(now: Long, zone: ZoneId): WeeklyGoals {
        val thisWeek = weekRangeOf(now, zone)
        val lastWeek = weekRangeOf(thisWeek.startAt - 1, zone)
        val requiredSessions = BaselineCalculator.requiredSessions(settings.settings.first().relaxedBaseline, debugBuild)

        return database.withTransaction {
            for (goal in goalDao.getUnjudgedBefore(thisWeek.startAt)) {
                val week = weekRangeOf(goal.weekStartAt, zone)
                goalDao.upsert(goal.judged(weekStats(week.startAt, week.endAt)))
            }

            if (goalDao.getByWeek(thisWeek.startAt) == null) {
                val sessions = sessionDao
                    .getFinishedStartedBetween(BaselineCalculator.windowStartAt(lastWeek.startAt, zone), thisWeek.startAt)
                    .map { it.toSessionStats() }
                val baseline = BaselineCalculator.calculate(sessions, lastWeek.startAt, zone, requiredSessions)
                val lastWeekStats = WeekStats.of(sessions.filter { it.startAt in lastWeek })
                weeklyGoalFor(thisWeek.startAt, lastWeekStats, baseline)?.let { goalDao.insertIfAbsent(it) }
            }

            WeeklyGoals(
                current = goalDao.getByWeek(thisWeek.startAt),
                achievedLastWeek = achievedBadge(goalDao.getByWeek(lastWeek.startAt)),
            )
        }
    }

    private suspend fun weekStats(startAt: Long, endAt: Long): WeekStats? =
        WeekStats.of(sessionDao.getFinishedStartedBetween(startAt, endAt).map { it.toSessionStats() })
}
