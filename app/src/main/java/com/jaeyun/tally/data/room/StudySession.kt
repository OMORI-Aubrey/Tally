package com.jaeyun.tally.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 공부 세션 (§9). 모든 시각은 epoch millis(UTC).
 *
 * 시작 즉시 저장하고(§6.3 #5) 종료 시 지표를 채운다. 진행 중에는 지표가 기본값(0)이다.
 */
@Entity
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** null = 과목 미지정. 별도 엔티티 없이 라벨 문자열로만 둔다 (§12.2) */
    val subjectName: String?,
    val startAt: Long,
    /** null = 진행 중 또는 강제 종료로 끝나지 않은 세션(복구 대상, §6.3 #5) */
    val endAt: Long? = null,

    /** 주 1회만 입력된다. 대부분 null (§3.4) */
    val perceivedFocusMin: Int? = null,

    // 산출 지표 (§5.2) — 조회 성능을 위해 비정규화 저장
    val tTotalSec: Int = 0,
    val tFocusSec: Int = 0,
    val tDistSec: Int = 0,
    val lfsSec: Int = 0,
    val densityPct: Float = 0f,
    val interruptionCount: Int = 0,
    /**
     * 위 지표를 계산한 규칙 버전(`MetricsVersion`). 진행 중에는 `MetricsVersion.NOT_COMPUTED`.
     * 규칙이 바뀌었을 때 다시 계산할 세션을 가려낸다 (§17.4)
     */
    val metricsVersion: Int = 0,

    /** 3분 미만·180분 초과 세션은 false (§6.3 #3·#4, PLAN.md P0-3) */
    val isValidForStats: Boolean = false,
    /** 사용 기록으로 타임라인을 복원했는지. false면 착석 외 지표가 없어 기준선·판정에서 제외한다 (PLAN.md §4) */
    val hasTimeline: Boolean = false,
    val createdAt: Long,
)
