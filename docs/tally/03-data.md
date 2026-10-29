# 03. 데이터

세션 데이터는 Room, 앱 설정은 DataStore에 둔다. 서버가 없고, 저장한 데이터는 기기를 벗어나지 않는다.

| 저장소 | 이름 | 담는 것 | 코드 |
|---|---|---|---|
| Room | `tally.db` (버전 1) | 세션, 타임라인 구간, 앱 분류, 주간 목표 — 세션 데이터의 단일 진실 공급원 | [`TallyDatabase`](../../app/src/main/java/com/jaeyun/tally/data/room/TallyDatabase.kt) |
| DataStore (Preferences) | `settings` | 온보딩 완료, 자기보고 값, 과목 숨김·추가, 체감 질문·재부팅 질문 기록, 개발자 설정 | [`AppSettingsStore`](../../app/src/main/java/com/jaeyun/tally/data/datastore/AppSettingsStore.kt) |
| 캐시 폴더 | `cache/exports/` | debug 빌드의 CSV 내보내기 파일. 내보낼 때마다 이전 파일을 지운다 | [`AppDataRepository`](../../app/src/main/java/com/jaeyun/tally/data/repository/AppDataRepository.kt) |

- 모든 시각은 epoch millis(UTC)로 저장하고, 화면에 보일 때 기기 시간대(`ZoneId.systemDefault()`)로 바꾼다.
- 기준선·판정 결과는 저장하지 않는다(P6에서 조회할 때 계산한다). 세션 지표는 조회 성능을 위해 세션에 함께 저장한다(비정규화).
- `Room`·`DataStore` 인스턴스는 [`AppContainer`](../../app/src/main/java/com/jaeyun/tally/AppContainer.kt)에서만 `by lazy`로 하나씩 만든다.

## Room 엔티티 4종

enum은 Room 기본 지원으로 상수 **이름**(TEXT)이 저장된다(`TypeConverter` 없음). 그래서 `Category`, `ClassificationSource`, `GoalMetric`의 상수 이름을 바꾸면 저장된 데이터를 읽지 못한다.

### `StudySession` — 공부 세션

[`StudySession.kt`](../../app/src/main/java/com/jaeyun/tally/data/room/StudySession.kt)

