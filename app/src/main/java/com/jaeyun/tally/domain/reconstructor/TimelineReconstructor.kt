package com.jaeyun.tally.domain.reconstructor

import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.PseudoPackage
import com.jaeyun.tally.domain.model.RawEvent
import com.jaeyun.tally.domain.model.RawEventType
import com.jaeyun.tally.domain.model.Segment

/**
 * @param segments `[sessionStart, sessionEnd]`를 빈틈·겹침 없이 덮는 구간. 인접한 두 구간의 분류는 항상 다르다
 * @param invariantViolation 불변식이 깨졌으면 그 내용, 아니면 null. 깨진 타임라인으로 계산한 `LFS`와 스트립은 틀리므로(§6.2.2)
 *   호출하는 쪽이 정책을 적용한다: debug 빌드는 예외, release 빌드는 로그를 남기고 `isValidForStats = false` (#12)
 */
data class ReconstructedTimeline(
    val segments: List<Segment>,
    val invariantViolation: String?,
)

/**
 * 세션 구간의 사용 기록 이벤트로 타임라인을 복원한다 (§6.2). Android 의존성 없는 순수 함수다.
 *
 * 1. 이벤트 수집: 세션 구간 안의 이벤트만 시각순으로
 * 2. 시작 구간 seed: 시작 버튼을 누른 본 앱이 이미 포그라운드라 RESUMED가 오지 않는다. seed가 없으면 첫 이벤트까지가 빠진다
 * 3. 구간 생성: 이벤트마다 상태가 바뀌면 구간을 끊는다. `KEYGUARD_HIDDEN`은 화면 켜짐(앱 없음) 구간일 때만
 *    마지막으로 RESUMED된 앱으로 전환한다(PLAN.md §4, #1 실측)
 * 4. 짧은 구간 흡수: 5초 미만(화면 켜짐 구간만 10초 미만)은 지우지 않고 앞 구간의 끝을 늘려 흡수한다.
 *    첫 구간이면 다음 구간의 시작을 당긴다
 * 5. 분류 태깅
 * 6. 같은 분류가 이어지면 병합. 4단계 뒤에 해야 "유튜브 → 홈 2초 → 유튜브"가 이탈 1회가 된다
 * 7. 불변식 검증
 */
object TimelineReconstructor {

    /** 일반 구간의 흡수 임계값 (PLAN.md P0-3) */
    const val ABSORB_MILLIS = 5_000L

    /** `__SCREEN_ON_UNKNOWN__` 구간의 흡수 임계값. 느린 잠금해제(최대 4.7초 실측)를 이탈로 잡지 않는다 (§6.2.1) */
    const val ABSORB_SCREEN_ON_MILLIS = 10_000L

    /**
     * @param selfPackage 본 앱 패키지. seed 구간과 `lastResumed`의 초깃값
     * @param classify 앱 패키지 → `DISTRACT` 또는 `ALLOWED`. 의사 패키지로는 부르지 않는다
     */
    fun reconstruct(
        events: List<RawEvent>,
        sessionStart: Long,
        sessionEnd: Long,
        selfPackage: String,
        classify: (packageName: String) -> Category,
    ): ReconstructedTimeline {
        require(sessionEnd >= sessionStart) { "sessionEnd($sessionEnd) < sessionStart($sessionStart)" }

        val inSession = events
            .filter { it.timestamp in sessionStart..sessionEnd }
            .sortedBy { it.timestamp }
        val drafts = buildDrafts(inSession, sessionStart, sessionEnd, selfPackage)
        absorbShort(drafts)
        val segments = mergeSameCategory(drafts.map { it to categoryOf(it.packageName, classify) })
        return ReconstructedTimeline(segments, checkInvariant(segments, sessionStart, sessionEnd))
    }

    /** 2·3단계. 루프 내내 current는 열려 있다 */
    private fun buildDrafts(events: List<RawEvent>, sessionStart: Long, sessionEnd: Long, selfPackage: String): MutableList<Draft> {
        val drafts = mutableListOf<Draft>()
        var current = Draft(selfPackage, sessionStart, sessionStart)
        var lastResumed = selfPackage
        for (event in events) {
            val pkg = when (event.type) {
                RawEventType.ACTIVITY_RESUMED -> event.packageName?.also { lastResumed = it } ?: continue
                RawEventType.SCREEN_NON_INTERACTIVE -> PseudoPackage.SCREEN_OFF
                RawEventType.SCREEN_INTERACTIVE -> PseudoPackage.SCREEN_ON_UNKNOWN
                // 잠금이 풀리면 잠금화면이 아니라 마지막으로 RESUMED된 앱을 보고 있다. 화면 켜짐 구간이 아니면 무시한다
                RawEventType.KEYGUARD_HIDDEN ->
                    if (current.packageName == PseudoPackage.SCREEN_ON_UNKNOWN) lastResumed else continue
            }
            if (pkg == current.packageName) continue
            current.endAt = event.timestamp
            drafts += current
            current = Draft(pkg, event.timestamp, event.timestamp)
        }
        current.endAt = sessionEnd
        drafts += current
        return drafts
    }

