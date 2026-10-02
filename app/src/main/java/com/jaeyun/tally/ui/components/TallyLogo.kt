package com.jaeyun.tally.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.TallyTheme

/** 앱 아이콘과 같은 스톱워치 그림 + "Tally" (PLAN.md §4 앱 아이콘·로고) */
@Composable
fun TallyLogo(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(width = 26.dp, height = 30.dp),
        )
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = Pencil)
    }
}

@Preview
@Composable
private fun TallyLogoPreview() {
    TallyTheme {
        Box(Modifier.notebookPaper().padding(start = NotebookPaper.ContentStart, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
            TallyLogo()
        }
    }
}
