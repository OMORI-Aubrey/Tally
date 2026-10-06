package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 탭 밖 전체 화면(세션 리포트, 앱 분류)의 맨 위 줄: `← 닫기 ········· 세션 리포트`. 왼쪽은 돌아가기, 오른쪽은 화면 이름이다.
 * 노트 위에 연필로 적은 것처럼 펜 글씨로만 쓴다.
 */
@Composable
fun NoteTopBar(title: String, backLabel: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
            // 요약 줄의 화살표를 뒤집어 돌아가는 화살표로 쓴다
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = PencilSoft,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f),
            )
            Text(backLabel, style = MaterialTheme.typography.bodyMedium, color = PencilSoft, modifier = Modifier.padding(start = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
    }
}

@Preview(widthDp = 360)
@Composable
private fun NoteTopBarPreview() {
    TallyTheme {
        Column(Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, end = 24.dp)) {
            NoteTopBar(stringResource(R.string.report_title), stringResource(R.string.report_close), onBack = {})
            NoteTopBar(stringResource(R.string.app_class_title), stringResource(R.string.app_class_back), onBack = {})
        }
    }
}
