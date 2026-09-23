# Tally

공부 세션 중 스마트폰 사용 기록을 복원해 순공 구조를 보여주는 Android 앱. 기획의 단일 출처는 `docs/Tally-MVP-Proposal.md`이며, 화면 배치는 `docs/Tally-wireframe.md`를 따른다. 본문에서 `§n`은 기획서 절 번호다.

## 빌드

```bash
./gradlew assembleDebug        # 디버그 APK
./gradlew testDebugUnitTest    # JVM 단위 테스트 (도메인 로직)
```

- AGP 9 built-in Kotlin을 쓴다. `org.jetbrains.kotlin.android` / `kotlin-kapt` 플러그인을 추가하지 않는다. 어노테이션 처리는 KSP만 쓴다.
- 버전은 `gradle/libs.versions.toml`에서만 관리한다.
- Room 스키마는 `app/schemas/`에 내보내지며 커밋 대상이다. 엔티티를 바꾸면 DB 버전을 올리고 마이그레이션을 작성한다.
- `UsageStatsManager` 기능은 에뮬레이터로 검증할 수 없다. 실기기에서 확인한다 (§6.4).

## 확정 스택 (§10.1)

| 계층 | 선택 |
|---|---|
| SDK | minSdk 29 / compileSdk 37 / targetSdk 37 |
| UI | Jetpack Compose + Material 3, Navigation Compose(type-safe route, `@Serializable`) |
| 아키텍처 | MVVM + Repository, ViewModel이 `StateFlow`로 UI 상태 노출, UI는 `collectAsStateWithLifecycle()` |
| DI | 수동 DI — `AppContainer` (Hilt/Koin 도입 금지) |
| 저장 | Room = 세션 데이터 SSOT, DataStore(Preferences) = 앱 설정 |
| 수집 | `UsageStatsManager.queryEvents` — 세션 종료 시 일괄 조회. 포그라운드 서비스 없음 (§3.3) |
| 차트 | Compose 기본 레이아웃(`Row`/`Column`/`Box`)만. 외부 차트 라이브러리 금지 |
| 테스트 | JUnit4 + kotlinx-coroutines-test |

서버 없음. 앱 사용 로그는 기기를 벗어나지 않는다 (§10.2, §11). 네트워크 권한·분석 SDK를 추가하지 않고, 백업 규칙은 모든 도메인을 제외한다.

## 패키지 구조 (§10.4)

단일 모듈, 패키지로만 분리한다.

```
com.jaeyun.tally
├── TallyApplication, AppContainer, MainActivity
├── ui/
│   ├── theme/        TallyTheme
│   ├── components/   공용 컴포저블 (타임라인 스트립 등)
│   └── screens/      화면별 Screen + ViewModel
├── domain/           Android 의존성 없는 순수 Kotlin
│   ├── model/
│   ├── reconstructor/  TimelineReconstructor (§6)
│   ├── metrics/        MetricsCalculator (§5.2)
│   ├── baseline/       BaselineCalculator (§5.3)
│   ├── verdict/        VerdictResolver (§7.2)
│   ├── goal/           GoalSuggester (§7.3)
│   └── report/         TemplateReportGenerator (§7.4)
├── data/
│   ├── room/         Entity 4종, DAO, Database (§9)
│   ├── usagestats/   UsageStatsManager 래퍼
│   ├── datastore/    selfReportedDailyMin, hiddenSubjects
│   └── repository/
└── util/
```

## 아키텍처 규칙 (§10.3)

- 의존 방향: `ui → domain`, `ui → data/repository`, `data → domain/model`. `domain`은 `android.*`를 import하지 않는다.
- UseCase 계층을 두지 않는다. 알고리즘 6종만 `domain`의 순수 함수로 두고, 나머지는 ViewModel이 Repository를 직접 호출한다.
- 도메인 순수 함수는 입력·출력을 명시적으로 받는다. 현재 시각이 필요하면 인자로 받는다 (테스트 고정용, §14.2).
- `TimelineReconstructor`는 원본 이벤트를 받는 순수 함수다. `UsageStatsManager` 호출은 `data/usagestats`가 맡는다.
- 모든 시각은 epoch millis(UTC)로 저장하고 표시 시점에 로컬 시간대로 변환한다 (§9).
- 기준선·판정 결과는 저장하지 않고 조회 시 계산한다. `WeeklyGoal`만 제시 당시 값을 보존한다 (§9).
- ViewModel 생성은 `viewModelFactory { initializer { ... } }` + `CreationExtras.appContainer`로 한다.