| 열 | 타입 | 기본값 | 뜻 |
|---|---|---|---|
| `id` | INTEGER, 자동 증가 | — | |
| `subjectName` | TEXT, null 가능 | — | 과목 라벨. null = 과목 없이 시작. 과목 테이블은 없다 |
| `startAt` | INTEGER | — | 시작 시각 |
| `endAt` | INTEGER, null 가능 | null | 종료 시각. null = 진행 중(또는 재부팅으로 끝나지 않은 세션) |
| `perceivedFocusMin` | INTEGER, null 가능 | null | 체감 집중 시간(분). 주 1회 묻는 세션에만 값이 있다 |
| `tTotalSec` | INTEGER | 0 | 착석 `T_total`(초). 화면에서는 "공부" |
| `tFocusSec` | INTEGER | 0 | 순공 `T_focus`(초) |
| `tDistSec` | INTEGER | 0 | 딴짓 `T_dist`(초) |
| `lfsSec` | INTEGER | 0 | 최장 무방해 구간 `LFS`(초) |
| `densityPct` | REAL | 0 | 집중 밀도 `D`(%) |
| `interruptionCount` | INTEGER | 0 | 이탈 횟수 `N_int` |
| `metricsVersion` | INTEGER | 0 | 위 지표를 계산한 규칙 버전. 0 = 계산 안 함, 1 = 현재 규칙([05-metrics](05-metrics.md#지표-규칙-버전)) |
| `isValidForStats` | INTEGER(Boolean) | false | 통계에 넣는지. 3분 미만·180분 초과·불변식 위반은 false |
| `hasTimeline` | INTEGER(Boolean) | false | 사용 기록으로 타임라인을 복원했는지. false면 착석 외 지표가 없다 |
| `createdAt` | INTEGER | — | 행을 만든 시각(시작 시각과 같다) |

`countsInStats = isValidForStats && hasTimeline` 확장 속성이 "통계에 셀 세션"이다. 주간 요약 줄, 체감 질문 후보, 리포트의 `기록`이 이 기준을 쓴다.

**세션 행이 바뀌는 때** — 모두 [`SessionRepository`](../../app/src/main/java/com/jaeyun/tally/data/repository/SessionRepository.kt)가 한다(과목 이름은 `SubjectRepository`, 체감은 `PerceivedFocusRepository`).

| 때 | 바뀌는 것 |
|---|---|
| 시작(`start`) | `subjectName`, `startAt = createdAt = 지금`으로 바로 넣는다. 이미 진행 중인 세션이 있으면 새로 만들지 않고 그 id를 돌려준다 |
| 종료(`finish`) | `endAt`과 지표 6개, `metricsVersion`, `isValidForStats`, `hasTimeline`. 사용 기록 권한이 없으면 `tTotalSec`(세션 길이)만 채우고 `hasTimeline = false`, `metricsVersion = 0` |
| ① 자리 비움(`markAway`) | 구간 분류를 바꾸고 지표·통계 포함 여부를 다시 계산 |
| ② 180분 초과 답(`answerOverLong`) | `isValidForStats`만 |
| 리포트 안 재분류(`reclassify`) | 다시 복원한 구간과 지표. `isValidForStats`는 그대로 둔다 |
| 체감 입력 | `perceivedFocusMin` |
| 과목 이름 바꾸기 | 그 과목의 모든 세션 `subjectName` (과거 세션까지) |
| ③ 재부팅 복구 `[삭제]` | 끝나지 않은 세션 행을 지운다 |

### `AppSegment` — 복원된 타임라인 구간

[`AppSegment.kt`](../../app/src/main/java/com/jaeyun/tally/data/room/AppSegment.kt)

| 열 | 뜻 |
|---|---|
| `id` | 자동 증가 |
| `sessionId` | 세션 id. 인덱스 `index_AppSegment_sessionId`, 외래키 `StudySession.id` + `ON DELETE CASCADE` |
| `packageName` | 앱 패키지, 또는 의사 패키지 `__SCREEN_OFF__`(화면 꺼짐)·`__SCREEN_ON_UNKNOWN__`(화면만 켜짐) |
| `category` | `DISTRACT` / `ALLOWED` / `SCREEN_OFF` / `AWAY` |
| `startAt`, `endAt` | 구간 `[startAt, endAt)` |

- 저장하는 것은 짧은 구간을 흡수하고 같은 분류끼리 합친 **최종 구간**이다. 원본 사용 기록 이벤트는 저장하지 않는다([04-timeline](04-timeline.md)).
- 세션의 구간은 언제나 통째로 바꾼다(`deleteBySession` 후 `insertAll`). 종료, ① 자리 비움, 리포트 안 재분류가 그렇다.
- 세션을 지우면 구간도 함께 지워진다(CASCADE).
- 사용 기록 권한 없이 끝낸 세션은 구간이 없다.

### `AppClassification` — 앱 분류

[`AppClassification.kt`](../../app/src/main/java/com/jaeyun/tally/data/room/AppClassification.kt)

| 열 | 뜻 |
|---|---|
| `packageName` | 기본 키 |
| `appLabel` | 앱 이름. 이탈 목록·앱 분류 화면이 이 이름을 보여 준다 |
| `category` | `DISTRACT` 또는 `ALLOWED`만 |
| `source` | `AUTO_CATEGORY`(자동 분류) / `USER`(사용자가 정함) |

- 행은 **세션에서 감지된 앱에만** 생긴다(설치된 앱 전체가 아니다). 세션을 끝낼 때 그 세션에 나온 앱 중 `USER`가 아닌 앱을 지금 규칙으로 다시 자동 분류해 덮어쓴다(`upsertAll`). 자동 분류 규칙이나 앱 이름이 바뀌어도 반영된다.
- `USER` 행은 자동 분류가 덮어쓰지 않는다.
- 본 앱(Tally)은 저장하지 않는다. 언제나 허용이다.
- 규칙은 [06-classification](06-classification.md).

### `WeeklyGoal` — 주간 목표 (P6에서 사용)

[`WeeklyGoal.kt`](../../app/src/main/java/com/jaeyun/tally/data/room/WeeklyGoal.kt)

| 열 | 뜻 |
|---|---|
| `weekStartAt` | 기본 키. 월요일 00:00(로컬 시간대) |
| `metric` | `LFS` / `INTERRUPTION` / `FOCUS_TOTAL` |
| `targetValue` | 목표값(분 또는 회, 소수) |
| `baselineValue` | 제시 당시의 기준선 값 |
| `achievedValue`, `isAchieved` | 주가 끝난 뒤 채운다. null 가능 |

엔티티와 DAO(`upsert`, `getByWeek`)만 있고 **아직 쓰는 코드가 없다.** 목표 생성은 P6 #27이다.

## DAO

| DAO | 함수 | 쓰는 곳 |
|---|---|---|
| [`StudySessionDao`](../../app/src/main/java/com/jaeyun/tally/data/room/StudySessionDao.kt) | `insert`, `update`, `delete`, `getById` | 세션 시작·종료·답 저장·삭제 |
| | `getUnfinished` / `observeUnfinished` | `endAt IS NULL`인 가장 최근 세션. 타이머가 IDLE/RUNNING을 이것으로 정한다 |
| | `observeFinished` | 끝난 세션 전체, 최근 시작 순. 기록 탭 목록, 주간 요약 줄 |
| | `getFinishedStartedBetween` | 기간 안에 시작해 끝난 세션, 시작 순. 체감 질문 트리거 |
| | `getBestLfsExcept` | 그 세션을 뺀 통계 세션(`isValidForStats = 1 AND hasTimeline = 1`)의 최장 구간 최댓값. 리포트의 `기록` |
| | `setPerceivedFocus` | 체감 입력 |
| | `observeSubjectStats` | 과목별 세션 수와 마지막 시작 시각(`GROUP BY subjectName`) |
| | `getLastSubject` | 가장 최근 세션의 과목. 앱을 열 때 칩 선택 |
| | `renameSubject` | 과목 이름 소급 변경 |
| | `getFinished`, `hasAny`, `observeHasAny` | CSV 내보내기, 데모 데이터 조건(세션이 하나라도 있는지, 진행 중 포함) |
| | `getLatestFinished` | debug 프로브만 |
| [`AppSegmentDao`](../../app/src/main/java/com/jaeyun/tally/data/room/AppSegmentDao.kt) | `insertAll`, `getBySession`(시각순), `deleteBySession` | 종료·자리 비움·재분류·리포트 |
| [`AppClassificationDao`](../../app/src/main/java/com/jaeyun/tally/data/room/AppClassificationDao.kt) | `insertIfAbsent`(충돌 무시) | 데모 데이터(사용자 분류를 덮지 않게) |
| | `upsert`, `get` | 사용자 분류 저장(앱 분류 화면, 리포트 안 재분류) |
| | `upsertAll`, `getByPackages` | 세션 종료 때 자동 분류 갱신, 리포트의 앱 이름 |
| | `observeAll` | 앱 분류 화면(정렬은 화면이 한국어 순서로) |
| | `getAll` | 계측 테스트만 |
| [`WeeklyGoalDao`](../../app/src/main/java/com/jaeyun/tally/data/room/WeeklyGoalDao.kt) | `upsert`, `getByWeek` | 아직 없음(P6) |

## 스키마와 마이그레이션

- 스키마는 [`app/schemas/`](../../app/schemas)에 내보내고 커밋한다(`exportSchema = true`, `room { schemaDirectory(...) }`). 지금은 버전 1 하나뿐이고 마이그레이션은 없다.
- 지표를 세션에 저장하므로, 엔티티를 바꾸면 DB 버전을 올리고 마이그레이션을 작성한다(CLAUDE.md). 지표 **규칙**이 바뀌면 스키마가 아니라 `MetricsVersion.CURRENT`를 올린다([05-metrics](05-metrics.md#지표-규칙-버전)).
- 계측 테스트가 스키마 파일을 읽을 수 있도록 `androidTest` assets에 `app/schemas`를 넣어 두었다(`app/build.gradle.kts`).

## DataStore 키

[`AppSettingsStore`](../../app/src/main/java/com/jaeyun/tally/data/datastore/AppSettingsStore.kt)가 `AppSettings` 하나로 읽는다. 파일을 읽다 `IOException`이 나면 모두 기본값으로 본다.

| 키 | 타입 · 기본값 | 쓰는 곳 → 읽는 곳 | 뜻 |
|---|---|---|---|
| `onboardingDone` | Boolean · false | 온보딩 완료 → 앱 시작 목적지 | false면 온보딩부터 연다. 앱을 열 때 한 번만 읽는다 |
| `selfReportedDailyMin` | Int · 없음 | 온보딩 슬라이더(시간 × 60) → (P6 #28 1주차 격차) | 건너뛰면 지운다. 지금은 저장만 하고 읽는 화면이 없다(debug 프로브만 보여 준다) |
| `hiddenSubjects` | Set&lt;String&gt; · 빈 집합 | 과목 시트 `칩에서 숨기기` → 칩 목록 | 칩에서만 뺀다. 세션은 그대로 |
| `addedSubjects` | Set&lt;String&gt; · 빈 집합 | 과목 시트 `+ 추가` → 칩·시트 | `"추가시각\t과목 이름"` 문자열로 저장. 아직 세션에 쓰지 않은 과목도 칩에 남게 한다 |
| `relaxedBaseline` | Boolean · false | 설정 탭 개발자 영역 → (P6 #24 기준선) | 기준선 활성 조건 12 → 6세션. 지금은 값만 저장한다 |
| `perceivedAskedAt` | Long · 없음 | 체감 질문을 띄우기 직전 → 체감 질문 트리거 | 마지막으로 물은 세션의 **시작 시각**. 그 세션이 속한 주에는 다시 묻지 않는다 |
| `recoveryAnsweredAt` | Long · 없음 | ③ `[이어서 기록]` → 재부팅 복구 질문 | 같은 재부팅을 두 번 묻지 않도록 답한 시각을 둔다 |
| `quickScreenOffCheck` | Boolean · false | debug 프로브 스위치 → `AppContainer` | **임시(#21).** debug 빌드에서 ① 화면 꺼짐 질문 기준을 60분에서 1분으로 낮춘다 |

과목 데이터가 둘로 나뉜 이유: 과목은 세션의 라벨 문자열이라 별도 테이블이 없다. 세션에 한 번도 쓰지 않은 과목(시트에서 추가만 한 과목)과 숨김 목록만 DataStore에 둔다. 과목 목록은 [`SubjectRepository`](../../app/src/main/java/com/jaeyun/tally/data/repository/SubjectRepository.kt)가 세션 통계(`observeSubjectStats`)와 `addedSubjects`·`hiddenSubjects`를 합쳐 만든다([07-screens](07-screens.md#과목-칩과-과목-시트)).

## 지우기

| 무엇 | 범위 | 코드 |
|---|---|---|
| ③ 재부팅 복구 `[삭제]` | 끝나지 않은 그 세션 한 행. 진행 중 세션에는 구간이 없다 | `SessionRepository.deleteUnfinished` |
| 설정 탭 데이터 전체 삭제 | Room 전체(`clearAllTables`), DataStore 전체(`clear`), `cache/exports/` | `AppDataRepository.deleteAll` |

- 전체 삭제 뒤에는 `onboardingDone`도 지워져 온보딩 전 상태가 되고, 화면은 바로 온보딩으로 간다([07-screens](07-screens.md#설정-탭)).
- **사용 기록 접근 권한은 앱이 끌 수 없어서 전체 삭제 뒤에도 켜진 채로 남는다.** 설정 탭 권한 줄에서 시스템 설정을 열어 끈다.
- 세션 하나를 골라 지우는 기능은 없다.

## 기기 밖으로 나가는 길

- **네트워크 권한이 없다.** 합쳐진 매니페스트의 권한은 `PACKAGE_USAGE_STATS`와 androidx가 넣는 내부 권한(`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`)뿐이다. 분석 SDK도 없다.
- **백업 제외.** `android:allowBackup="false"`이고, [`backup_rules.xml`](../../app/src/main/res/xml/backup_rules.xml)(Android 11 이하)과 [`data_extraction_rules.xml`](../../app/src/main/res/xml/data_extraction_rules.xml)(Android 12+, 클라우드 백업·기기 간 이전)이 root·file·database·sharedpref·external을 모두 뺀다.
- **debug 빌드의 CSV 내보내기**가 유일하게 파일을 앱 밖으로 넘기는 길이다. FileProvider는 debug 매니페스트에만 있어 release 빌드에는 이 길이 없다([10-development](10-development.md#개발자-영역)).
