package com.jaeyun.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Highlighter
import com.jaeyun.tally.ui.theme.Paper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme
import com.jaeyun.tally.ui.theme.TapeBlue
import com.jaeyun.tally.ui.theme.TapeMint
import com.jaeyun.tally.ui.theme.TapePink

private val TapeColors = listOf(TapeBlue, TapePink, TapeMint)

/** 칩마다 살짝 다르게 기울여 손으로 붙인 테이프처럼 보이게 한다 */
private val TapeTilts = listOf(-2f, 1.5f, -1f, 2f, -1.5f)

/**
 * 마스킹테이프 모양의 과목 칩 + 끝의 `+` 칩 (§8.1.1). 고른 과목은 형광펜 색으로 칠한다.
 *
 * 칩을 누르면 고르고, 고른 칩을 다시 누르면 선택을 푼다(과목 없이 시작). 길게 누르면 [이름 바꾸기]·[칩에서 숨기기]
 * 메뉴가 뜬다. 삭제는 없다(§8.1.1).
 *
 * [subjects]가 비면 `+ 과목 추가 (선택)` 칩 하나만 보인다. 과목은 선택 사항이라 "(선택)"을 붙인다(와이어프레임 §2-A).
 * 칩이 한 줄을 넘으면 다음 줄로 넘어간다.
 */
@Composable
fun SubjectChipRow(
    subjects: List<String>,
    selected: String?,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit,
    onRename: (String) -> Unit,
    onHide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        subjects.forEachIndexed { index, name ->
            SubjectChip(
                name = name,
                index = index,
                selected = name == selected,
                onClick = { onToggle(name) },
                onRename = { onRename(name) },
                onHide = { onHide(name) },
            )
        }
        AddChip(showOptionalLabel = subjects.isEmpty(), onClick = onAdd)
    }
}

@Composable
private fun SubjectChip(
    name: String,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val menuLabel = stringResource(R.string.subject_chip_menu)
    Box {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .rotate(TapeTilts[index % TapeTilts.size])
                .background(if (selected) Highlighter else TapeColors[index % TapeColors.size])
                .combinedClickable(
                    role = Role.RadioButton,
                    onClick = onClick,
                    onLongClick = { menuOpen = true },
                    onLongClickLabel = menuLabel,
                )
                .semantics { this.selected = selected }
                .heightIn(min = 36.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name,
                style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
                color = Pencil,
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
                text = { Text(stringResource(R.string.subject_hide)) },
                onClick = {
                    menuOpen = false
                    onHide()
                },
            )
        }
    }
}

/** 점선 테두리 칩. 과목이 없을 때는 "+ 과목 추가 (선택)", 있을 때는 "+"만 */
@Composable
private fun AddChip(showOptionalLabel: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.subject_add)
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .drawBehind {
                drawRect(Paper)
                drawRect(
                    color = PencilSoft,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .heightIn(min = 36.dp)
            .then(if (showOptionalLabel) Modifier.padding(horizontal = 14.dp) else Modifier.width(44.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(if (showOptionalLabel) R.string.subject_add_optional else R.string.subject_add_short),
            style = MaterialTheme.typography.bodyLarge,
            color = Pencil,
        )
    }
}

@Preview(widthDp = 320)
@Composable
private fun SubjectChipRowPreview() {
    TallyTheme {
        Column(Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
            SubjectChipRow(
                subjects = listOf("코틀린", "자바", "리액트", "한국사", "수학"),
                selected = "코틀린",
                onToggle = {},
                onAdd = {},
                onRename = {},
                onHide = {},
            )
        }
    }
}

@Preview(widthDp = 320)
@Composable
private fun SubjectChipRowEmptyPreview() {
    TallyTheme {
        Column(Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
            SubjectChipRow(subjects = emptyList(), selected = null, onToggle = {}, onAdd = {}, onRename = {}, onHide = {})
        }
    }
}
