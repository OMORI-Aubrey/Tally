package com.jaeyun.tally.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jaeyun.tally.R

// 손글씨 글꼴 (#4). 둘 다 SIL OFL 1.1이고 라이선스 전문은 licenses/fonts/에 있다
private val Gaegu = FontFamily(
    Font(R.font.gaegu_regular, FontWeight.Normal),
    Font(R.font.gaegu_bold, FontWeight.Bold),
)
private val PenScript = FontFamily(Font(R.font.nanum_pen_script))

private fun gaegu(weight: FontWeight, size: Int, lineHeight: Int) =
    TextStyle(fontFamily = Gaegu, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

private fun penScript(size: Int, lineHeight: Int) =
    TextStyle(fontFamily = PenScript, fontWeight = FontWeight.Normal, fontSize = size.sp, lineHeight = lineHeight.sp)

private val Default = Typography()

/**
 * 와이어프레임 §0 타이포 위계(L1~L3)를 Material 슬롯에 대응시킨다. 색은 스타일에 넣지 않고 쓰는 곳에서 토큰으로 준다.
 *
 * 손글씨 글꼴은 같은 크기에서 글자가 작게 보여 일반 글꼴보다 크게 잡았다. 실기기에서 조정한다.
 */
val Typography = Typography(
    displayLarge = gaegu(FontWeight.Bold, 84, 92),     // 타이머 RUNNING 경과 시간
    displayMedium = gaegu(FontWeight.Bold, 52, 64),    // L1 최대: 시작 버튼, 격차 포스트잇의 최장 구간
    headlineLarge = gaegu(FontWeight.Bold, 36, 48),    // L1: 최장 구간·조각 수·이탈 횟수, 선택한 과목
    headlineMedium = penScript(44, 64),                // 판정 문구 (BluePen)
    titleLarge = gaegu(FontWeight.Bold, 24, 32),       // L2: 순공·착석·딴짓, 지표 카드 값, 다음 목표
    bodyLarge = gaegu(FontWeight.Normal, 24, 32),      // 본문: 목록 항목, 입력창
    bodyMedium = penScript(26, 34),                    // L3: 메타, 근거, 라벨, 주간 요약 줄
    labelLarge = gaegu(FontWeight.Bold, 24, 32),       // 버튼, 탭, 고른 칩, 목록의 과목 이름. bodyLarge와 같은 크기의 굵은 짝
    labelMedium = penScript(18, 24),                   // 캡션: 범례, 안내 문구, 섹션 제목

    // 쓰지 않는 슬롯도 개구체로 바꿔 Material 기본 컴포넌트에 시스템 글꼴이 섞이지 않게 한다
    displaySmall = Default.displaySmall.copy(fontFamily = Gaegu),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Gaegu),
    titleMedium = Default.titleMedium.copy(fontFamily = Gaegu),
    titleSmall = Default.titleSmall.copy(fontFamily = Gaegu),
    bodySmall = Default.bodySmall.copy(fontFamily = Gaegu),
    labelSmall = Default.labelSmall.copy(fontFamily = Gaegu),
)
