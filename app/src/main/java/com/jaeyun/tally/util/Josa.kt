package com.jaeyun.tally.util

/**
 * 앞말의 받침에 맞는 조사. 과목·앱 이름처럼 바뀌는 말 뒤에 붙인다("수햑"을, "영어"를, "카카오톡"은).
 * 끝 글자가 한글이 아니면 받침을 알 수 없어 둘을 함께 쓴다("Java"을(를)).
 */
object Josa {
    /** 을/를 */
    fun objectOf(word: String): String = pick(word, withFinal = "을", withoutFinal = "를")

    /** 은/는 */
    fun topicOf(word: String): String = pick(word, withFinal = "은", withoutFinal = "는")

    /** 으로/로. ㄹ 받침 뒤에는 "로"를 쓴다("국어"로, "수학"으로, "물리"로, "문학"으로, "철"로) */
    fun directionOf(word: String): String {
        val final = finalConsonantIndex(word) ?: return "(으)로"
        return if (final == 0 || final == RIEUL) "로" else "으로"
    }

    private fun pick(word: String, withFinal: String, withoutFinal: String): String =
        when (finalConsonantIndex(word)) {
            null -> "$withFinal($withoutFinal)"
            0 -> withoutFinal
            else -> withFinal
        }

    /** 끝 글자의 받침 번호(0이면 받침 없음). 끝 글자가 완성형 한글이 아니면 null */
    private fun finalConsonantIndex(word: String): Int? {
        val last = word.lastOrNull() ?: return null
        if (last !in '가'..'힣') return null
        return (last - '가') % 28
    }

    private const val RIEUL = 8
}
