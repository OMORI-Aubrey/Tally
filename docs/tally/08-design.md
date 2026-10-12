# 08. 디자인

**종이 노트 콘셉트.** 줄 노트 바탕에 손글씨 글꼴로 쓰고, 형광펜·마스킹테이프·포스트잇으로 꾸민다. 라이트 모드 전용이다. 와이어프레임은 배치와 위계만 정했고 색·글꼴·치수는 정하지 않았다 — 콘셉트는 PLAN.md §4 P1 "디자인 콘셉트" 결정이다.

| 무엇 | 코드 |
|---|---|
| 색 토큰 | [`Color.kt`](../../app/src/main/java/com/jaeyun/tally/ui/theme/Color.kt) |
| 글자 스타일 | [`Type.kt`](../../app/src/main/java/com/jaeyun/tally/ui/theme/Type.kt) |
| Material 색 역할 연결 | [`Theme.kt`](../../app/src/main/java/com/jaeyun/tally/ui/theme/Theme.kt) |
| 컴포넌트 | [`ui/components/`](../../app/src/main/java/com/jaeyun/tally/ui/components) |

**규칙(CLAUDE.md)** — 색은 `Color.kt`, 글자 스타일은 `Type.kt`에 있는 것만 쓴다. 코드에 `Color(...)`, `TextStyle(...)`, `fontSize`를 직접 쓰지 않는다. 토큰을 더하거나 바꾸려면 먼저 사용자 승인을 받는다. 모든 색은 불투명이고 alpha를 조절해 새 색을 만들지 않는다.

## 색

21개.

**종이**

| 토큰 | 값 | 쓰임 |
|---|---|---|
| `Paper` | `#FDFBF4` | 모든 화면 바탕, 시트·다이얼로그. `res/values/colors.xml`의 `paper`(창 배경)와 같은 값 |
| `PaperLine` | `#CFDDEA` | 노트 가로줄, 스프링 구멍 테두리 |
| `MarginLine` | `#EBA8A8` | 빨간 여백선, 지표 카드 윗선 |
| `SpiralHole` | `#E6E0D2` | 왼쪽 스프링 구멍 |
| `StickyNote` | `#FFF3A3` | 포스트잇(격차 포스트잇, 권한 순서 안내) |
| `IndexCard` | `#FFFFFF` | 지표 카드 |

**필기구 (글씨·선)**

| 토큰 | 값 | 쓰임 |
|---|---|---|
| `Pencil` | `#2C2C2A` | 기본 글씨, 손그림 선 |
| `PencilSoft` | `#6B675C` | L3 보조 글씨, 안내 문구. 종이 위 대비 5.5:1 |
| `PencilFaint` | `#948F84` | 한발 물러난 글씨(통계 제외 세션, 소제목 점선). 대비 3.1:1이라 큰 글씨에만 |
| `BluePen` | `#2B4C7E` | 강조 숫자(경과 시간, 온보딩 `11분`, 주간 요약 줄 숫자, 격차 포스트잇의 최장 구간, 권한 순서 번호), 과소평가 표시 |
| `RedPen` | `#C8352E` | **세 곳에만:** 목표 달성 도장(P6), 과대평가 표시, 데이터 전체 삭제 |

**꾸밈**

| 토큰 | 값 | 쓰임 |
|---|---|---|
| `Highlighter` | `#FFE27A` | 고른 것: 고른 과목 칩, 선택된 탭, 앱 분류의 고른 쪽 |
| `HighlighterSoft` | `#FEECAB` | 시작 버튼 칠, 순공 강조 |
| `TapeBlue`·`TapePink`·`TapeMint` | `#BFDDEF`·`#F5C9C9`·`#CDE8C4` | 과목 칩 테이프(셋을 돌려 쓴다), 포스트잇 테이프(`TapeBlue`), 하단 탭(타이머 파랑·기록 분홍·설정 민트) |

**구간** — 타임라인 스트립, 이탈 목록, 앱 분류 화면이 같은 규칙을 쓴다.

| 토큰 | 값 | 구간 |
|---|---|---|
| `SegmentAllowed` | `#AAD1EA` | 허용 앱. 차분 |
| `SegmentScreenOff` | `#FEF0BD` | 화면 꺼짐. 가장 연함 |
| `SegmentDistract` | `#F28F9C` | 딴짓. 경고 |
| `SegmentScreenOn` | `#F7C5CA` | 화면 켜짐(앱 없음). 딴짓 계열, 약하게 |
| `SegmentAway` | `#E9E5DB` | 자리 비움. 순공·착석 어디에도 들지 않는다 |

