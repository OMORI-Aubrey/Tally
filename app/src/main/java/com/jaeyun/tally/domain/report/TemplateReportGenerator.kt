package com.jaeyun.tally.domain.report

import com.jaeyun.tally.domain.model.DistractApp
import com.jaeyun.tally.domain.model.ReportSentence
import com.jaeyun.tally.domain.model.WeeklyReportInput

/**
 * 템플릿 요약 문단 (§7.4). 사실이 있는 문장만 이 순서로 고른다.
 *
 * 1. 총량: 세션 수, 공부, 순공 — 늘 쓴다
 * 2. 앞 주 대비 순공 증감 — 앞 주에 통계 세션이 있을 때. 화면에 보이는 자릿수로 견준다(같아 보이면 같았다)
 * 3. 최장 구간 기록 갱신 — 앞선 기록이 있고 그 주 최댓값이 더 길 때(첫 주는 모든 것이 기록이라 쓰지 않는다)
 * 4. 이탈이 가장 많았던 딴짓 앱 — 이탈이 있을 때. 횟수가 많은 앱, 같으면 시간이 긴 앱. "주로"라고 단정하지 않고 몇 회 중 몇 회인지 쓴다
 *
 * 판정 문구와 달리 좋다·나쁘다를 말하지 않는다. 늘었으면 늘었다고, 갱신했으면 갱신했다고만 쓴다.
 */
class TemplateReportGenerator : ReportGenerator {

    override suspend fun generate(input: WeeklyReportInput): List<ReportSentence> = buildList {
        add(ReportSentence.Totals(input.sessionCount, input.sittingSec, input.focusSec))
        input.previousFocusSec?.let { previous -> add(ReportSentence.FocusChange(shownSeconds(input.focusSec) - shownSeconds(previous))) }
        input.previousBestLfsSec?.let { best -> if (input.longestSec > best) add(ReportSentence.NewRecord(input.longestSec)) }
        mainDistraction(input)?.let(::add)
    }

    private fun mainDistraction(input: WeeklyReportInput): ReportSentence.MainDistraction? {
        if (input.interruptionCount == 0) return null
        val top = input.distractApps
            .sortedWith(compareByDescending<DistractApp> { it.count }.thenByDescending { it.durationSec })
            .firstOrNull() ?: return null
        // 앱별 횟수는 근삿값이라 합보다 크게 나오면 "31회 중 33회"가 되므로 쓰지 않는다
        if (top.count > input.interruptionCount) return null
        return ReportSentence.MainDistraction(top.appLabel, top.count, input.interruptionCount)
    }

    /** 화면에 보이는 자릿수의 초. 1분 이상은 분까지만 보인다(PLAN.md §4 시간 표기) */
    private fun shownSeconds(seconds: Int): Int = if (seconds < 60) seconds else seconds - seconds % 60
}
