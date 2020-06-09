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
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.ui.inputfilters.NumericBoundsInputFilter
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher.OnTextChangedListener

open class NutritionValuesWrapper
        @JvmOverloads constructor(private val layout: ConstraintLayout, withCalories: Boolean = true) {
    private val nutritionChangeCallbacks = mutableListOf<Runnable>()

    private val proteinLayout: ConstraintLayout
    private val fatsLayout: ConstraintLayout
    private val carbsLayout: ConstraintLayout
    private val caloriesLayout: ConstraintLayout?

    private val proteinTextView: TextView
    private val fatsTextView: TextView
    private val carbsTextView: TextView
    private val caloriesTextView: TextView?

    private val proteinEditText: EditText?
    private val fatsEditText: EditText?
    private val carbsEditText: EditText?
    private val caloriesEditText: EditText?

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
        tryJoinTextViews(proteinEditText, proteinTextView)
        tryJoinTextViews(fatsEditText, fatsTextView)
        tryJoinTextViews(carbsEditText, carbsTextView)
        tryJoinTextViews(caloriesEditText, caloriesTextView)

        proteinTextView.addTextChangedListener(SimpleTextWatcher(proteinTextView,
                OnTextChangedListener { nutritionChangeCallbacks.forEach { it.run() } }))
        fatsTextView.addTextChangedListener(SimpleTextWatcher(fatsTextView,
                OnTextChangedListener { nutritionChangeCallbacks.forEach { it.run() } }))
        carbsTextView.addTextChangedListener(SimpleTextWatcher(carbsTextView,
                OnTextChangedListener { nutritionChangeCallbacks.forEach { it.run() } }))
        caloriesTextView?.addTextChangedListener(SimpleTextWatcher(caloriesTextView,
                OnTextChangedListener { nutritionChangeCallbacks.forEach { it.run() } }))

        if (proteinEditText != null) {
            proteinEditText.filters += NumericBoundsInputFilter.withBounds(0f, 9999f)
        }
        if (fatsEditText != null) {
            fatsEditText.filters += NumericBoundsInputFilter.withBounds(0f, 9999f)
        }
        if (carbsEditText != null) {
            carbsEditText.filters += NumericBoundsInputFilter.withBounds(0f, 9999f)
        }
        if (caloriesEditText != null) {
            caloriesEditText.filters += NumericBoundsInputFilter.withBounds(0f, 9999f)
        }
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

    private fun tryJoinTextViews(editText: EditText?, textView: TextView?) {
        if (editText == null || textView == null) {
            return
        }
        editText.addTextChangedListener(SimpleTextWatcher(editText,
                OnTextChangedListener { textView.text = editText.text }))
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
        val editText = nutritionLayout.findViewById<EditText>(R.id.nutrition_edit_text)
        if (editText != null) {
            editText.setText(DecimalUtils.toDecimalString(nutritionValue))
            return
        }
        val textView = nutritionLayout.findViewById<TextView>(R.id.nutrition_text_view)
        textView.text = DecimalUtils.toDecimalString(nutritionValue)
    }

    val proteinValue: Double
        get() = proteinTextView.text.toString().toDoubleOrNull() ?: 0.0

    val fatsValue: Double
        get() = fatsTextView.text.toString().toDoubleOrNull() ?: 0.0

    val carbsValue: Double
        get() = carbsTextView.text.toString().toDoubleOrNull() ?: 0.0

    val caloriesValue: Double
        get() = caloriesTextView!!.text.toString().toDoubleOrNull() ?: 0.0

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

    fun addNutritionChangeCallback(callback: Runnable) {
        nutritionChangeCallbacks += callback
    }

    fun removeNutritionChangeCallback(callback: Runnable) {
        nutritionChangeCallbacks -= callback
    }
}