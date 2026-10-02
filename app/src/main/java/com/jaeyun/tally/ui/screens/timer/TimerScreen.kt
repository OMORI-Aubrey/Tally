package com.jaeyun.tally.ui.screens.timer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.HighlightedText
import com.jaeyun.tally.ui.components.NotebookPaper
import com.jaeyun.tally.ui.components.SketchBoxButton
import com.jaeyun.tally.ui.components.SketchCircleButton
import com.jaeyun.tally.ui.components.SubjectChipRow
import com.jaeyun.tally.ui.components.TabularDigitsText
import com.jaeyun.tally.ui.components.TallyLogo
import com.jaeyun.tally.ui.components.WeeklySummaryLine
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.util.formatElapsedClock
import kotlinx.coroutines.delay

/**
 * 타이머 탭. IDLE은 주간 요약 줄 + 과목 칩 + 시작 버튼 + 세션 경계 안내, RUNNING은 경과 시간·과목·종료 버튼만 둔다(§8.5).
 * 과목은 선택 사항이라 시작 버튼은 항상 누를 수 있다. `+` 칩은 과목 시트를 연다(§8.1.1).
 *
 * 시작하면 하단 탭이 내려가는 동안 종료 버튼이 위에서 내려오며 나타나고, 이어서 경과 시간이 왼쪽 글자부터 나타난다.
 * 앱을 다시 열어 RUNNING을 복원할 때는 애니메이션 없이 그린다.
 *
 * @param onRunningChange 진행 중 세션을 확인하면 진행 중 여부를 알린다. 앱이 진행 중에는 하단 탭을 숨긴다(와이어프레임 §2-B)
 * @param onSessionFinished 종료한 세션. 체감을 물을 세션이면 리포트 전에 체감 입력으로 간다
 * @param onOpenRecords 주간 요약 줄을 눌렀다. 기록 탭으로 간다
 */
