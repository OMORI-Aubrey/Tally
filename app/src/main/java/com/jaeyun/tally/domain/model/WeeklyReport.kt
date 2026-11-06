package com.jaeyun.tally.domain.model

/**
 * 요약 문단(§7.4)에 쓸 한 주의 사실. 모두 통계에 셀 세션만 센다
 *
 * @param sittingSec 공부(착석, `T_total`) 시간 합
 * @param previousFocusSec 앞 주 순공. 앞 주에 통계 세션이 없으면 null
 * @param longestSec 그 주 세션 `LFS`의 최댓값
 * @param previousBestLfsSec 그 주보다 앞선 통계 세션 `LFS`의 최댓값. 앞선 세션이 없거나 그 기록이 0초뿐이면 null
 * @param interruptionCount 그 주 이탈 횟수의 합
 * @param distractApps 그 주 딴짓 앱별 이탈 횟수와 시간. 화면만 켠 구간은 앱이 아니라 들어 있지 않다
 */
data class WeeklyReportInput(
    val sessionCount: Int,
    val sittingSec: Int,
    val focusSec: Int,
    val previousFocusSec: Int?,
    val longestSec: Int,
    val previousBestLfsSec: Int?,
    val interruptionCount: Int,
    val distractApps: List<DistractApp>,
)

/**
 * 딴짓 앱 하나의 이탈. [count]는 딴짓 구간 수, [durationSec]는 그 시간의 합.
 * 같은 분류의 구간을 합칠 때 가장 오래 머문 앱을 대표로 남기므로(§6.2) 앱별 값은 근삿값이다
 */
data class DistractApp(val appLabel: String, val count: Int, val durationSec: Int)

/**
 * 요약 문단의 한 문장 (§7.4). 문장 하나가 수치 하나에서 나온다. 문구는 화면이 문자열 리소스로 만든다.
 * 좋다·나쁘다를 말하지 않고, 이탈은 동반 발생으로만 서술한다(§5.5).
 */
sealed interface ReportSentence {
    /** `세션 18회, 공부 27시간 30분 중 순공은 최대 21시간 24분이었어요` */
    data class Totals(val sessionCount: Int, val sittingSec: Int, val focusSec: Int) : ReportSentence

    /** `지난주보다 순공이 3시간 20분 늘었어요`. [diffSec]가 음수면 줄었고 0이면 같았다. 화면에 보이는 자릿수로 뺀 값이다 */
    data class FocusChange(val diffSec: Int) : ReportSentence

    /** `최장 구간 기록을 47분으로 갱신했어요` */
    data class NewRecord(val lfsSec: Int) : ReportSentence

    /** `이탈 31회 중 12회는 카카오톡에서 있었어요` */
    data class MainDistraction(val appLabel: String, val count: Int, val totalCount: Int) : ReportSentence
}
