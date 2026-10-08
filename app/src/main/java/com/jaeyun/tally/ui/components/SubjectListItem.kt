package com.jaeyun.tally.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Paper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 과목 시트의 한 줄: 이름 · 세션 수 · ⋮ (§8.1.1).
 *
 * 세션 수는 오타를 알아보는 근거라 이름보다 약하게 둔다("수햑 1세션"). ⋮에는 이름 바꾸기와 숨기기만 있고 삭제는 없다.
 * 칩에서 숨긴 과목은 이름을 흐리게 그리고 빗금 친 눈을 붙인다. ⋮에서 다시 표시할 수 있다.
 */
@Composable
fun SubjectListItem(
    name: String,
    sessionCount: Int,
    hidden: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val nameColor = if (hidden) PencilSoft else Pencil
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (hidden) {
            Icon(
                painter = painterResource(R.drawable.ic_hidden),
                contentDescription = stringResource(R.string.subject_hidden_badge),
                tint = PencilSoft,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp),
            )
        }
        // 추가만 하고 아직 쓰지 않은 과목은 "0세션" 대신 세션 수를 비운다(와이어프레임 §0 원칙 2)
        if (sessionCount > 0) {
            Text(
                text = stringResource(R.string.subject_session_count, sessionCount),
                style = MaterialTheme.typography.bodyMedium,
                color = PencilSoft,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.subject_menu, name),
                    tint = nameColor,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Paper) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.subject_rename)) },
                    onClick = {
                        menuOpen = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(if (hidden) R.string.subject_unhide else R.string.subject_hide)) },
                    onClick = {
                        menuOpen = false
                        onToggleHidden()
                    },
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun SubjectListItemPreview() {
    TallyTheme {
        Column(Modifier.notebookPaper().width(360.dp).padding(vertical = 8.dp)) {
            SubjectListItem("수학", sessionCount = 12, hidden = false, onClick = {}, onRename = {}, onToggleHidden = {})
            SubjectListItem("수햑", sessionCount = 1, hidden = false, onClick = {}, onRename = {}, onToggleHidden = {})
            SubjectListItem("코딩 테스트", sessionCount = 3, hidden = true, onClick = {}, onRename = {}, onToggleHidden = {})
            SubjectListItem("코틀린", sessionCount = 0, hidden = false, onClick = {}, onRename = {}, onToggleHidden = {})
        }
    }
}
