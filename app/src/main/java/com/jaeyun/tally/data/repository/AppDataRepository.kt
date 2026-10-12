package com.jaeyun.tally.data.repository

import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.TallyDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 앱 데이터 전체를 다룬다: 데이터 전체 삭제(§11).
 */
class AppDataRepository(
    private val database: TallyDatabase,
    private val settings: AppSettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /**
     * Room 전체와 설정(DataStore)을 지운다. 온보딩 완료도 지워져 온보딩 전 상태가 된다.
     * 사용 기록 접근 권한은 앱이 끌 수 없어 그대로 남는다.
     */
    suspend fun deleteAll() {
        withContext(ioDispatcher) {
            database.clearAllTables()
            settings.clear()
        }
    }
}
