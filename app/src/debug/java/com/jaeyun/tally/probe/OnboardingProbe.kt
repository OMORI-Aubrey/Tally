package com.jaeyun.tally.probe

import com.jaeyun.tally.AppContainer
import kotlinx.coroutines.flow.first

/** 온보딩 상태 (#19). 한 번 마치면 다시 나오지 않아서, 세션 기록을 지우지 않고 다시 시험할 수 있게 한다 */
data class OnboardingInfo(
    val done: Boolean,
    /** 마지막으로 마친 온보딩에서 저장한 값. 건너뛰었으면 null */
    val selfReportedDailyMin: Int?,
)

internal suspend fun loadOnboarding(container: AppContainer): OnboardingInfo {
    val settings = container.settingsStore.settings.first()
    return OnboardingInfo(done = settings.onboardingDone, selfReportedDailyMin = settings.selfReportedDailyMin)
}

/**
 * 온보딩 완료만 되돌린다. 본 앱을 새로 열면 온보딩이 시작 화면이 된다.
 * 세션 기록·앱 분류는 그대로 두고, 자기보고 값은 온보딩을 다시 마칠 때 새로 저장된다.
 */
internal suspend fun resetOnboarding(container: AppContainer) {
    container.settingsStore.setOnboardingDone(false)
}
