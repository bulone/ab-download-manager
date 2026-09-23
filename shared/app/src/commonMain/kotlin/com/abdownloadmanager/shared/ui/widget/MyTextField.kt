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
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.edit
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import com.abdownloadmanager.shared.util.ui.theme.myShapes
import com.abdownloadmanager.shared.util.ui.theme.mySpacings

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    // state-based BasicTextField: the input session is shared per window, so focus
    // moving between two fields no longer stops and restarts a session (the legacy
    // value/onValueChange overload did, and this device ran the hide(ime()) to
    // completion before the show(ime()), dropping the keyboard for ~260ms per tap).
    val state = rememberTextFieldState(text)
    var lastEmittedText by remember { mutableStateOf(text) }
    androidx.compose.runtime.LaunchedEffect(text) {
        if (text != state.text) {
            state.edit { replace(0, length, text) }
        }
    }
    androidx.compose.runtime.LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow { state.text }.collect { newText ->
            if (newText != lastEmittedText) {
                lastEmittedText = newText
                onTextChange(newText)
            }
        }
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
            state = state,
            lineLimits = if (singleLine) {
                TextFieldLineLimits.SingleLine
            } else {
                TextFieldLineLimits.MultiLine(
                    minHeightInLines = minLines,
                    maxHeightInLines = maxLines,
                )
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
                        state.text.isEmpty(),
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
