package com.jaeyun.tally.data.repository

import androidx.room.withTransaction
import com.jaeyun.tally.data.datastore.AppSettingsStore
import com.jaeyun.tally.data.room.TallyDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 앱 데이터 전체를 다룬다: 데이터 전체 삭제(§11)와 개발자 영역의 CSV 내보내기·데모 데이터(§14.4).
 *
 * @param exportDir CSV를 쓰는 캐시 폴더. debug 매니페스트의 FileProvider가 이 폴더만 공유한다
 * @param selfPackage 본 앱 패키지. 데모 세션의 시작·종료 구간 앱이다
 */
class AppDataRepository(
    private val database: TallyDatabase,
    private val settings: AppSettingsStore,
    private val exportDir: File,
    private val selfPackage: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val sessionDao = database.studySessionDao()

    /** 세션이 하나라도 있는지. 진행 중인 세션도 센다 */
    val hasSessions: Flow<Boolean> = sessionDao.observeHasAny()

    /**
     * Room 전체와 설정(DataStore)을 지운다. 온보딩 완료도 지워져 온보딩 전 상태가 된다.
     * 내보낸 CSV도 함께 지운다. 사용 기록 접근 권한은 앱이 끌 수 없어 그대로 남는다.
     */
    suspend fun deleteAll() {
        withContext(ioDispatcher) {
            database.clearAllTables()
            settings.clear()
            exportDir.deleteRecursively()
        }
    }

    /** 끝난 세션을 CSV 파일로 쓴다. 파일 이름에 내보낸 시각을 넣고, 이전에 내보낸 파일은 지운다 */
    suspend fun exportSessionsCsv(now: Long, zone: ZoneId): File = withContext(ioDispatcher) {
        val csv = sessionsCsv(sessionDao.getFinished(), zone)
        exportDir.deleteRecursively()
        exportDir.mkdirs()
        val stamp = FILE_STAMP.format(Instant.ofEpochMilli(now).atZone(zone))
        File(exportDir, "tally-sessions-$stamp.csv").apply { writeText(CSV_BOM + csv, Charsets.UTF_8) }
    }

    /**
     * 데모 데이터(5주 합성 세션)를 넣는다. 실측 기록과 섞이지 않도록 세션이 하나도 없을 때만 넣고, 넣었으면 true
     * (PLAN.md §4). 데모 앱의 분류는 사용자가 정한 분류를 덮지 않는다.
     */
    suspend fun insertDemoData(now: Long, zone: ZoneId): Boolean = withContext(ioDispatcher) {
        val demo = demoData(now, zone, selfPackage)
        database.withTransaction {
            if (sessionDao.hasAny()) return@withTransaction false
            demo.sessions.forEach { (session, segments) ->
                val id = sessionDao.insert(session)
                database.appSegmentDao().insertAll(segments.map { it.copy(sessionId = id) })
            }
            database.appClassificationDao().insertIfAbsent(demo.classifications)
            true
        }
    }

    private companion object {
        val FILE_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
    }
}
