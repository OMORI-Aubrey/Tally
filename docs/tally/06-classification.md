# 06. 앱 분류

앱은 **딴짓(`DISTRACT`)** 과 **허용(`ALLOWED`)** 두 가지뿐이다. 딴짓 앱 구간은 순공에서 빠지고 최장 구간을 끊는다. 화면 꺼짐·자리 비움은 앱 분류가 아니라 구간 분류다([05-metrics](05-metrics.md)).

| 무엇 | 코드 |
|---|---|
| 자동 분류 규칙(순수 함수 `autoCategory`)과 앱 정보 수집 | [`AppClassifier.kt`](../../app/src/main/java/com/jaeyun/tally/data/usagestats/AppClassifier.kt) |
| 처음부터 허용할 공부 앱 목록 | [`StudyApps.kt`](../../app/src/main/java/com/jaeyun/tally/data/usagestats/StudyApps.kt) |
| 분류 저장 | [`AppClassification`](../../app/src/main/java/com/jaeyun/tally/data/room/AppClassification.kt) 테이블([03-data](03-data.md#appclassification--앱-분류)) |
| 세션 종료 때 분류표 갱신, 리포트 안 재분류 | [`SessionRepository`](../../app/src/main/java/com/jaeyun/tally/data/repository/SessionRepository.kt) |
| 앱 분류 화면 | [`AppClassificationRepository`](../../app/src/main/java/com/jaeyun/tally/data/repository/AppClassificationRepository.kt), [`ui/screens/appclass/`](../../app/src/main/java/com/jaeyun/tally/ui/screens/appclass) |

## 자동 분류 — 기본은 딴짓

`autoCategory(AppFacts)`가 아래 순서대로 처음 맞는 규칙을 쓴다. 근거는 `AutoCategoryReason`으로 함께 돌려준다.

| 순서 | 근거 | 판별 | 분류 |
|---|---|---|---|
| 1 | `SELF` | 본 앱(Tally) | 허용 |
| 2 | `LAUNCHER` | **기본** 홈 앱(`ACTION_MAIN` + `CATEGORY_HOME`을 `MATCH_DEFAULT_ONLY`로 찾은 앱). 기본값이 없어 선택 화면(`android`)이 오면 없음 | 허용 |
| 3 | `STUDY_APP` | 공부 앱 목록(`STUDY_APPS`)에 있다 | 허용 |
| 4 | `BROWSER` | `https://` 주소를 여는 `ACTION_VIEW` + `BROWSABLE`에 응답하는 앱 | 딴짓 |
| 5 | `PHONE` | `TelecomManager`의 기본 전화 앱·시스템 전화 앱, 또는 패키지가 `.incallui`로 끝나는 통화 화면 | 딴짓 |
| 6 | `DISTRACT_CATEGORY` | `ApplicationInfo.category`가 SOCIAL·VIDEO·GAME이거나 옛 게임 표시(`FLAG_IS_GAME`) | 딴짓 |
| 7 | `SYSTEM` | 시스템 앱(`FLAG_SYSTEM`), 또는 정보를 읽을 수 없는 앱 | 허용 |
| 8 | `INSTALLED` | 그 밖에 사용자가 설치한 앱 | **딴짓** |

- 브라우저·전화·SNS·영상·게임은 시스템 앱이어도 딴짓이다(4~6이 7보다 먼저다). 미리 깔린 유튜브도 6에서 걸린다.
- 정보를 읽을 수 없는 앱을 시스템으로 보는 이유: 런처 아이콘이 있는 앱은 매니페스트 `<queries>`로 모두 보이므로, 보이지 않는 앱은 권한 창·공유 창 같은 시스템 부품이다.
- 금융·쇼핑 같은 나머지 설치 앱은 딴짓으로 시작한다. 모르는 앱이 순공에 섞이면 "순공 최대"가 부풀기 때문이다. 사용자가 고친다.
- 앱 이름은 `PackageManager.getApplicationLabel`. 앱을 찾지 못하면 패키지 이름을 쓴다.

**공부 앱 목록**(`STUDY_APPS`, 19개) — `ApplicationInfo.category`에 교육·금융 분류가 없어 알려진 패키지로 판별한다.

| 묶음 | 앱 |
|---|---|
| 화상 강의·회의 | Zoom, Google Meet(현재·이전 앱), Microsoft Teams, Webex, Google Classroom |
| 온라인 강의·학습 | Coursera, Khan Academy, Duolingo, Quizlet, AnkiDroid, Photomath |
| 사전·번역 | 네이버 사전, 파파고, Google 번역 |
| 노트·문서 | Notion, OneNote, Flexcil, Adobe Acrobat |

목록에 없는 공부 앱(학교 LMS, 인강 앱 등)은 사용자가 허용으로 바꾼다. 삼성 노트·계산기 같은 시스템 앱은 7번 규칙으로 허용된다.

### 패키지 가시성

Android 11+에서 다른 앱 정보를 읽으려면 그 앱이 보여야 한다. `QUERY_ALL_PACKAGES` 대신 [매니페스트](../../app/src/main/AndroidManifest.xml) `<queries>`에 필요한 인텐트만 선언했다.

| `<queries>` 인텐트 | 쓰는 규칙 |
|---|---|
| `MAIN` + `LAUNCHER` | 런처 아이콘이 있는 앱의 이름·카테고리 |
| `MAIN` + `HOME` | 기본 홈 앱 |
| `VIEW` + `BROWSABLE` + `https` | 브라우저 |

전화 앱은 `ACTION_DIAL`로 찾지 않는다. 줌처럼 전화를 걸 수 있는 앱도 응답해서 강의 앱이 딴짓이 되기 때문이다.

## 분류가 정해지고 바뀌는 때

| 때 | 하는 일 | 지난 세션 |
|---|---|---|
| 세션 종료 | 그 세션에 나온 앱 중 `USER`가 아닌 앱을 지금 규칙으로 다시 자동 분류해 저장(`AUTO_CATEGORY`) | — |
| 리포트의 미분류 앱 질문에 답함 | `USER`로 저장. 분류가 바뀌었으면 **그 세션만** 다시 계산 | 그 세션만 |
| 리포트 이탈 목록에서 `허용 앱으로 바꾸기` | 위와 같다 | 그 세션만 |
| 설정 → 앱 분류 화면에서 바꿈 | `USER`로 저장만 | 다시 계산하지 않음(다음 세션부터) |

- `USER` 분류는 자동 분류가 덮어쓰지 않는다.
- 분류표의 행은 세션에서 감지된 앱에만 생긴다. 본 앱은 저장하지 않고 늘 허용이다.

### 리포트 안 재분류

`SessionRepository.reclassify(sessionId, packageName, category)`. 분류가 그대로면 `USER`로 확정만 한다(`UNCHANGED`). 바뀌었으면 다음을 한다.

1. 그 세션의 사용 기록을 다시 조회한다. 세션이 없거나, 끝나지 않았거나, 타임라인이 없거나, 권한이 없으면 분류만 저장한다(`SAVED_ONLY`).
2. 다시 조회한 기록을 **지금까지의 분류**(분류표 그대로)로 복원하고, ①에서 바꾼 자리 비움을 같은 자리에 다시 입힌다.
3. 그 결과가 저장된 구간과 **완전히 같을 때만** 다음으로 간다. 다르면(오래돼 기록이 지워졌거나 일부만 남음) 엉뚱한 숫자로 덮어쓰지 않고 분류만 저장한다(`SAVED_ONLY`).
4. 바꾼 분류로 다시 복원하고(자리 비움 다시 입힘) 지표를 다시 계산해, 분류·세션·구간을 한 트랜잭션으로 저장한다(`RECOMPUTED`). **통계 포함 여부(`isValidForStats`)는 그대로 둔다** — ②에서 답한 것이 분류와 관계없어서다.

다른 세션은 다시 계산하지 않는다. 병합해 저장한 구간으로는 다시 나눌 수 없어서 사용 기록을 다시 조회해야 하기 때문이다.

리포트는 결과를 한 줄로 알린다.

| 결과 | 문구 |
|---|---|
| `RECOMPUTED` | `"카카오톡"을 허용 앱으로 바꾸고 이 세션을 다시 계산했어요` |
| `UNCHANGED` | `"카카오톡"을 허용 앱으로 정했어요` |
| `SAVED_ONLY` | 위 문구 + `이 세션은 다시 계산할 수 없어 다음 세션부터 반영돼요` |

(딴짓으로 정했으면 `딴짓 앱으로`. 조사 `을/를`은 앱 이름의 받침에 맞춰 [`Josa`](../../app/src/main/java/com/jaeyun/tally/util/Josa.kt)가 고른다.)

## 화면에서 묻고 고치는 곳

| 자리 | 동작 |
|---|---|
| 리포트 ⑥ 미분류 앱 질문 | `"카카오톡"은 어떤 앱인가요?` `[딴짓]` `[허용]`. 이 세션에서 딴짓으로 잡혔고 아직 사용자가 정하지 않은(`USER`가 아닌) 앱을 **딴짓 시간이 긴 순서로 2개까지**. 나머지는 그동안 자동 분류를 그대로 쓰고, 그 앱이 나오는 다음 리포트에서 다시 후보가 된다 |
| 리포트 ⑤ 이탈 목록 | 앱 줄을 누르거나 길게 누르면 `허용 앱으로 바꾸기` 메뉴. 화면만 켜진 구간(`화면 켜짐 (앱 없음)`)은 앱이 아니라 메뉴가 없다 |
| 설정 → 앱 분류 | 감지된 앱 전체를 이름 순서(한국어 가나다, 영문은 대소문자 무시)로. 앱마다 `딴짓 | 허용` 중 하나를 고른다. 분류로 나누지 않아 바꿔도 줄이 움직이지 않는다. 아래에 `브라우저는 강의 시청과 딴짓을 구분할 수 없어 기본값이 딴짓이에요` |

리포트에서는 답을 처리하는 동안 다른 답을 받지 않는다(연타, 두 앱 연달아 답하기).
