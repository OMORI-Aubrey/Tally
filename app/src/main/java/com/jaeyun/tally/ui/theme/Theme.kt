package com.jaeyun.tally.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 종이 노트 콘셉트라 라이트 테마만 둔다 (#4).
 *
 * Material 기본 컴포넌트(시트·다이얼로그·입력창 등)에 템플릿 보라색이 새지 않도록 모든 역할을 토큰으로 채운다.
 */
private val TallyColorScheme = lightColorScheme(
    primary = BluePen,
    onPrimary = Paper,
    primaryContainer = Highlighter,
    onPrimaryContainer = Pencil,
    inversePrimary = Highlighter,
    secondary = Highlighter,
    onSecondary = Pencil,
    secondaryContainer = HighlighterSoft,
    onSecondaryContainer = Pencil,
    tertiary = BluePen,
    onTertiary = Paper,
    tertiaryContainer = TapeBlue,
    onTertiaryContainer = Pencil,
    background = Paper,
    onBackground = Pencil,
    surface = Paper,
    onSurface = Pencil,
    surfaceVariant = SpiralHole,
    onSurfaceVariant = PencilSoft,
    surfaceTint = Paper,
    inverseSurface = Pencil,
    inverseOnSurface = Paper,
    error = RedPen,
    onError = Paper,
    errorContainer = TapePink,
    onErrorContainer = Pencil,
    outline = PencilSoft,
    outlineVariant = PaperLine,
    surfaceBright = Paper,
    surfaceDim = Paper,
    surfaceContainerLowest = IndexCard,
    surfaceContainerLow = Paper,
    surfaceContainer = Paper,
    surfaceContainerHigh = Paper,
    surfaceContainerHighest = Paper,
)

@Composable
fun TallyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TallyColorScheme,
        typography = Typography,
        content = content
    )
}
