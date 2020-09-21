package korablique.recipecalculator.ui.bucketlist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import korablique.recipecalculator.R
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.TimeProvider
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.ActivityScope
import korablique.recipecalculator.database.DatabaseWorker
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Nutrition
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.model.WeightedFoodstuff
import korablique.recipecalculator.ui.DecimalUtils
import korablique.recipecalculator.ui.NutritionValuesWrapper
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityCookingState
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityDisplayRecipeState
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityRecipeEditingState
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityState
import korablique.recipecalculator.ui.bucketlist.states.BucketListActivityState.FinishResult
import korablique.recipecalculator.ui.calckeyboard.CalcEditText
import korablique.recipecalculator.ui.calckeyboard.CalcKeyboardController
import korablique.recipecalculator.ui.pluralprogressbar.PluralProgressBar
import korablique.recipecalculator.util.FloatUtils
import javax.inject.Inject

const val EXTRA_CURRENT_STATE = "EXTRA_CURRENT_STATE"
const val EXTRA_CURRENT_STATE_TYPE = "EXTRA_CURRENT_STATE_TYPE"

@ActivityScope
class BucketListActivityController @Inject constructor(
        private val activity: BucketListActivity,
        private val databaseWorker: DatabaseWorker,
        private val recipesRepository: RecipesRepository,
        private val bucketList: BucketList,
        private val mainThreadExecutor: MainThreadExecutor,
        private val calcKeyboardController: CalcKeyboardController,
        private val timeProvider: TimeProvider)
    : ActivityCallbacks.Observer, BucketListActivityState.Delegate {
    private lateinit var pluralProgressBar: PluralProgressBar
    private lateinit var nutritionValuesWrapper: NutritionValuesWrapper
    private lateinit var adapter: BucketListAdapter
    private lateinit var commentLayoutController: CommentLayoutController
    private lateinit var currentState: BucketListActivityState

    companion object {
        fun createRecipeResultIntent(recipe: Recipe?): Intent {
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_PRODUCED_RECIPE, recipe)
            return resultIntent
        }

        fun createAddToHistoryResultIntent(foodstuff: WeightedFoodstuff): Intent {
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_WEIGHTED_FOODSTUFF_TO_HISTORY, foodstuff)
            return resultIntent
        }

        fun start(
                fragment: Fragment,
                requestCode: Int) {
            fragment.startActivityForResult(createIntent(fragment.requireContext()), requestCode)
        }

        fun createIntent(context: Context): Intent {
            return Intent(context, BucketListActivity::class.java)
        }

        fun startForRecipe(
                fragment: Fragment,
                requestCode: Int,
                recipe: Recipe,
                editing: Boolean) {
            fragment.startActivityForResult(
                    createIntent(fragment.requireContext(), recipe, editing), requestCode)
        }

        fun createIntent(context: Context, recipe: Recipe, editing: Boolean): Intent {
            val intent = Intent(context, BucketListActivity::class.java)
            intent.action = ACTION_DISPLAY_RECIPE
            intent.putExtra(EXTRA_RECIPE, recipe)
            intent.putExtra(EXTRA_EDIT_RECIPE, editing)
            return intent
        }
    }

    init {
        activity.activityCallbacks.addObserver(this)
    }

    override fun onActivityDestroy() {
        activity.activityCallbacks.removeObserver(this)
    }

    override fun onActivitySaveInstanceState(outState: Bundle) {
        outState.putInt(EXTRA_CURRENT_STATE_TYPE, currentState.getStateID().ordinal)
        outState.putBundle(EXTRA_CURRENT_STATE, currentState.saveInstanceState())
    }

    override fun onActivityCreate(savedInstanceState: Bundle?) {
        pluralProgressBar = findViewById(R.id.nutrition_progress_bar)
        nutritionValuesWrapper = NutritionValuesWrapper(
                findViewById<ConstraintLayout>(R.id.nutrition_progress_with_values))

        adapter = BucketListAdapter(activity, calcKeyboardController)
        findViewById<RecyclerView>(R.id.ingredients_list).adapter = adapter

        commentLayoutController = CommentLayoutController(findViewById(R.id.comment_layout))

        calcKeyboardController.useCalcKeyboardWith(
                findViewById(R.id.total_weight_edit_text),
                activity)

        switchStateImpl(createFirstState(savedInstanceState), first = true)
        onRecipeUpdated(currentState.getRecipe())
    }

    private fun createFirstState(savedInstanceState: Bundle?): BucketListActivityState {
        if (savedInstanceState != null) {
            val stateIdOrdinal = savedInstanceState.getInt(EXTRA_CURRENT_STATE_TYPE)
            val stateId = BucketListActivityState.ID.values()[stateIdOrdinal]
            val stateOfState = savedInstanceState.getBundle(EXTRA_CURRENT_STATE)!!
            return when (stateId) {
                BucketListActivityState.ID.DisplayState -> {
                    BucketListActivityDisplayRecipeState(
                            stateOfState, commentLayoutController, activity, bucketList,
                            databaseWorker, recipesRepository,
                            mainThreadExecutor, timeProvider)
                }
                BucketListActivityState.ID.EditingState -> {
                    BucketListActivityRecipeEditingState(
                            stateOfState, commentLayoutController, activity, bucketList,
                            databaseWorker, recipesRepository,
                            mainThreadExecutor, timeProvider)
                }
                BucketListActivityState.ID.CookingState -> {
                    BucketListActivityCookingState(stateOfState, commentLayoutController,
                            activity, bucketList, databaseWorker,
                            recipesRepository, mainThreadExecutor,
                            timeProvider)
                }
            }
        }

        return if (ACTION_DISPLAY_RECIPE == activity.intent.action) {
            val recipe: Recipe = activity.intent.getParcelableExtra(EXTRA_RECIPE)
            if (activity.intent.getBooleanExtra(EXTRA_EDIT_RECIPE, false)) {
                BucketListActivityRecipeEditingState(
                        recipe, commentLayoutController, activity, bucketList, databaseWorker,
                        recipesRepository, mainThreadExecutor, timeProvider)
            } else {
                BucketListActivityDisplayRecipeState(
                        recipe, commentLayoutController, activity, bucketList, databaseWorker,
                        recipesRepository, mainThreadExecutor, timeProvider)
            }
        } else {
            BucketListActivityRecipeEditingState(
                    bucketList.getRecipe(), commentLayoutController, activity,
                    bucketList, databaseWorker, recipesRepository, mainThreadExecutor, timeProvider)
        }
    }

    override fun <T : View?> findViewById(@IdRes id: Int): T {
        return activity.findViewById<T>(id)
    }

    override fun innerLayout(): ConstraintLayout {
        return findViewById(R.id.bucket_list_activity_main_content_layout)
    }

    override fun outerLayout(): ConstraintLayout {
        return findViewById(R.id.bucket_list_activity_layout)
    }

    private fun updateNutritionWrappers() {
        val recipe = currentState.getRecipe()
        var nutrition = Nutrition.zero()
        if (!FloatUtils.areFloatsEquals(0f, recipe.weight, 0.0001f)) {
            nutrition = Nutrition.of100gramsOf(recipe.foodstuff)
        }
        nutritionValuesWrapper.setNutrition(nutrition)
        pluralProgressBar.setProgress(
                nutrition.protein.toFloat(),
                nutrition.fats.toFloat(),
                nutrition.carbs.toFloat())
    }

    override fun onRecipeUpdated(recipe: Recipe) {
        updateNutritionWrappers()
        val weightEditText = findViewById<CalcEditText>(R.id.total_weight_edit_text)
        val weightInEditText = weightEditText.getCurrentCalculatedValue()
        val nameEditText = findViewById<TextView>(R.id.recipe_name_edit_text)

        if (weightEditText.text.toString().isBlank()
                && FloatUtils.areFloatsEquals(recipe.weight, 0f)) {
            // Don't erase weight when text is empty and weight is 0,
            // because empty weight IS 0 weight.
        } else if (weightInEditText == null
                || !FloatUtils.areFloatsEquals(recipe.weight, weightInEditText, 0.01f)) {
            weightEditText.setText(DecimalUtils.toDecimalString(recipe.weight))
        }

        val gramsText = findViewById<TextView>(R.id.weight_measurement_unit_text_view)
        gramsText.text = gramsText.resources
                .getQuantityString(R.plurals.grams, weightInEditText?.toInt() ?: 0)

        if (recipe.name != nameEditText.text.toString()) {
            nameEditText.text = recipe.name
        }
        adapter.setItems(recipe.ingredients)

        commentLayoutController.setComment(recipe.comment)
    }

    override fun switchState(newState: BucketListActivityState) {
        switchStateImpl(newState, first = false)
    }

    private fun switchStateImpl(state: BucketListActivityState, first: Boolean) {
        // We want all transitions of all children object to be run with pretty animations
        TransitionManager.beginDelayedTransition(findViewById(R.id.bucket_list_activity_layout))

        // Avoid old observers notifications as the recipe and current state
        // are changing
        adapter.deinitAllItemsObservers()

        if (!first) {
            currentState.destroy()
        }
        currentState = state
        currentState.init(this, adapter)
        onRecipeUpdated(currentState.getRecipe())

        // Force reset displayed weights so that weights like "10+20"
        // would turn into calculated values.
        forceResetDisplayedWeight()
        adapter.forceResetDisplayedWeights()

        adapter.setUpAddIngredientButton(currentState.createAddIngredientClickObserver())
        adapter.setOnItemClickedObserver(currentState.createIngredientsClickObserver())
        adapter.setOnItemCommentButtonClicked(currentState.createIngredientsCommentClickObserver())
        adapter.setOnItemLongClickedObserver(currentState.createIngredientsLongClickObserver())
        adapter.setUpWeightEditing(currentState.createIngredientWeightEditionObserver())
        adapter.initDragAndDrop(currentState.createIngredientsDragAndDropObserver())

        findViewById<TextView>(R.id.title_text).setText(currentState.getTitleStringID())
    }

    private fun forceResetDisplayedWeight() {
        val weightEditText = findViewById<CalcEditText>(R.id.total_weight_edit_text)
        val weightInEditText = weightEditText.getCurrentCalculatedValue() ?: 0f
        weightEditText.setText(DecimalUtils.toDecimalString(weightInEditText))
    }

    override fun finish(finishResult: FinishResult) {
        when (finishResult) {
            is FinishResult.Ok -> {
                activity.setResult(
                        Activity.RESULT_OK,
                        createRecipeResultIntent(finishResult.recipe))
            }
            is FinishResult.OkAddToHistory -> {
                activity.setResult(
                        Activity.RESULT_OK,
                        createAddToHistoryResultIntent(finishResult.foodstuff))
            }
            is FinishResult.Canceled -> {
                activity.setResult(Activity.RESULT_CANCELED)
            }
        }
        activity.finish()
    }

    override fun onActivityBackPressed(): Boolean {
        return currentState.onActivityBackPressed()
    }
}