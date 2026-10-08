package com.jaeyun.tally.ui.components.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.TapedNote
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.BluePen
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 사용 기록 접근 권한을 켜는 순서 (§8.7). 시스템 설정은 앱을 지정해 열 수 없고 설치된 앱 전체 목록이 열려서(§6.4)
 * 이 안내가 유일한 길잡이다. 포스트잇에 붙이고 번호를 파란 볼펜으로 크게 써서 눈에 띄게 한다(와이어프레임 §1).
 *
 * @param lead 첫 줄. 처음에는 `[권한 설정하기]를 누르면 시스템 설정이 열립니다`, 설정에 다녀와도 꺼져 있으면 `아직 꺼져 있어요`
 */
@Composable
fun PermissionSteps(lead: String, modifier: Modifier = Modifier) {
    TapedNote(modifier) {
        Text(lead, style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        Spacer(Modifier.height(4.dp))
        PermissionStep(1, stringResource(R.string.permission_step_find, stringResource(R.string.app_name)))
        PermissionStep(2, stringResource(R.string.permission_step_switch))
    }
}

@Composable
private fun PermissionStep(number: Int, text: String) {
    Row {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = BluePen,
            modifier = Modifier.width(28.dp).alignByBaseline(),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Pencil, modifier = Modifier.alignByBaseline())
    }
}

@Preview(widthDp = 360)
@Composable
private fun PermissionStepsPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PermissionSteps(stringResource(R.string.permission_steps_lead))
            PermissionSteps(stringResource(R.string.permission_steps_still_off))
        }
    }
}
