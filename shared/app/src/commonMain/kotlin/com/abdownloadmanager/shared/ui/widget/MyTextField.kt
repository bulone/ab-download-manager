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
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.text.input.ImeAction
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
        // state.text is a CharSequence here, so compare it as a String: a String never
        // equals a non-String CharSequence, and that would make this push run on every
        // recomposition and keep throwing the caret to the end while typing.
        if (text != state.text.toString()) {
            state.edit { replace(0, length, text) }
        }
    }
    androidx.compose.runtime.LaunchedEffect(state) {
        androidx.compose.runtime.snapshotFlow { state.text }.collect { newText ->
            val newString = newText.toString()
            if (newString != lastEmittedText) {
                lastEmittedText = newString
                onTextChange(newString)
            }
        }
    }

    // The state-based field takes a KeyboardActionHandler instead of KeyboardActions,
    // and that handler is not told which IME action fired, so map the action configured
    // in keyboardOptions back onto the matching legacy callback.
    val keyboardActionHandler = remember(keyboardActions, keyboardOptions) {
        KeyboardActionHandler { performDefaultAction ->
            val scope = object : KeyboardActionScope {
                override fun defaultKeyboardAction(imeAction: ImeAction) {
                    performDefaultAction()
                }
            }
            val action = when (keyboardOptions.imeAction) {
                ImeAction.Done -> keyboardActions.onDone
                ImeAction.Go -> keyboardActions.onGo
                ImeAction.Next -> keyboardActions.onNext
                ImeAction.Previous -> keyboardActions.onPrevious
                ImeAction.Search -> keyboardActions.onSearch
                ImeAction.Send -> keyboardActions.onSend
                else -> null
            }
            if (action != null) action.invoke(scope) else performDefaultAction()
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
            decorator = TextFieldDecorator { innerTextField ->
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
            onKeyboardAction = keyboardActionHandler,
            keyboardOptions = keyboardOptions,
        )
        end?.let {
            it()
        }
    }
}
