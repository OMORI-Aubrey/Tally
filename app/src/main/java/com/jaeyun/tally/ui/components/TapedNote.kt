package com.jaeyun.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.StickyNote
import com.jaeyun.tally.ui.theme.TallyTheme
import com.jaeyun.tally.ui.theme.TapeBlue

/** 마스킹테이프로 붙인 포스트잇. 살짝 기울어져 있다. 격차 포스트잇(§8.2 ⓪), 권한 단계 안내(§8.7)에 쓴다 */
@Composable
fun TapedNote(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    // 테이프가 종이 위로 삐져나오는 자리
    Box(modifier.padding(top = 10.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .rotate(-1.5f)
                .background(StickyNote)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            content = content,
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .rotate(3f)
                .size(width = 64.dp, height = 18.dp)
                .background(TapeBlue),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun TapedNotePreview() {
    TallyTheme {
        Box(Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp)) {
            TapedNote {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.bodyLarge, color = Pencil)
            }
        }
    }
}