    /** 4단계. 지우는 대신 이웃의 경계를 늘려 인접성을 지킨다 */
    private fun absorbShort(drafts: MutableList<Draft>) {
        var i = 0
        while (i < drafts.size && drafts.size > 1) {
            val draft = drafts[i]
            if (draft.durationMillis >= absorbThreshold(draft.packageName)) {
                i++
                continue
            }
            if (i > 0) {
                drafts[i - 1].endAt = draft.endAt
            } else {
                drafts[1].startAt = draft.startAt
            }
            drafts.removeAt(i)
            // 첫 구간을 흡수했으면 새 첫 구간도 다시 본다. 앞 구간으로 흡수했으면 i는 이미 다음 구간을 가리킨다
        }
    }

    private fun absorbThreshold(packageName: String): Long =
        if (packageName == PseudoPackage.SCREEN_ON_UNKNOWN) ABSORB_SCREEN_ON_MILLIS else ABSORB_MILLIS

    /** 5단계 */
    private fun categoryOf(packageName: String, classify: (String) -> Category): Category = when (packageName) {
        PseudoPackage.SCREEN_OFF -> Category.SCREEN_OFF
        PseudoPackage.SCREEN_ON_UNKNOWN -> Category.DISTRACT
        else -> classify(packageName)
    }

    /** 6단계. 합친 구간의 패키지는 그 안에서 가장 오래 머문 패키지(같으면 먼저 나온 쪽) */
    private fun mergeSameCategory(tagged: List<Pair<Draft, Category>>): List<Segment> {
        val runs = mutableListOf<MutableList<Pair<Draft, Category>>>()
        for (item in tagged) {
            val last = runs.lastOrNull()
            if (last != null && last.last().second == item.second) last += item else runs += mutableListOf(item)
        }
        return runs.map { run ->
            val durations = linkedMapOf<String, Long>()
            run.forEach { (draft, _) -> durations[draft.packageName] = (durations[draft.packageName] ?: 0L) + draft.durationMillis }
            val dominant = durations.entries.fold(durations.entries.first()) { best, e -> if (e.value > best.value) e else best }.key
            Segment(
                packageName = dominant,
                category = run.first().second,
                startAt = run.first().first.startAt,
                endAt = run.last().first.endAt,
            )
        }
    }

    /**
     * 7단계. 구간이 `[sessionStart, sessionEnd]`를 빈틈·겹침 없이 덮는지 본다 (§6.2, §14.2 `assertInvariant`).
     * 깨졌으면 무엇이 깨졌는지 돌려준다.
     */
    fun checkInvariant(segments: List<Segment>, sessionStart: Long, sessionEnd: Long): String? {
        if (segments.isEmpty()) return "구간이 없다"
        if (segments.first().startAt != sessionStart) return "첫 구간 시작 ${segments.first().startAt} ≠ 세션 시작 $sessionStart"
        if (segments.last().endAt != sessionEnd) return "마지막 구간 끝 ${segments.last().endAt} ≠ 세션 끝 $sessionEnd"
        segments.forEachIndexed { i, segment ->
            if (segment.endAt < segment.startAt) return "${i}번 구간 길이가 음수"
        }
        segments.zipWithNext().forEachIndexed { i, (a, b) ->
            if (a.endAt != b.startAt) return "${i}번과 ${i + 1}번 구간 사이 ${if (a.endAt < b.startAt) "빈틈" else "겹침"}"
        }
        val covered = segments.sumOf { it.durationMillis }
        if (covered != sessionEnd - sessionStart) return "구간 길이 합 $covered ≠ 세션 길이 ${sessionEnd - sessionStart}"
        return null
    }

    private class Draft(val packageName: String, var startAt: Long, var endAt: Long) {
        val durationMillis: Long get() = endAt - startAt
    }
}
