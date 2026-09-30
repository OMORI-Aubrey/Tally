package com.jaeyun.tally.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jaeyun.tally.domain.model.Category

/** 앱 분류 (§5.1, §9). 세션 중 감지된 앱만 행이 생긴다 */
@Entity
data class AppClassification(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    /** [Category.DISTRACT] 또는 [Category.ALLOWED]만 쓴다 */
    val category: Category,
    val source: ClassificationSource,
)

/** Room에는 이름(TEXT)으로 저장되므로 상수 이름을 바꾸면 저장된 데이터를 읽지 못한다 */
enum class ClassificationSource {
    /** `ApplicationInfo.category` 등으로 자동 분류. 사용자가 아직 확인하지 않았다 */
    AUTO_CATEGORY,

    /** 사용자가 직접 정했다. 자동 분류로 덮어쓰지 않는다 */
    USER,
}
