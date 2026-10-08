package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.ui.components.control.SketchBoxButton
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.TallyTheme
import com.jaeyun.tally.util.Josa

/**
 * 미분류 앱 질문 (§8.2 ⑥): `"카카오톡"은 어떤 앱인가요?` `[딴짓]` `[허용]`. 리포트 아래에 세션당 2개까지 둔다(§5.1).
 * 질문은 하나, 답은 둘뿐이다.
 *
 * @param enabled 앞의 답을 처리하는 동안 누르지 못하게 한다
 */
@Composable
fun UnclassifiedAppPrompt(
    appLabel: String,
    onAnswer: (Category) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.unclassified_question, appLabel, Josa.topicOf(appLabel)),
            style = MaterialTheme.typography.bodyLarge,
            color = Pencil,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SketchBoxButton(
                text = stringResource(R.string.unclassified_distract),
                onClick = { onAnswer(Category.DISTRACT) },
                enabled = enabled,
                modifier = Modifier.size(width = 112.dp, height = 56.dp),
            )
            SketchBoxButton(
                text = stringResource(R.string.unclassified_allowed),
                onClick = { onAnswer(Category.ALLOWED) },
                enabled = enabled,
                modifier = Modifier.size(width = 112.dp, height = 56.dp),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun UnclassifiedAppPromptPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            UnclassifiedAppPrompt(appLabel = "카카오톡", onAnswer = {})
            UnclassifiedAppPrompt(appLabel = "유튜브", onAnswer = {}, enabled = false)
        }
    }
}
