package com.jaeyun.tally.probe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * P0 사용 기록 프로브 (#1). debug 빌드에만 있는 별도 런처 진입점이다.
 * 실기기에서 queryEvents가 §6.2 알고리즘에 필요한 이벤트를 주는지 확인하고,
 * 잠금해제 소요 시간을 실측해 흡수 임계값 확정 근거를 남긴다 (§6.2.1).
 */
class UsageProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TallyTheme {
                UsageProbeRoute()
            }
        }
    }
}

@Composable
private fun UsageProbeRoute(viewModel: UsageProbeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipLabel = stringResource(R.string.probe_clip_label)

    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    UsageProbeScreen(
        state = state,
        onOpenSettings = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        onSelectWindow = viewModel::selectWindow,
        onTargetOnlyChange = viewModel::setTargetOnly,
        onQuery = { viewModel.query() },
        onClearPerceived = viewModel::clearPerceivedWeek,
        onExtendRunning = viewModel::extendRunningSession,
        onCopy = {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(
                ClipData.newPlainText(clipLabel, viewModel.dumpText()),
            )
        },
    )
}
