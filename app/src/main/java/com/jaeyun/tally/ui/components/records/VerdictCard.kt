package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.Verdict
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.components.text.totalDurationText
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme
import com.jaeyun.tally.util.Josa

/**
 * 판정 (§7.2, §8.4 (1)): 판정 문구를 이 화면에서 가장 크게 파란 볼펜으로 쓰고, 그 아래 판정이 어느 수치에서 나왔는지 근거를 둔다.
 *
 * ```
 * 지난 4주보다 잘하고 있어요
 * 최장 평균 34분 (평소 38분)
 * 집중 밀도 78% (평소 71%)
 * 순공 최대 21h (평소 19h)
 * ```
 *
 * 판정은 집중 밀도로 정하지만 근거는 1차 지표인 최장 구간을 맨 앞에 둔다(§7.2.2). 기준선이 세션 `LFS`의 평균이라
 * 이번 주도 평균으로 견준다(PLAN.md §4). 부정적인 판정도 이번 주 가장 긴 구간([longestSec])을 먼저 말한다(§5.6).
 *
 * 총량 가드가 판정을 내린 주([clamped])는 판정 문구 대신 전용 문구와 그 근거를 쓴다(§7.2.1):
 * `이번 주는 짧게 자주 하셨네요. 집중 밀도는 82%로 좋았어요` / `집중 밀도 82% · 4주 기준선 71%` / `순공 최대 6h 12m (평소 21h)`
 *
 * 0인 값은 그 줄을 감추고, 기준선 값이 0이면 `(평소 …)`를 붙이지 않는다(§8.5.1).
 *
 * @param averageLfsSec 이번 주 세션 `LFS`의 평균
 * @param baselineLfsSec 기준선 `LFS`(직전 4주 세션 평균)
 * @param focusSec 이번 주 순공 총량
 * @param baselineWeeklyFocusSec 기준선 주간 순공 총량(직전 4주 합 / 4)
 */
@Composable
fun VerdictCard(
    verdict: Verdict,
    clamped: Boolean,
    longestSec: Int,
    averageLfsSec: Int,
    baselineLfsSec: Int,
    densityPct: Int,
    baselineDensityPct: Int,
    focusSec: Int,
    baselineWeeklyFocusSec: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = if (clamped) stringResource(R.string.verdict_clamped, densityPct) else verdictText(verdict, longestSec),
            style = MaterialTheme.typography.headlineMedium,
            color = BluePen,
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (clamped) {
                BasisLine(stringResource(R.string.verdict_basis_clamped_density, densityPct, baselineDensityPct))
            } else {
                if (averageLfsSec > 0) {
                    val lfs = stringResource(R.string.verdict_basis_lfs, sessionDurationText(averageLfsSec.toLong()))
                    BasisLine(
                        if (baselineLfsSec > 0) {
                            stringResource(R.string.verdict_basis_usual, lfs, sessionDurationText(baselineLfsSec.toLong()))
                        } else {
                            lfs
                        },
                    )
                }
                if (densityPct > 0) {
                    val density = stringResource(R.string.verdict_basis_density, densityPct)
                    BasisLine(
                        if (baselineDensityPct > 0) stringResource(R.string.verdict_basis_usual_density, density, baselineDensityPct) else density,
                    )
                }
            }
            if (focusSec > 0) {
                val focus = stringResource(R.string.verdict_basis_focus, stringResource(R.string.focus_max, totalDurationText(focusSec.toLong())))
                BasisLine(
                    if (baselineWeeklyFocusSec > 0) {
                        stringResource(R.string.verdict_basis_usual, focus, totalDurationText(baselineWeeklyFocusSec.toLong()))
                    } else {
                        focus
                    },
                )
            }
        }
    }
}

/** 판정 5구간의 문구 (§7.2). 부정적인 두 구간은 이번 주 최장 구간을 먼저 말한다 */
@Composable
private fun verdictText(verdict: Verdict, longestSec: Int): String {
    val longest = sessionDurationText(longestSec.toLong())
    return when (verdict) {
        Verdict.BEST -> stringResource(R.string.verdict_best)
        Verdict.BETTER -> stringResource(R.string.verdict_better)
        Verdict.STEADY -> stringResource(R.string.verdict_steady)
        Verdict.SLIPPED -> stringResource(R.string.verdict_slipped, longest, Josa.objectOf(longest))
        Verdict.TOUGH -> stringResource(R.string.verdict_tough, longest, Josa.pastCopulaOf(longest))
    }
}

@Composable
private fun BasisLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
}

@Preview(widthDp = 360)
@Composable
private fun VerdictCardPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            VerdictCard(
                verdict = Verdict.BETTER, clamped = false, longestSec = 47 * 60,
                averageLfsSec = 34 * 60, baselineLfsSec = 38 * 60,
                densityPct = 78, baselineDensityPct = 71,
                focusSec = 21 * 3600, baselineWeeklyFocusSec = 19 * 3600,
            )
            VerdictCard(
                verdict = Verdict.TOUGH, clamped = false, longestSec = 40,
                averageLfsSec = 25 * 60, baselineLfsSec = 38 * 60,
                densityPct = 58, baselineDensityPct = 71,
                focusSec = 15 * 3600, baselineWeeklyFocusSec = 19 * 3600,
            )
            VerdictCard(
                verdict = Verdict.STEADY, clamped = true, longestSec = 52 * 60,
                averageLfsSec = 40 * 60, baselineLfsSec = 38 * 60,
                densityPct = 82, baselineDensityPct = 71,
                focusSec = 6 * 3600 + 12 * 60, baselineWeeklyFocusSec = 21 * 3600,
            )
        }
    }
}
