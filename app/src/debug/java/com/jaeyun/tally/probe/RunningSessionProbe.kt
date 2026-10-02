package com.jaeyun.tally.probe

import com.jaeyun.tally.AppContainer
import com.jaeyun.tally.data.room.StudySession

// 임시 도구(#14 실기기 확인용). 1시간 이상 세션의 체감 입력처럼 오래 기다려야 하는 화면을 바로 시험한다.
// 시작 시각만 당기므로 종료할 때 당긴 구간의 실제 폰 사용 기록도 세션에 들어간다(지표는 실제와 다르다)

/** 진행 중 세션. 없으면 null. DB 조회라 IO 디스패처에서 부른다 */
internal suspend fun loadRunningSession(container: AppContainer): StudySession? =
    container.database.studySessionDao().getUnfinished()

/** 진행 중 세션의 시작 시각을 [minutes]분 앞으로 당겨 그만큼 오래 공부한 것처럼 만든다 */
internal suspend fun extendRunningSession(container: AppContainer, minutes: Int) {
    val dao = container.database.studySessionDao()
    val running = dao.getUnfinished() ?: return
    dao.update(running.copy(startAt = running.startAt - minutes * 60_000L))
}
