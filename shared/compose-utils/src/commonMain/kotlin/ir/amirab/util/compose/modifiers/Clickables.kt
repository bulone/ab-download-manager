package ir.amirab.util.compose.modifiers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.hijackClick(): Modifier {
    // Deliberately not Modifier.clickable: clickable brings a focusable node with it, and
    // this modifier wraps whole sheets. With a focusable node around them, pressing a
    // text field inside the sheet moved focus to that node first, which ends the field's
    // input session and hides the keyboard; the field only regained focus on release, so
    // the keyboard went down and came back on every tap. Consuming the pointer events
    // directly stops the tap from leaking to the layer behind without joining the focus
    // system.
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent().changes.forEach { it.consume() }
            }
        }
    }
}

fun Modifier.silentClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier {
    return clickable(
        enabled = enabled,
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}
