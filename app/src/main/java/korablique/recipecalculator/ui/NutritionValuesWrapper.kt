package korablique.recipecalculator.ui

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import korablique.recipecalculator.R
import korablique.recipecalculator.base.NTuple4
import korablique.recipecalculator.model.Nutrient
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.ui.inputfilters.NumericBoundsInputFilter
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher.OnTextChangedListener

open class NutritionValuesWrapper
        @JvmOverloads constructor(private val layout: ConstraintLayout, withCalories: Boolean = true) {
    private val nutritionChangeCallbacks = mutableListOf<NutritionChangeCallback>()

    private val proteinLayout: ConstraintLayout
    private val fatsLayout: ConstraintLayout
    private val carbsLayout: ConstraintLayout
    private val caloriesLayout: ConstraintLayout?

    protected val proteinTextView: TextView
    protected val fatsTextView: TextView
    protected val carbsTextView: TextView
    protected val caloriesTextView: TextView?

    private val proteinEditText: EditText?
    private val fatsEditText: EditText?
    private val carbsEditText: EditText?
    private val caloriesEditText: EditText?

    private val pendingChangedByUserNutrients = setOf<Nutrient>()

    interface NutritionChangeCallback {
        fun onNutritionChange(
                oldNutrition: Nutrition,
                newNutrition: Nutrition,
                changedNutrient: Nutrient,
                byUser: Boolean)
    }
    private var lastNutrition = Nutrition.zero()

    init {
        proteinLayout = layout.findViewById(R.id.protein_layout)
        fatsLayout = layout.findViewById(R.id.fats_layout)
        carbsLayout = layout.findViewById(R.id.carbs_layout)
        if (withCalories) {
            caloriesLayout = layout.findViewById(R.id.calories_layout)
        } else {
            layout.findViewById<View>(R.id.calories_layout).visibility = View.GONE
            caloriesLayout = null
        }

        proteinTextView = proteinLayout.findViewById(R.id.nutrition_text_view)
        proteinEditText = proteinLayout.findViewById(R.id.nutrition_edit_text)
        fatsTextView = fatsLayout.findViewById(R.id.nutrition_text_view)
        fatsEditText = fatsLayout.findViewById(R.id.nutrition_edit_text)
        carbsTextView = carbsLayout.findViewById(R.id.nutrition_text_view)
        carbsEditText = carbsLayout.findViewById(R.id.nutrition_edit_text)
        if (caloriesLayout != null) {
            caloriesTextView = caloriesLayout.findViewById(R.id.nutrition_text_view)
            caloriesEditText = caloriesLayout.findViewById(R.id.nutrition_edit_text)
        } else {
            caloriesTextView = null
            caloriesEditText = null
        }

        setNutritionTable(proteinLayout, R.string.protein, R.drawable.new_card_protein_icon)
        setNutritionTable(fatsLayout, R.string.fats, R.drawable.new_card_fats_icon)
        setNutritionTable(carbsLayout, R.string.carbs, R.drawable.new_card_carbs_icon)
        if (caloriesLayout != null) {
            setNutritionTable(caloriesLayout, R.string.calories, R.drawable.invisible_drawable)
        }

        initTextViews(proteinLayout, proteinEditText, proteinTextView, Nutrient.PROTEIN)
        initTextViews(fatsLayout, fatsEditText, fatsTextView, Nutrient.FATS)
        initTextViews(carbsLayout, carbsEditText, carbsTextView, Nutrient.CARBS)
        initTextViews(caloriesLayout, caloriesEditText, caloriesTextView, Nutrient.CALORIES)
    }

    // только задает цвета кружкам и названия в шапке
    private fun setNutritionTable(
            nutritionLayout: ViewGroup,
            @StringRes nutritionName: Int,
            @DrawableRes drawable: Int) {
        val header = nutritionLayout.findViewById<TextView>(R.id.nutrition_name)
        header.setText(nutritionName)
        val coloredCircle = nutritionLayout.findViewById<View>(R.id.colored_circle)
        if (coloredCircle != null) {
            coloredCircle.background = nutritionLayout.resources.getDrawable(drawable)
        }
    }

    private fun initTextViews(
            layout: ViewGroup?,
            editText: EditText?,
            textView: TextView?,
            nutrient: Nutrient) {
        if (editText != null && textView != null) {
            editText.filters += NumericBoundsInputFilter.withBounds(0f, 9999f)
            editText.addTextChangedListener(SimpleTextWatcher(editText,
                    OnTextChangedListener {
                        textView.text = editText.text
                        val oldNutrition = lastNutrition
                        updateLastNutrition(textView, nutrient)
                        if (!oldNutrition.equals(lastNutrition)) {
                            val userInited = editText.hasFocus()
                            nutritionChangeCallbacks.forEach {
                                it.onNutritionChange(oldNutrition, lastNutrition, nutrient, userInited)
                            }
                        }
                    }))
        } else if (textView != null && layout != null) {
            setNutritionTable(layout, nutrient.strID(), nutrient.iconID())
            textView.addTextChangedListener(SimpleTextWatcher(editText,
                    OnTextChangedListener {
                        val oldNutrition = lastNutrition
                        updateLastNutrition(textView, nutrient)
                        if (!oldNutrition.equals(lastNutrition)) {
                            nutritionChangeCallbacks.forEach {
                                it.onNutritionChange(oldNutrition, lastNutrition, nutrient, byUser = false)
                            }
                        }
                    }))
        } else {
            // Nothing to do
        }
    }

    private fun updateLastNutrition(textView: TextView,
                                    nutrient: Nutrient) {
        val value = textView.text.toString().toDoubleOrNull() ?: 0.0
        lastNutrition = lastNutrition.update(nutrient, value)
    }

    fun setNutrition(nutrition: Nutrition) {
        setNutritionValue(proteinLayout, nutrition.protein)
        setNutritionValue(fatsLayout, nutrition.fats)
        setNutritionValue(carbsLayout, nutrition.carbs)
        val caloriesLayout = caloriesLayout
        if (caloriesLayout != null) {
            setNutritionValue(caloriesLayout, nutrition.calories)
        }
    }

    private fun setNutritionValue(nutritionLayout: ViewGroup, nutritionValue: Double) {
        val strValue = DecimalUtils.toDecimalString(nutritionValue)
        val editText = nutritionLayout.findViewById<EditText>(R.id.nutrition_edit_text)
        if (editText != null) {
            if (editText.text.toString() != strValue) {
                editText.setText(strValue)
            }
            return
        }
        val textView = nutritionLayout.findViewById<TextView>(R.id.nutrition_text_view)
        if (textView.text.toString() != strValue) {
            textView.text = strValue
        }
    }

    val proteinValue: Double
        get() = proteinTextView.text.toString().toDoubleOrNull() ?: 0.0

    val fatsValue: Double
        get() = fatsTextView.text.toString().toDoubleOrNull() ?: 0.0

    val carbsValue: Double
        get() = carbsTextView.text.toString().toDoubleOrNull() ?: 0.0

    val caloriesValue: Double
        get() = caloriesTextView!!.text.toString().toDoubleOrNull() ?: 0.0

    val nutrition: Nutrition
        get() = Nutrition.withValues(proteinValue, fatsValue, carbsValue, caloriesValue)

    fun setEditable(editable: Boolean) {
        TransitionManager.beginDelayedTransition(layout)
        setEditable(editable, proteinLayout)
        setEditable(editable, fatsLayout)
        setEditable(editable, carbsLayout)
        setEditable(editable, caloriesLayout)
    }

    private fun setEditable(editable: Boolean, layout: ConstraintLayout?) {
        val newConstraintSet = ConstraintSet()
        newConstraintSet.clone(layout)
        if (editable) {
            newConstraintSet.setVisibility(R.id.nutrition_edit_text, View.VISIBLE)
            newConstraintSet.setVisibility(R.id.nutrition_text_view, View.GONE)
        } else {
            newConstraintSet.setVisibility(R.id.nutrition_edit_text, View.GONE)
            newConstraintSet.setVisibility(R.id.nutrition_text_view, View.VISIBLE)
        }
        newConstraintSet.applyTo(layout)
    }

    fun addNutritionChangeCallback(callback: NutritionChangeCallback) {
        nutritionChangeCallbacks += callback
    }

    fun removeNutritionChangeCallback(callback: NutritionChangeCallback) {
        nutritionChangeCallbacks -= callback
    }
}

private fun Nutrition.update(
        nutrient: Nutrient,
        value: Double): Nutrition {
    val (protein, fats, carbs, calories) = when (nutrient) {
        Nutrient.PROTEIN -> {
            NTuple4(value, fats, carbs, calories)
        }
        Nutrient.FATS -> {
            NTuple4(protein, value, carbs, calories)
        }
        Nutrient.CARBS -> {
            NTuple4(protein, fats, value, calories)
        }
        Nutrient.CALORIES -> {
            NTuple4(protein, fats, carbs, value)
        }
    }
    return Nutrition.withValues(protein, fats, carbs, calories)
}

private fun Nutrient.strID() = when (this) {
    Nutrient.PROTEIN -> R.string.protein
    Nutrient.FATS -> R.string.fats
    Nutrient.CARBS -> R.string.carbs
    Nutrient.CALORIES -> R.string.calories
}

private fun Nutrient.iconID() = when (this) {
    Nutrient.PROTEIN -> R.drawable.new_card_protein_icon
    Nutrient.FATS -> R.drawable.new_card_fats_icon
    Nutrient.CARBS -> R.drawable.new_card_carbs_icon
    Nutrient.CALORIES -> R.drawable.invisible_drawable
}