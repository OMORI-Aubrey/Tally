# 10. 개발

빌드·테스트 구성과 개발용 도구(debug 프로브, 설정 탭 개발자 영역), 작업 흐름. 규칙의 원본은 [CLAUDE.md](../../CLAUDE.md)와 [PLAN.md](../../PLAN.md)다.

## 빌드 환경

버전은 [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml)에서만 관리한다.

| 항목 | 버전 |
|---|---|
| Android Gradle Plugin | 9.4.1 — **built-in Kotlin**(`org.jetbrains.kotlin.android`·`kotlin-kapt` 플러그인을 쓰지 않는다) |
| Gradle | 9.6.0 (daemon JVM toolchain 21) |
| Kotlin 플러그인(compose, serialization) | 2.4.20 |
| KSP | 2.3.12 — 어노테이션 처리는 KSP만(Room) |
| SDK | minSdk 29 / compileSdk 37 / targetSdk 37, Java 17 |
| Compose BOM | 2026.09.00 (Material 3) |
| AndroidX | core-ktx 1.19.0, activity-compose 1.13.0, lifecycle 2.11.0, navigation-compose 2.10.1, Room 2.8.5, DataStore 1.2.1 |
| Kotlin 라이브러리 | coroutines 1.11.0, serialization 1.11.0 |
| 테스트 | JUnit 4.13.2, kotlinx-coroutines-test, androidx.test.ext:junit 1.3.0, room-testing |

- minSdk 29는 `SCREEN_INTERACTIVE`·`SCREEN_NON_INTERACTIVE`(API 28)와 `ACTIVITY_RESUMED`(API 29)에 의존하기 때문이다.
- AGP·Gradle은 Android Studio가 지원하는 버전이어야 동기화가 된다. Studio를 먼저 올린 뒤 올린다.
- `gradle.properties`: configuration cache, build cache, parallel을 켰다.

```bash
./gradlew assembleDebug          # 디버그 APK
./gradlew testDebugUnitTest      # JVM 단위 테스트
./gradlew lintDebug              # 린트
./gradlew compileDebugAndroidTestKotlin compileReleaseKotlin   # 계측 테스트·release 소스 컴파일만
```

## debug와 release

[`app/build.gradle.kts`](../../app/build.gradle.kts), debug 전용 소스는 `app/src/debug/`.

| | debug | release |
|---|---|---|
| 사용 기록 프로브(런처의 "Tally 프로브") | 기본으로 켜짐. `-Ptally.probe=false`로 빌드하면 꺼진다(`manifestPlaceholders["probeEnabled"]`) | 없음 |
| 설정 탭 개발자 영역 | 보임(`BuildConfig.DEBUG`) | 없음 |
| CSV용 FileProvider | debug 매니페스트에만 있음 | 없음 — 앱 밖으로 파일을 내보내는 길이 없다 |
| 타임라인 불변식 위반 | 예외로 바로 멈춘다 | 로그 + 통계 제외 |
| ① 화면 꺼짐 기준 1분 스위치(임시) | 프로브에서 켤 수 있다 | 늘 60분 |
| 코드 축소 | — | `isMinifyEnabled = false` |

프로브는 개발 기간에는 기본 활성, 테스트 기간에는 끈다(PLAN.md P0-1).

## 테스트

| 종류 | 위치 | 개수 | 내용 |
|---|---|---|---|
| JVM 단위 테스트 | `app/src/test` | 167 | 아래 표 |
| debug 전용 JVM 테스트 | `app/src/testDebug` | 9 | 프로브의 화면 켜짐 분석(`ScreenOnAnalyzerTest`) |
| 계측 테스트 | `app/src/androidTest` | 38 | Room DB·DAO, Session·Subject·AppClassification·AppData Repository |

`./gradlew testDebugUnitTest`가 `test`와 `testDebug`를 함께 돌린다(176개).

**JVM 단위 테스트 내용**

