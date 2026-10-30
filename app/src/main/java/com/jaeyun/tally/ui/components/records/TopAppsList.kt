package com.jaeyun.tally.ui.components.records

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/** 공부 중 켠 앱 한 줄. [count]는 딴짓으로 들어간 횟수, [durationSec]는 그 시간의 합 */
data class TopApp(val appLabel: String, val count: Int, val durationSec: Int)

/**
 * 공부 중 켠 앱 상위 5개 (§8.4 (2)): `1  YouTube ······ 12회  1시간 8분`. 횟수와 시간을 함께 둔다 —
 * 자주 짧게 켠 앱과 드물게 길게 켠 앱은 대책이 다르다. 앱 이름이 길면 말줄임한다.
 */
@Composable
fun TopAppsList(apps: List<TopApp>, modifier: Modifier = Modifier) {
    Column(modifier) {
        apps.forEachIndexed { index, app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) { },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.bodyLarge, color = PencilSoft, modifier = Modifier.width(28.dp))
                Text(
                    text = app.appLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Pencil,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.count_times, app.count.toString()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Pencil,
                    modifier = Modifier.padding(start = 12.dp),
                )
                Text(
                    text = sessionDurationText(app.durationSec.toLong()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Pencil,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun TopAppsListPreview() {
    TallyTheme {
        TopAppsList(
            apps = listOf(
                TopApp("YouTube", count = 12, durationSec = 68 * 60),
                TopApp("카카오톡", count = 31, durationSec = 42 * 60),
                TopApp("아주 긴 이름의 앱이 들어가면 말줄임", count = 2, durationSec = 50),
            ),
            modifier = Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 8.dp),
        )
    }
}
