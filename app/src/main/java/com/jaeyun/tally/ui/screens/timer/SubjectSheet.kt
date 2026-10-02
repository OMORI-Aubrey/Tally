package com.jaeyun.tally.ui.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.data.repository.SubjectSummary
import com.jaeyun.tally.domain.model.SubjectName
import com.jaeyun.tally.ui.components.SubjectListItem
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft

/**
 * 과목 선택 시트 (§8.1.1, 와이어프레임 §2-C). 전체 목록(세션 많은 순) + 새 과목 입력. 독립 라우트가 아니다.
 *
 * 줄을 누르면 그 과목을 고르고 닫는다. ⋮에서 이름 바꾸기·칩에서 숨기기. 삭제는 두지 않는다.
 *
 * 과목이 많아도 시트가 화면을 다 덮지 않도록 내용 높이를 쓸 수 있는 높이의 [MAX_HEIGHT_FRACTION]로 묶고, 목록만 스크롤한다.
 * 키보드가 올라오면 쓸 수 있는 높이가 줄어 목록이 먼저 줄어들고 입력창은 그대로 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubjectSheet(
    subjects: List<SubjectSummary>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onAdd: (String) -> Boolean,
    onRenameRequest: (String) -> Unit,
    onToggleHidden: (SubjectSummary) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        BoxWithConstraints {
            val maxContentHeight = if (constraints.hasBoundedHeight) maxHeight * MAX_HEIGHT_FRACTION else Dp.Unspecified
            Column(Modifier.heightIn(max = maxContentHeight).imePadding()) {
                Text(
                    text = stringResource(R.string.subject_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Pencil,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(8.dp))
                if (subjects.isEmpty()) {
                    EmptySubjects()
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(subjects, key = { it.name }) { subject ->
                            SubjectListItem(
                                name = subject.name,
                                sessionCount = subject.sessionCount,
                                hidden = subject.hidden,
                                onClick = { onPick(subject.name) },
                                onRename = { onRenameRequest(subject.name) },
                                onToggleHidden = { onToggleHidden(subject) },
                            )
                        }
                    }
                }
                NewSubjectField(onAdd = onAdd)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private const val MAX_HEIGHT_FRACTION = 0.6f

@Composable
private fun EmptySubjects() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.subject_sheet_empty_title), style = MaterialTheme.typography.titleLarge, color = Pencil)
        Text(stringResource(R.string.subject_sheet_empty_body), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
    }
}

@Composable
private fun NewSubjectField(onAdd: (String) -> Boolean) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = { if (onAdd(text)) text = "" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= SubjectName.MAX_LENGTH) text = it },
            placeholder = { Text(stringResource(R.string.subject_new_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
        )
        TextButton(onClick = submit) { Text(stringResource(R.string.subject_new_confirm)) }
    }
}
