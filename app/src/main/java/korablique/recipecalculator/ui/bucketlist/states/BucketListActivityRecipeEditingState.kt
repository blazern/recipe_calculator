package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.TOP
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.TimeProvider
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.logging.Log
import korablique.recipecalculator.database.CreateRecipeResult
import korablique.recipecalculator.database.DatabaseWorker
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.database.UpdateRecipeResult
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.EditTextsVisualDisabler
import korablique.recipecalculator.ui.TwoOptionsDialog
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.bucketlist.BucketListAdapter
import korablique.recipecalculator.ui.bucketlist.CommentLayoutController
import korablique.recipecalculator.ui.bucketlist.IngredientCommentDialog
import korablique.recipecalculator.ui.calckeyboard.CalcEditText
import korablique.recipecalculator.util.FloatUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

private const val TAG_CANCELLATION_DIALOG = "TAG_CANCELLATION_DIALOG"
private const val EXTRA_INITIAL_RECIPE = "EXTRA_INITIAL_RECIPE"
private const val EXTRA_COMMENTED_INGREDIENT_POSITION = "EXTRA_COMMENTED_INGREDIENT_POSITION"

class BucketListActivityRecipeEditingState private constructor(
        private val initialDisplayedRecipe: Recipe,
        private var commentDialogIngredientPosition: Int?,
        private val commentLayoutController: CommentLayoutController,
        private val savedInstanceState: Bundle?,
        private val activity: BaseActivity,
        private val bucketList: BucketList,
        private val databaseWorker: DatabaseWorker,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor,
        private val timeProvider: TimeProvider
) : BucketListActivityState() {
    private lateinit var buttonClose: View
    private lateinit var buttonDelete: View
    private lateinit var totalWeightEditText: CalcEditText
    private lateinit var recipeNameEditText: EditText
    private lateinit var saveAsRecipeButton: Button

    private lateinit var totalWeightTextWatcher: TextWatcherAdapter
    private lateinit var recipeNameTextWatcher: TextWatcherAdapter
    private lateinit var commentEditsObserver: CommentLayoutController.CommentEditsObserver

    constructor(
            initialRecipe: Recipe,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            databaseWorker: DatabaseWorker,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor,
            timeProvider: TimeProvider) :
            this(initialRecipe, null, commentLayoutController, null,
                    activity, bucketList, databaseWorker, recipesRepository, mainThreadExecutor,
                    timeProvider)

    constructor(
            savedInstanceState: Bundle,
            commentLayoutController: CommentLayoutController,
            activity: BaseActivity,
            bucketList: BucketList,
            databaseWorker: DatabaseWorker,
            recipesRepository: RecipesRepository,
            mainThreadExecutor: MainThreadExecutor,
            timeProvider: TimeProvider) :
            this(savedInstanceState.getParcelable(EXTRA_INITIAL_RECIPE) as Recipe,
                    extractCommentedIngredientPosition(savedInstanceState),
                    commentLayoutController,
                    savedInstanceState,
                    activity, bucketList, databaseWorker,
                    recipesRepository, mainThreadExecutor,
                    timeProvider)

    override fun getStateID(): ID = ID.EditingState
    override fun getTitleStringID(): Int =
        when (initialDisplayedRecipe.isFromDB) {
            true -> R.string.bucket_list_title_recipe_editing
            else -> R.string.bucket_list_title_recipe_creation
        }

    override fun saveInstanceState(): Bundle {
        val result = Bundle()
        result.putParcelable(EXTRA_INITIAL_RECIPE, initialDisplayedRecipe)
        val commentedIngredientPosition = commentDialogIngredientPosition ?: -1
        result.putInt(EXTRA_COMMENTED_INGREDIENT_POSITION, commentedIngredientPosition)
        return result
    }

    override fun initImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet) {
        innerConstraints.setVisibility(R.id.button_delete_rippled_wrapper, View.VISIBLE)
        innerConstraints.setVisibility(R.id.button_edit_rippled_wrapper, View.GONE)
        outerConstraints.clear(R.id.actions_layout, TOP)
        outerConstraints.connect(R.id.actions_layout, BOTTOM, PARENT_ID, BOTTOM)

        // Reinit comment dialog - the user might've written a long comment
        // and it's bad to lose it
        val ingredientCommentDialog = IngredientCommentDialog.findDialog(activity.supportFragmentManager)
        if (ingredientCommentDialog != null) {
            val pos = commentDialogIngredientPosition
            if (pos != null) {
                initCommentDialog(ingredientCommentDialog, pos)
            } else {
                // We don't know for which position the dialog is shown
                ingredientCommentDialog.dismiss()
                Log.e("Ingredient comment dialog is dismissed because ingredient pos is unknown")
            }
        }

        buttonClose = findViewById(R.id.button_close)
        buttonDelete = findViewById(R.id.button_delete_recipe)
        saveAsRecipeButton = findViewById(R.id.recipe_action_button)
        recipeNameEditText = findViewById(R.id.recipe_name_edit_text)
        totalWeightEditText = findViewById(R.id.total_weight_edit_text)
        EditTextsVisualDisabler.enable(recipeNameEditText)
        saveAsRecipeButton.setText(R.string.save_recipe)

        if (initialDisplayedRecipe.isFromDB) {
            buttonDelete.visibility = View.VISIBLE
        } else {
            buttonDelete.visibility = View.GONE
        }

        recipeNameEditText.isEnabled = true

        // If we're not restored and BucketList doesn't contain the initial recipe yet for
        // some reason - let's put it in.
        // If we're restored - we have already put the recipe into BucketList in the past
        // and should do it again, because at this moment BucketList might contain a modified
        // version of our initial recipe.
        if (savedInstanceState == null
                && bucketList.getRecipe() != initialDisplayedRecipe) {
            bucketList.setRecipe(initialDisplayedRecipe)
        }

        buttonClose.setOnClickListener {
            GlobalScope.launch(mainThreadExecutor) {
                onUserExitAttempt()
            }
        }
        buttonDelete.setOnClickListener {
            val dialog = TwoOptionsDialog.showDialog(
                    activity,
                    TAG_CANCELLATION_DIALOG,
                    R.string.recipe_deletion_dialog_title,
                    R.string.recipe_deletion_dialog_confirmation,
                    R.string.recipe_deletion_dialog_cancellation)
            dialog.setOnButtonsClickListener {
                dialog.dismiss()
                when (it) {
                    TwoOptionsDialog.ButtonName.POSITIVE -> {
                        recipesRepository.deleteRecipe(initialDisplayedRecipe)
                        bucketList.clear()
                        finish(FinishResult.Ok(null))
                    }
                    TwoOptionsDialog.ButtonName.NEGATIVE -> {
                        // Nothing to do
                    }
                }
            }
        }

        saveAsRecipeButton.setOnClickListener {
            bucketList.setComment(bucketList.getComment().trim())
            if (initialDisplayedRecipe.isFromDB) {
                saveAndDisplayRecipe()
            } else {
                createRecipeAndFinish()
            }
        }

        recipeNameTextWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable) {
                updateSaveButtonsEnability()

                if (s.toString() == bucketList.getName()) {
                    return
                }
                bucketList.setName(s.toString())
                onRecipeUpdated(getRecipe())
            }
        }
        totalWeightTextWatcher = object : TextWatcherAdapter() {
            override fun afterTextChanged(s: Editable) {
                updateSaveButtonsEnability()

                var totalWeight = totalWeightEditText.getCurrentCalculatedValue()
                if (totalWeight == null) {
                    totalWeight = 0f
                }
                if (totalWeight == bucketList.getTotalWeight()) {
                    return;
                }
                bucketList.setTotalWeight(totalWeight)
                onRecipeUpdated(getRecipe())
            }
        }
        recipeNameEditText.addTextChangedListener(recipeNameTextWatcher)
        totalWeightEditText.addTextChangedListener(totalWeightTextWatcher)

        commentEditsObserver = object : CommentLayoutController.CommentEditsObserver {
            override fun onCommentViewTextEdited(comment: String) {
                val recipe = getRecipe().copy(comment = comment)
                bucketList.setRecipe(recipe)
                onRecipeUpdated(recipe)
            }
        }
        commentLayoutController.setEditable(true)
        commentLayoutController.addCommentEditsObserver(commentEditsObserver)

        updateSaveButtonsEnability()
    }

    private fun saveAndDisplayRecipe() {
        GlobalScope.launch(mainThreadExecutor) {
            val result = recipesRepository.updateRecipe(initialDisplayedRecipe, getRecipe())
            val recipe = when (result) {
                is UpdateRecipeResult.Ok -> {
                    result.recipe
                }
                is UpdateRecipeResult.UpdatedRecipeNotFound -> {
                    Log.w("saveAndDisplayRecipe: UpdatedRecipeNotFound")
                    Toast.makeText(activity, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    return@launch
                }
            }
            bucketList.clear()
            switchState(BucketListActivityDisplayRecipeState(
                    recipe, commentLayoutController, activity,
                    bucketList, databaseWorker, recipesRepository, mainThreadExecutor,
                    timeProvider))
        }
    }

    private fun createRecipeAndFinish() {
        GlobalScope.launch(mainThreadExecutor) {
            val result = recipesRepository.saveRecipe(getRecipe())
            val recipe = when (result) {
                is CreateRecipeResult.Ok -> {
                    result.recipe
                }
                is CreateRecipeResult.FoodstuffDuplicationError -> {
                    Toast.makeText(activity, R.string.foodstuff_already_exists, Toast.LENGTH_LONG).show()
                    return@launch
                }
            }
            bucketList.clear()
            finish(FinishResult.Ok(recipe))
        }
    }

    override fun destroyImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet) {
        buttonClose.setOnClickListener(null)
        buttonDelete.setOnClickListener(null)
        saveAsRecipeButton.setOnClickListener(null)
        recipeNameEditText.removeTextChangedListener(recipeNameTextWatcher)
        totalWeightEditText.removeTextChangedListener(totalWeightTextWatcher)
        commentLayoutController.removeCommentEditsObserver(commentEditsObserver)
        commentLayoutController.setEditable(false)
    }

    override fun getRecipe(): Recipe = bucketList.getRecipe()

    private fun updateSaveButtonsEnability() {
        val weightVal = totalWeightEditText.getCurrentCalculatedValue()
        val name = recipeNameEditText.text.toString().trim()
        saveAsRecipeButton.isEnabled =
                weightVal != null
                        && !name.isEmpty()
                        && !FloatUtils.areFloatsEquals(weightVal, 0f)
                        && !bucketList.getList().isEmpty()
    }

    override fun createIngredientWeightEditionObserver(): BucketListAdapter.ItemWeightEditionObserver? {
        return object : BucketListAdapter.ItemWeightEditionObserver {
            override fun onItemWeightEdited(ingredient: Ingredient, newWeight: Float, position: Int) {
                var recipe = getRecipe()
                val newIngredients = recipe.ingredients.toMutableList()
                newIngredients[position] = recipe.ingredients[position].copy(weight = newWeight)

                var newTotalWeight = 0f
                newIngredients.forEach { newTotalWeight += it.weight }

                recipe = recipe.copy(ingredients = newIngredients, weight = newTotalWeight)
                recipe = recipe.recalculateNutrition()
                bucketList.setRecipe(recipe)
                onRecipeUpdated(recipe)
                updateSaveButtonsEnability()
            }
        }
    }

    override fun createIngredientsCommentClickObserver(): BucketListAdapter.OnItemClickedObserver? {
        return object : BucketListAdapter.OnItemClickedObserver {
            override fun onItemClicked(ingredient: Ingredient, position: Int) {
                val dialog = IngredientCommentDialog.showDialog(activity.supportFragmentManager, ingredient.comment)
                initCommentDialog(dialog, position)
            }
        }
    }

    override fun createIngredientsLongClickObserver(): BucketListAdapter.OnItemLongClickedObserver? {
        return object : BucketListAdapter.OnItemLongClickedObserver {
            override fun onItemLongClicked(ingredient: Ingredient, position: Int, view: View): Boolean {
                val menu = PopupMenu(activity, view)
                menu.inflate(R.menu.bucket_list_menu)
                menu.show()
                menu.setOnMenuItemClickListener { item: MenuItem ->
                    if (item.itemId == R.id.delete_ingredient) {
                        var recipe = getRecipe()
                        val newIngredients = recipe.ingredients.toMutableList()
                        newIngredients.removeAt(position)

                        var newTotalWeight = 0f
                        newIngredients.forEach { newTotalWeight += it.weight }

                        recipe = recipe.copy(ingredients = newIngredients, weight = newTotalWeight)
                        recipe = recipe.recalculateNutrition()
                        bucketList.setRecipe(recipe)
                        onRecipeUpdated(bucketList.getRecipe())
                        updateSaveButtonsEnability()
                        true
                    } else {
                        false
                    }
                }
                return true
            }
        }
    }

    private fun initCommentDialog(dialog: IngredientCommentDialog, position: Int) {
        commentDialogIngredientPosition = position
        dialog.setOnDismissListener {
            commentDialogIngredientPosition = null
        }
        dialog.setOnSaveButtonClickListener { newComment ->
            var recipe = getRecipe()
            val newIngredients = recipe.ingredients.toMutableList()
            newIngredients[position] = newIngredients[position].copy(comment = newComment.trim())
            recipe = recipe.copy(ingredients = newIngredients)
            bucketList.setRecipe(recipe)
            onRecipeUpdated(bucketList.getRecipe())
            dialog.dismiss()
        }
    }

    override fun onActivityBackPressed(): Boolean {
        GlobalScope.launch(mainThreadExecutor) {
            onUserExitAttempt()
        }
        return true
    }

    private suspend fun onUserExitAttempt() {
        if (getRecipe().isFromDB) {
            val recipeBeforeEditing = recipesRepository.getRecipeOfFoodstuff(initialDisplayedRecipe.foodstuff)
            if (recipeBeforeEditing == getRecipe()) {
                // The recipe is unchanged and it's original state will be displayed anyway -
                // let's clean BucketList and display the recipe.
                bucketList.clear()
                switchState(BucketListActivityDisplayRecipeState(
                        recipeBeforeEditing, commentLayoutController, activity,
                        bucketList, databaseWorker, recipesRepository, mainThreadExecutor,
                        timeProvider))
                return
            }
        }

        val titleId = if (initialDisplayedRecipe.isFromDB) {
            R.string.cancel_recipe_editing_dialog_title
        } else {
            R.string.cancel_recipe_creation_dialog_title
        }
        val dialog = TwoOptionsDialog.showDialog(
                activity,
                TAG_CANCELLATION_DIALOG,
                titleId,
                R.string.cancel_recipe_dialog_confirmation,
                R.string.cancel_recipe_dialog_cancellation)
        dialog.setOnButtonsClickListener {
            dialog.dismiss()
            when (it) {
                TwoOptionsDialog.ButtonName.POSITIVE -> {
                    bucketList.clear()
                    if (initialDisplayedRecipe.isFromDB) {
                        GlobalScope.launch(mainThreadExecutor) {
                            val recipeBeforeEditing =
                                    recipesRepository.getRecipeOfFoodstuff(initialDisplayedRecipe.foodstuff)
                            if (recipeBeforeEditing == null) {
                                throw IllegalStateException("Initial displayed recipe is from "
                                        + "DB, but it wasn't found within recipes repository. "
                                        + "Recipe: $initialDisplayedRecipe")
                            }
                            switchState(BucketListActivityDisplayRecipeState(
                                    recipeBeforeEditing, commentLayoutController,
                                    activity, bucketList, databaseWorker, recipesRepository,
                                    mainThreadExecutor, timeProvider))
                        }
                    } else {
                        finish(FinishResult.Canceled)
                    }
                }
                TwoOptionsDialog.ButtonName.NEGATIVE -> {
                    // The dialog is dismissed anyway
                }
            }
        }
    }

    override fun createIngredientsDragAndDropObserver(): BucketListAdapter.ItemDragAndDropObserver? {
        return object : BucketListAdapter.ItemDragAndDropObserver {
            override fun onItemDraggedAndDropped(oldPosition: Int, newPosition: Int) {
                var recipe = getRecipe()
                val newIngredients = recipe.ingredients.toMutableList()
                newIngredients.add(newPosition, newIngredients.removeAt(oldPosition))
                recipe = recipe.copy(ingredients = newIngredients)
                bucketList.setRecipe(recipe)
                onRecipeUpdated(bucketList.getRecipe())
            }
        }
    }

    override fun createAddIngredientClickObserver(): Runnable? {
        return Runnable {
            // Finish the Activity without cleaning BucketList - filled bucket list
            // effectively keeps all changes and lets the user to add new ingredients into it.
            finish(FinishResult.Canceled)
        }
    }
}

private fun extractCommentedIngredientPosition(savedState: Bundle): Int? {
    val position = savedState.getInt(EXTRA_COMMENTED_INGREDIENT_POSITION, -1)
    return if (position >= 0) {
        position
    } else {
        null
    }
}