**Material 색 역할** — 시트·다이얼로그·입력창 같은 Material 기본 컴포넌트에 기본 보라색이 새지 않도록 `lightColorScheme`의 모든 역할을 토큰으로 채웠다(`primary = BluePen`, `background`·`surface` = `Paper`, `error = RedPen` 등).

**다크 모드 없음** — `android:forceDarkAllowed=false`로 시스템이 강제로 어둡게 바꾸지 못하게 하고, 기기가 다크 모드여도 상태바·내비게이션 바 아이콘을 어둡게 고정한다(`MainActivity`). 앱을 켤 때 흰 화면이 번쩍이지 않게 창 배경을 종이 색으로 깐다.

## 글꼴과 글자 스타일

손글씨 글꼴 2종(모두 SIL OFL 1.1, 라이선스 전문은 [`licenses/fonts/`](../../licenses/fonts)).

- **개구체**(Gaegu, Regular·Bold) — 숫자·본문·버튼
- **나눔손글씨 펜체**(Nanum Pen Script) — 라벨·안내·메타. `ⓘ`는 펜체에만 있는 글자라 안내 문구는 펜체로 쓴다

와이어프레임 §0의 위계 L1~L3를 Material 타이포 슬롯에 대응시켰다. 손글씨는 같은 크기에서 작게 보여 일반 글꼴보다 크게 잡았다. 색은 스타일에 넣지 않고 쓰는 곳에서 준다.

| 슬롯 | 글꼴 · 크기/줄 높이(sp) | 쓰임 |
|---|---|---|
| `displayLarge` | 개구체 Bold 84/92 | 타이머 RUNNING 경과 시간 |
| `displayMedium` | 개구체 Bold 52/64 | L1 최대: 시작 버튼, 온보딩 `11분`, 격차 포스트잇의 최장 구간 |
| `headlineLarge` | 개구체 Bold 36/48 | L1: 최장 구간·조각 수, RUNNING의 과목 |
| `headlineMedium` | 펜체 44/64 | 판정 문구 (P6, 아직 쓰는 곳 없음) |
| `titleLarge` | 개구체 Bold 24/32 | L2: 순공 줄(타임라인 없는 세션은 공부 줄), 지표 카드 값, 손그림 네모 버튼 글자, 기록 탭 날짜, 권한 순서 번호, 온보딩 헤드라인 |
| `bodyLarge` | 개구체 24/32 | 본문: `공부 … · 딴짓 …` 줄, 목록 항목, 입력창, 질문, 고르지 않은 탭·칩·선택지 |
| `bodyMedium` | 펜체 26/34 | L3: 메타, 근거, 라벨, 주간 요약 줄, 안내 |
| `labelLarge` | 개구체 Bold 24/32 | 선택된 탭·고른 칩·앱 분류의 고른 쪽(bodyLarge의 굵은 짝), 세션 목록의 과목 이름 |
| `labelMedium` | 펜체 18/24 | 캡션: 범례, 안내 한 줄, 섹션 머리말 |

쓰지 않는 나머지 슬롯도 개구체로 바꿔 Material 기본 컴포넌트에 시스템 글꼴이 섞이지 않게 했다.

## 노트 꾸밈

| 부품 | 모양 | 쓰는 곳 |
|---|---|---|
| `Modifier.notebookPaper()` | 종이 + 가로줄(32dp 간격) + 빨간 여백선 + 왼쪽 스프링 구멍. 줄은 화면에 고정이라 스크롤해도 움직이지 않는다. 글자를 줄에 맞추지는 않는다 | 모든 화면 바탕(`TallyApp`) |
| `NotebookPaper.ContentStart` (56dp) | 내용의 왼쪽 시작. 여백선 오른쪽 | 모든 화면 |
| `TapedNote` | 마스킹테이프로 붙인 포스트잇, 살짝 기울어짐 | 격차 포스트잇, 권한 순서 안내 |
| `HighlightedText` | 글자 아래쪽 절반에 형광펜 | RUNNING의 과목, 순공, 기록 탭 날짜 머리말 |
| `Modifier.pencilStrike()` | 살짝 기울어진 연필 가운뎃줄. 요소마다 따로 긋는다 | 통계 제외 세션 |
| `NoteSectionTitle` | 펜 글씨 소제목 + 점선 밑줄 | 리포트 `흐름`·`딴짓한 순간`, 설정 `개발자` |
| `NoteTopBar` | `← 닫기 ········· 세션 리포트`처럼 왼쪽 돌아가기, 오른쪽 화면 이름 | 세션 리포트, 앱 분류 |
| `SketchCircleButton` | 연필로 두 번 덧그린 동그라미 안을 형광펜으로 칠한 버튼 | 타이머 `[시작]` |
| `SketchBoxButton` | 손으로 그린 네모 테두리 버튼. 누를 수 없으면 흐리게 | `[종료]`, 체감 `[확인]`, 다이얼로그 선택지, `[권한 설정하기]`, `[시작하기]`, 미분류 앱 `[딴짓]`·`[허용]` |
| `IndexTabBar` | 노트 아래로 삐져나온 색인 탭. 선택된 탭이 더 길게 튀어나오고 형광펜 색 | 하단 탭 |

