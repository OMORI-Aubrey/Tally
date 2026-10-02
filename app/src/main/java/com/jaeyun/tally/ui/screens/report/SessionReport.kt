package com.jaeyun.tally.ui.screens.report

import com.jaeyun.tally.data.repository.SessionReportData
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.data.room.countsInStats
import com.jaeyun.tally.domain.metrics.MetricsCalculator
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.PseudoPackage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * 세션 리포트 한 장 (§8.2). 화면은 위에서 아래로 그리기만 한다. 판정·목표는 주 단위라 들어 있지 않다.
 * 2번 타임라인 스트립은 #17에서 붙는다.
 */
data class SessionReport(
    /** ⓪ 체감을 적었고 실측과 견줄 수 있을 때만 */
    val gap: GapHeadline?,
    /** ① 타임라인이 없으면(사용 기록 권한 없이 끝낸 세션) null */
    val structure: StructureSummary?,
    /** ③ */
    val totals: Totals,
    /** ④ 타임라인이 없으면 null */
    val cards: MetricCards?,
    /** ⑤ 시각 순. 이탈이 없으면 비어 있다 */
    val distractions: List<Distraction>,
    /** ⑥ 아직 사용자가 정하지 않은 딴짓 앱. 세션당 최대 2개 */
    val unclassified: List<UnclassifiedApp>,
    /** ⑦ */
    val meta: SessionMeta,
)

/** 체감과 실측의 격차. [gapPct]는 `G`를 반올림한 값이고 양수가 과대평가다(§5.2) */
data class GapHeadline(val perceivedMin: Int, val focusSec: Int, val gapPct: Int, val lfsSec: Int)

/**
 * 구조 헤드라인. 조각 수는 `N_int + 1`이다.
 * @param lfsSharePct 착석 대비 최장 구간 비율(버림). 0%로 떨어지거나 착석이 0이면 null(0을 보이지 않는다)
 */
data class StructureSummary(
    val lfsSec: Int,
    val interruptionCount: Int,
    val sittingSec: Int,
    val lfsSharePct: Int?,
    val record: RecordMark?,
) {
    val pieces: Int get() = interruptionCount + 1
}

/** 다른 세션과 견준 최장 구간 기록 */
sealed interface RecordMark {
    /** 다른 어떤 세션보다 길다 */
    data object NewRecord : RecordMark

    /** 기록은 다른 세션에 있다 */
    data class Best(val lfsSec: Int) : RecordMark
}

/** 총량. 타임라인이 없으면 순공·딴짓을 알 수 없어 null */
data class Totals(val focusSec: Int?, val sittingSec: Int, val distractSec: Int?)

/** 지표 카드. 밀도가 0%로 떨어지면(세션 내내 딴짓) 0을 보이지 않도록 [densityPct]가 null */
data class MetricCards(val densityPct: Int?, val lengthSec: Int)

/** 이탈 한 번. [appLabel]이 null이면 화면만 켜고 앱은 열지 않은 구간이다(`화면 켜짐 (앱 없음)`) */
data class Distraction(val startTime: LocalTime, val appLabel: String?, val durationSec: Int)

data class UnclassifiedApp(val packageName: String, val appLabel: String)

/** 세션 메타. [excludedFromStats]면 통계에 넣지 않는 세션임을 함께 알린다 */
data class SessionMeta(
    val subjectName: String?,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val excludedFromStats: Boolean,
)

/** 미분류 앱 질문은 세션당 이만큼만 (§5.1) */
internal const val MAX_UNCLASSIFIED_PROMPTS = 2

