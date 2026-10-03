# Tally

공부 세션 중 스마트폰 사용 기록을 복원해 순공 구조를 보여주는 Android 앱. 기획의 단일 출처는 `docs/Tally-MVP-Proposal.md`이며, 화면 배치는 `docs/Tally-wireframe.md`를 따른다. 본문에서 `§n`은 기획서 절 번호다.

진행 계획(단계별 작업, 착수 전 확정할 결정)은 `PLAN.md`를 따른다.

## 진행 상황 갱신

- 단계(P0~P7)의 마지막 이슈를 커밋할 때 `README.md`의 `진행 상황` 표에서 그 단계를 ✅로 바꾸고, 다음 단계를 `다음`으로 올린다. 그 이슈의 docs 커밋에 함께 넣는다.
- 그 단계에서 생긴 기능은 README의 `지금 되는 것`에, 구현한 도메인 함수는 `아키텍처` 그림의 표시(✅ / Pn)에 반영한다.
- `PLAN.md` 진행 기록에 그 단계의 줄이 있으면 완료 날짜를 적는다.

## 빌드

```bash
./gradlew assembleDebug        # 디버그 APK
./gradlew testDebugUnitTest    # JVM 단위 테스트 (도메인 로직)
```

- AGP 9 built-in Kotlin을 쓴다. `org.jetbrains.kotlin.android` / `kotlin-kapt` 플러그인을 추가하지 않는다. 어노테이션 처리는 KSP만 쓴다.
- 버전은 `gradle/libs.versions.toml`에서만 관리한다.
- Room 스키마는 `app/schemas/`에 내보내지며 커밋 대상이다. 엔티티를 바꾸면 DB 버전을 올리고 마이그레이션을 작성한다.
- `UsageStatsManager` 기능은 에뮬레이터로 검증할 수 없다. 실기기에서 확인한다 (§6.4).
- 사용 기록 프로브(`app/src/debug/.../probe`, #1)는 debug 빌드 전용 진단 화면이며 debug 빌드에서는 기본으로 켜진다(Android Studio 실행 버튼으로 설치해도 런처에 "Tally 프로브" 아이콘이 함께 생긴다). 끄려면 `-Ptally.probe=false`로 빌드한다. release 빌드에는 들어가지 않는다.

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

## UI 규칙

- 컴포넌트 기반으로 개발한다. 재사용 가능한 단위는 `ui/components/`로 분리하고, 새로 만들기 전에 기존 컴포넌트를 먼저 찾아 재사용한다.
- 컴포넌트는 `@Preview`를 만든다. 스크린은 실기기로만 테스트하므로 `@Preview`를 만들지 않는다.
- 색은 `ui/theme/Color.kt`, 텍스트 스타일은 `ui/theme/Type.kt`에 있는 것만 쓴다. 임의의 `Color(...)`, `TextStyle(...)`, `fontSize`를 코드에 직접 쓰지 않는다.
- `Color.kt`, `Type.kt`에 항목을 임의로 추가하지 않는다. 필요하면 사용자에게 먼저 묻는다.

## docs/

`docs/`는 읽기 전용이다. 사용자가 수정을 요청할 때만 수정한다.

## Git 규칙

- 커밋은 사용자가 요청할 때만 한다. 실기기 테스트가 OK일 때만 커밋하기 때문이다. 작업이 끝나면 변경 사항을 working tree에 그대로 두고, 커밋하지 않은 상태로 보고한다.
- `git push` 금지. 사용자가 직접 한다.
- PR 생성 금지. 사용자가 직접 한다. 단 PR 메시지 작성은 요청하면 해준다 (양식: `.github/pull_request_template.md`).
- 브랜치 생성과 `git switch`도 사용자가 하고 공지한다. 먼저 만들지 않는다.
- 이슈는 사용자가 요청하면 Claude가 `gh`로 만든다. `.github/ISSUE_TEMPLATE/` 양식을 따르고, 종류 라벨(`enhancement` / `bug` / `chore` / `documentation`)과 단계 라벨(`P0`~`P7`)을 붙인다. 셸 PATH에 `gh`가 없으면 `C:\Program Files\GitHub CLI\gh.exe`를 쓴다.
- 작업 중인 이슈의 체크박스는 Claude가 `gh issue edit`로 직접 체크한다. 구현하고 빌드·테스트로 확인한 항목은 바로 체크하고, 실기기 확인이 필요한 항목은 사용자가 OK를 알린 뒤에 체크한다. 진행 중에 결정이 바뀌면 해당 항목 문구도 고친다.

### 커밋 메시지

```
[커밋태그/#이슈번호] 제목

본문(선택)
```

이슈 번호가 없는 간단한 수정이면 번호를 생략하고 `[커밋태그] 제목`.

```
[feat/#12] 제보 사진 업로드 연결
[chore] 린트 규칙 정리
```

커밋 태그: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

### 브랜치 이름

`커밋태그/#이슈번호-설명` (예: `feat/#12-report-upload`)
