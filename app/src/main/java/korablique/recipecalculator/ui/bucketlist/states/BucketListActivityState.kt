package korablique.recipecalculator.ui.bucketlist.states

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT_SPREAD
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.RIGHT
import androidx.constraintlayout.widget.ConstraintSet.TOP
import korablique.recipecalculator.R
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.EditTextsVisualDisabler
import korablique.recipecalculator.ui.bucketlist.BucketListAdapter

abstract class BucketListActivityState {
    private lateinit var delegate: Delegate
    private lateinit var adapter: BucketListAdapter

    enum class ID {
        DisplayState,
        EditingState,
        CookingState
    }

    interface Delegate {
        fun onRecipeUpdated(recipe: Recipe)
        fun innerLayout(): ConstraintLayout
        fun outerLayout(): ConstraintLayout
        fun <T : View?> findViewById(@IdRes id: Int): T
        fun switchState(newState: BucketListActivityState)
        fun finish(finishResult: FinishResult)
    }
    sealed class FinishResult {
        data class Ok(val recipe: Recipe?) : FinishResult()
        object Canceled : FinishResult()
    }

    fun init(delegate: Delegate, adapter: BucketListAdapter) {
        this.delegate = delegate
        this.adapter = adapter

        val innerConstraints = ConstraintSet()
        val outerConstraints = ConstraintSet()
        innerConstraints.clone(delegate.innerLayout())
        outerConstraints.clone(delegate.outerLayout())

        initDefaults(innerConstraints, outerConstraints)
        initImpl(innerConstraints, outerConstraints)

        innerConstraints.applyTo(delegate.innerLayout())
        outerConstraints.applyTo(delegate.outerLayout())
    }

    /**
     * Most spread defaults
     */
    private fun initDefaults(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet) {
        findViewById<View>(R.id.button_close).setOnClickListener(null)
        findViewById<View>(R.id.button_edit).setOnClickListener(null)
        findViewById<View>(R.id.button_delete_recipe).setOnClickListener(null)

        innerConstraints.setVisibility(R.id.button_edit_rippled_wrapper, View.VISIBLE)
        innerConstraints.setVisibility(R.id.button_delete_rippled_wrapper, View.GONE)

        outerConstraints.clear(R.id.actions_layout, BOTTOM)
        outerConstraints.connect(R.id.actions_layout, TOP, PARENT_ID, BOTTOM)

        EditTextsVisualDisabler.disable(findViewById(R.id.recipe_name_edit_text))
        EditTextsVisualDisabler.enable(findViewById(R.id.total_weight_edit_text))
    }

    fun destroy() {
        val innerConstraints = ConstraintSet()
        val outerConstraints = ConstraintSet()
        innerConstraints.clone(delegate.innerLayout())
        outerConstraints.clone(delegate.outerLayout())

        destroyImpl(innerConstraints, outerConstraints)

        innerConstraints.applyTo(delegate.innerLayout())
        outerConstraints.applyTo(delegate.outerLayout())
    }

    protected fun onRecipeUpdated(recipe: Recipe) {
        delegate.onRecipeUpdated(recipe)
    }

    protected fun <T : View?> findViewById(@IdRes id: Int): T {
        return delegate.findViewById<T>(id)
    }

    protected fun finish(finishResult: FinishResult) {
        delegate.finish(finishResult)
    }

    protected fun switchState(newState: BucketListActivityState) {
        delegate.switchState(newState)
    }

    abstract fun saveInstanceState(): Bundle
    abstract fun getStateID(): ID
    abstract fun getTitleStringID(): Int

    protected abstract fun initImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet)
    protected abstract fun destroyImpl(innerConstraints: ConstraintSet, outerConstraints: ConstraintSet)
    abstract fun getRecipe(): Recipe
    open fun onActivityBackPressed(): Boolean = false
    open fun createIngredientsDragAndDropObserver(): BucketListAdapter.ItemDragAndDropObserver? = null
    open fun createIngredientsClickObserver(): BucketListAdapter.OnItemClickedObserver? = null
    open fun createIngredientsCommentClickObserver(): BucketListAdapter.OnItemClickedObserver? = null
    open fun createIngredientsLongClickObserver(): BucketListAdapter.OnItemLongClickedObserver? = null
    open fun createAddIngredientClickObserver(): Runnable? = null
    open fun createIngredientWeightEditionObserver(): BucketListAdapter.ItemWeightEditionObserver? = null
}