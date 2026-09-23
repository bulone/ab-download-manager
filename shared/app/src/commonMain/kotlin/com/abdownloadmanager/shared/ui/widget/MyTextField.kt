package com.abdownloadmanager.shared.ui.widget

import com.abdownloadmanager.shared.util.ui.LocalContentColor
import com.abdownloadmanager.shared.util.ui.LocalTextStyle
import com.abdownloadmanager.shared.util.ui.myColors
import ir.amirab.util.ifThen
import com.abdownloadmanager.shared.util.div
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import com.abdownloadmanager.shared.util.ui.theme.myShapes
import com.abdownloadmanager.shared.util.ui.theme.mySpacings

@Composable
fun MyTextField(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    background: Color = myColors.surface,
    contentColor: Color = myColors.getContentColorFor(background).takeIf { it.isSpecified }
        ?: LocalContentColor.current,
    focusedBorderColor: Color = myColors.primary,
    borderColor: Color = myColors.onBackground / 0.1f,
    shape: Shape = myShapes.defaultRounded,
    textPadding: PaddingValues = PaddingValues(8.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    fontSize: TextUnit = TextUnit.Unspecified,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    start: @Composable (RowScope.() -> Unit)? = null,
    end: @Composable (RowScope.() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    // temporary trace: remove once the keyboard flicker is gone.
    // The enter/leave pair tells us whether the field is being disposed and rebuilt on
    // every tap - a rebuild would drop the input session and therefore the keyboard.
    val traceId = androidx.compose.runtime.remember { kotlin.random.Random.nextInt(1000, 9999) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        com.abdownloadmanager.shared.util.debugTrace("ABDM_FOCUS", "#" + traceId + " enter")
        onDispose {
            com.abdownloadmanager.shared.util.debugTrace("ABDM_FOCUS", "#" + traceId + " leave")
        }
    }
    androidx.compose.runtime.LaunchedEffect(isFocused) {
        com.abdownloadmanager.shared.util.debugTrace(
            "ABDM_FOCUS",
            "#" + traceId + " focused=" + isFocused + " text='" + text + "'",
        )
    }
    // The String overload of BasicTextField throws the caret position away whenever
    // the caller hands the text back, so the caret landed at index 0 on the first tap
    // and a second tap was needed to place it. Keep a TextFieldValue instead and only
    // adopt external text changes, preserving the selection.
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    // Track the text we handed out last so an echo of our own edit can be told apart
    // from a value the caller pushes (initial load, prettify, clamp). A pushed value
    // used to keep selection 0, so the caret sat before the text and a second tap was
    // needed to place it.
    var lastEmittedText by remember { mutableStateOf(text) }
    if (fieldValue.text != text) {
        val selection = if (text == lastEmittedText) {
            val current = fieldValue.selection
            TextRange(
                current.start.coerceAtMost(text.length),
                current.end.coerceAtMost(text.length),
            )
        } else {
            TextRange(text.length)
        }
        fieldValue = TextFieldValue(text = text, selection = selection)
        lastEmittedText = text
    }

    val textSize = fontSize.takeOrElse { LocalTextStyle.current.fontSize }
    Row(
        modifier
            .ifThen(!enabled) {
                alpha(0.5f)
            }
            .clip(shape)
            .heightIn(mySpacings.thumbSize)
            .height(IntrinsicSize.Max)
//            .height(32.dp)
            .pointerHoverIcon(
                if (enabled) PointerIcon.Text
                else PointerIcon.Default
            )
            .border(
                1.dp,
                animateColorAsState(
                    if (isFocused) focusedBorderColor
                    else borderColor
                ).value,
                shape
            )
            .ifThen(background.isSpecified) {
                background(background)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        start?.let {
            it()
        }

        BasicTextField(
            value = fieldValue,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            onValueChange = {
                fieldValue = it
                lastEmittedText = it.text
                onTextChange(it.text)
            },
            interactionSource = interactionSource,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = LocalTextStyle.current.merge(
                TextStyle(
                    color = LocalContentColor.current.ifThen(!enabled) {
                        copy(0.5f)
                    },
                    fontSize = fontSize
                )
            ),
            decorationBox = { innerTextField ->
                // The padding lives here, not on the text field, so the field's own
                // bounds cover it and a tap on the padding focuses the field and places
                // the caret by itself - no parent tap handler to fight over focus.
                Box(Modifier.padding(textPadding)) {
                    androidx.compose.animation.AnimatedVisibility(
                        text.isEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            text = placeholder,
                            maxLines = 1,
                            color = contentColor / 50,
                            fontSize = textSize
                        )
                    }
                    innerTextField()
                }
            },
            cursorBrush = SolidColor(myColors.primary),
            keyboardActions = keyboardActions,
            keyboardOptions = keyboardOptions,
        )
        end?.let {
            it()
        }
    }
}
