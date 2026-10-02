package com.jaeyun.tally.domain.model

/** 과목 이름 입력 규칙 (§8.1.1). 과목은 세션의 라벨 문자열이다(§12.2) */
object SubjectName {
    const val MAX_LENGTH = 20

    /** 앞뒤 공백을 지우고 연속 공백을 하나로 줄인다. 비었으면 null, 길면 [MAX_LENGTH]에서 자른다 */
    fun normalize(raw: String): String? =
        raw.trim().replace(Whitespace, " ").take(MAX_LENGTH).trimEnd().ifEmpty { null }

    private val Whitespace = Regex("""\s+""")
}
