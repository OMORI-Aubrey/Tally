package com.jaeyun.tally.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.feedback.TallyDialog
import com.jaeyun.tally.ui.components.feedback.TallyDialogChoice
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.settings.SettingsRow
import com.jaeyun.tally.ui.theme.PencilSoft

/**
 * 설정 탭 (와이어프레임 §6): 앱 분류, 사용 정보 접근 권한, 데이터 전체 삭제.
 *
 * 권한 줄은 켜져 있든 꺼져 있든 누르면 시스템 설정을 연다 — 켜러 가거나 끄러 간다(§11 철회 가능).
 * 데이터 전체 삭제는 확인 다이얼로그를 거친다. 지우면 온보딩 전 상태가 되어 [onDataDeleted]로 온보딩부터 다시 연다.
 *
 * @param onOpenAppClassification 앱 분류 화면을 연다
 * @param onDataDeleted 데이터를 모두 지웠다
 */
@Composable
fun SettingsScreen(
    onOpenAppClassification: () -> Unit,
    onDataDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val currentOnDataDeleted by rememberUpdatedState(onDataDeleted)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.event.collect { event ->
                when (event) {
                    SettingsEvent.DataDeleted -> currentOnDataDeleted()
                }
            }
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }

    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 24.dp, bottom = 24.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
        Spacer(Modifier.height(8.dp))
        SettingsRow(
            title = stringResource(R.string.settings_app_class),
            description = stringResource(R.string.settings_app_class_description),
            onClick = onOpenAppClassification,
            navigates = true,
        )
        SettingsRow(
            title = stringResource(R.string.settings_permission),
            description = stringResource(if (uiState.permissionGranted) R.string.settings_permission_on else R.string.settings_permission_off),
            // 앱을 지정해 열 수 없어 설치된 앱 전체 목록이 열린다(§6.4)
            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            navigates = true,
        )
        Spacer(Modifier.height(24.dp))
        SettingsRow(
            title = stringResource(R.string.settings_delete_all),
            onClick = { confirmingDelete = true },
            warning = true,
        )
    }

    if (confirmingDelete) {
        TallyDialog(
            message = stringResource(R.string.settings_delete_all_dialog),
            detail = stringResource(R.string.settings_delete_all_dialog_detail),
            choices = listOf(
                TallyDialogChoice(stringResource(R.string.settings_delete_all_keep)) { confirmingDelete = false },
                TallyDialogChoice(stringResource(R.string.settings_delete_all_confirm)) {
                    confirmingDelete = false
                    viewModel.deleteAll()
                },
            ),
        )
    }
}
