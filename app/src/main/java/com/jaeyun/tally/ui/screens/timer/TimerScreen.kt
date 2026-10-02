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
import com.jaeyun.tally.ui.components.TabularDigitsText
import com.jaeyun.tally.ui.components.TallyLogo
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.util.formatElapsedClock
import kotlinx.coroutines.delay

/**
 * 타이머 탭. IDLE은 시작 버튼 + 세션 경계 안내, RUNNING은 경과 시간·과목·종료 버튼만 둔다(§8.5).
 *
 * 시작하면 하단 탭이 내려가는 동안 종료 버튼이 위에서 내려오며 나타나고, 이어서 경과 시간이 왼쪽 글자부터 나타난다.
 * 앱을 다시 열어 RUNNING을 복원할 때는 애니메이션 없이 그린다.
 *
 * @param onRunningChange 진행 중 세션을 확인하면 진행 중 여부를 알린다. 앱이 진행 중에는 하단 탭을 숨긴다(와이어프레임 §2-B)
 * @param onSessionFinished 종료한 세션 id
 */
@Composable
fun TimerScreen(
    onSessionFinished: (sessionId: Long) -> Unit,
    onRunningChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = viewModel(factory = TimerViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState !is TimerUiState.Loading) onRunningChange(uiState is TimerUiState.Running)
    }
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
        TimerUiState.Idle -> TimerIdle(
            onStart = {
                animateEntrance = true
                viewModel.start()
            },
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
private fun TimerIdle(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TallyLogo(Modifier.align(Alignment.Start))
        // TODO(#7) 과목 칩, TODO(#13) 주간 요약 줄
        Spacer(Modifier.weight(1f))
        SketchCircleButton(
            text = stringResource(R.string.timer_start),
            onClick = onStart,
            modifier = Modifier.size(232.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.timer_boundary_hint), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
        // 남는 공간을 위:아래 = 1:0.4로 나눠 시작 버튼을 탭 바에서 조금 띄운다
        Spacer(Modifier.weight(0.4f))
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
            HighlightedText(state.subjectName, style = MaterialTheme.typography.titleLarge, modifier = frameModifier)
        } else {
            Text("", style = MaterialTheme.typography.titleLarge, color = Pencil)
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
