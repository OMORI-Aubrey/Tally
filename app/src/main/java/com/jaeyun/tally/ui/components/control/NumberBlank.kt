package com.jaeyun.tally.ui.components.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaeyun.tally.R
import com.jaeyun.tally.ui.components.notebook.NotebookPaper
import com.jaeyun.tally.ui.components.notebook.notebookPaper
import com.jaeyun.tally.ui.theme.Pencil
import com.jaeyun.tally.ui.theme.TallyTheme
import kotlin.math.roundToInt

/**
 * 숫자를 손으로 적는 빈칸 하나와 단위: `__ 분`. 숫자만 [maxDigits]자리까지 받고 숫자 키패드를 띄운다.
 * 체감 입력(§3.4)의 시간·분 칸으로 쓴다.
 *
 * 미리 채운 숫자나 예시 숫자를 두지 않는다. 화면에 보이는 숫자에 답이 끌린다.
 * 한 자리 칸이 차 있을 때 숫자를 또 치면 새 숫자로 바꾼다. 여러 자리 칸은 다 차면 더 받지 않는다.
 * [onValueChange]는 숫자를 치거나 지웠을 때만 부른다(기호 키는 무시). 한 자리 칸에 같은 숫자를 다시 쳐도 부른다.
 *
 * @param label 화면 낭독기가 읽는 이름
 * @param onImeAction 키패드의 [imeAction] 키
 * @param autoFocus 처음 그릴 때 커서를 두고 키패드를 띄운다
 * @param onBackspaceWhenEmpty 빈칸에서 지우기를 눌렀다. 앞 칸으로 돌아갈 때 쓴다(키패드가 키 이벤트를 보낼 때만)
 */
@Composable
fun NumberBlank(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    maxDigits: Int,
    label: String,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    autoFocus: Boolean = false,
    onBackspaceWhenEmpty: (() -> Unit)? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val textStyle = MaterialTheme.typography.displayMedium.copy(color = Pencil, textAlign = TextAlign.Center)
    // 빈칸은 숫자가 있을 때와 높이·기준선이 달라 그대로 두면 숫자를 적는 순간 칸이 커지고 아래 버튼이 밀린다.
    // 다 찬 칸을 미리 재서 칸의 크기와 기준선을 고정한다
    val measurer = rememberTextMeasurer()
    val full = remember(textStyle, measurer, maxDigits) { measurer.measure(REFERENCE_DIGIT.repeat(maxDigits), textStyle) }
    val density = LocalDensity.current
    val fieldWidth = with(density) { full.size.width.toDp() } + FieldPadding
    val fieldHeight = with(density) { full.size.height.toDp() }
    val baseline = full.firstBaseline.roundToInt()

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicTextField(
            value = value,
            onValueChange = { typed ->
                // 붙여넣기나 키패드의 기호 키로 숫자 아닌 글자가 들어와도 숫자만 남긴다
                val digits = typed.filter(Char::isDigit)
                val next = when {
                    digits.length <= maxDigits -> digits
                    maxDigits == 1 -> digits.replaceFirst(value, "").take(1)
                    else -> value
                }
                // 기호만 쳤으면 아무 일도 없던 것으로 둔다. 한 자리 칸에 같은 숫자를 다시 친 것은 입력으로 알린다
                val typedDigit = digits.length > value.length
                if (next != value || typedDigit) onValueChange(next)
            },
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(Pencil),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            modifier = Modifier
                .alignBy { baseline }
                .size(width = fieldWidth, height = fieldHeight)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    val backspace = event.key == Key.Backspace && event.type == KeyEventType.KeyDown
                    if (backspace && value.isEmpty() && onBackspaceWhenEmpty != null) {
                        onBackspaceWhenEmpty()
                        true
                    } else {
                        false
                    }
                }
                .semantics { contentDescription = label }
                .drawBehind {
                    // 연필로 그은 밑줄. 숫자에서 조금 띄우고, 가운데가 살짝 처진다
                    val y = size.height + 1.dp.toPx()
                    val path = Path().apply {
                        moveTo(0f, y - 1.dp.toPx())
                        quadraticTo(size.width / 2, y + 2.dp.toPx(), size.width, y - 2.dp.toPx())
                    }
                    drawPath(path, Pencil, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                },
        )
        Text(unit, style = MaterialTheme.typography.headlineLarge, color = Pencil, modifier = Modifier.alignByBaseline())
    }
}

/** 칸 크기를 잴 숫자. 손글씨 숫자 중 가장 넓다. 한 줄 높이는 글자가 아니라 글꼴과 줄 높이로 정해진다 */
private const val REFERENCE_DIGIT = "0"

/** 다 찬 칸의 숫자 양옆 여백 합 */
private val FieldPadding = 24.dp

@Preview
@Composable
private fun NumberBlankPreview() {
    TallyTheme {
        Column(
            Modifier
                .notebookPaper()
                .padding(start = NotebookPaper.ContentStart, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            NumberBlank(value = "", onValueChange = {}, unit = stringResource(R.string.minutes_unit), maxDigits = 2, label = "", imeAction = ImeAction.Done, onImeAction = {})
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NumberBlank(value = "2", onValueChange = {}, unit = stringResource(R.string.hours_unit), maxDigits = 1, label = "", imeAction = ImeAction.Next, onImeAction = {})
                NumberBlank(value = "30", onValueChange = {}, unit = stringResource(R.string.minutes_unit), maxDigits = 2, label = "", imeAction = ImeAction.Done, onImeAction = {})
            }
        }
    }
}
