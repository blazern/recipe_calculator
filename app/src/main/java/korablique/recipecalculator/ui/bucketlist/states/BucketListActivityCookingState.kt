package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintSet
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.bucketlist.BucketListAdapter
import korablique.recipecalculator.ui.bucketlist.CommentLayoutController
import korablique.recipecalculator.ui.calckeyboard.CalcEditText
import korablique.recipecalculator.ui.numbersediting.SimpleTextWatcher
import korablique.recipecalculator.util.FloatUtils.areFloatsEquals
import kotlin.math.abs

private const val EXTRA_INITIAL_RECIPE = "EXTRA_INITIAL_RECIPE"
private const val EXTRA_DISPLAYED_RECIPE = "EXTRA_DISPLAYED_RECIPE"

class BucketListActivityCookingState private constructor(
        private val initialRecipe: Recipe,
        private var displayedRecipe: Recipe,
        private val commentLayoutController: CommentLayoutController,
        private val activity: BaseActivity,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor
) : BucketListActivityState() {
    private lateinit var totalWeightTextWatcher: SimpleTextWatcher<CalcEditText>
    private lateinit var totalWeightEditText: CalcEditText
    private lateinit var totalWeightErrorTextView: TextView
    private lateinit var weightsRecalculationCheckbox: CheckBox

    private var commonWeightFactor = 1f
    private val ingredientsSpecificWeightsFactors = mutableMapOf<Long, Float>()
    private var totalWeightSpecificFactor = 1f

    constructor(
            savedState: Bundle,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor)
            : this(
            savedState.getParcelable(EXTRA_INITIAL_RECIPE) as Recipe,
            savedState.getParcelable(EXTRA_DISPLAYED_RECIPE) as Recipe,
            commentLayoutController,
            activity,
            bucketList,
            recipesRepository,
            mainThreadExecutor
    )

    constructor(
            initialRecipe: Recipe,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor)
            : this(
            initialRecipe,
            initialRecipe,
            commentLayoutController,
            activity,
            bucketList,
            recipesRepository,
            mainThreadExecutor
    )

    override fun saveInstanceState(): Bundle {
        val state = Bundle()
        state.putParcelable(EXTRA_INITIAL_RECIPE, initialRecipe)
        state.putParcelable(EXTRA_DISPLAYED_RECIPE, displayedRecipe)
        return state
    }

    override fun getStateID(): ID = ID.CookingState
    override fun getTitleStringID(): Int = R.string.bucket_list_title_cooking
    override fun getRecipe(): Recipe = displayedRecipe

    override fun initImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet) {
        findViewById<EditText>(R.id.recipe_name_edit_text).isEnabled = false
        commentLayoutController.setEditable(false)

        findViewById<View>(R.id.button_close).setOnClickListener {
            onActivityBackPressed()
        }

        totalWeightEditText = activity.findViewById(R.id.total_weight_edit_text);
        totalWeightErrorTextView = activity.findViewById(R.id.total_weight_error_text)
        totalWeightTextWatcher = SimpleTextWatcher(totalWeightEditText) {
            val updatedWeight = totalWeightEditText.getCurrentCalculatedValue() ?: 0f
            val recalculatedFactors = recalculateFactorsOnNewWeight(
                    initialRecipe.weight,
                    displayedRecipe.weight,
                    updatedWeight,
                    totalWeightSpecificFactor,
                    commonWeightFactor)
            if (recalculatedFactors != null) {
                commonWeightFactor = recalculatedFactors.first
                totalWeightSpecificFactor = recalculatedFactors.second
                updateRecipeWithCurrentFactors()

                val areAllWeightsEqual = areTotalWeightAndIngredientsWeightsSumAprxEqual()
                if (areAllWeightsEqual) {
                    totalWeightErrorTextView.visibility = View.GONE
                }
            }
        }
        totalWeightEditText.addTextChangedListener(totalWeightTextWatcher)

        innerConstraints.setVisibility(R.id.button_delete_rippled_wrapper, View.GONE)
        innerConstraints.setVisibility(R.id.button_edit_rippled_wrapper, View.GONE)
        innerConstraints.setVisibility(R.id.weights_recalculation_checkbox, View.VISIBLE)
        weightsRecalculationCheckbox = findViewById(R.id.weights_recalculation_checkbox)
        weightsRecalculationCheckbox.isChecked = true
    }

    private fun recalculateFactorsOnNewWeight(
            initialWeight: Float,
            currentWeight: Float,
            newWeight: Float,
            specificFactor: Float,
            commonFactor: Float): Pair<Float, Float>? {
        if (areFloatsEquals(newWeight, currentWeight)
                || areFloatsEquals(0f, initialWeight)) {
            return null
        }

        // If specific weight was set to 0, and while in recalculation mode same weight was
        // changed, then the foodstuff-specific factor must be reset back to 1 to avoid
        // division by 0.
        val specificFactorFixed = if (weightsRecalculationCheckbox.isChecked
                && areFloatsEquals(0f, specificFactor)) {
            1f
        } else {
            specificFactor
        }

        return if (weightsRecalculationCheckbox.isChecked) {
            val newCommonFactor = newWeight/initialWeight/specificFactorFixed
            Pair(newCommonFactor, specificFactorFixed)
        } else {
            val newSpecificFactor = newWeight/initialWeight/commonFactor
            Pair(commonFactor, newSpecificFactor)
        }
    }

    override fun destroyImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet) {
        totalWeightEditText.removeTextChangedListener(totalWeightTextWatcher)
        innerConstraints.setVisibility(totalWeightErrorTextView.id, View.GONE)
    }

    override fun createIngredientWeightEditionObserver(): BucketListAdapter.ItemWeightEditionObserver? {
        return object : BucketListAdapter.ItemWeightEditionObserver {
            override fun onItemWeightEdited(ingredient: Ingredient, newWeight: Float, position: Int) {
                val initialWeight = initialRecipe.ingredients[position].weight
                val specificFactor = ingredientsSpecificWeightsFactors[ingredient.id] ?: 1f

                val recalculatedFactors = recalculateFactorsOnNewWeight(
                        initialWeight,
                        ingredient.weight,
                        newWeight,
                        specificFactor,
                        commonWeightFactor)
                if (recalculatedFactors != null) {
                    commonWeightFactor = recalculatedFactors.first
                    ingredientsSpecificWeightsFactors[ingredient.id] = recalculatedFactors.second

                    val wereAllWeightsEqual = areTotalWeightAndIngredientsWeightsSumAprxEqual()
                    updateRecipeWithCurrentFactors()
                    val areAllWeightsEqual = areTotalWeightAndIngredientsWeightsSumAprxEqual()

                    if (wereAllWeightsEqual && !areAllWeightsEqual) {
                        val ingredientsWeightsSum = displayedRecipe.ingredients
                                .sumByDouble { it.weight.toDouble() }.toFloat()
                        displayedRecipe = displayedRecipe.copy(weight = ingredientsWeightsSum)
                        onRecipeUpdated(displayedRecipe)
                        totalWeightErrorTextView.visibility = View.GONE
                    } else if (areAllWeightsEqual) {
                        totalWeightErrorTextView.visibility = View.GONE
                    } else {
                        totalWeightErrorTextView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun areTotalWeightAndIngredientsWeightsSumAprxEqual(): Boolean {
        val totalWeight = displayedRecipe.weight
        val ingredientsWeight = displayedRecipe.ingredients.sumByDouble { it.weight.toDouble() }.toFloat()
        return abs(totalWeight - ingredientsWeight) < 0.01f * totalWeight
    }

    private fun updateRecipeWithCurrentFactors() {
        val updatedIngredients =
                initialRecipe.ingredients.map {
                    it.copy(
                        weight = it.weight * commonWeightFactor * specificFactorOf(it))
                }

        displayedRecipe = initialRecipe.copy(
                weight = initialRecipe.weight * commonWeightFactor * totalWeightSpecificFactor,
                ingredients = updatedIngredients)
        onRecipeUpdated(displayedRecipe)
    }

    private fun specificFactorOf(ingredient: Ingredient): Float {
        return ingredientsSpecificWeightsFactors[ingredient.id] ?: 1f
    }

    override fun onActivityBackPressed(): Boolean {
        switchState(
                BucketListActivityDisplayRecipeState(
                        initialRecipe, commentLayoutController, activity,
                        bucketList, recipesRepository, mainThreadExecutor))
        return true
    }
}