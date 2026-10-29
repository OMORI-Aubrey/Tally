package com.jaeyun.tally.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jaeyun.tally.domain.model.Category

/**
 * 복원된 타임라인 구간 (§9). 세션당 수십 건이 생기고 세션별 조회가 주 패턴이라 `sessionId`에 인덱스를 둔다.
 * 세션을 지우면 구간도 함께 지워진다(미완료 세션 `[삭제]`, 데이터 전체 삭제).
 */
@Entity(
    indices = [Index("sessionId")],
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AppSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    /** `__SCREEN_OFF__`, `__SCREEN_ON_UNKNOWN__` 의사 패키지 포함 */
    val packageName: String,
    val category: Category,
    val startAt: Long,
    val endAt: Long,
)
