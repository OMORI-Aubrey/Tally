package com.jaeyun.tally.data.usagestats

/**
 * 처음부터 허용으로 둘 공부 앱(화상 강의·온라인 강의·사전·번역·노트·학습). 사용자가 설치한 앱은 딴짓이 기본값이라
 * (PLAN.md §4) 공부에 쓰는 앱만 미리 빼 둔다.
 *
 * `ApplicationInfo.category`에는 교육이 없어 알려진 패키지로 판별한다. 목록에 없는 공부 앱(학교 LMS, 인강 앱 등)은
 * 사용자가 이탈 목록·앱 분류 화면에서 허용으로 바꾼다(#16, #18). 시스템 앱(삼성 노트·계산기 등)은 따로 허용된다.
 */
internal val STUDY_APPS: Set<String> = setOf(
    // 화상 강의·회의
    "us.zoom.videomeetings",                        // Zoom
    "com.google.android.apps.tachyon",              // Google Meet
    "com.google.android.apps.meetings",             // Google Meet(이전 앱)
    "com.microsoft.teams",                          // Microsoft Teams
    "com.cisco.webex.meetings",                     // Webex
    "com.google.android.apps.classroom",            // Google Classroom
    // 온라인 강의·학습
    "org.coursera.android",                         // Coursera
    "org.khanacademy.android",                      // Khan Academy
    "com.duolingo",                                 // Duolingo
    "com.quizlet.quizletandroid",                   // Quizlet
    "com.ichi2.anki",                               // AnkiDroid
    "com.microblink.photomath",                     // Photomath
    // 사전·번역
    "com.nhn.android.naverdic",                     // 네이버 사전
    "com.naver.labs.translator",                    // 파파고
    "com.google.android.apps.translate",            // Google 번역
    // 노트·문서
    "notion.id",                                    // Notion
    "com.microsoft.office.onenote",                 // OneNote
    "com.flexcil.flexcilnote",                      // Flexcil
    "com.adobe.reader",                             // Adobe Acrobat
)