/**
 * 저장된 세션·구간·분류로 리포트를 만든다.
 *
 * - 격차: 체감을 적었고 타임라인이 있고 `G`가 정의될 때(순공이 0이 아닐 때)만
 * - 기록: 통계에 셀 세션만 견준다. 다른 세션이 없으면(첫 세션) 기록 줄을 두지 않는다
 * - 숫자가 0으로 떨어지는 줄(비율 0%, 밀도 0%, 기록 0초)은 0을 쓰지 않고 감춘다(§8.5.1)
 * - 미분류 앱: 이번 세션에 딴짓으로 잡혔고 사용자가 아직 정하지 않은(자동 분류) 앱을 딴짓 시간이 긴 순서로 2개까지.
 *   나머지는 다음 세션으로 넘어가고 그동안 자동 분류를 그대로 쓴다(§5.1)
 */
internal fun buildSessionReport(data: SessionReportData, zone: ZoneId): SessionReport {
    val session = data.session
    val endAt = session.endAt ?: session.startAt
    val timeline = session.hasTimeline

    val gap = session.perceivedFocusMin?.takeIf { timeline }?.let { perceived ->
        MetricsCalculator.perceptionGapPct(perceived, session.tFocusSec)?.let { g ->
            GapHeadline(perceived, session.tFocusSec, g.roundToInt(), session.lfsSec)
        }
    }

    // 다른 세션의 기록이 0초뿐이면 "기록 0초"를 쓰지 않도록 견줄 기록이 없는 것으로 본다
    val bestOther = data.bestOtherLfsSec?.takeIf { it > 0 }
    val record = when {
        !session.countsInStats || bestOther == null -> null
        session.lfsSec > bestOther -> RecordMark.NewRecord
        else -> RecordMark.Best(bestOther)
    }
    val structure = if (timeline) {
        StructureSummary(
            lfsSec = session.lfsSec,
            interruptionCount = session.interruptionCount,
            sittingSec = session.tTotalSec,
            lfsSharePct = if (session.tTotalSec > 0) (session.lfsSec * 100 / session.tTotalSec).takeIf { it > 0 } else null,
            record = record,
        )
    } else {
        null
    }

    val distractSegments = data.segments.filter { it.category == Category.DISTRACT }
    val distractions = distractSegments.map { segment ->
        Distraction(
            startTime = Instant.ofEpochMilli(segment.startAt).atZone(zone).toLocalTime(),
            appLabel = if (segment.packageName == PseudoPackage.SCREEN_ON_UNKNOWN) {
                null
            } else {
                data.classifications[segment.packageName]?.appLabel ?: segment.packageName
            },
            durationSec = ((segment.endAt - segment.startAt) / 1000).toInt(),
        )
    }
    val unclassified = distractSegments
        .filter { it.packageName != PseudoPackage.SCREEN_ON_UNKNOWN }
        .filter { data.classifications[it.packageName]?.source != ClassificationSource.USER }
        .groupBy { it.packageName }
        .entries
        .sortedByDescending { (_, segments) -> segments.sumOf { it.endAt - it.startAt } }
        .take(MAX_UNCLASSIFIED_PROMPTS)
        .map { (pkg, _) -> UnclassifiedApp(pkg, data.classifications[pkg]?.appLabel ?: pkg) }

    val start = Instant.ofEpochMilli(session.startAt).atZone(zone)
    return SessionReport(
        gap = gap,
        structure = structure,
        totals = Totals(
            focusSec = if (timeline) session.tFocusSec else null,
            sittingSec = session.tTotalSec,
            distractSec = if (timeline) session.tDistSec else null,
        ),
        cards = if (timeline) {
            MetricCards(
                densityPct = session.densityPct.toInt().takeIf { it > 0 },
                lengthSec = ((endAt - session.startAt) / 1000).toInt(),
            )
        } else {
            null
        },
        distractions = distractions,
        unclassified = unclassified,
        meta = SessionMeta(
            subjectName = session.subjectName,
            date = start.toLocalDate(),
            startTime = start.toLocalTime(),
            endTime = Instant.ofEpochMilli(endAt).atZone(zone).toLocalTime(),
            excludedFromStats = !session.isValidForStats,
        ),
    )
}
