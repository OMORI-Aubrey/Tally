package com.jaeyun.tally.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 설정 탭의 한 줄 (와이어프레임 §6): 이름 아래에 한 줄 설명, 오른쪽에 연필 화살표. 누르면 그 화면으로 간다.
 */
@Composable
fun SettingsEntryRow(title: String, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Pencil)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = PencilSoft,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun SettingsEntryRowPreview() {
    TallyTheme {
        Column(Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, end = 24.dp)) {
            SettingsEntryRow(
                title = stringResource(R.string.settings_app_class),
                description = stringResource(R.string.settings_app_class_description),
                onClick = {},
            )
        }
    }
}
