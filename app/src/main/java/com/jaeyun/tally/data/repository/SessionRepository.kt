package com.jaeyun.tally.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.AppSegment
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.data.room.StudySession
import com.jaeyun.tally.data.room.TallyDatabase
import com.jaeyun.tally.data.usagestats.AppAutoClassifier
import com.jaeyun.tally.data.usagestats.UsageEventQuery
import com.jaeyun.tally.data.usagestats.UsageQueryResult
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.RawEvent
import com.jaeyun.tally.domain.model.RawEventType
import com.jaeyun.tally.domain.reconstructor.TimelineReconstructor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** 세션 리포트(§8.2)를 그릴 재료. 판정·기준선은 들어 있지 않다 */
data class SessionReportData(
    val session: StudySession,
    /** 시각 오름차순 */
    val segments: List<AppSegment>,
    /** 구간에 나온 앱의 분류(패키지 → 분류). 앱 이름과 사용자가 정했는지를 본다 */
    val classifications: Map<String, AppClassification>,
    /** 이 세션을 뺀, 통계에 셀 세션의 최장 구간 최댓값. 그런 세션이 없으면 null */
    val bestOtherLfsSec: Int?,
)

/** 리포트에서 앱 분류를 정한 결과 */
enum class ReclassifyResult {
    /** 분류가 바뀌어 이 세션의 타임라인과 지표를 다시 계산했다 */
    RECOMPUTED,

    /** 분류가 그대로라 사용자 분류로 확정만 했다 */
    UNCHANGED,

    /** 분류는 저장했지만 이 세션은 다시 계산하지 못했다. 사용 기록이 지워졌거나 권한이 없다. 다음 세션부터 반영된다 */
    SAVED_ONLY,
}

/**
 * 세션 시작·종료 (§6.3 #5). 진행 중 세션은 동시에 하나만 둔다.
 *
 * 종료하면 사용 기록을 한 번 조회해 타임라인을 복원하고 지표를 계산해 저장한다(§3.3, #12).
 * 리포트에서 앱 분류를 바꾸면 그 세션을 다시 조회해 다시 계산한다(§5.1, PLAN.md §4 재분류 소급 범위).
 *
 * @param selfPackage 본 앱 패키지. 복원의 seed 구간이고 분류는 항상 허용이다
 * @param strictInvariant 복원 불변식이 깨지면 예외를 던질지. debug 빌드에서 켠다
 */
