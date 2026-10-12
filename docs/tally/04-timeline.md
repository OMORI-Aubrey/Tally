# 04. 타임라인 복원

세션을 끝낼 때 OS 사용 기록을 **한 번** 조회해 세션 동안 화면이 어떻게 쓰였는지 구간으로 되살린다. 세션 중에는 앱이 아무것도 하지 않는다(포그라운드 서비스·주기 조회 없음).

| 무엇 | 코드 |
|---|---|
| 사용 기록 조회 (`UsageStatsManager` 래퍼) | [`UsageEventSource`](../../app/src/main/java/com/jaeyun/tally/data/usagestats/UsageEventSource.kt) |
| 복원 알고리즘 (순수 함수) | [`TimelineReconstructor`](../../app/src/main/java/com/jaeyun/tally/domain/reconstructor/TimelineReconstructor.kt) |
| 긴 화면 꺼짐 찾기, 자리 비움으로 바꾸기 | [`LongScreenOff.kt`](../../app/src/main/java/com/jaeyun/tally/domain/reconstructor/LongScreenOff.kt) |
| 저장할 값 만들기 (순수 함수) | [`SessionCompletion.kt`](../../app/src/main/java/com/jaeyun/tally/data/repository/SessionCompletion.kt) |
| 종료 파이프라인, 질문 답 저장, 재분류 | [`SessionRepository`](../../app/src/main/java/com/jaeyun/tally/data/repository/SessionRepository.kt) |
| 종료 후 질문 순서 | [`TimerViewModel`](../../app/src/main/java/com/jaeyun/tally/ui/screens/timer/TimerViewModel.kt) |

## 세션 종료 흐름

```
[종료] 누름
  │
  ├─ SessionRepository.finish(sessionId, now)
  │    1. 아직 진행 중인 세션인지 확인 (아니면 아무것도 하지 않는다 — 종료 연타 등)
  │    2. 사용 기록 조회 (IO 스레드, 트랜잭션 밖)
  │         권한 없음 → 타임라인 없이 착석만 저장
  │         이벤트    → 이 세션에 나온 앱 자동 분류·저장 → 복원
  │    3. completeSession: 지표 계산, 통계 포함 여부, 불변식 정책
  │    4. 트랜잭션: 아직 진행 중인지 다시 확인 → 세션 갱신 + 구간 통째로 교체
  │
  ├─ ① 화면 꺼짐 단일 구간 > 60분이면 구간마다 묻는다 → [자리 비웠어요]면 markAway
  ├─ ② 착석 > 180분이면 묻는다 → answerOverLong
  ├─ 체감 질문 여부를 정한다 (①②의 답을 저장한 뒤)
  └─ 체감 입력 또는 세션 리포트로 이동
```

