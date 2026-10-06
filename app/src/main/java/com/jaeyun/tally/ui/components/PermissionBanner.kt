package com.jaeyun.tally.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 권한 유도 배너 (§8.2 ②, §8.5.1). 사용 기록 권한 없이 끝낸 세션의 리포트에서 타임라인 스트립 자리에 둔다.
 *
 * 이 세션은 공부 시간만 남았다고 알린 뒤, 지금 권한이 꺼져 있으면([permissionGranted] false) 켜면 보이는 것과
 * 켜는 순서, [권한 설정하기]를 둔다. 이미 켜져 있으면 다음 세션부터 보인다는 한 줄만 둔다 — 이 세션은 다시 계산하지 않는다.
 */
@Composable
fun PermissionBanner(
    permissionGranted: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.report_no_timeline), style = MaterialTheme.typography.bodyLarge, color = Pencil)
        Spacer(Modifier.height(5.dp))
        if (permissionGranted) {
            Text(stringResource(R.string.permission_banner_on), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        } else {
            Text(stringResource(R.string.permission_banner_benefit), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
            Spacer(Modifier.height(8.dp))
            PermissionSteps(stringResource(R.string.permission_steps_lead))
            Spacer(Modifier.height(16.dp))
            SketchBoxButton(
                text = stringResource(R.string.permission_banner_open),
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 220.dp, height = 72.dp),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun PermissionBannerPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            PermissionBanner(permissionGranted = false, onOpenSettings = {})
            PermissionBanner(permissionGranted = true, onOpenSettings = {})
        }
    }
}
