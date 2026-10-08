package com.jaeyun.tally.ui.screens.appclass

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.feedback.EmptyState
import com.jaeyun.tally.ui.components.notebook.NoteTopBar
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.AppClassificationListItem
import com.jaeyun.tally.ui.theme.PencilSoft

/**
 * 앱 분류 (§5.1, 와이어프레임 §7). 설치된 앱 전체가 아니라 공부 중 감지된 앱만 보여준다.
 * 바꾼 분류는 다음 세션부터 반영된다. 아래에 브라우저 기본값 안내를 둔다.
 *
 * @param onBack 설정 탭으로 돌아간다. 뒤로가기와 같다
 */
@Composable
fun AppClassificationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppClassificationViewModel = viewModel(factory = AppClassificationViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val caption = MaterialTheme.typography.bodyMedium
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = NotebookPaper.ContentStart, end = 24.dp, top = 8.dp, bottom = 32.dp),
    ) {
        item {
            NoteTopBar(stringResource(R.string.app_class_title), stringResource(R.string.app_class_back), onBack = onBack)
        }
        when (val state = uiState) {
            AppClassificationUiState.Loading -> Unit
            AppClassificationUiState.Empty -> item {
                EmptyState(stringResource(R.string.app_class_empty), Modifier.padding(top = 48.dp))
            }
            is AppClassificationUiState.Content -> {
                item {
                    Text(stringResource(R.string.app_class_intro), style = caption, color = PencilSoft, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                }
                items(state.rows, key = { it.packageName }) { row ->
                    AppClassificationListItem(
                        appLabel = row.appLabel,
                        category = row.category,
                        onSelect = { category -> if (category != row.category) viewModel.setCategory(row.packageName, category) },
                    )
                }
                item {
                    Text(stringResource(R.string.app_class_browser_note), style = caption, color = PencilSoft, modifier = Modifier.padding(top = 24.dp))
                }
            }
        }
    }
}
