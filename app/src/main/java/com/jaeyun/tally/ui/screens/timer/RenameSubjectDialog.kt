package com.jaeyun.tally.ui.screens.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.data.repository.SubjectSummary
import com.jaeyun.tally.domain.model.SubjectName
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.util.Josa

/**
 * 과목 이름 바꾸기. 과거 세션까지 바뀌는 유일한 조작이라 영향 범위를 반드시 보여준다(와이어프레임 §2-C).
 * 이미 있는 과목 이름이면 두 과목이 합쳐진다는 것도 알린다. 세션이 없는 과목은 바뀔 기록이 없어 안내하지 않는다.
 * 칩 길게 누르기와 과목 시트 ⋮ 양쪽에서 연다.
 */
@Composable
internal fun RenameSubjectDialog(
    subject: SubjectSummary,
    existing: List<SubjectSummary>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable(subject.name) { mutableStateOf(subject.name) }
    val newName = SubjectName.normalize(text)
    val changed = newName != null && newName != subject.name
    val mergeTarget = existing.firstOrNull { it.name == newName && it.name != subject.name }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subject_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= SubjectName.MAX_LENGTH) text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (changed) onConfirm(text) }),
                )
                if (changed && subject.sessionCount > 0) {
                    Text(
                        text = stringResource(
                            R.string.subject_rename_warning,
                            subject.name,
                            Josa.objectOf(subject.name),
                            newName!!,
                            Josa.directionOf(newName),
                            subject.sessionCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Pencil,
                    )
                }
                if (mergeTarget != null && mergeTarget.sessionCount > 0) {
                    Text(
                        text = stringResource(R.string.subject_rename_merge, mergeTarget.name, mergeTarget.sessionCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Pencil,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = changed) {
                Text(stringResource(R.string.subject_rename_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
