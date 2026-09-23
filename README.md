# 📊 Tally

**어디서 샜는지 보여주고, 다음 한 걸음을 제안합니다.**

공부 세션이 **몇 조각으로 부서졌는지**를 자동으로 측정하고, 자신의 과거 기록과 비교해 이번 주를 판정하며, 다음 주에 시도할 목표 하나를 제안하는 Android 앱입니다. 기록은 기기 밖으로 나가지 않습니다.

> "몇 시간"이 아니라 "몇 조각"이 이 앱의 1차 질문입니다.

## 설계 철학

```
총량이 아니라 구조를 센다  →  남과 비교하지 않는다  →  다음 한 걸음을 제안한다
```

- **차단하지 않는다.** 차단하면 데이터가 남지 않는다. 대신 정확하게 기록하고 정직하게 보여준다.
- **비교 대상은 언제나 자기 자신이다.** 랭킹·그룹·공유 기능이 없고, 계정도 서버도 없다.
- **압박하지 않는다.** 목표는 제안일 뿐이며 알림으로 상기시키지 않는다. 목표는 기록 화면 안에만 존재한다.

## 동작 방식

```
[1] 타이머 시작        과목(선택)을 고르고 세션 시작
[2] 세션 진행          앱은 아무것도 하지 않는다. 차단도, 알림도 없다
[3] 타이머 종료
[4] 타임라인 복원      UsageStatsManager로 세션 구간의 앱 전환 이벤트를 일괄 조회
[5] (주 1회) 체감 질문  "이번 세션에서 몇 분 집중한 것 같나요?"
[6] 세션 리포트        최장 구간 · 이탈 횟수 · 순공 · 딴짓 · 타임라인 스트립
[7] 기록 화면          판정 + 다음 목표 + 주간 집계 + 누적 패턴
```

세션 중에는 포그라운드 서비스 없이 아무것도 하지 않고, 종료 시점에 한 번에 복원합니다.

```
19:00 ─────────────────────── 19:34 ──── 19:41 ──────────── 20:12 ── 20:15 ───────── 20:38
    ■■■■■■■■■■■■■■■■■■■■■■■■■  ▓▓▓▓▓▓▓  ■■■■■■■■■■■■■■■■■  ▓▓▓▓  ■■■■■■■■■■■■
       학습 34분                YouTube 7분      학습 31분         카톡 3분   학습 23분

    착석 98분 · 순공 88분 · 딴짓 10분 · 최장구간 34분 · 이탈 2회
```

## 지표

**1차 지표 — 세션 구조**

| 지표 | 정의 |
|---|---|
| 최장 무방해 구간 `LFS` | 연속된 비딴짓 구간 중 최대 길이. 앱의 대표 지표 |
| 이탈 횟수 `N_int` | 딴짓 구간의 개수 |

**2차 지표 — 시간 총량**

| 지표 | 정의 |
|---|---|
| 착석 시간 `T_total` | 종료 시각 − 시작 시각 |
| 딴짓 시간 `T_dist` | 딴짓 구간 길이의 합 |
| 순공시간 `T_focus` | `T_total − T_dist` |
| 집중 밀도 `D` | `T_focus / T_total × 100` (%). 판정의 근거값 |

앱은 `DISTRACT`(딴짓) / `ALLOWED`(허용) 두 가지로만 분류하며, 화면이 꺼진 구간은 순공에 포함합니다. 판정과 목표는 절대 기준이 아니라 **최근 4주 개인 기준선**과 비교해 산출합니다.

## 기술 스택

| 계층 | 선택 |
|---|---|
| 플랫폼 | Android, minSdk 29 (Android 10) |
| UI | Kotlin + Jetpack Compose (Material 3), Navigation Compose |
| 아키텍처 | MVVM + Repository, ViewModel + `StateFlow` |
| DI | 수동 DI (`AppContainer`) |
| 저장소 | Room (세션 데이터), DataStore (앱 설정) |
| 앱 사용 수집 | `UsageStatsManager` — 세션 종료 시 일괄 조회 |
| 차트 | Compose 기본 레이아웃만 사용 (외부 라이브러리 없음) |
| 도메인 로직 | Kotlin 순수 함수 |
| 테스트 | JUnit4 |

서버는 없습니다. 판정과 목표도 모두 기기 안에서 계산합니다.

## 아키텍처

```
   Compose UI
       ↕ StateFlow
   ViewModel
       ↓
   도메인 로직 (순수 함수 — 단위 테스트 대상)
    ├── TimelineReconstructor
    ├── MetricsCalculator
    ├── BaselineCalculator
    ├── VerdictResolver
    ├── GoalSuggester
    └── TemplateReportGenerator
       ↓
   Repository
       ↓
   Room (단일 진실 공급원)
```

UseCase 계층은 두지 않습니다. 알고리즘 성격이 강한 6개만 도메인 순수 함수로 분리하고, 나머지는 ViewModel이 Repository를 직접 호출합니다.

단일 모듈 안에서 패키지로 나눕니다.

```
com.jaeyun.tally
├── ui/        theme, components, screens
├── domain/    model, reconstructor, metrics, baseline, verdict, goal, report
├── data/      room, usagestats, datastore, repository
└── util/
```

## 프라이버시

| 원칙 | 구현 |
|---|---|
| 로컬 전용 | 앱 사용 로그는 기기를 벗어나지 않는다. 클라우드 백업·기기 간 이전도 제외 |
| 명시적 고지 | 온보딩에서 수집 항목과 비수집 항목(앱 내용, 화면 내용, 위치, 연락처)을 명시 |
| 철회 가능 | 설정에서 전체 데이터 삭제 |
| 감시 도구화 방지 | 공유·랭킹·그룹 기능 없음 |

## 빌드

**요구 사항**

- Android Studio (AGP 9 지원 버전)
- Android SDK Platform 37
- JDK 21 (Gradle daemon toolchain, 없으면 자동 다운로드)

```bash
./gradlew assembleDebug        # 디버그 APK
./gradlew testDebugUnitTest    # 단위 테스트
./gradlew lintDebug            # 린트
```

**실기기 테스트가 필수입니다.** 에뮬레이터에는 앱 사용 기록이 거의 없어 타임라인 복원을 검증할 수 없습니다.

앱은 `PACKAGE_USAGE_STATS` 특수 권한을 사용합니다. 런타임 팝업으로 요청할 수 없으므로 **설정 → 사용 기록 접근 허용**에서 Tally를 직접 허용해야 합니다. 권한이 없으면 타이머 전용 모드로 동작합니다.

## 문서

| 문서 | 내용 |
|---|---|
| [MVP 기획서](docs/Tally-MVP-Proposal.md) | 문제 정의, 지표, 알고리즘, 화면·데이터 설계, 범위 — 기획의 단일 출처 |
| [화면 구성 설계](docs/Tally-wireframe.md) | 화면별 배치와 위계 |
| [사업성 분석](docs/Tally-Feasibility-Study-Report.md) | 실제 출시를 가정한 시장·경쟁·사업성 분석 |
| [CLAUDE.md](CLAUDE.md) | 개발 규칙 — 아키텍처, UI, Git 컨벤션 |

## 기여 규칙

커밋 메시지는 `[커밋태그/#이슈번호] 제목` 형식을 따릅니다. 이슈 번호가 없으면 `[커밋태그] 제목`.

```
[feat/#12] 제보 사진 업로드 연결
[chore] 린트 규칙 정리
```

커밋 태그: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
브랜치 이름: `커밋태그/#이슈번호-설명` (예: `feat/#12-report-upload`)
