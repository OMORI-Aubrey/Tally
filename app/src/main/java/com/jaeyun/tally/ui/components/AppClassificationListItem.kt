package com.jaeyun.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.domain.model.Category
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Highlighter
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft
import com.jaeyun.tally.ui.theme.SegmentAllowed
import com.jaeyun.tally.ui.theme.SegmentDistract
import com.jaeyun.tally.ui.theme.TallyTheme

/**
 * 앱 분류 화면의 한 줄 (와이어프레임 §7): `● 유튜브 ········ [딴짓] 허용`. 점 색은 스트립·이탈 목록과 같은 규칙이다.
 *
 * 켜고 끄는 스위치 대신 둘 중 하나를 고르게 한다. "켜짐"이 딴짓인지 허용인지 헷갈리지 않게 하려는 것이다.
 * 고른 쪽은 과목 칩처럼 형광펜으로 칠한다.
 *
 * @param category [Category.DISTRACT] 또는 [Category.ALLOWED]
 */
@Composable
fun AppClassificationListItem(
    appLabel: String,
    category: Category,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier
                .size(10.dp)
                .background(if (category == Category.DISTRACT) SegmentDistract else SegmentAllowed, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = appLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = Pencil,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CategoryChoice(stringResource(R.string.app_class_distract), category == Category.DISTRACT) { onSelect(Category.DISTRACT) }
            CategoryChoice(stringResource(R.string.app_class_allowed), category == Category.ALLOWED) { onSelect(Category.ALLOWED) }
        }
    }
}

@Composable
private fun CategoryChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
            color = if (selected) Pencil else PencilSoft,
            modifier = Modifier
                .then(if (selected) Modifier.rotate(-2f).background(Highlighter) else Modifier)
                .padding(horizontal = 8.dp),
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun AppClassificationListItemPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 8.dp),
        ) {
            AppClassificationListItem("유튜브", Category.DISTRACT, onSelect = {})
            AppClassificationListItem("네이버 사전", Category.ALLOWED, onSelect = {})
            AppClassificationListItem("아주 긴 이름을 가진 메신저 앱 이름", Category.DISTRACT, onSelect = {})
        }
    }
}
