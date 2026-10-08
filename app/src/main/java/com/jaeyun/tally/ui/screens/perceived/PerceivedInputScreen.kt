package com.jaeyun.tally.ui.screens.perceived

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.control.NumberBlank
import com.jaeyun.tally.ui.components.control.SketchBoxButton
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft

/**
 * 체감 입력 (§3.4, 와이어프레임 §3). 질문 하나, 빈칸(1시간 이상 세션은 시간·분), 확인과 건너뛰기뿐이다.
 *
 * 실측값은 어디에도 보이지 않는다 — 이 화면의 목적이 편향 없는 자기 예측이다. 건너뛰기와 뒤로가기는 답 없이 리포트로 간다.
 * 리포트를 먼저 보고 돌아와 답하는 길은 없다.
 *
 * @param onDone 답을 저장했거나 건너뛰었다. 리포트로 간다
 */
@Composable
fun PerceivedInputScreen(
    sessionId: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PerceivedInputViewModel = viewModel(factory = PerceivedInputViewModel.factory(sessionId)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnDone by rememberUpdatedState(onDone)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.done.collect { currentOnDone() }
        }
    }
    BackHandler(onBack = viewModel::skip)

    when (val state = uiState) {
        PerceivedInputUiState.Loading -> Box(modifier.fillMaxSize())
        PerceivedInputUiState.Missing -> LaunchedEffect(Unit) { viewModel.skip() }
        is PerceivedInputUiState.Ready -> PerceivedInputForm(
            maxMinutes = state.maxMinutes,
            onSubmit = viewModel::submit,
            onSkip = viewModel::skip,
            modifier = modifier,
        )
    }
}

@Composable
private fun PerceivedInputForm(
    maxMinutes: Int,
    onSubmit: (minutes: Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hourBlank = hasHourBlank(maxMinutes)
    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    val parsed = parsePerceivedMinutes(hours, minutes, maxMinutes)
    val submit = { if (parsed is PerceivedMinutes.Valid) onSubmit(parsed.minutes) }
    val hoursFocus = remember { FocusRequester() }
    val minutesFocus = remember { FocusRequester() }

    // 키패드가 화면을 반쯤 가리므로 위에서부터 쌓고, 좁은 화면에서는 밀어 올려 볼 수 있게 한다
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 48.dp, bottom = 24.dp),
    ) {
        Text(stringResource(R.string.perceived_question_lead), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Text(stringResource(R.string.perceived_question), style = MaterialTheme.typography.headlineLarge, color = Pencil)
        Spacer(Modifier.height(40.dp))
        // 1시간 이상 세션은 `_ 시간 __ 분`. 시간 칸에 세션 길이 안의 숫자를 적으면 분 칸으로 넘어간다
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (hourBlank) {
                NumberBlank(
                    value = hours,
                    onValueChange = {
                        hours = it
                        if (hourFitsSession(it, maxMinutes)) minutesFocus.requestFocus()
                    },
                    unit = stringResource(R.string.hours_unit),
                    maxDigits = 1,
                    label = stringResource(R.string.perceived_hours_label),
                    imeAction = ImeAction.Next,
                    onImeAction = { minutesFocus.requestFocus() },
                    focusRequester = hoursFocus,
                    autoFocus = true,
                )
            }
            NumberBlank(
                value = minutes,
                onValueChange = { minutes = it },
                unit = stringResource(R.string.minutes_unit),
                maxDigits = 2,
                label = stringResource(R.string.perceived_minutes_label),
                imeAction = ImeAction.Done,
                onImeAction = submit,
                focusRequester = minutesFocus,
                autoFocus = !hourBlank,
                onBackspaceWhenEmpty = if (hourBlank) ({ hoursFocus.requestFocus() }) else null,
            )
        }
        // 안내는 늘 그려 두고 보이기만 바꾼다. 빈 글자는 높이가 달라서, 안내가 나타날 때 아래 버튼이 움직인다
        val hint = when (parsed) {
            PerceivedMinutes.MinutesOver59 -> stringResource(R.string.perceived_minutes_over)
            else -> stringResource(R.string.perceived_too_long, sessionDurationText(maxMinutes * 60L))
        }
        val showHint = parsed is PerceivedMinutes.TooLong || parsed is PerceivedMinutes.MinutesOver59
        Text(
            text = hint,
            style = MaterialTheme.typography.labelMedium,
            color = PencilSoft,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .alpha(if (showHint) 1f else 0f)
                .then(if (showHint) Modifier else Modifier.clearAndSetSemantics {}),
        )
        Spacer(Modifier.height(24.dp))
        SketchBoxButton(
            text = stringResource(R.string.perceived_confirm),
            onClick = submit,
            enabled = parsed is PerceivedMinutes.Valid,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 200.dp, height = 72.dp),
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.perceived_skip), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        }
    }
}
