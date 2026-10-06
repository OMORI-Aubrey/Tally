package com.jaeyun.tally.ui.screens.timer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.TallyDialog
import com.jaeyun.tally.ui.components.TallyDialogChoice
import com.jaeyun.tally.ui.components.sessionDurationText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val DateTimeFormat = DateTimeFormatter.ofPattern("M/d HH:mm")

private fun Long.format(formatter: DateTimeFormatter): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(formatter)

/**
 * 타이머 화면의 다이얼로그 (와이어프레임 §8). 첫 선택지가 기록을 그대로 두는 쪽이고, 두 번째가 바꾸는 쪽이다.
 *
 * @param onAnswerScreenOff ① true면 `[자리 비웠어요]`
 * @param onAnswerOverLong ② true면 `[종료를 잊었어요]`
 * @param onAnswerRecovery ③ true면 `[삭제]`
 */
@Composable
internal fun TimerDialogHost(
    dialog: TimerDialog,
    onAnswerScreenOff: (away: Boolean) -> Unit,
    onAnswerOverLong: (forgot: Boolean) -> Unit,
    onAnswerRecovery: (delete: Boolean) -> Unit,
) {
    when (dialog) {
        is TimerDialog.LongScreenOff -> TallyDialog(
            message = stringResource(R.string.dialog_screen_off, dialog.startAt.format(TimeFormat), dialog.endAt.format(TimeFormat)),
            detail = stringResource(R.string.dialog_screen_off_detail),
            choices = listOf(
                TallyDialogChoice(stringResource(R.string.dialog_screen_off_studied)) { onAnswerScreenOff(false) },
                TallyDialogChoice(stringResource(R.string.dialog_screen_off_away)) { onAnswerScreenOff(true) },
            ),
        )
        is TimerDialog.OverLong -> TallyDialog(
            message = stringResource(R.string.dialog_over_long, sessionDurationText(dialog.totalSec.toLong())),
            detail = stringResource(R.string.dialog_over_long_detail),
            choices = listOf(
                TallyDialogChoice(stringResource(R.string.dialog_over_long_studied)) { onAnswerOverLong(false) },
                TallyDialogChoice(stringResource(R.string.dialog_over_long_forgot)) { onAnswerOverLong(true) },
            ),
        )
        is TimerDialog.Recovery -> {
            val started = dialog.startAt.format(DateTimeFormat)
            TallyDialog(
                message = stringResource(R.string.dialog_recovery),
                // 과목이 없으면 자리를 생략한다(와이어프레임 §0 원칙 6)
                detail = if (dialog.subjectName != null) {
                    stringResource(R.string.dialog_recovery_started_subject, dialog.subjectName, started)
                } else {
                    stringResource(R.string.dialog_recovery_started, started)
                },
                choices = listOf(
                    TallyDialogChoice(stringResource(R.string.dialog_recovery_continue)) { onAnswerRecovery(false) },
                    TallyDialogChoice(stringResource(R.string.dialog_recovery_delete)) { onAnswerRecovery(true) },
                ),
            )
        }
    }
}
