# 02. 아키텍처

단일 모듈(`app`)이고 패키지로만 나눈다. Activity는 [`MainActivity`](../../app/src/main/java/com/jaeyun/tally/MainActivity.kt) 하나이고 화면은 모두 Jetpack Compose다. MVVM + Repository, 수동 DI, 서버 없음.

## 패키지

`com.jaeyun.tally` — 실제 폴더 그대로다.

```
com.jaeyun.tally
├── TallyApplication     AppContainer를 만들어 들고 있는다
├── AppContainer         수동 DI 컨테이너
├── MainActivity         edge-to-edge + TallyTheme { TallyApp() }
├── ui/
│   ├── TallyApp, TallyAppViewModel, Routes    앱 루트: 하단 탭 + NavHost, 시작 목적지, 라우트 7개
│   ├── theme/           Color, Type, Theme
│   ├── components/      공용 컴포저블. 하위 패키지에만 둔다
│   │   ├── notebook/  text/  control/  feedback/  permission/     여러 화면이 쓰는 것(역할별)
│   │   └── timer/  report/  records/  settings/                    한 화면만 쓰는 것(화면별)
│   └── screens/         화면마다 Screen + ViewModel
│       └── onboarding/  timer/  perceived/  report/  records/  settings/  appclass/
├── domain/              Android 의존성 없는 순수 Kotlin
│   ├── model/           Category, Segment, RawEvent, SessionMetrics, SubjectName, GoalMetric
│   ├── reconstructor/   TimelineReconstructor, LongScreenOff
│   ├── metrics/         MetricsCalculator, SessionValidity, MetricsVersion
│   └── baseline/  verdict/  goal/  report/     빈 폴더(.gitkeep) — P6·P7
├── data/
│   ├── room/            엔티티 4종, DAO 4개, TallyDatabase
│   ├── datastore/       AppSettingsStore
│   ├── usagestats/      UsageEventSource, AppClassifier, StudyApps
│   └── repository/      Session·Subject·PerceivedFocus·AppClassification·AppData Repository, 순수 함수 파일
└── util/                WeekRange, Duration, ElapsedClock, Josa
```

