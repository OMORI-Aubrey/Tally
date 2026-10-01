package com.jaeyun.tally.ui

import androidx.annotation.StringRes
import com.jaeyun.tally.R
import kotlinx.serialization.Serializable

// 라우트 7종 (와이어프레임 §9). 세션 진행(RUNNING)은 타이머의 상태이고 과목 관리는 타이머의 바텀시트라 라우트가 아니다.

/** 최초 1회 시작 화면. 시작 목적지로 쓰는 판단은 P5 온보딩에서 붙인다 */
@Serializable
data object Onboarding

@Serializable
data object Timer

@Serializable
data object Records

@Serializable
data object Settings

/** 설정 → 앱 분류 (`settings/app_class`) */
@Serializable
data object AppClassification

/** 세션 종료 직후 주 1회 체감 집중 시간 입력 (§3.4) */
@Serializable
data class PerceivedInput(val sessionId: Long)

/** 세션 종료 직후 또는 기록 탭 세션 목록에서 연다 */
@Serializable
data class SessionReport(val sessionId: Long)

/** 하단 탭 3개. 이 목적지에서만 탭 바를 보인다 */
enum class TopLevelTab(val route: Any, @StringRes val label: Int) {
    TIMER(Timer, R.string.tab_timer),
    RECORDS(Records, R.string.tab_records),
    SETTINGS(Settings, R.string.tab_settings),
}