| 영역 | 테스트 파일 |
|---|---|
| 타임라인 복원 | `TimelineReconstructorTest`(26) — 시나리오마다 불변식 검사, 흡수·병합·`KEYGUARD_HIDDEN`·seed |
| 지표·통계 포함 | `MetricsCalculatorTest`(11), `SessionValidityTest`(4) |
| 종료 시 저장값 | `SessionCompletionTest`(7) |
| 자동 분류 | `AutoCategoryTest`(7), `UsageEventMappingTest`(2) |
| 설정 저장 | `AppSettingsStoreTest`(10) — 메모리 DataStore로 키·기본값·전체 삭제 |
| 과목 | `MergeSubjectsTest`(4), `SubjectChipsTest`(4), `SubjectNameTest`(3) |
| 체감 질문 | `PerceivedTriggerTest`(7), `PerceivedMinutesTest`(10) |
| 리포트 | `SessionReportTest`(13) |
| 타이머 | `WeeklySummaryTest`(7), `RecoveryTest`(4) |
| 온보딩·기록·앱 분류 | `OnboardingStepTest`(5), `GroupByDayTest`(3), `AppRowsTest`(4), `SketchSliderTest`(3) |
| 개발자 영역 | `DemoDataTest`(10), `SessionCsvTest`(4) |
| 공통 | `DurationTest`(4), `ElapsedClockTest`(3), `JosaTest`(4), `WeekRangeTest`(6), `DomainPurityTest`(1) |

(나머지 1개는 템플릿 `ExampleUnitTest`.)

**계측 테스트 주의** — `./gradlew connectedDebugAndroidTest`는 끝나면 기기에서 앱을 지운다. 세션 기록과 사용 기록 권한도 함께 사라진다. 실제 기록이 있는 폰에서는 컴파일까지만 확인한다.

**실기기 확인** — `UsageStatsManager`는 에뮬레이터에 사용 기록이 거의 없어 검증할 수 없다. 이슈마다 실기기 체크리스트로 확인한 뒤 커밋한다. 화면은 와이어프레임 §10 디자인 체크리스트로 본다.

## 사용 기록 프로브

`app/src/debug/.../probe` — debug 빌드에만 있는 진단 화면. 본 앱과 다른 태스크로 열리고 런처에 "Tally 프로브" 아이콘으로 따로 보인다. **프로브는 Tally 안의 화면이라 프로브 아이콘을 지우면 Tally 전체가 삭제된다.**

