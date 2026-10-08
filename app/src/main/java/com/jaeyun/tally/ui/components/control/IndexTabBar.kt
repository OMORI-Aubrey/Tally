package com.jaeyun.tally.ui.components.control

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.theme.Highlighter
import com.jaeyun.tally.ui.theme.Paper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.TallyTheme
import com.jaeyun.tally.ui.theme.TapeBlue
import com.jaeyun.tally.ui.theme.TapeMint
import com.jaeyun.tally.ui.theme.TapePink
import kotlinx.coroutines.delay

/** [color]는 선택되지 않았을 때의 탭 색. 선택된 탭은 [Highlighter]로 칠한다 */
@Immutable
data class IndexTab(val label: String, val color: Color)

private val TabShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
private val TabHeight = 48.dp
private val SelectedTabHeight = 56.dp
private const val TAB_SLIDE_MILLIS = 260
private const val TAB_STAGGER_MILLIS = 70L

/**
 * 노트 아래로 삐져나온 색인 탭. 하단 탭 바로 쓴다.
 *
 * 선택된 탭이 더 길게 튀어나온다. 탭 색은 내비게이션 바 영역까지 이어 칠한다(edge-to-edge).
 *
 * [tabsVisible]이 false가 되면 탭이 왼쪽부터 차례로 아래로 내려가 숨고, true가 되면 같은 순서로 올라온다.
 * 숨어도 탭 바의 자리(높이)는 그대로라서 위 화면이 위아래로 튀지 않는다. 처음 그릴 때는 애니메이션 없이 그 상태로 그린다.
 */
@Composable
fun IndexTabBar(
    tabs: List<IndexTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabsVisible: Boolean = true,
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = NotebookPaper.ContentStart)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            // 0 = 보임, 1 = 탭 높이만큼 내려가 숨음
            val hidden = remember { Animatable(if (tabsVisible) 0f else 1f) }
            LaunchedEffect(tabsVisible) {
                delay(index * TAB_STAGGER_MILLIS)
                hidden.animateTo(if (tabsVisible) 0f else 1f, tween(TAB_SLIDE_MILLIS, easing = FastOutSlowInEasing))
            }
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = hidden.value * size.height }
                    .then(if (tabsVisible) Modifier else Modifier.clearAndSetSemantics {})
                    .height((if (selected) SelectedTabHeight else TabHeight) + bottomInset)
                    .widthIn(min = 88.dp)
                    .clip(TabShape)
                    .background(if (selected) Highlighter else tab.color)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(start = 20.dp, end = 20.dp, bottom = bottomInset),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    style = if (selected) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
                    // 테이프 색 위에서 PencilSoft는 대비가 4.5:1에 못 미친다. 선택 여부는 칠·높이·굵기로 구분한다
                    color = Pencil,
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun IndexTabBarPreview() {
    TallyTheme {
        Box(Modifier.background(Paper).padding(top = 24.dp)) {
            IndexTabBar(
                tabs = listOf(
                    IndexTab(stringResource(R.string.tab_timer), TapeBlue),
                    IndexTab(stringResource(R.string.tab_records), TapePink),
                    IndexTab(stringResource(R.string.tab_settings), TapeMint),
                ),
                selectedIndex = 1,
                onSelect = {},
            )
        }
    }
}
