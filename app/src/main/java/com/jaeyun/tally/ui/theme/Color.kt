package com.jaeyun.tally.ui.theme

import androidx.compose.ui.graphics.Color

// 노트 콘셉트 팔레트 (#4). 화면·컴포넌트는 여기 있는 값만 쓴다.
// 항목을 추가하거나 값을 바꿀 때는 사용자 승인을 먼저 받는다 (CLAUDE.md).
// 모두 불투명 색이다. alpha를 조절해 새 색을 만들지 않는다.

// 종이
val Paper = Color(0xFFFDFBF4)            // 모든 화면 바탕, 시트·다이얼로그. res/values/colors.xml의 paper와 같은 값
val PaperLine = Color(0xFFCFDDEA)        // 노트 줄, 구분선
val MarginLine = Color(0xFFEBA8A8)       // 여백선, 색인 카드 윗선
val SpiralHole = Color(0xFFE6E0D2)       // 스프링 구멍
val StickyNote = Color(0xFFFFF3A3)       // 인식 격차·다음 목표 포스트잇
val IndexCard = Color(0xFFFFFFFF)        // 지표 카드

// 필기구 (글씨·선)
val Pencil = Color(0xFF2C2C2A)           // 기본 글씨, 손그림 선
val PencilSoft = Color(0xFF6B675C)       // L3 보조 글씨, 안내 문구. Paper 위 대비 5.5:1
val BluePen = Color(0xFF2B4C7E)          // 판정 문구, 경과 시간, 강조 숫자, 비교값
val RedPen = Color(0xFFC8352E)           // 목표 달성 도장, 과대평가 표시. 이 두 곳에만 쓴다

// 꾸밈
val Highlighter = Color(0xFFFFE27A)      // 선택한 과목, 활성 탭
val HighlighterSoft = Color(0xFFFEECAB)  // 시작 버튼 칠, 순공 강조
val TapeBlue = Color(0xFFBFDDEF)         // 과목 테이프, 포스트잇 테이프, 탭
val TapePink = Color(0xFFF5C9C9)         // 과목 테이프, 탭
val TapeMint = Color(0xFFCDE8C4)         // 과목 테이프, 탭

// 구간 — 타임라인 스트립과 이탈 목록이 같은 규칙을 쓴다 (와이어프레임 §0)
val SegmentScreenOff = Color(0xFFFEF0BD) // SCREEN_OFF. 가장 연함
val SegmentAllowed = Color(0xFFAAD1EA)   // ALLOWED. 차분
val SegmentDistract = Color(0xFFF28F9C)  // DISTRACT. 경고
val SegmentScreenOn = Color(0xFFF7C5CA)  // __SCREEN_ON_UNKNOWN__ 화면 켜짐(앱 없음). DISTRACT 계열, 약하게
val SegmentAway = Color(0xFFE9E5DB)      // AWAY 자리 비움. 순공·착석 어디에도 들지 않는다
