package korablique.recipecalculator.ui

import android.graphics.drawable.Drawable
import android.widget.EditText
import korablique.recipecalculator.R
import java.util.*

private const val ENABLED_STATE_KEY = R.id.TAG_EDIT_TEXT_ENABLED_STATE

object EditTextsVisualDisabler {
    private val transparentBackgrounds = WeakHashMap<Drawable.ConstantState, Drawable>()

    fun disable(editText: EditText) {
        if (editText.getTag(ENABLED_STATE_KEY) != null) {
            return
        }
        val state = EnabledState(editText.background)
        editText.setTag(ENABLED_STATE_KEY, state)

        val constantState = editText.background.constantState!!
        if (transparentBackgrounds[constantState] == null) {
            val newDrawable = constantState.newDrawable().mutate()
            newDrawable.alpha = 0
            transparentBackgrounds[constantState] = newDrawable
        }

        editText.background = transparentBackgrounds[constantState]
        editText.isEnabled = false
    }

    fun enable(editText: EditText) {
        val state = editText.getTag(ENABLED_STATE_KEY)
        if (state == null) {
            return
        }
        editText.setTag(ENABLED_STATE_KEY, null)
        editText.background = (state as EnabledState).background
        editText.isEnabled = true
    }

    fun setFullyVisuallyEnabled(editText: EditText, enabled: Boolean) {
        if (enabled) {
            enable(editText)
        } else {
            disable(editText)
        }
    }
}

fun EditText.fullyVisuallyDisable() {
    EditTextsVisualDisabler.disable(this)
}

fun EditText.fullyVisuallyEnable() {
    EditTextsVisualDisabler.enable(this)
}

fun EditText.setFullyVisuallyEnabled(enabled: Boolean) {
    EditTextsVisualDisabler.setFullyVisuallyEnabled(this, enabled)
}

private data class EnabledState(val background: Drawable)
