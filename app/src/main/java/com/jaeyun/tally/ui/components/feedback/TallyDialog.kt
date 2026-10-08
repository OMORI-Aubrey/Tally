package com.jaeyun.tally.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.control.SketchBoxButton
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Paper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/** 다이얼로그 선택지 하나 */
class TallyDialogChoice(val label: String, val onClick: () -> Unit)

/**
 * 종이 노트 다이얼로그 (와이어프레임 §8). 세션을 끝낸 뒤 리포트 전에 묻는 것과 재부팅 뒤 미완료 세션 복구에 쓴다.
 *
 * 질문 [message], 덧붙이는 설명 [detail], 손그림 버튼 선택지를 위에서부터 둔다. 답이 기록을 바꾸므로 뒤로가기나 바깥을
 * 눌러 닫을 수 없다. 반드시 하나를 고른다.
 */
@Composable
fun TallyDialog(
    message: String,
    choices: List<TallyDialogChoice>,
    detail: String? = null,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        TallyDialogContent(message, choices, detail)
    }
}

@Composable
private fun TallyDialogContent(message: String, choices: List<TallyDialogChoice>, detail: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Paper, RoundedCornerShape(16.dp))
            .padding(24.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Pencil)
        if (detail != null) {
            Spacer(Modifier.height(5.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        }
        Spacer(Modifier.height(20.dp))
        choices.forEachIndexed { index, choice ->
            if (index > 0) Spacer(Modifier.height(8.dp))
            SketchBoxButton(
                text = choice.label,
                onClick = choice.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
    }
}

// Dialog는 창을 따로 띄워 미리보기에 그려지지 않아 안쪽만 그린다
@Preview(widthDp = 360)
@Composable
private fun TallyDialogPreview() {
    TallyTheme {
        Box(Modifier.notebookPaper().padding(24.dp)) {
            TallyDialogContent(
                message = stringResource(R.string.dialog_screen_off, "23:14", "00:51"),
                choices = listOf(
                    TallyDialogChoice(stringResource(R.string.dialog_screen_off_studied)) {},
                    TallyDialogChoice(stringResource(R.string.dialog_screen_off_away)) {},
                ),
                detail = stringResource(R.string.dialog_screen_off_detail),
            )
        }
    }
}
