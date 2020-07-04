package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT_WRAP
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.RIGHT
import androidx.constraintlayout.widget.ConstraintSet.TOP
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.EditTextsVisualDisabler
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.bucketlist.CommentLayoutController

private const val EXTRA_DISPLAYED_RECIPE = "EXTRA_DISPLAYED_RECIPE"

class BucketListActivityDisplayRecipeState(
        private val recipe: Recipe,
        private val commentLayoutController: CommentLayoutController,
        private val activity: BaseActivity,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository,
        private val mainThreadExecutor: MainThreadExecutor
) : BucketListActivityState() {

    constructor(savedInstanceState: Bundle,
                commentLayoutController: CommentLayoutController,
                activity: BaseActivity,
                bucketList: BucketList,
                recipesRepository: RecipesRepository,
                mainThreadExecutor: MainThreadExecutor):
            this(savedInstanceState.getParcelable(EXTRA_DISPLAYED_RECIPE) as Recipe,
                    commentLayoutController,
                    activity, bucketList, recipesRepository, mainThreadExecutor)

    override fun getStateID(): ID = ID.DisplayState
    override fun getTitleStringID(): Int = R.string.bucket_list_title_recipe

    override fun saveInstanceState(): Bundle {
        val result = Bundle()
        result.putParcelable(EXTRA_DISPLAYED_RECIPE, recipe)
        return result
    }

    override fun initImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet) {
        findViewById<View>(R.id.button_close).setOnClickListener { finish(FinishResult.Canceled) }
        EditTextsVisualDisabler.disable(findViewById(R.id.total_weight_edit_text))

        findViewById<View>(R.id.button_edit).setOnClickListener {
            switchState(BucketListActivityRecipeEditingState(
                    recipe, commentLayoutController, activity, bucketList,
                    recipesRepository, mainThreadExecutor))
        }

        val actionButton = findViewById<Button>(R.id.recipe_action_button)
        actionButton.setText(R.string.bucket_list_action_button_cooking)
        actionButton.setOnClickListener {
            switchState(BucketListActivityCookingState(
                    recipe, commentLayoutController, activity, bucketList,
                    recipesRepository, mainThreadExecutor))
        }

        commentLayoutController.setEditable(false)
        findViewById<View>(R.id.button_delete_rippled_wrapper).visibility = View.GONE

        innerConstraints.constrainDefaultWidth(R.id.total_weight_edit_text, MATCH_CONSTRAINT_WRAP)
        innerConstraints.constrainWidth(R.id.total_weight_edit_text, ConstraintSet.WRAP_CONTENT)
        innerConstraints.clear(R.id.total_weight_edit_text, RIGHT)
        outerConstraints.clear(R.id.actions_layout, TOP)
        outerConstraints.connect(R.id.actions_layout, BOTTOM, PARENT_ID, BOTTOM)
    }

    override fun destroyImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet) = Unit
    override fun getRecipe(): Recipe = recipe
}