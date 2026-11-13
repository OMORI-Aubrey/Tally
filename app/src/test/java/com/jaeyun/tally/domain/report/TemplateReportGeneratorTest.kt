package com.jaeyun.tally.domain.report

import com.jaeyun.tally.domain.model.DistractApp
import com.jaeyun.tally.domain.model.ReportSentence
import com.jaeyun.tally.domain.model.WeeklyReportInput
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** 세션 18회, 공부 27시간 30분, 순공 21시간 24분. 앞 주·앞선 기록·이탈은 없다 */
private fun input(
    focusSec: Int = 21 * 3600 + 24 * 60,
    previousFocusSec: Int? = null,
    longestSec: Int = 47 * 60,
    previousBestLfsSec: Int? = null,
    interruptionCount: Int = 0,
    distractApps: List<DistractApp> = emptyList(),
) = WeeklyReportInput(
    sessionCount = 18,
    sittingSec = 27 * 3600 + 30 * 60,
    focusSec = focusSec,
    previousFocusSec = previousFocusSec,
    longestSec = longestSec,
    previousBestLfsSec = previousBestLfsSec,
    interruptionCount = interruptionCount,
    distractApps = distractApps,
)

private val TOTALS = ReportSentence.Totals(18, 27 * 3600 + 30 * 60, 21 * 3600 + 24 * 60)

class TemplateReportGeneratorTest {

    private val generator: ReportGenerator = TemplateReportGenerator()

    @Test
    fun `사실이 모두 있으면 총량, 증감, 기록 갱신, 이탈 순서로 쓴다`() = runTest {
        val sentences = generator.generate(
            input(
                previousFocusSec = 18 * 3600 + 4 * 60,
                previousBestLfsSec = 38 * 60,
                interruptionCount = 31,
                distractApps = listOf(DistractApp("YouTube", 9, 3000), DistractApp("카카오톡", 12, 600)),
            ),
        )

        assertEquals(
            listOf(
                TOTALS,
                ReportSentence.FocusChange(3 * 3600 + 20 * 60),
                ReportSentence.NewRecord(47 * 60),
                ReportSentence.MainDistraction("카카오톡", 12, 31),
            ),
            sentences,
        )
    }

    @Test
    fun `첫 주에 이탈이 없으면 총량만 쓴다`() = runTest {
        assertEquals(listOf(TOTALS), generator.generate(input()))
    }

    @Test
    fun `순공 증감은 화면에 보이는 자릿수로 견준다`() = runTest {
        // 21시간 24분 50초와 21시간 24분 10초는 같아 보인다
        assertEquals(ReportSentence.FocusChange(0), generator.generate(input(focusSec = 77_090, previousFocusSec = 77_050))[1])
        assertEquals(ReportSentence.FocusChange(-40 * 60), generator.generate(input(focusSec = 3 * 3600, previousFocusSec = 3 * 3600 + 40 * 60 + 30))[1])
        // 1분 미만은 초까지 보인다
        assertEquals(ReportSentence.FocusChange(10), generator.generate(input(focusSec = 30, previousFocusSec = 20))[1])
    }

    @Test
    fun `앞선 기록과 같으면 갱신이라고 쓰지 않는다`() = runTest {
        assertEquals(listOf(TOTALS), generator.generate(input(previousBestLfsSec = 47 * 60)))
    }

    @Test
    fun `초만 길어 같은 숫자로 보이면 갱신이라고 쓰지 않는다`() = runTest {
        // 47분 40초와 47분 10초는 둘 다 `47분`으로 보인다
        assertEquals(listOf(TOTALS), generator.generate(input(longestSec = 47 * 60 + 40, previousBestLfsSec = 47 * 60 + 10)))
        // 1분 미만은 초까지 보여 1초만 길어도 갱신이다
        assertEquals(ReportSentence.NewRecord(41), generator.generate(input(longestSec = 41, previousBestLfsSec = 40)).last())
    }

    @Test
    fun `이탈이 가장 많은 앱을 고르고 횟수가 같으면 시간이 긴 앱이다`() = runTest {
        val sentences = generator.generate(
            input(interruptionCount = 10, distractApps = listOf(DistractApp("메신저", 4, 100), DistractApp("영상", 4, 900))),
        )

        assertEquals(ReportSentence.MainDistraction("영상", 4, 10), sentences.last())
    }

    @Test
    fun `이탈이 없거나 앱을 모르거나 앱 횟수가 합보다 크면 이탈 문장을 쓰지 않는다`() = runTest {
        // 화면만 켠 구간뿐이라 앱이 없다
        assertEquals(listOf(TOTALS), generator.generate(input(interruptionCount = 3)))
        assertEquals(listOf(TOTALS), generator.generate(input(interruptionCount = 0, distractApps = listOf(DistractApp("영상", 1, 60)))))
        assertEquals(listOf(TOTALS), generator.generate(input(interruptionCount = 2, distractApps = listOf(DistractApp("영상", 3, 60)))))
    }
}
