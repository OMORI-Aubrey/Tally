package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.htmlEncode
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.ReportSentence
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.TallyTheme
import com.jaeyun.tally.util.Josa
import kotlin.math.absoluteValue

/**
 * 요약 문단 (§7.4): 생성기가 고른 문장을 한 줄에 하나씩 쓰고, 값(횟수·시간·앱 이름)은 굵게 쓴다.
 *
 * ```
 * 세션 **18회**, 공부 **27시간 30분** 중 순공은 최대 **21시간 24분**이었어요.
 * 지난주보다 순공이 **3시간 20분** 늘었어요.
 * 최장 구간 기록을 **47분**으로 갱신했어요.
 * 이탈 31회 중 **12회**는 **카카오톡**에서 있었어요.
 * ```
 *
 * 문장 속 시간은 총량도 한글 표기(`21시간 24분`)로 쓴다 — `21h 24m` 뒤에는 붙일 조사를 정할 수 없다(PLAN.md §4).
 * 굵은 글씨는 문자열 리소스의 `<b>`로 표시한다. 앱 이름은 HTML로 이스케이프해 넣는다.
 */
@Composable
fun WeekSummary(sentences: List<ReportSentence>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sentences.forEach { sentence ->
            Text(
                text = AnnotatedString.fromHtml(summarySentenceHtml(sentence)),
                style = MaterialTheme.typography.bodyLarge,
                color = Pencil,
            )
        }
    }
}

@Composable
private fun summarySentenceHtml(sentence: ReportSentence): String = when (sentence) {
    is ReportSentence.Totals -> {
        val focus = sessionDurationText(sentence.focusSec.toLong())
        stringResource(
            R.string.summary_totals,
            sentence.sessionCount,
            sessionDurationText(sentence.sittingSec.toLong()),
            focus,
            Josa.pastCopulaOf(focus),
        )
    }
    is ReportSentence.FocusChange -> {
        val diff = sessionDurationText(sentence.diffSec.absoluteValue.toLong())
        when {
            sentence.diffSec > 0 -> stringResource(R.string.summary_focus_up, diff)
            sentence.diffSec < 0 -> stringResource(R.string.summary_focus_down, diff)
            else -> stringResource(R.string.summary_focus_same)
        }
    }
    is ReportSentence.NewRecord -> {
        val longest = sessionDurationText(sentence.lfsSec.toLong())
        stringResource(R.string.summary_record, longest, Josa.directionOf(longest))
    }
    is ReportSentence.MainDistraction ->
        stringResource(R.string.summary_distraction, sentence.totalCount, sentence.count, sentence.appLabel.htmlEncode())
}

@Preview(widthDp = 360)
@Composable
private fun WeekSummaryPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            WeekSummary(
                listOf(
                    ReportSentence.Totals(sessionCount = 18, sittingSec = 27 * 3600 + 30 * 60, focusSec = 21 * 3600 + 24 * 60),
                    ReportSentence.FocusChange(diffSec = 3 * 3600 + 20 * 60),
                    ReportSentence.NewRecord(lfsSec = 47 * 60),
                    ReportSentence.MainDistraction(appLabel = "카카오톡", count = 12, totalCount = 31),
                ),
            )
            // 첫 주: 앞 주·앞선 기록이 없고 이탈도 없었다
            WeekSummary(listOf(ReportSentence.Totals(sessionCount = 2, sittingSec = 50 * 60, focusSec = 50 * 60)))
            WeekSummary(
                listOf(
                    ReportSentence.Totals(sessionCount = 5, sittingSec = 4 * 3600, focusSec = 3 * 3600),
                    ReportSentence.FocusChange(diffSec = -40 * 60),
                ),
            )
        }
    }
}
