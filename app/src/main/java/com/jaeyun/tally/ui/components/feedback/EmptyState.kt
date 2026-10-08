package com.jaeyun.tally.ui.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 빈 상태 안내 (§8.5.1). 데이터가 아직 없는 자리에 숫자 대신 문장 하나와 덧붙이는 설명([detail])을 가운데 둔다.
 *
 * 0이나 빈칸(`--분`)을 보이지 않는다. 사용자 행동으로 채워지는 자리는 무엇을 하면 채워지는지 알리고,
 * 조건이 분명해 조용히 감출 자리(누적 섹션 등)에는 쓰지 않는다. 놓을 자리와 여백은 화면이 정한다.
 */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, detail: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = PencilSoft, textAlign = TextAlign.Center)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = PencilSoft, textAlign = TextAlign.Center)
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun EmptyStatePreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            EmptyState(stringResource(R.string.records_empty))
            EmptyState(stringResource(R.string.subject_sheet_empty_title), detail = stringResource(R.string.subject_sheet_empty_body))
        }
    }
}