| 구역 | 하는 일 |
|---|---|
| 권한 | 사용 기록 접근 상태, `설정 열기` |
| 조회 | 최근 5·15·30·60분 원본 이벤트(대상 4종만 보기 선택), `다시 조회`, `텍스트 복사` |
| 요약 | 이벤트 종류별 개수, 최신 이벤트, 마지막 RESUMED가 본 앱인지(조회 지연 확인) |
| 화면 켜짐 → 다음 이벤트 | 잠금 해제 소요 시간을 §6.2 원안 규칙과 확정 규칙(`KEYGUARD_HIDDEN`)으로 함께 잰다 — 흡수 임계값 실측용(#1) |
| 마지막 세션 (#12) | 저장된 지표·규칙 버전·통계 포함·타임라인 여부, 구간 목록, 구간 합 = 세션 길이 검사, 체감 값 |
| 진행 중 세션 (임시) | `+10분`·`+30분`·`+60분` — 시작 시각을 당겨 경과 시간을 늘린다. 당긴 구간의 실제 폰 사용도 세션에 들어가 지표는 실제와 다르다 |
| 체감 질문 (#14) | 이번 주에 물었는지, 후보 세션 수, `이번 주 체감 기록 지우기` |
| 화면 꺼짐 확인 (임시) | `기준을 1분으로 낮추기` — 다음에 끝내는 세션부터 ①을 1분 기준으로 묻는다 |
| 온보딩 (#19) | 완료 여부, 저장된 자기보고, `온보딩 다시 보기`(온보딩 완료만 되돌린다. 세션·앱 분류는 그대로) |
| 자동 분류 | 조회 범위에서 RESUMED된 앱의 분류와 근거(`AutoCategoryReason`) |

## 개발자 영역

설정 탭 아래, debug 빌드에서만 보인다(§14.4 발표 준비용). 코드는 [`AppDataRepository`](../../app/src/main/java/com/jaeyun/tally/data/repository/AppDataRepository.kt), [`SessionCsv.kt`](../../app/src/main/java/com/jaeyun/tally/data/repository/SessionCsv.kt), [`DemoData.kt`](../../app/src/main/java/com/jaeyun/tally/data/repository/DemoData.kt).

### CSV 내보내기

- 끝난 세션을 시작 순서로 한 줄씩 `cache/exports/tally-sessions-yyyyMMdd-HHmmss.csv`에 쓰고, 이전에 내보낸 파일은 지운다.
- FileProvider(`${applicationId}.fileprovider`, `cache-path exports/`)로 URI를 만들어 `ACTION_SEND`(`text/csv`) 공유 시트에 넘긴다.
- UTF-8 + BOM(엑셀이 한글을 UTF-8로 읽게), 줄 끝 CRLF. 쉼표·따옴표·줄바꿈이 든 값은 따옴표로 감싼다.
- 열: `id`, `subjectName`, `startLocal`, `endLocal`(기기 시간대 `yyyy-MM-dd HH:mm:ss`), `startAt`, `endAt`(epoch millis), `tTotalSec`, `tFocusSec`, `tDistSec`, `lfsSec`, `densityPct`(소수 둘째 자리), `interruptionCount`, `perceivedFocusMin`, `isValidForStats`, `hasTimeline`, `metricsVersion`. 값이 없으면 빈칸.
- 구간·앱 분류는 내보내지 않는다.

### 데모 데이터 주입

- **세션이 하나도 없을 때만**(진행 중 세션 포함) 넣는다. 실측 기록과 섞이지 않게 하려는 것이다(PLAN.md §4). 지우는 방법은 데이터 전체 삭제뿐이다.
- 이번 주와 앞 4주, 계획된 세션 21개 중 **지금보다 앞서 끝난 것만** 넣는다(이번 주의 뒤 요일 세션은 빠진다). 같은 시각이면 늘 같은 데이터가 나온다.
- 앞 4주에 통계 세션 16개(기준선 12세션·누적 15세션 조건을 앞 4주만으로 채운다) + 3주 전에 2분짜리 오조작 세션 하나(통계 제외).
- 흐름: 평범한 두 주 → 밤 세션이 많고 이탈이 잦은 힘든 주 → 회복 → 이번 주 나아짐. 과목은 수학·영어·물리.
- 구간은 `본 앱 20초 → (화면 꺼짐 ↔ 앱 사용) 반복 → 본 앱 30초`. 딴짓 앱은 카카오톡·YouTube·Instagram을 돌려 쓰고, 1시간 이상 세션에는 계산기(허용)를 한 번 넣는다. 복원 불변식을 지키고 지표는 실제 세션과 같은 `completeSession`으로 계산한다.
- 체감은 앞 4주에 주마다 셋째 세션에 세션 길이의 95%로 적었다(힘든 주에 격차가 가장 크다).
- 데모 앱 4개(YouTube·Instagram·카카오톡 딴짓, 계산기 허용)의 분류를 `AUTO_CATEGORY`로 넣는다. 이미 있는 행은 건드리지 않는다.
- 주간 목표(`WeeklyGoal`)와 DataStore 값(자기보고, 체감 질문 기록)은 넣지 않는다.

### 기준선 조건 완화

DataStore `relaxedBaseline`을 켜고 끈다. 기준선 활성 조건 12세션 → 6세션(§5.3, 사용성 평가에서 테스터가 12세션을 못 채울 때). 지금은 값만 저장하고, 기준선 계산(P6 #24)이 읽는다. release 빌드에는 적용되지 않는다.

## 작업 흐름

- 이슈 하나 = 브랜치 하나(`커밋태그/#이슈번호-설명`). 이슈는 `.github/ISSUE_TEMPLATE/` 양식, 종류 라벨 + 단계 라벨(`P0`~`P7`).
- 구현 → `testDebugUnitTest`·`assembleDebug`·`lintDebug` 통과 → 실기기 확인 → 커밋. 커밋 메시지는 `[커밋태그/#이슈번호] 제목`(태그: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`).
- PR은 `.github/pull_request_template.md` 양식.
- 단계의 마지막 이슈를 커밋할 때 README 진행 상황·지금 되는 것·아키텍처 표시와 PLAN.md 진행 기록을 고친다.
- `docs/`의 원안 문서(기획서·와이어프레임·사업성 분석)는 읽기 전용이다. 이 폴더(`docs/tally/`)는 구현을 따라 자유롭게 고친다.