@Composable
fun TimerScreen(
    onSessionFinished: (FinishedSession) -> Unit,
    onRunningChange: (Boolean) -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = viewModel(factory = TimerViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val running = when (uiState) {
        TimerUiState.Loading -> null
        is TimerUiState.Idle -> false
        is TimerUiState.Running -> true
    }
    LaunchedEffect(running) { if (running != null) onRunningChange(running) }
    // 이 화면에서 시작을 눌렀을 때만 RUNNING 등장 애니메이션을 한다
    var animateEntrance by remember { mutableStateOf(false) }

    val currentOnSessionFinished by rememberUpdatedState(onSessionFinished)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.finishedSessions.collect { currentOnSessionFinished(it) }
        }
    }

    when (val state = uiState) {
        TimerUiState.Loading -> Box(modifier.fillMaxSize())
        is TimerUiState.Idle -> TimerIdle(
            state = state,
            onStart = {
                animateEntrance = true
                viewModel.start()
            },
            onToggleSubject = viewModel::toggleSubject,
            onPickSubject = viewModel::selectSubject,
            onAddSubject = viewModel::addSubject,
            onRenameSubject = viewModel::renameSubject,
            onSetSubjectHidden = viewModel::setSubjectHidden,
            onOpenRecords = onOpenRecords,
            modifier = modifier,
        )
        is TimerUiState.Running -> TimerRunning(
            state = state,
            animateEntrance = animateEntrance,
            onFinish = {
                animateEntrance = false
                viewModel.finish()
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun TimerIdle(
    state: TimerUiState.Idle,
    onStart: () -> Unit,
    onToggleSubject: (String) -> Unit,
    onPickSubject: (String) -> Unit,
    onAddSubject: (String) -> Boolean,
    onRenameSubject: (oldName: String, newName: String) -> Boolean,
    onSetSubjectHidden: (name: String, hidden: Boolean) -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    // 이름을 바꾸는 중인 과목. 칩 길게 누르기와 시트 ⋮ 양쪽에서 연다
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TallyLogo(Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        WeeklySummaryLine(state.weeklySummary, onClick = onOpenRecords, modifier = Modifier.align(Alignment.Start))
        // 남는 공간을 요약 줄~과목 : 과목~시작 버튼 : 아래 = 0.2 : 0.7 : 0.5로 나눈다.
        // 요약 줄이 들어온 만큼 위쪽 비중을 줄여 시작 버튼 자리를 #6 때와 비슷하게 둔다
        Spacer(Modifier.weight(0.2f))
        Column(Modifier.align(Alignment.Start)) {
            Text(stringResource(R.string.subject_chips_label), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
            Spacer(Modifier.height(4.dp))
            SubjectChipRow(
                subjects = state.chips,
                selected = state.selected,
                onToggle = onToggleSubject,
                onAdd = { sheetOpen = true },
                onRename = { renaming = it },
                onHide = { onSetSubjectHidden(it, true) },
            )
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.weight(0.7f))
        SketchCircleButton(
            text = stringResource(R.string.timer_start),
            onClick = onStart,
            modifier = Modifier.size(232.dp),
        )
        Spacer(Modifier.height(16.dp))
        // 과목이 없으면 끊을 경계도 없으니, 과목 없이 시작해도 된다고 알린다(§8.5.1)
        val hint = if (state.chips.isEmpty()) R.string.timer_no_subject_hint else R.string.timer_boundary_hint
        Text(stringResource(hint), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
        Spacer(Modifier.weight(0.5f))
    }

    if (sheetOpen) {
        SubjectSheet(
            subjects = state.sheetSubjects,
            onDismiss = { sheetOpen = false },
            onPick = {
                onPickSubject(it)
                sheetOpen = false
            },
            onAdd = { raw -> onAddSubject(raw).also { added -> if (added) sheetOpen = false } },
            onRenameRequest = { renaming = it },
            onToggleHidden = { onSetSubjectHidden(it.name, !it.hidden) },
        )
    }

    val renameTarget = state.sheetSubjects.firstOrNull { it.name == renaming }
    if (renameTarget != null) {
        RenameSubjectDialog(
            subject = renameTarget,
            existing = state.sheetSubjects,
            onDismiss = { renaming = null },
            onConfirm = { newName -> if (onRenameSubject(renameTarget.name, newName)) renaming = null },
        )
    }
}

@Composable
private fun TimerRunning(
    state: TimerUiState.Running,
    animateEntrance: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elapsedText = formatElapsedClock(rememberElapsedMillis(state.startAt))

    // 0 → 1. 과목·종료 버튼이 먼저(하단 탭이 내려가는 동안), 경과 시간 글자가 그다음
    val frame = remember { Animatable(if (animateEntrance) 0f else 1f) }
    val digits = remember { Animatable(if (animateEntrance) 0f else 1f) }
    LaunchedEffect(Unit) {
        frame.animateTo(1f, tween(FRAME_ENTER_MILLIS, easing = FastOutSlowInEasing))
        digits.animateTo(1f, tween(DIGITS_REVEAL_MILLIS, easing = LinearEasing))
    }
    val dropPx = with(LocalDensity.current) { 24.dp.toPx() }
    val frameModifier = Modifier.graphicsLayer {
        alpha = frame.value
        translationY = (frame.value - 1f) * dropPx
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 남는 공간을 위:아래 = 0.8:1.2로 나눠 경과 시간을 가운데보다 조금 위에 둔다
        Spacer(Modifier.weight(0.8f))
        // 과목이 없으면 빈 줄로 둔다. "(과목 없음)"으로 채우지 않는다(와이어프레임 §0 원칙 6)
        if (state.subjectName != null) {
            HighlightedText(state.subjectName, style = MaterialTheme.typography.headlineLarge, modifier = frameModifier)
        } else {
            Text("", style = MaterialTheme.typography.headlineLarge, color = Pencil)
        }
        Spacer(Modifier.height(8.dp))
        TabularDigitsText(
            text = elapsedText,
            style = MaterialTheme.typography.displayLarge,
            color = BluePen,
            charAlpha = { i -> revealAlpha(digits.value, i, elapsedText.length) },
        )
        Spacer(Modifier.weight(1.2f))
        SketchBoxButton(
            text = stringResource(R.string.timer_finish),
            onClick = onFinish,
            modifier = frameModifier.size(width = 200.dp, height = 72.dp),
        )
    }
}

private const val FRAME_ENTER_MILLIS = 400
private const val DIGITS_REVEAL_MILLIS = 700

/** 글자 [index]의 투명도. [progress]가 0→1로 가는 동안 왼쪽 글자부터 차례로, 앞 글자와 겹치며 나타난다 */
private fun revealAlpha(progress: Float, index: Int, length: Int): Float {
    val overlap = 2f
    return ((progress * (length + overlap) - index) / overlap).coerceIn(0f, 1f)
}

/** 화면이 보일 때만 초 단위로 다시 그린다. 경과 시간의 초가 바뀌는 순간에 맞춰 깨어난다 */
@Composable
private fun rememberElapsedMillis(startAt: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(startAt, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                val elapsed = now - startAt
                delay(if (elapsed < 0) 1_000 else 1_000 - elapsed % 1_000)
            }
        }
    }
    return (now - startAt).coerceAtLeast(0)
}
