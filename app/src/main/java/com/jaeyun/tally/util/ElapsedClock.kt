package com.jaeyun.tally.util

/**
 * 타이머 RUNNING의 경과 시간 표기. 항상 `HH:MM:SS`라서 1시간이 지나도 자릿수가 바뀌지 않는다.
 *
 * 숫자는 로케일과 무관하게 ASCII로 만든다(`String.format`은 로케일에 따라 다른 숫자를 쓸 수 있다).
 */
fun formatElapsedClock(elapsedMillis: Long): String {
    val totalSec = (elapsedMillis / 1000).coerceAtLeast(0)
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return "${hours.twoDigits()}:${minutes.twoDigits()}:${seconds.twoDigits()}"
}

private fun Long.twoDigits() = toString().padStart(2, '0')
