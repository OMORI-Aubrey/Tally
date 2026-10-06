package com.jaeyun.tally.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.NotebookPaper
import com.jaeyun.tally.ui.components.SettingsEntryRow
import com.jaeyun.tally.ui.theme.PencilSoft

/**
 * 설정 탭 (와이어프레임 §6). 지금은 앱 분류로 가는 행 하나만 둔다(#18). 권한 상태·데이터 전체 삭제·개발자 항목은 #23에서 붙는다.
 *
 * @param onOpenAppClassification 앱 분류 화면을 연다
 */
@Composable
fun SettingsScreen(onOpenAppClassification: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 24.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.labelMedium, color = PencilSoft)
        Spacer(Modifier.height(8.dp))
        SettingsEntryRow(
            title = stringResource(R.string.settings_app_class),
            description = stringResource(R.string.settings_app_class_description),
            onClick = onOpenAppClassification,
        )
        // TODO(#23) 사용 정보 접근 권한 상태, 데이터 전체 삭제, 개발자 항목
    }
}