class SessionRepository(
    private val database: TallyDatabase,
    private val usageEvents: UsageEventQuery,
    private val classifier: AppAutoClassifier,
    private val selfPackage: String,
    private val strictInvariant: Boolean,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val dao = database.studySessionDao()
    private val segmentDao = database.appSegmentDao()
    private val classificationDao = database.appClassificationDao()

    /** 진행 중 세션. 없으면 null */
    val runningSession: Flow<StudySession?> = dao.observeUnfinished()

    /** 끝난 세션 전체. 최근에 시작한 순서 */
    val finishedSessions: Flow<List<StudySession>> = dao.observeFinished()

    /**
     * 시작 즉시 저장해 앱이 강제 종료·재부팅돼도 시작 시각이 남게 한다.
     * 이미 진행 중인 세션이 있으면(버튼 연타 등) 새로 만들지 않고 그 세션 id를 돌려준다.
     */
    suspend fun start(subjectName: String?, now: Long): Long = database.withTransaction {
        dao.getUnfinished()?.id
            ?: dao.insert(StudySession(subjectName = subjectName, startAt = now, createdAt = now))
    }

    /**
     * 세션을 끝낸다.
     * 1. 사용 기록 조회(1회) → 2. 새로 본 앱의 자동 분류 저장 → 3. 타임라인 복원 → 4. 지표 계산 →
     * 5. 세션 지표와 구간을 한 트랜잭션으로 저장
     *
     * 사용 기록 권한이 없으면 착석만 저장한다(`hasTimeline = false`). 조회와 분류는 오래 걸릴 수 있어 트랜잭션 밖에서 하고,
     * 저장 직전에 아직 진행 중인지 다시 확인한다. 이미 끝났거나 없는 세션이면 아무것도 바꾸지 않고 null(종료 버튼 연타 등).
     */
    suspend fun finish(sessionId: Long, now: Long): StudySession? {
        val session = dao.getById(sessionId)?.takeIf { it.endAt == null } ?: return null

        val timeline = withContext(ioDispatcher) {
            when (val result = usageEvents.query(session.startAt, now)) {
                UsageQueryResult.NoPermission -> null
                is UsageQueryResult.Events -> reconstruct(result.events, session.startAt, now, classificationsFor(result.events))
            }
        }
        val completed = completeSession(session, now, timeline, strictInvariant) { Log.w(TAG, it) }

        return database.withTransaction {
            val current = dao.getById(sessionId)
            if (current == null || current.endAt != null) return@withTransaction null
            dao.update(completed.session)
            segmentDao.deleteBySession(sessionId)
            segmentDao.insertAll(completed.segments)
            completed.session
        }
    }

    /** 리포트 재료. 끝나지 않았거나 없는 세션이면 null */
    suspend fun loadReport(sessionId: Long): SessionReportData? {
        val session = dao.getById(sessionId)?.takeIf { it.endAt != null } ?: return null
        val segments = segmentDao.getBySession(sessionId)
        val classifications = classificationDao.getByPackages(segments.map { it.packageName }.distinct())
            .associateBy { it.packageName }
        return SessionReportData(session, segments, classifications, dao.getBestLfsExcept(sessionId))
    }

    /**
     * 리포트에서 앱 분류를 정한다(미분류 앱 질문 #15, 이탈 목록 길게 누르기 #16). 분류는 사용자 분류(`USER`)로 저장한다.
     *
     * 분류가 바뀌면 그 세션의 사용 기록을 다시 조회해 타임라인과 지표를 다시 계산한다. 합쳐 둔 구간으로는 다시 나눌 수
     * 없어서다. 다시 조회한 기록을 지금까지의 분류로 복원해 저장된 구간과 같을 때만 계산한다 — 오래돼 기록이 지워졌거나
     * 일부만 남았으면 엉뚱한 숫자로 덮어쓰지 않고 분류만 저장한다([ReclassifyResult.SAVED_ONLY]).
     * 다른 세션은 다시 계산하지 않는다.
     */
    suspend fun reclassify(sessionId: Long, packageName: String, category: Category): ReclassifyResult {
        val existing = classificationDao.get(packageName)
        val userRow = AppClassification(packageName, existing?.appLabel ?: packageName, category, ClassificationSource.USER)
        // 분류표에 없는 앱은 없어야 하지만, 생긴다면 자동 분류 기본값(딴짓)으로 복원된 셈이다
        if ((existing?.category ?: Category.DISTRACT) == category) {
            classificationDao.upsert(userRow)
            return ReclassifyResult.UNCHANGED
        }

        val session = dao.getById(sessionId)
        val endAt = session?.endAt
        val result = if (session == null || endAt == null || !session.hasTimeline) {
            null
        } else {
            withContext(ioDispatcher) { usageEvents.query(session.startAt, endAt) }
        }
        if (session == null || endAt == null || result !is UsageQueryResult.Events) {
            classificationDao.upsert(userRow)
            return ReclassifyResult.SAVED_ONLY
        }

        val before = storedClassifications(result.events)
        val replayed = reconstruct(result.events, session.startAt, endAt, before)
        if (!sameTimeline(replayed, segmentDao.getBySession(sessionId))) {
            classificationDao.upsert(userRow)
            return ReclassifyResult.SAVED_ONLY
        }
        val after: (String) -> Category = { pkg -> if (pkg == packageName) category else before(pkg) }
        val completed = completeSession(session, endAt, reconstruct(result.events, session.startAt, endAt, after), strictInvariant) {
            Log.w(TAG, it)
        }
        database.withTransaction {
            classificationDao.upsert(userRow)
            dao.update(completed.session)
            segmentDao.deleteBySession(sessionId)
            segmentDao.insertAll(completed.segments)
        }
        return ReclassifyResult.RECOMPUTED
    }

    private fun reconstruct(events: List<RawEvent>, startAt: Long, endAt: Long, classify: (String) -> Category) =
        TimelineReconstructor.reconstruct(
            events = events,
            sessionStart = startAt,
            sessionEnd = endAt,
            selfPackage = selfPackage,
            classify = classify,
        )

    /** 분류표에 저장된 분류 그대로. 자동 분류를 새로 하지 않는다(종료 때 쓴 분류를 재현한다) */
    private suspend fun storedClassifications(events: List<RawEvent>): (String) -> Category {
        val categories = classificationDao.getByPackages(resumedPackages(events)).associate { it.packageName to it.category }
        return { pkg -> if (pkg == selfPackage) Category.ALLOWED else categories[pkg] ?: Category.DISTRACT }
    }

    /** 사용 기록에서 포그라운드로 온 앱. 본 앱은 뺀다 */
    private fun resumedPackages(events: List<RawEvent>): List<String> = events
        .filter { it.type == RawEventType.ACTIVITY_RESUMED }
        .mapNotNull { it.packageName }
        .distinct()
        .filter { it != selfPackage }

    /**
     * 세션에 나온 앱의 분류를 정한다. 사용자가 정한 분류(`USER`)는 그대로 쓰고, 나머지는 지금 규칙으로 다시 자동 분류해
     * 저장한다 — 자동 분류 규칙이나 앱 이름이 바뀌어도 반영되게 한다. 본 앱은 저장하지 않고 항상 허용이다.
     */
    private suspend fun classificationsFor(events: List<RawEvent>): (String) -> Category {
        val packages = resumedPackages(events)
        if (packages.isEmpty()) return { Category.ALLOWED }

        val existing = classificationDao.getByPackages(packages).associateBy { it.packageName }
        val auto = classifier.classifyAll(packages.filter { existing[it]?.source != ClassificationSource.USER })
            .map { AppClassification(it.packageName, it.appLabel, it.category, ClassificationSource.AUTO_CATEGORY) }
        classificationDao.upsertAll(auto)

        val categories = existing.mapValues { it.value.category } + auto.associate { it.packageName to it.category }
        // 분류표에 없는 앱은 없어야 하지만, 생긴다면 자동 분류 기본값(딴짓)을 따른다 (PLAN.md §4)
        return { pkg -> if (pkg == selfPackage) Category.ALLOWED else categories[pkg] ?: Category.DISTRACT }
    }

    private companion object {
        const val TAG = "Tally"
    }
}
