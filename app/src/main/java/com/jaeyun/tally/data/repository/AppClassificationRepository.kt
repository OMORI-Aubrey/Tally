package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.data.room.AppClassificationDao
import com.jaeyun.tally.data.room.ClassificationSource
import com.jaeyun.tally.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * 앱 분류 화면 (§5.1, #18). 세션에서 감지된 앱만 행이 있다(설치된 앱 전체가 아니다).
 *
 * 여기서 바꾼 분류는 다음 세션부터 반영한다(PLAN.md §4 재분류 소급 범위). 지난 세션을 다시 계산하는 것은 리포트 안
 * 재분류(`SessionRepository.reclassify`)뿐이다.
 */
class AppClassificationRepository(private val dao: AppClassificationDao) {

    /** 감지된 앱 전체. 순서는 정하지 않는다 */
    val detectedApps: Flow<List<AppClassification>> = dao.observeAll()

    /** 사용자 분류(`USER`)로 저장한다. 자동 분류가 다시 덮어쓰지 않는다. 행이 없는 앱(있을 수 없다)은 무시한다 */
    suspend fun setCategory(packageName: String, category: Category) {
        val current = dao.get(packageName) ?: return
        dao.upsert(current.copy(category = category, source = ClassificationSource.USER))
    }
}
