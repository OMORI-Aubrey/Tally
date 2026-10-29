package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.StudySession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 스프레드시트가 한글을 UTF-8로 읽도록 파일 맨 앞에 둔다(엑셀은 BOM이 없으면 시스템 인코딩으로 읽는다) */
internal const val CSV_BOM = '\uFEFF'

private const val CSV_LINE_END = "\r\n"

private val LOCAL_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

/** 열 이름은 `StudySession` 필드 이름 그대로 둔다(§9). 시각은 읽기 쉽게 로컬 시각 열을 앞에 더한다 */
private val CSV_HEADER = listOf(
    "id", "subjectName", "startLocal", "endLocal", "startAt", "endAt",
    "tTotalSec", "tFocusSec", "tDistSec", "lfsSec", "densityPct", "interruptionCount",
    "perceivedFocusMin", "isValidForStats", "hasTimeline", "metricsVersion",
)

/**
 * 세션을 한 줄씩 CSV로 쓴다 (개발자 영역 CSV 내보내기, §14.4). 발표용 그래프를 스프레드시트에서 그리는 데 쓴다.
 * 줄 끝은 CRLF(RFC 4180)이고 [CSV_BOM]은 넣지 않는다 — 파일에 쓸 때 붙인다.
 * 값이 없으면(과목 미지정, 체감 미입력, 진행 중 세션의 종료 시각) 빈칸이다.
 */
internal fun sessionsCsv(sessions: List<StudySession>, zone: ZoneId): String = buildString {
    append(CSV_HEADER.joinToString(",")).append(CSV_LINE_END)
    for (s in sessions) {
        val fields = listOf(
            s.id.toString(),
            s.subjectName.orEmpty(),
            localTime(s.startAt, zone),
            s.endAt?.let { localTime(it, zone) }.orEmpty(),
            s.startAt.toString(),
            s.endAt?.toString().orEmpty(),
            s.tTotalSec.toString(),
            s.tFocusSec.toString(),
            s.tDistSec.toString(),
            s.lfsSec.toString(),
            String.format(Locale.ROOT, "%.2f", s.densityPct),
            s.interruptionCount.toString(),
            s.perceivedFocusMin?.toString().orEmpty(),
            s.isValidForStats.toString(),
            s.hasTimeline.toString(),
            s.metricsVersion.toString(),
        )
        append(fields.joinToString(",") { csvField(it) }).append(CSV_LINE_END)
    }
}

private fun localTime(at: Long, zone: ZoneId): String = LOCAL_TIME.format(Instant.ofEpochMilli(at).atZone(zone))

/** 쉼표·따옴표·줄바꿈이 있으면 따옴표로 감싸고 안의 따옴표는 두 번 쓴다 (RFC 4180) */
internal fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
