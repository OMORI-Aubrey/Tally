package com.jaeyun.tally.data.room

/** [StudySessionDao.observeSubjectStats]의 결과 행. [lastStartAt]은 그 과목 마지막 세션의 시작 시각 */
data class SubjectStat(val name: String, val sessionCount: Int, val lastStartAt: Long)
