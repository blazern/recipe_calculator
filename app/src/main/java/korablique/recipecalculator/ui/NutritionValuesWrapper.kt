package korablique.recipecalculator.ui

import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import korablique.recipecalculator.R
import korablique.recipecalculator.base.NTuple4
import korablique.recipecalculator.model.Nutrient
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.ui.DecimalUtils.toDecimalString
import korablique.recipecalculator.ui.inputfilters.NumericBoundsInputFilter
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher.OnTextChangedListener
import korablique.recipecalculator.util.FloatUtils

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
        fatsTextView = fatsLayout.findViewById(R.id.nutrition_text_view)
        carbsTextView = carbsLayout.findViewById(R.id.nutrition_text_view)
        if (caloriesLayout != null) {
            caloriesTextView = caloriesLayout.findViewById(R.id.nutrition_text_view)
        } else {
            caloriesTextView = null
        }

        setNutritionTable(proteinLayout, R.string.protein, R.drawable.new_card_protein_icon)
        setNutritionTable(fatsLayout, R.string.fats, R.drawable.new_card_fats_icon)
        setNutritionTable(carbsLayout, R.string.carbs, R.drawable.new_card_carbs_icon)
        if (caloriesLayout != null) {
            setNutritionTable(caloriesLayout, R.string.calories, R.drawable.invisible_drawable)
        }

        initTextView(proteinTextView, Nutrient.PROTEIN)
        initTextView(fatsTextView, Nutrient.FATS)
        initTextView(carbsTextView, Nutrient.CARBS)
        initTextView(caloriesTextView, Nutrient.CALORIES)

        // Not editable by default
        setEditable(false)
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

    private fun initTextView(
            textView: TextView?,
            nutrient: Nutrient) {
        if (textView == null) {
            return
        }
        textView.filters += NumericBoundsInputFilter.withBounds(0f, 9999f)
        textView.addTextChangedListener(SimpleTextWatcher(textView,
                OnTextChangedListener {
                    val oldNutrition = lastNutrition
                    updateLastNutrition(textView, nutrient)
                    if (!oldNutrition.equals(lastNutrition)) {
                        val userInited = textView.hasFocus()
                        nutritionChangeCallbacks.forEach {
                            it.onNutritionChange(oldNutrition, lastNutrition, nutrient, userInited)
                        }
                    }
                }))
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
        val textView = nutritionLayout.findViewById<TextView>(R.id.nutrition_text_view)
        if (textView != null) {
            setNutritionTextViewValue(textView, nutritionValue)
            return
        }
        setNutritionTextViewValue(nutritionLayout.findViewById(R.id.nutrition_text_view), nutritionValue)
    }

    private fun setNutritionTextViewValue(textView: TextView, nutritionValue: Double) {
        val text = textView.text.toString()
        if (text.isEmpty()
                && textView is EditText
                && textView.hasFocus()) {
            if (!FloatUtils.areFloatsEquals(0.0, nutritionValue)) {
                textView.setText(toDecimalString(nutritionValue))
            } else {
                // textView is already empty, no need to change its value
            }
            return
        }
        if (text.isEmpty() || !FloatUtils.areFloatsEquals(nutritionValue, text.toDouble())) {
            textView.text = toDecimalString(nutritionValue)
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
        if (proteinTextView is EditText) {
            EditTextsVisualDisabler.setFullyVisuallyEnabled(proteinTextView, editable)
        }
        if (fatsTextView is EditText) {
            EditTextsVisualDisabler.setFullyVisuallyEnabled(fatsTextView, editable)
        }
        if (carbsTextView is EditText) {
            EditTextsVisualDisabler.setFullyVisuallyEnabled(carbsTextView, editable)
        }
        if (caloriesTextView is EditText) {
            EditTextsVisualDisabler.setFullyVisuallyEnabled(caloriesTextView, editable)
        }
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
