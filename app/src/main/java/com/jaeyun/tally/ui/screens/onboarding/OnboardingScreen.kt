package com.jaeyun.tally.ui.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.control.SketchBoxButton
import com.jaeyun.tally.ui.components.control.SketchSlider
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.TallyLogo
import com.jaeyun.tally.ui.components.permission.PermissionSteps
import com.jaeyun.tally.ui.components.text.sessionDurationText
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft

/**
 * 온보딩 1화면 (§8.7, 와이어프레임 §1). 가치 설명, 자기보고, 세션 경계 안내와 수집 고지, 권한 요청을 한 화면에 담는다.
 * 과목은 받지 않는다.
 *
 * 위쪽은 스크롤되고, 권한 단계 안내와 버튼은 화면 아래에 고정해 [권한 설정하기]가 첫 화면에서 늘 보이게 한다.
 * 시스템 설정은 결과를 돌려주지 않아서 화면이 다시 보일 때마다 권한을 확인한다(§6.4). 설정에 다녀와 켰으면 바로 타이머로 간다.
 *
 * @param onDone 온보딩을 마쳤다. 타이머로 간다
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnDone by rememberUpdatedState(onDone)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.done.collect { currentOnDone() }
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    val context = LocalContext.current
    Column(modifier.fillMaxSize()) {
        OnboardingIntro(
            selfReportedHours = uiState.selfReportedHours,
            onHoursChange = viewModel::setSelfReportedHours,
            modifier = Modifier.weight(1f),
        )
        PermissionArea(
            step = uiState.permissionStep,
            onOpenSettings = {
                viewModel.onPermissionRequested()
                // 앱을 지정해 열 수 없어 설치된 앱 전체 목록이 열린다(§6.4)
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            onLater = viewModel::finish,
            onStart = viewModel::finish,
        )
    }
}

@Composable
private fun OnboardingIntro(
    selfReportedHours: Int?,
    onHoursChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val note = MaterialTheme.typography.bodyMedium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 24.dp, bottom = 16.dp),
    ) {
        TallyLogo()
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.onboarding_headline_lead), style = MaterialTheme.typography.titleLarge, color = Pencil)
        // 헤드라인의 숫자가 이 앱의 첫인상이라 가장 크게 쓴다(와이어프레임 §1)
        Row(Modifier.semantics(mergeDescendants = true) {}) {
            Text(
                text = stringResource(R.string.onboarding_headline_value),
                style = MaterialTheme.typography.displayMedium,
                color = BluePen,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = stringResource(R.string.onboarding_headline_tail),
                style = MaterialTheme.typography.titleLarge,
                color = Pencil,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.onboarding_daily_question), style = note, color = PencilSoft)
        SketchSlider(
            value = selfReportedHours,
            onValueChange = onHoursChange,
            valueRange = SelfReportHours,
            valueText = selfReportedHours?.let { sessionDurationText(it * 3600L) },
            label = stringResource(R.string.onboarding_daily_label),
            emptyText = stringResource(R.string.onboarding_daily_empty),
        )
        Spacer(Modifier.height(16.dp))
        OnboardingNote(stringResource(R.string.onboarding_boundary))
        OnboardingNote(stringResource(R.string.onboarding_collected))
        OnboardingNote(stringResource(R.string.onboarding_not_collected))
    }
}

/** `ⓘ` 뒤에 안내 문구. 줄이 바뀌어도 글은 ⓘ 오른쪽에 맞춘다 */
@Composable
private fun OnboardingNote(text: String) {
    val style = MaterialTheme.typography.bodyMedium
    Row(Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.onboarding_note_mark),
            style = style,
            color = PencilSoft,
            modifier = Modifier
                .padding(end = 6.dp)
                .clearAndSetSemantics {},
        )
        Text(text, style = style, color = PencilSoft)
    }
}

@Composable
private fun PermissionArea(
    step: PermissionStep,
    onOpenSettings: () -> Unit,
    onLater: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (step) {
            PermissionStep.GUIDE, PermissionStep.STILL_OFF -> {
                val lead = if (step == PermissionStep.STILL_OFF) R.string.permission_steps_still_off else R.string.permission_steps_lead
                PermissionSteps(stringResource(lead))
                Spacer(Modifier.height(16.dp))
                SketchBoxButton(
                    text = stringResource(R.string.onboarding_permission_open),
                    onClick = onOpenSettings,
                    modifier = Modifier.size(width = 220.dp, height = 72.dp),
                )
                TextButton(onClick = onLater) {
                    Text(stringResource(R.string.onboarding_later), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
                }
            }
            PermissionStep.GRANTED -> {
                Text(stringResource(R.string.onboarding_granted), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
                Spacer(Modifier.height(16.dp))
                SketchBoxButton(
                    text = stringResource(R.string.onboarding_start),
                    onClick = onStart,
                    modifier = Modifier.size(width = 220.dp, height = 72.dp),
                )
            }
        }
    }
}
