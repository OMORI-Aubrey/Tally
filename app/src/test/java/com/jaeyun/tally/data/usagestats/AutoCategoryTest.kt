package com.jaeyun.tally.data.usagestats

import android.content.pm.ApplicationInfo
import com.jaeyun.tally.domain.model.Category.ALLOWED
import com.jaeyun.tally.domain.model.Category.DISTRACT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCategoryTest {

    private fun facts(
        isSelf: Boolean = false,
        isDefaultLauncher: Boolean = false,
        isStudyApp: Boolean = false,
        isBrowser: Boolean = false,
        isPhone: Boolean = false,
        appCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED,
        legacyGameFlag: Boolean = false,
        isSystemApp: Boolean = false,
    ) = AppFacts(isSelf, isDefaultLauncher, isStudyApp, isBrowser, isPhone, appCategory, legacyGameFlag, isSystemApp)

    @Test
    fun `사용자가 설치한 앱은 딴짓이 기본값`() {
        // 토스(생산성), 모르는 앱(미지정) 모두 딴짓
        assertEquals(DISTRACT to AutoCategoryReason.INSTALLED, autoCategory(facts(appCategory = ApplicationInfo.CATEGORY_PRODUCTIVITY)))
        assertEquals(DISTRACT to AutoCategoryReason.INSTALLED, autoCategory(facts()))
    }

    @Test
    fun `본 앱·기본 홈·공부 앱·시스템 앱은 허용`() {
        assertEquals(ALLOWED to AutoCategoryReason.SELF, autoCategory(facts(isSelf = true)))
        assertEquals(ALLOWED to AutoCategoryReason.LAUNCHER, autoCategory(facts(isDefaultLauncher = true)))
        assertEquals(ALLOWED to AutoCategoryReason.STUDY_APP, autoCategory(facts(isStudyApp = true)))
        assertEquals(ALLOWED to AutoCategoryReason.SYSTEM, autoCategory(facts(isSystemApp = true)))
    }

    @Test
    fun `브라우저·전화·SNS·영상·게임은 딴짓`() {
        assertEquals(DISTRACT to AutoCategoryReason.BROWSER, autoCategory(facts(isBrowser = true)))
        assertEquals(DISTRACT to AutoCategoryReason.PHONE, autoCategory(facts(isPhone = true)))
        listOf(ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_GAME).forEach {
            assertEquals(DISTRACT to AutoCategoryReason.DISTRACT_CATEGORY, autoCategory(facts(appCategory = it)))
        }
        assertEquals(DISTRACT to AutoCategoryReason.DISTRACT_CATEGORY, autoCategory(facts(legacyGameFlag = true)))
    }

    @Test
    fun `미리 깔린 시스템 앱이라도 브라우저·전화·영상이면 딴짓`() {
        // 유튜브, 삼성 인터넷, 삼성 전화처럼 기기에 미리 깔린 앱
        assertEquals(DISTRACT, autoCategory(facts(isSystemApp = true, appCategory = ApplicationInfo.CATEGORY_VIDEO)).first)
        assertEquals(DISTRACT, autoCategory(facts(isSystemApp = true, isBrowser = true)).first)
        assertEquals(DISTRACT, autoCategory(facts(isSystemApp = true, isPhone = true)).first)
    }

    @Test
    fun `공부 앱은 영상 카테고리여도 허용`() {
        assertEquals(ALLOWED to AutoCategoryReason.STUDY_APP, autoCategory(facts(isStudyApp = true, appCategory = ApplicationInfo.CATEGORY_VIDEO)))
    }

    @Test
    fun `우선순위 — 본 앱·홈이 가장 먼저`() {
        assertEquals(AutoCategoryReason.LAUNCHER, autoCategory(facts(isDefaultLauncher = true, isBrowser = true)).second)
        assertEquals(ALLOWED, autoCategory(facts(isDefaultLauncher = true, appCategory = ApplicationInfo.CATEGORY_GAME)).first)
    }

    @Test
    fun `줌은 공부 앱 목록에 있다`() {
        // 줌은 ACTION_DIAL에 응답하고 기본 딴짓이라, 목록이 없으면 딴짓이 된다 (#11 실기기 확인)
        assertTrue("us.zoom.videomeetings" in STUDY_APPS)
    }
}
