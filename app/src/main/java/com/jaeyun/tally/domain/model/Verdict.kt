package com.jaeyun.tally.domain.model

/**
 * 주간 판정 5구간 (§7.2). 약한 것부터 강한 것 순서라 `minOf(verdict, STEADY)`가 총량 가드의 클램프다(§7.2.1).
 * 저장하지 않고 조회할 때 계산한다(§9).
 */
enum class Verdict {
    /** 편차 −10% 이하. "이번 주 최장 구간은 {LFS}분이었어요. 조금 힘든 주였네요" */
    TOUGH,

    /** −10% < 편차 ≤ −3%. "최장 구간 {LFS}분을 만들었어요. 딴짓은 조금 늘었네요" */
    SLIPPED,

    /** −3% < 편차 < +3%. "평소 페이스를 유지했어요" */
    STEADY,

    /** +3% ≤ 편차 < +10%. "지난 4주보다 잘하고 있어요" */
    BETTER,

    /** 편차 +10% 이상. "지난 4주 중 가장 잘한 주예요" */
    BEST,
}

/**
 * 판정 결과. 계산은 `VerdictResolver`(#25)가 한다.
 *
 * @param clamped 총량 가드가 판정을 [Verdict.STEADY]로 내렸는지. true면 판정 문구 대신 전용 문구
 *   "이번 주는 짧게 자주 하셨네요. 집중 밀도는 82%로 좋았어요"를 쓴다(§7.2.1). 가드 조건이 맞아도 원래 판정이
 *   유지 이하였으면 내릴 것이 없어 false다
 */
data class VerdictResult(val verdict: Verdict, val clamped: Boolean)