**아이콘** — 앱 아이콘은 연필로 그린 스톱워치이고, 형광펜으로 칠한 부분이 공부한 시간이다. 종이 바탕 적응형 아이콘 + 테마 아이콘용 단색 레이어(`mipmap-anydpi/ic_launcher.xml`, PNG 없음). 앱 안 로고(`TallyLogo`)도 같은 그림 + "Tally". 그 밖의 아이콘은 벡터 3개: 연필 화살표(`ic_arrow_right`, 손글씨 글꼴에 `›`가 없어 그림으로 그린다), 빗금 친 눈(`ic_hidden`), ⋮(`ic_more_vert`).

## 컴포넌트

`ui/components/`의 하위 패키지에만 둔다(CLAUDE.md UI 규칙). 여러 화면이 쓰면 역할별 패키지, 한 화면에서만 쓰면 그 화면 이름의 패키지. 모든 컴포넌트에 `@Preview`가 있고, 화면(`ui/screens/`)에는 없다(화면은 실기기로 확인).

| 패키지 | 컴포넌트 | 하는 일 |
|---|---|---|
| `notebook/` | `NotebookPaper`, `TapedNote`, `HighlightedText`, `PencilStrike`, `NoteSectionTitle`, `NoteTopBar`, `TallyLogo` | 노트 바탕과 꾸밈(위 표) |
| `text/` | `DurationText` | 시간 표기 `sessionDurationText`·`totalDurationText`([05-metrics](05-metrics.md#시간-표기)) |
| | `TabularDigitsText` | 숫자마다 같은 폭 칸에 그린다. 손글씨 숫자는 폭이 제각각이라 매초 바뀌는 경과 시간이 흔들리지 않게 한다. 폭이 모자라면 비율대로 줄인다 |
| `control/` | `SketchButtons` | `SketchCircleButton`, `SketchBoxButton` |
| | `SketchSlider` | 연필 눈금 위를 형광펜으로 칠하는 슬라이더. 값이 없으면 손잡이 없이 눈금만(앵커링 방지) |
| | `NumberBlank` | 숫자를 적는 빈칸 + 단위. 숫자 키패드, 미리 채운 숫자 없음 |
| | `IndexTabBar` | 하단 색인 탭. 숨길 때 왼쪽부터 차례로 내려가고 자리는 남긴다 |
| `feedback/` | `TallyDialog` | 종이 다이얼로그. 뒤로가기·바깥 누르기로 닫히지 않는다 |
| | `EmptyState` | 빈 상태 문장(+ 설명) |
| `permission/` | `PermissionSteps` | 권한 켜는 순서 포스트잇(번호를 파란 볼펜으로 크게) |
| | `PermissionBanner` | 권한 없이 끝낸 세션의 리포트 배너 |
| `timer/` | `SubjectChipRow`, `SubjectListItem`, `WeeklySummaryLine` | 과목 칩, 과목 시트 한 줄, 주간 요약 줄 |
| `report/` | `GapNote`, `StructureHeadline`, `TimelineStrip`, `TotalsRow`, `MetricCard`, `DistractionListItem`, `UnclassifiedAppPrompt` | 리포트 ⓪~⑥([07-screens](07-screens.md#세션-리포트)) |
| `records/` | `SessionListItem` | 기록 탭 세션 목록 한 줄 |
| `settings/` | `SettingsRow` | 설정 탭 한 줄: 설명·상태 줄, 화살표, 빨간 이름, 누를 수 없는 줄 |
| | `AppClassificationListItem` | 앱 분류 한 줄: 색 점 + 이름 + `딴짓 | 허용` |

`TimelineStrip`은 Canvas 없이 `Row` + `weight(구간 길이)`로만 그린다. 외부 차트 라이브러리는 쓰지 않는다.