- ①②는 [다이얼로그](07-screens.md#다이얼로그)로 묻는다. 뒤로가기·바깥 누르기로 닫히지 않고, 한 번에 하나만 뜬다.
- 답하는 도중 앱이 정리되면 묻지 않은 상태로 남는다. 화면 꺼짐은 공부한 시간이고, 180분 초과는 통계에서 빠진 채다.
- 체감 질문 규칙은 [05-metrics](05-metrics.md#체감-질문-주-1회).

## 입력 이벤트 4종

`UsageStatsManager.queryEvents(세션 시작, 종료)`의 결과에서 4종만 남기고 시각순으로 정렬한다. 나머지 이벤트(`ACTIVITY_PAUSED` 등)는 버린다.

| `UsageEvents.Event` | 도메인 타입 `RawEventType` | 패키지 이름 |
|---|---|---|
| `ACTIVITY_RESUMED` | `ACTIVITY_RESUMED` — 앱이 포그라운드로 왔다 | 담는다 |
| `SCREEN_INTERACTIVE` | `SCREEN_INTERACTIVE` — 화면이 켜졌다 | 없음 |
| `SCREEN_NON_INTERACTIVE` | `SCREEN_NON_INTERACTIVE` — 화면이 꺼졌다 | 없음 |
| `KEYGUARD_HIDDEN` | `KEYGUARD_HIDDEN` — 잠금이 풀렸다 | 없음 |

- **권한 확인:** `AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS)`가 `MODE_ALLOWED`인지 본다. 꺼져 있으면 조회하지 않고 `NoPermission`을 돌려준다. 확인과 조회 사이에 권한이 꺼져 `SecurityException`이 나도 `NoPermission`이다.
- 도메인에는 Android 타입 대신 [`RawEvent`](../../app/src/main/java/com/jaeyun/tally/domain/model/RawEvent.kt)(시각·타입·패키지)만 넘긴다.

## 복원 알고리즘

`TimelineReconstructor.reconstruct(events, sessionStart, sessionEnd, selfPackage, classify)` — Android 의존성 없는 순수 함수다.

1. **이벤트 고르기** — 시각이 `sessionStart ≤ t ≤ sessionEnd`인 이벤트만, 시각순으로.
2. **시작 구간(seed)** — 본 앱 구간을 `sessionStart`에서 연다. 시작 버튼을 누른 본 앱은 이미 포그라운드라 `ACTIVITY_RESUMED`가 오지 않기 때문이다. 마지막으로 RESUMED된 앱(`lastResumed`)의 초깃값도 본 앱이다.
3. **구간 만들기** — 이벤트마다 아래 패키지로 바꾸고, 지금 구간과 다르면 구간을 끊는다. 같으면 끊지 않는다.

   | 이벤트 | 새 구간의 패키지 |
   |---|---|
   | `ACTIVITY_RESUMED` | 그 앱 (`lastResumed`도 갱신) |
   | `SCREEN_NON_INTERACTIVE` | `__SCREEN_OFF__` |
   | `SCREEN_INTERACTIVE` | `__SCREEN_ON_UNKNOWN__` (화면만 켜지고 앱은 아직 없음 — 잠금화면·알림창) |
   | `KEYGUARD_HIDDEN` | 지금 구간이 `__SCREEN_ON_UNKNOWN__`일 때만 `lastResumed`. 아니면 무시 |

   마지막 구간은 `sessionEnd`에서 닫는다.
4. **짧은 구간 흡수** — 앞에서부터 보며 짧은 구간을 지우지 않고 이웃의 경계를 늘려 흡수한다. 앞 구간이 있으면 앞 구간의 끝을 늘리고, 첫 구간이면 다음 구간의 시작을 당긴다.

   | 구간 | 흡수 기준 |
   |---|---|
   | `__SCREEN_ON_UNKNOWN__` | 10초 미만 (`ABSORB_SCREEN_ON_MILLIS`) |
   | 그 밖의 모든 구간 | 5초 미만 (`ABSORB_MILLIS`) |
5. **분류** — `__SCREEN_OFF__` → `SCREEN_OFF`, `__SCREEN_ON_UNKNOWN__` → `DISTRACT`, 그 밖의 앱은 `classify(패키지)`(→ `DISTRACT` / `ALLOWED`). 본 앱은 언제나 `ALLOWED`다.
6. **같은 분류 병합** — 분류가 같은 구간이 이어지면 하나로 합친다. 합친 구간의 패키지는 **그 안에서 가장 오래 머문 앱**이다(같으면 먼저 나온 앱). 이탈 목록에 보이는 앱 이름이 이것이다.
7. **불변식 검사** — 결과 구간이 세션을 빈틈·겹침 없이 덮는지 본다(아래).

4단계를 6단계보다 먼저 해야 `유튜브 → 홈 2초 → 유튜브`가 이탈 1회가 된다. 홈 2초가 앞의 유튜브에 흡수되고, 남은 두 유튜브 구간이 병합된다.

결과 구간은 인접한 두 구간의 분류가 항상 다르다.

### 잠금 해제 규칙 (`KEYGUARD_HIDDEN`)

실기기(삼성 One UI)에서는 화면을 끄기 직전의 앱(런처 포함)이 `SCREEN_INTERACTIVE`보다 **먼저** RESUMED되는 경우가 있다. 그러면 잠금을 푼 뒤 새 RESUMED가 오지 않아, 잠금을 풀고 그 앱에 머문 시간 전체가 `__SCREEN_ON_UNKNOWN__`(딴짓)이 된다. 그래서 화면만 켜진 구간에서 잠금이 풀리면 마지막으로 RESUMED된 앱으로 바꾼다.

- 잠금을 풀지 않고 알림만 보고 끄면 해제 이벤트가 없어 그 구간은 `DISTRACT`로 남는다(10초 미만이면 흡수된다).
- 잠금 화면을 쓰지 않는 기기에서는 해제 이벤트가 오지 않으므로, 화면을 켠 뒤 새 앱을 열 때까지가 화면만 켜진 구간(딴짓)으로 남는다.

근거 실측과 결정은 PLAN.md §4 "잠금 해제 후 홈 체류 처리", "흡수 임계값".

### 불변식

`TimelineReconstructor.checkInvariant(segments, sessionStart, sessionEnd)`가 다음을 차례로 보고, 처음 깨진 것을 문장으로 돌려준다(모두 지키면 null).

- 구간이 하나 이상 있다
- 첫 구간의 시작 = 세션 시작, 마지막 구간의 끝 = 세션 끝
- 길이가 음수인 구간이 없다
- 이웃한 구간 사이에 빈틈·겹침이 없다
- 구간 길이의 합 = 세션 길이

깨졌을 때의 처리는 `completeSession`이 한다.

| 빌드 | 처리 |
|---|---|
| debug | 예외를 던져 바로 멈춘다(`strictInvariant = true`) |
| release | 경고 로그를 남기고 `isValidForStats = false`로 통계에서 뺀다. 지표와 구간은 그대로 저장한다 |

깨진 타임라인에서는 `LFS`가 줄어들고 스트립 비율이 조용히 틀어지기 때문이다(기획서 §6.2.2).

## 앱 분류표를 만드는 때

복원 5단계의 `classify`는 세션을 끝낼 때 만든다(`SessionRepository.classificationsFor`).

1. 이벤트에서 RESUMED된 앱을 모은다(본 앱 제외)
2. 분류표에서 그 앱들의 행을 읽는다
3. `USER`가 아닌 앱은 지금 규칙으로 다시 자동 분류해 분류표에 덮어쓴다(`AUTO_CATEGORY`)
4. 분류표에 없는 앱이 생기면 딴짓, 본 앱은 허용으로 본다

자동 분류 규칙은 [06-classification](06-classification.md).

## 예외 처리 6건 (§6.3)

| # | 상황 | 구현 |
|---|---|---|
| 1 | 사용 기록 권한 없음 | 착석(`tTotalSec` = 세션 길이)만 저장하고 `hasTimeline = false`, `metricsVersion = 0`. 구간 없음. 리포트는 [권한 유도 화면](07-screens.md#세션-리포트) |
| 2 | 화면이 오래 꺼짐 | `SCREEN_OFF` 구간. 순공에 들어간다 |
| 3 | 3분 미만 | 착석(자리 비움을 뺀 길이)으로 잰다. `isValidForStats = false` — 기록은 남고 통계에서만 빠진다 |
| 4 | 180분 초과 | 착석이 180분을 **넘을** 때(정각은 아님). 종료 직후에는 통계에서 빠진 상태이고, ② 다이얼로그로 묻는다. `[계속 공부했어요]`면 통계에 넣고(불변식이 깨진 세션은 계속 뺀다), `[종료를 잊었어요]`면 뺀다 |
| 5 | 강제 종료·재부팅 | 시작하자마자 세션 행을 저장하고, 타이머 상태는 그 행(`endAt = null`)으로 정한다. **강제 종료·최근 앱에서 지우기는 묻지 않고 그대로 이어진다.** 세션을 시작한 뒤 재부팅했으면 앱을 열 때 ③ 다이얼로그로 묻는다 |
| 6 | 타이머를 켠 채 잠듦 | 화면 꺼짐 **단일 구간**이 60분을 넘으면(`LONG_SCREEN_OFF_MILLIS`) 그 구간마다 ① 다이얼로그로 묻는다. `[자리 비웠어요]`면 그 구간이 `AWAY`가 된다 |

### ① 자리 비움으로 바꾸기

`SessionRepository.markAway` — 사용 기록을 다시 조회하지 않고 저장된 구간에서 분류만 바꾼다.

- 시작·끝이 정확히 같은 `SCREEN_OFF` 구간만 `AWAY`로 바꾼다. 경계가 그대로라 불변식이 유지되고, 이웃은 화면 꺼짐이 아니라(이미 병합됐다) 새로 합칠 구간도 없다.
- 지표와 통계 포함 여부를 다시 계산한다. 자리 비움은 착석·순공에서 빠지고 최장 구간 연속을 끊는다([05-metrics](05-metrics.md)).
- ② 질문 여부는 자리 비움을 반영한 착석으로 정한다. 4시간 세션에서 2시간을 자리 비움으로 바꾸면 ②를 묻지 않는다.

### ③ 재부팅 확인

`asksRecovery(startAt, bootAt, answeredAt)` — 앱을 열 때(타이머 화면 ViewModel이 만들어질 때) 진행 중 세션이 있으면 한 번 본다.

- `bootAt` = 지금 − 부팅 후 경과 시간(`System.currentTimeMillis() − SystemClock.elapsedRealtime()`)
- `bootAt > startAt`(세션 시작 뒤 재부팅)이고, `[이어서 기록]`이라고 답한 시각(`recoveryAnsweredAt`)보다도 뒤에 재부팅했을 때만 묻는다
- `[이어서 기록]` → 답한 시각을 DataStore에 남기고 세션은 그대로 진행. `[삭제]` → 그 세션 행을 지운다

## debug 전용 조정 (임시)

debug 빌드에서 프로브의 스위치(`quickScreenOffCheck`)를 켜면 ①의 기준이 60분에서 **1분**으로 낮아진다. `AppContainer`가 `SessionRepository`에 기준을 함수로 넘긴다(`longScreenOffMillis`). release 빌드는 언제나 60분이다. #21에서 넣은 임시 도구다([10-development](10-development.md#사용-기록-프로브)).
