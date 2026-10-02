package com.jaeyun.tally.probe

import android.app.usage.UsageEvents
import android.content.res.Resources
import com.jaeyun.tally.R
import com.jaeyun.tally.data.usagestats.AutoCategoryReason
import com.jaeyun.tally.data.usagestats.AutoClassification
import com.jaeyun.tally.data.room.AppSegment
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.domain.model.PseudoPackage
import com.jaeyun.tally.util.formatElapsedClock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 화면과 복사용 텍스트가 같은 문장을 쓰도록 모은다 */
internal object ProbeText {

    /** 본 앱 패키지. debug 빌드도 applicationIdSuffix를 쓰지 않는다 */
    private const val SELF_PACKAGE = "com.jaeyun.tally"

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val countedTypes = listOf(
        UsageEvents.Event.ACTIVITY_RESUMED,
        UsageEvents.Event.SCREEN_INTERACTIVE,
        UsageEvents.Event.SCREEN_NON_INTERACTIVE,
        UsageEvents.Event.KEYGUARD_HIDDEN,
    )

    fun time(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

    fun seconds(res: Resources, millis: Long): String =
        res.getString(R.string.probe_seconds, millis / 1000f)

    fun range(res: Resources, result: ProbeResult): String =
        res.getString(R.string.probe_range, time(result.beginAt), time(result.endAt), result.rows.size)

    fun latest(res: Resources, result: ProbeResult): String {
        val latest = result.latest ?: return res.getString(R.string.probe_latest_none)
        return res.getString(
            R.string.probe_latest,
            time(latest.event.timestamp),
            eventTypeName(latest.event.type),
            seconds(res, result.endAt - latest.event.timestamp),
        )
    }

    fun selfCheck(res: Resources, result: ProbeResult): String? {
        if (result.latest == null) return null
        return res.getString(
            if (result.selfResumedIsLatest) R.string.probe_self_resumed_ok else R.string.probe_self_resumed_missing,
        )
    }

    fun typeCounts(res: Resources, result: ProbeResult): String =
        countedTypes.joinToString(" · ") { type ->
            res.getString(R.string.probe_type_count, eventTypeName(type), result.typeCounts[type] ?: 0)
        }

    fun statsLines(res: Resources, stats: ScreenOnStats): List<String> = buildList {
        addAll(durationLines(res, res.getString(R.string.probe_rule_current), stats.current))
        addAll(durationLines(res, res.getString(R.string.probe_rule_keyguard), stats.keyguardAware))
        add(res.getString(R.string.probe_screen_on_glance, stats.screenOffCount))
    }

    /** 샘플이 없으면 빈 목록 */
    private fun durationLines(res: Resources, rule: String, stats: DurationStats): List<String> {
        if (stats.medianMs == null || stats.maxMs == null) return emptyList()
        return listOf(
            res.getString(R.string.probe_screen_on_stats, rule, stats.count, seconds(res, stats.medianMs), seconds(res, stats.maxMs)),
            res.getString(R.string.probe_screen_on_over, stats.atLeast5sCount, stats.atLeast10sCount),
        )
    }

    fun sample(res: Resources, sample: ScreenOnSample, labels: Map<String, String>): String {
        val outcome = when (sample.outcome) {
            ScreenOnOutcome.APP_RESUMED -> res.getString(
                R.string.probe_sample_app,
                seconds(res, sample.durationMs),
                sample.resumedPackage?.let { labels[it] ?: it }.orEmpty(),
            )

            ScreenOnOutcome.SCREEN_OFF -> res.getString(R.string.probe_sample_off, seconds(res, sample.durationMs))
            ScreenOnOutcome.NO_EVENT -> res.getString(R.string.probe_sample_none, seconds(res, sample.durationMs))
        }
        val keyguard = sample.keyguardHiddenMs
            ?.let { " · " + res.getString(R.string.probe_sample_keyguard, seconds(res, it)) }
            .orEmpty()
        return "${time(sample.screenOnAt)} $outcome$keyguard"
    }

    fun eventTitle(row: ProbeRow): String =
        "${time(row.event.timestamp)}  ${eventTypeName(row.event.type)}"

    fun eventDetail(res: Resources, row: ProbeRow): String {
        val tags = buildList {
            if (row.isLauncher) add(res.getString(R.string.probe_tag_launcher))
            if (row.isSelf) add(res.getString(R.string.probe_tag_self))
        }.joinToString("") { " [$it]" }
        return "${row.label} (${row.event.packageName})$tags"
    }

    fun classification(res: Resources, item: AutoClassification): String = res.getString(
        R.string.probe_classification_row,
        item.appLabel,
        res.getString(if (item.category == Category.DISTRACT) R.string.probe_category_distract else R.string.probe_category_allowed),
        res.getString(
            when (item.reason) {
                AutoCategoryReason.SELF -> R.string.probe_reason_self
                AutoCategoryReason.LAUNCHER -> R.string.probe_reason_launcher
                AutoCategoryReason.STUDY_APP -> R.string.probe_reason_study
                AutoCategoryReason.BROWSER -> R.string.probe_reason_browser
                AutoCategoryReason.PHONE -> R.string.probe_reason_phone
                AutoCategoryReason.DISTRACT_CATEGORY -> R.string.probe_reason_category
                AutoCategoryReason.SYSTEM -> R.string.probe_reason_system
                AutoCategoryReason.INSTALLED -> R.string.probe_reason_installed
            },
        ),
        item.packageName,
    )

    fun lastSessionLines(res: Resources, info: LastSessionInfo): List<String> = buildList {
        val session = info.session
        add(
            res.getString(
                R.string.probe_last_session_header,
                time(session.startAt),
                time(session.endAt ?: session.startAt),
                formatElapsedClock(info.sessionMillis),
                res.getString(if (session.hasTimeline) R.string.probe_timeline_yes else R.string.probe_timeline_no),
                session.metricsVersion,
                res.getString(if (session.isValidForStats) R.string.probe_stats_in else R.string.probe_stats_out),
            ),
        )
        add(
            res.getString(
                R.string.probe_last_session_metrics,
                formatElapsedClock(session.tTotalSec * 1000L),
                formatElapsedClock(session.tFocusSec * 1000L),
                formatElapsedClock(session.tDistSec * 1000L),
                formatElapsedClock(session.lfsSec * 1000L),
                session.interruptionCount,
                session.densityPct,
            ),
        )
        if (session.hasTimeline) {
            add(
                if (info.covers) {
                    res.getString(R.string.probe_coverage_ok, info.segments.size, formatElapsedClock(info.coveredMillis))
                } else {
                    res.getString(
                        R.string.probe_coverage_bad,
                        info.segments.size,
                        formatElapsedClock(info.coveredMillis),
                        formatElapsedClock(info.sessionMillis),
                    )
                },
            )
        }
    }

    fun segment(res: Resources, segment: AppSegment, labels: Map<String, String>): String {
        val name = when (segment.packageName) {
            PseudoPackage.SCREEN_OFF -> res.getString(R.string.probe_pkg_screen_off)
            PseudoPackage.SCREEN_ON_UNKNOWN -> res.getString(R.string.probe_pkg_screen_on)
            SELF_PACKAGE -> res.getString(R.string.probe_tag_self)
            else -> labels[segment.packageName] ?: segment.packageName
        }
        val category = res.getString(
            when (segment.category) {
                Category.DISTRACT -> R.string.probe_category_distract
                Category.ALLOWED -> R.string.probe_category_allowed
                Category.SCREEN_OFF -> R.string.probe_pkg_screen_off
                Category.AWAY -> R.string.probe_category_away
            },
        )
        return res.getString(
            R.string.probe_segment_row,
            time(segment.startAt),
            time(segment.endAt),
            formatElapsedClock(segment.endAt - segment.startAt),
            category,
            name,
        )
    }

    fun lastSessionDump(res: Resources, info: LastSessionInfo): String = buildString {
        appendLine("## ${res.getString(R.string.probe_section_last_session)}")
        lastSessionLines(res, info).forEach(::appendLine)
        info.segments.forEach { appendLine(segment(res, it, info.labels)) }
    }

    fun dump(res: Resources, result: ProbeResult): String = buildString {
        appendLine("[${res.getString(R.string.probe_title)}]")
        appendLine(range(res, result))
        appendLine(latest(res, result))
        selfCheck(res, result)?.let(::appendLine)
        appendLine(typeCounts(res, result))
        appendLine()
        appendLine("## ${res.getString(R.string.probe_section_screen_on)}")
        statsLines(res, result.stats).forEach(::appendLine)
        result.samples.forEach { appendLine(sample(res, it, result.labels)) }
        appendLine()
        appendLine("## ${res.getString(R.string.probe_section_classification)}")
        result.classifications.forEach { appendLine(classification(res, it)) }
        appendLine()
        appendLine("## ${res.getString(R.string.probe_section_events_dump, result.rows.size)}")
        result.rows.forEach { appendLine("${eventTitle(it)}  ${eventDetail(res, it)}") }
    }
}
