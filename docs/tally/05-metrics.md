# 05. 지표

세션을 끝낼 때 복원한 구간으로 지표 6개를 계산해 세션에 저장한다. 인식 격차는 저장하지 않고 볼 때 계산한다. 기준선·판정·목표(P6)는 아직 없다.

| 무엇 | 코드 |
|---|---|
| 지표 6개, 인식 격차 | [`MetricsCalculator`](../../app/src/main/java/com/jaeyun/tally/domain/metrics/MetricsCalculator.kt) |
| 통계 포함 기준 | [`SessionValidity`](../../app/src/main/java/com/jaeyun/tally/domain/metrics/SessionValidity.kt) |
| 지표 규칙 버전 | [`MetricsVersion`](../../app/src/main/java/com/jaeyun/tally/domain/metrics/MetricsVersion.kt) |
| 시간 표기 | [`Duration.kt`](../../app/src/main/java/com/jaeyun/tally/util/Duration.kt), [`DurationText.kt`](../../app/src/main/java/com/jaeyun/tally/ui/components/text/DurationText.kt), [`ElapsedClock.kt`](../../app/src/main/java/com/jaeyun/tally/util/ElapsedClock.kt) |

## 세션 지표 6개

`MetricsCalculator.calculate(segments, sessionStart, sessionEnd)`가 구간을 한 번 훑어 계산한다.

| 지표 | 저장 필드 | 계산 | 화면에서 |
|---|---|---|---|
| 착석 `T_total` | `tTotalSec` | 세션 길이(종료 − 시작) − 자리 비움(`AWAY`) 구간 합. 음수면 0 | **"공부"** (`공부 1시간 38분`) |
| 딴짓 `T_dist` | `tDistSec` | `DISTRACT` 구간 길이의 합. 화면만 켜진 구간(`__SCREEN_ON_UNKNOWN__`)도 딴짓이다 | `딴짓 10분` |
| 순공 `T_focus` | `tFocusSec` | `T_total − T_dist`. 허용 앱과 화면 꺼짐이 순공에 들어간다 | 늘 "최대"를 붙인다 (`순공 최대 1시간 28분`) |
| 최장 무방해 구간 `LFS` | `lfsSec` | `ALLOWED`·`SCREEN_OFF` 구간이 이어진 길이 중 가장 긴 것. `DISTRACT`와 `AWAY`가 연속을 끊는다 | `최장 34분` — 리포트에서 가장 큰 숫자 |
| 이탈 `N_int` | `interruptionCount` | `DISTRACT` 구간의 개수. 바로 붙은 `DISTRACT`는 하나로 센다 | 조각 수 `N_int + 1` (`6조각`) |
| 집중 밀도 `D` | `densityPct` | `T_focus / T_total × 100`. 착석이 0이면 0 | 지표 카드 `집중 밀도 87%` |

구간 분류별로 정리하면 이렇다.

| 분류 | 착석 | 순공 | 딴짓 | 최장 구간 연속 |
|---|---|---|---|---|
| `ALLOWED` 허용 앱(본 앱 포함) | 들어감 | 들어감 | — | 이어짐 |
| `SCREEN_OFF` 화면 꺼짐 | 들어감 | 들어감 | — | 이어짐 |
| `DISTRACT` 딴짓 앱·화면만 켜짐 | 들어감 | 빠짐 | 들어감 | 끊김 |
| `AWAY` 자리 비움 | 빠짐 | 빠짐 | — | 끊김(시간도 더하지 않음) |

예: `공부 30분 → 자리 비움 1시간 → 공부 20분`이면 `LFS`는 30분이다. 자리 비운 앞뒤를 이으면 실제로 없던 50분짜리 집중이 생기기 때문이다(PLAN.md §4 "`AWAY`와 `LFS`").

### 초 단위와 버림

- 구간은 millis로 다루고, 저장할 때 초 단위로 **버린다**. 그래서 순공이 실제보다 커지지 않는다.
- `tDistSec`는 `tTotalSec`를 넘지 않게 자르고, `tFocusSec = tTotalSec − tDistSec`로 낸다. 저장값에서 `순공 + 딴짓 = 착석`이 늘 맞는다.
- `densityPct`는 버리기 전의 millis로 계산한 실수(Float)다. 화면은 정수로 버려 쓴다.