debug 빌드에만 있는 사용 기록 프로브는 `app/src/debug/.../probe`에 있다([10-development](10-development.md#사용-기록-프로브)).

## 계층과 의존 방향

```
   ui (Compose 화면 · ViewModel)
    │  StateFlow로 상태를 내보내고, 화면은 collectAsStateWithLifecycle()로 읽는다
    ├──────────────→ domain (순수 함수)
    ↓
   data/repository ──→ domain
    ↓
   data/room · data/datastore · data/usagestats
```

실제 import 기준이다.

| 쪽 | 쓰는 것 |
|---|---|
| `ui` | `data/repository`(Repository와 결과 타입), `data/room`(엔티티 타입·`countsInStats` — 5개 파일), `data/datastore`(`AppSettingsStore` — `TallyAppViewModel`, `OnboardingViewModel`, `SettingsViewModel`), `domain`, `util` |
| `data` | `domain`(모델, 복원, 지표), `util`(`DemoData`의 주 범위) |
| `domain` | 아무것도. `android.*`·`androidx.*`를 import하지 않는다 — [`DomainPurityTest`](../../app/src/test/java/com/jaeyun/tally/domain/DomainPurityTest.kt)가 지킨다 |
| `util` | 아무것도 |

- `data`·`domain`·`util`은 `ui`를 import하지 않는다.
- **UseCase 계층이 없다.** 알고리즘만 `domain`의 순수 함수로 두고, 나머지는 ViewModel이 Repository를 직접 부른다.
- 기획서의 도메인 함수 6개 중 지금 있는 것은 `TimelineReconstructor`·`MetricsCalculator` 2개다. `BaselineCalculator`·`VerdictResolver`·`GoalSuggester`(P6)와 `TemplateReportGenerator`(P7)는 아직 없다.

### 순수 함수로 뺀 규칙

`domain` 밖에도 규칙은 Android 없이 테스트할 수 있게 `internal` 순수 함수로 뺐다. 현재 시각·시간대가 필요하면 인자로 받는다(ViewModel은 `clock`·`zone` 람다를 받아 테스트에서 고정한다).

| 파일 | 함수 |
|---|---|
| `domain/…` | `TimelineReconstructor.reconstruct`·`checkInvariant`, `longScreenOffSegments`, `markAway`, `MetricsCalculator.calculate`·`perceptionGapPct`, `SessionValidity` |
| `data/repository/SessionCompletion.kt` | `completeSession`(종료 때 저장할 값), `sameTimeline`(재분류 전 비교) |
| `data/repository/SubjectRepository.kt` | `mergeSubjects`, `recentVisible`, `forSheet` |
| `data/repository/DemoData.kt`, `SessionCsv.kt` | `demoData`, `sessionsCsv`, `csvField` |
| `data/usagestats/AppClassifier.kt` | `autoCategory` |
| `ui/screens/timer/TimerViewModel.kt` | `subjectChips`, `weeklySummary`, `shouldAskPerceived`, `asksRecovery` |
| `ui/screens/report/SessionReport.kt` | `buildSessionReport`, `coversSession`, `timelineKind` |
| `ui/screens/perceived/PerceivedInputViewModel.kt` | `parsePerceivedMinutes`, `maxPerceivedMinutes`, `hasHourBlank`, `hourFitsSession` |
| `ui/screens/onboarding/OnboardingViewModel.kt` | `permissionStep`, `finishesOnReturn`, `selfReportedDailyMin` |
| `ui/screens/records/RecordsViewModel.kt` | `groupByDay` |
| `ui/screens/appclass/AppClassificationViewModel.kt` | `appRows` |
| `util/` | `weekRangeOf`, `durationParts`, `formatElapsedClock`, `Josa` |

## 수동 DI — `AppContainer`

[`AppContainer`](../../app/src/main/java/com/jaeyun/tally/AppContainer.kt)는 앱 전역에 하나이고 `TallyApplication.onCreate`에서 만든다. Hilt·Koin은 쓰지 않는다. 모두 `by lazy`로 처음 쓸 때 만든다.

| 이름 | 만드는 것 | 비고 |
|---|---|---|
| `database` | `TallyDatabase` (`tally.db`) | |
| `settingsStore` | `AppSettingsStore` (DataStore `settings`) | 파일 하나당 인스턴스가 하나여야 해서 여기서만 만든다 |
| `usageEventSource` | `UsageEventSource` | 권한 확인(`hasPermission`)도 여기서 |
| `appClassifier` | `AppClassifier` | |
| `sessionRepository` | `SessionRepository` | debug 빌드면 불변식 위반 시 멈춘다(`strictInvariant`), ① 기준을 프로브 스위치로 1분까지 낮출 수 있다 |
| `subjectRepository` | `SubjectRepository` | |
| `perceivedFocusRepository` | `PerceivedFocusRepository` | |
| `appClassificationRepository` | `AppClassificationRepository` | |
| `appDataRepository` | `AppDataRepository` | 전체 삭제·CSV·데모. CSV 폴더는 `cache/exports` |

**debug 판별이 두 가지다.** `AppContainer`는 앱의 `FLAG_DEBUGGABLE`로 판별하고(`strictInvariant`, `quickScreenOffCheck`), 설정 탭의 개발자 영역은 `BuildConfig.DEBUG`로 가린다(#23에서 `buildConfig = true`를 켰다). 둘 다 debug 빌드에서 참이다.

**ViewModel 만들기** — `viewModelFactory { initializer { … } }`에서 `CreationExtras.appContainer`(Application에서 꺼낸다)로 필요한 것만 넘긴다. 세션을 다루는 화면(체감 입력, 리포트)은 `factory(sessionId)`로 id를 함께 넘긴다.

## ViewModel과 상태

| 화면 | ViewModel | 상태 | 한 번만 처리할 일 |
|---|---|---|---|
| 앱 루트 | `TallyAppViewModel` | 시작 목적지(설정을 읽는 동안 null) | — |
| 온보딩 | `OnboardingViewModel` | 고른 시간, 권한 단계 (`SavedStateHandle`) | `done` → 타이머로 |
| 타이머 | `TimerViewModel` | `Loading` / `Idle`(칩·선택·시트 목록·주간 요약) / `Running`, 지금 띄울 다이얼로그 | `finishedSessions` → 체감 입력 또는 리포트로 |
| 체감 입력 | `PerceivedInputViewModel` | 최대 분 | `done` → 리포트로 |
| 세션 리포트 | `SessionReportViewModel` | 리포트 내용, 재분류 결과 한 줄, 지금 권한 | — |
| 기록 탭 | `RecordsViewModel` | 날짜별 세션 목록, 오늘 날짜 | — |
| 설정 탭 | `SettingsViewModel` | 권한, 기준선 완화, 세션 유무, 데모 주입 여부 | `ShareCsv` → 공유 시트, `DataDeleted` → 온보딩으로 |
| 앱 분류 | `AppClassificationViewModel` | 정렬한 앱 목록 | — |

- 상태는 `StateFlow`(`stateIn(..., SharingStarted.WhileSubscribed(5_000), …)`)로 내보내고 화면은 `collectAsStateWithLifecycle()`로 읽는다.
- 한 번만 처리할 일(이동, 공유)은 `Channel` → `receiveAsFlow()`로 내보내고, 화면이 `repeatOnLifecycle(STARTED)` 안에서 받는다.
- 사용 기록 권한은 결과 콜백이 없어서, 권한을 보여 주는 화면(온보딩, 리포트, 설정)은 `LifecycleResumeEffect`로 화면이 보일 때마다 다시 확인한다.
- 다이얼로그 답은 `CompletableDeferred`로 기다리고 `Mutex`로 한 번에 하나만 띄운다(`TimerViewModel.ask`).

## 내비게이션

Navigation Compose의 type-safe 라우트(`@Serializable` 객체·클래스, [`Routes.kt`](../../app/src/main/java/com/jaeyun/tally/ui/Routes.kt)). 화면 목록은 [07-screens](07-screens.md#화면-구성).

| 규칙 | 구현 ([`TallyApp.kt`](../../app/src/main/java/com/jaeyun/tally/ui/TallyApp.kt)) |
|---|---|
| 시작 목적지 | `TallyAppViewModel`이 앱을 열 때 `onboardingDone`을 **한 번** 읽어 `Onboarding` 또는 `Timer`. 읽는 동안은 빈 종이만 그린다. 온보딩을 마쳐도 바꾸지 않는다 — 바꾸면 NavHost가 그래프를 새로 만들어 백스택이 처음으로 돌아간다 |
| 온보딩을 마침 | `Timer`로 가며 `Onboarding`을 백스택에서 지운다. 타이머에서 뒤로가면 앱을 나간다 |
| 하단 탭 | 탭 목적지(타이머·기록·설정)에서만 보인다. 타이머가 진행 중이면 탭만 내려 숨기고 자리는 남긴다 |
| 탭 이동 | `popUpTo<Timer> { saveState = true }` + `launchSingleTop` + `restoreState`. 탭마다 백스택과 화면 상태를 따로 보존하고, 타이머가 아닌 탭에서 뒤로가면 타이머로 온다. 그래프의 시작 목적지가 아니라 타이머까지 꺼낸다(온보딩으로 시작한 날은 시작 목적지가 백스택에 없다) |
| 세션 종료 | 체감을 물을 세션이면 `PerceivedInput(id)`, 아니면 `SessionReport(id)` |
| 체감 입력을 마침 | 그 자리를 `SessionReport(id)`로 바꾼다. 리포트에서 뒤로가면 타이머로 온다 |
| 기록 탭 → 리포트 | `SessionReport(id)`, 닫기는 `popBackStack` |
| 설정 → 앱 분류 | `AppClassification`, 뒤로는 `popBackStack` |
| 데이터 전체 삭제 | 그래프 전체를 꺼내고 `Onboarding`만 남긴 뒤, 탭마다 보존한 백스택도 지운다(`clearBackStack`) — 지워진 세션의 리포트가 되살아나지 않게 |

## 스레드

- DAO는 모두 `suspend` 또는 `Flow`다. 여러 테이블을 바꾸는 일은 `withTransaction`으로 묶는다.
- 사용 기록 조회와 `PackageManager` 조회(자동 분류)는 블로킹이라 `Dispatchers.IO`에서 부르고, 트랜잭션 밖에서 한다. 저장 직전에 세션이 아직 진행 중인지 다시 확인한다.
- `clearAllTables`(전체 삭제)와 CSV 파일 쓰기도 IO 디스패처에서 한다.
- 연타는 ViewModel과 Repository에서 막는다. 종료를 두 번 눌러도 이미 끝난 세션이면 아무것도 하지 않고, 다이얼로그는 처음 답만 쓰며, 체감 입력·리포트 재분류·설정의 삭제·내보내기·데모 주입은 처리 중 플래그로 한 번만 한다.
