package com.jaeyun.tally.ui.components.settings

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
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.RedPen
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 설정 탭의 한 줄 (와이어프레임 §6): 이름 아래에 한 줄 설명이나 상태.
 *
 * @param onClick null이면 누를 수 없고 이름을 흐리게 쓴다
 * @param description 이름 아래 한 줄. 권한·토글처럼 상태가 있는 줄은 상태를 쓴다
 * @param navigates 누르면 다른 화면(앱 분류, 시스템 설정)으로 간다. 오른쪽에 연필 화살표를 그린다
 * @param warning 되돌릴 수 없는 일(데이터 전체 삭제). 이름을 빨간 펜으로 쓴다
 */
@Composable
fun SettingsRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
    navigates: Boolean = false,
    warning: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    warning -> RedPen
                    onClick == null -> PencilSoft
                    else -> Pencil
                },
            )
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
            }
        }
        if (navigates) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = PencilSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun SettingsRowPreview() {
    TallyTheme {
        Column(Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, end = 24.dp)) {
            SettingsRow(
                title = stringResource(R.string.settings_app_class),
                description = stringResource(R.string.settings_app_class_description),
                onClick = {},
                navigates = true,
            )
            SettingsRow(
                title = stringResource(R.string.settings_permission),
                description = stringResource(R.string.settings_permission_on),
                onClick = {},
                navigates = true,
            )
            SettingsRow(title = stringResource(R.string.settings_delete_all), onClick = {}, warning = true)
        }
    }
}