## 인식 격차 `G`

`MetricsCalculator.perceptionGapPct(perceivedFocusMin, tFocusSec)`

```
G = (체감 분 − 순공 분) / 순공 분 × 100     (순공 분 = tFocusSec / 60, 소수)
```

- **양수가 과대평가**다. 모든 화면이 이 부호를 쓴다.
- 체감을 적지 않았거나 순공이 0초면 null이다(0으로 나누지 않는다).
- 저장하지 않는다. 리포트는 반올림한 정수로 보여 준다: `+36% 과대평가`(빨간 펜 동그라미), `-12% 과소평가`(파란 볼펜), 0%면 `체감과 실측이 같아요`.
- 타임라인이 없는 세션(권한 없이 끝냄)은 체감 값이 있어도 격차를 보이지 않는다.

## 체감 질문 (주 1회)

세션을 끝낸 뒤 리포트 **전에** 실측을 보여 주지 않고 체감 집중 시간을 묻는다. 물을지는 `shouldAskPerceived`([`TimerViewModel.kt`](../../app/src/main/java/com/jaeyun/tally/ui/screens/timer/TimerViewModel.kt))가 정한다.

1. 방금 끝낸 세션이 **통계에 셀 세션**(`countsInStats`)이 아니면 묻지 않는다. 3분 미만·180분 초과·권한 없는 세션에 그 주의 한 번을 쓰지 않는다.
2. 그 세션이 시작한 주에 이미 물었으면 묻지 않는다. "물었다"는 `perceivedAskedAt`(마지막으로 물은 세션의 시작 시각)이 이번 주 안이거나, 이번 주 세션 중 체감 값이 있는 세션이 있다는 뜻이다. **건너뛰어도 물은 것으로 친다.**
3. 1/3 확률로 묻는다. 이번 주의 통계 세션 중 이 세션이 3번째 이후면 확정으로 묻는다.

물기로 정하면 화면을 띄우기 **전에** `perceivedAskedAt`을 적는다. 답하지 않고 앱을 닫아도 그 주에는 다시 묻지 않는다.

**입력 규칙** ([`PerceivedInputViewModel.kt`](../../app/src/main/java/com/jaeyun/tally/ui/screens/perceived/PerceivedInputViewModel.kt))

- 미리 채운 숫자 없이 빈칸에 숫자 키패드로 적는다.
- 적을 수 있는 최대값은 **타이머 길이**(종료 − 시작, 분 단위 올림)다. 착석이 아니다 — 자리 비움으로 빠진 시간에도 폰 밖에서 공부했을 수 있다.
- 최대값이 60분 이상이면 시간 칸(한 자리)과 분 칸(두 자리, 0~59)을 따로 둔다. 60분 미만이면 분 칸만 둔다.
- 한 칸만 적으면 다른 칸은 0으로 본다. 0분도 받는다.
- 최대값을 넘으면 `2시간 30분까지 적을 수 있어요`, 분 칸에 60 이상을 적으면 `분은 59까지 적을 수 있어요`.

## 통계 포함 기준

`StudySession.isValidForStats`. 기록은 남기고 통계에서만 뺀다.

| 조건 | 결과 |
|---|---|
| 착석(`tTotalSec`)이 180초 이상 10,800초(180분) 이하 | 통계에 넣는다(경계 포함) |
| 착석 3분 미만 | 뺀다(오조작) |
| 착석 180분 초과 | 일단 뺀다. ② 다이얼로그 `[계속 공부했어요]`면 넣는다 |
| 타임라인 불변식 위반 | 뺀다(release). debug는 예외로 멈춘다 |

- 착석으로 재므로 자리 비움을 빼고 판단한다. 자리 비움이 없으면 세션 길이와 같다.
- `SessionValidity`는 지표 정의가 아니다. 3분·180분을 바꿔도 지표 규칙 버전을 올리지 않고 `isValidForStats`만 다시 계산하면 된다.

**통계에 셀 세션** = `isValidForStats && hasTimeline` (`countsInStats`). 권한 없이 끝낸 세션은 `LFS`가 없어 숫자에 섞으면 0이 보이기 때문이다. 쓰는 곳:

| 쓰는 곳 | 기준 |
|---|---|
| 타이머 주간 요약 줄 | 이번 주에 시작한 `countsInStats` 세션의 `LFS` 최댓값·평균 |
| 체감 질문 후보, "이번 주 3번째" | `countsInStats` 세션만 센다 |
| 리포트의 `기록 47분` | 이 세션을 뺀 `countsInStats` 세션의 `LFS` 최댓값. 이 세션도 `countsInStats`일 때만 줄을 둔다 |
| 기록 탭 세션 목록 | `isValidForStats`가 false면 연필 가운뎃줄 |

## 주간 요약 줄의 숫자

타이머 IDLE의 `이번 주 최장 47분 · 평균 28분 →` — `weeklySummary`([`TimerViewModel.kt`](../../app/src/main/java/com/jaeyun/tally/ui/screens/timer/TimerViewModel.kt)).

- `최장` = 이번 주(월요일 00:00 시작)에 시작한 `countsInStats` 세션의 `LFS` 최댓값
- `평균` = 같은 세션들의 `LFS` 평균(초에서 버림). 세션 길이 평균이 아니다
- 셀 세션이 없거나 최댓값이 0초면 숫자 대신 `첫 세션을 시작해보세요`(누를 수 없다)

## 지표 규칙 버전

`MetricsVersion` — 세션의 `metricsVersion`에 함께 저장한다.

| 값 | 뜻 |
|---|---|
| `NOT_COMPUTED = 0` | 지표를 계산하지 않았다(진행 중 세션, 권한 없이 끝낸 세션) |
| `CURRENT = 1` | PLAN.md P0-3 확정 지표·임계값(2026-09-30) |

지표를 세션에 저장하므로 규칙이 바뀌면 이전 세션 값이 새 규칙과 어긋난다. 다음 중 하나가 바뀌면 `CURRENT`를 올리고, 저장 버전이 낮은 세션을 다시 계산할 대상으로 본다.

- 지표 정의(`AWAY` 제외 규칙 포함)
- 복원 알고리즘(입력 이벤트, 흡수 임계값, `KEYGUARD_HIDDEN` 규칙, 병합 순서)
- 구간 분류의 의미(`Category`)

다시 계산하는 코드는 아직 없다. 지금까지 버전은 1 하나뿐이다.

## 시간 표기

`durationParts(초)` — 1분 미만은 초, 1시간 미만은 분, 그 이상은 시간과 분(분이 0이면 시간만). **남는 분·초는 버린다.**

| 함수 | 쓰는 곳 | 예 |
|---|---|---|
| `sessionDurationText` | 세션 단위(리포트, 세션 목록, 주간 요약 줄, 다이얼로그, 체감 입력 안내, 온보딩 슬라이더) | `40초`, `25분`, `2시간`, `1시간 28분` |
| `totalDurationText` | 주간·누적 총량 — **아직 쓰는 화면이 없다**(P6·P7) | `40s`, `25m`, `2h`, `21h 24m` |
| `formatElapsedClock` | 타이머 RUNNING 경과 시간 | `01:11:18` — 늘 `HH:MM:SS`, ASCII 숫자 |

- 버려서 0초가 된 길이는 세션 단위에서 `1초 미만`으로 쓴다(세션 내내 딴짓인 순공, 1초가 안 된 세션 등). 0을 보이지 않기 위해서다.
- 예외: 리포트 격차 포스트잇의 체감 0분은 사용자가 적은 답이라 `0분` 그대로 쓴다.

## 0을 보이지 않는 곳

숫자가 0으로 떨어지면 0을 쓰지 않고 줄을 감추거나 문장으로 바꾼다.

| 자리 | 0일 때 |
|---|---|
| 주간 요약 줄 | `첫 세션을 시작해보세요` |
| 리포트 최장 구간 | 숫자를 두지 않고 조각 수만 |
| 리포트 조각 수(이탈 0회) | `이 세션에는 이탈이 없었어요` |
| `세션 1시간 38분 중 35%` | 0%로 떨어지거나 착석이 0이면 줄을 두지 않는다 |
| `기록 47분` | 다른 세션의 기록이 0초뿐이면 줄을 두지 않는다 |
| 총량 줄의 딴짓 | `· 딴짓 0분`을 쓰지 않고 `공부 …`만 |
| 집중 밀도 카드 | 0%면 카드를 두지 않는다 |
| 세션 단위 시간 | `1초 미만` |
