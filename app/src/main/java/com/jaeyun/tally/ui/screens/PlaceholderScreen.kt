package com.jaeyun.tally.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.PencilSoft

class PlaceholderAction(val label: String, val onClick: () -> Unit)

/**
 * 아직 만들지 않은 라우트의 빈 자리 (#5). 각 화면을 구현하면 그 라우트에서 이 화면과 `placeholder_` 문구를 지운다.
 *
 * 누른 횟수는 탭을 오가도 상태가 유지되는지 실기기에서 확인하는 용도다.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    actions: List<PlaceholderAction> = emptyList(),
) {
    var count by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = Pencil)
        Text(stringResource(R.string.placeholder_not_ready), style = MaterialTheme.typography.bodyMedium, color = PencilSoft)
        TextButton(onClick = { count++ }) {
            Text(
                if (count == 0) stringResource(R.string.placeholder_counter_start)
                else stringResource(R.string.placeholder_counter, count)
            )
        }
        actions.forEach { action ->
            TextButton(onClick = action.onClick) { Text(action.label) }
        }
    }
}
