package korablique.recipecalculator.ui.mainactivity.mainscreen.modes

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single
import korablique.recipecalculator.R
import korablique.recipecalculator.RequestCodes
import korablique.recipecalculator.base.BaseFragment
import korablique.recipecalculator.base.NTuple2
import korablique.recipecalculator.base.Optional
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.model.Ingredient.Companion.create
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.bucketlist.BucketListActivity
import korablique.recipecalculator.ui.card.Card
import korablique.recipecalculator.ui.mainactivity.mainscreen.SelectedFoodstuffsSnackbar

@StringRes
private val ADD_FOODSTUFF_TO_RECIPE_CARD_TEXT = R.string.add_foodstuff_to_recipe

class MainScreenRecipeMode(
        private val modesSwitcher: MainScreenMode.ModesSwitcher,
        private val fragment: BaseFragment,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository) : MainScreenMode {
    private val context = fragment.requireContext()
    private lateinit var snackbarController: SelectedFoodstuffsSnackbar
    private var bucketListChangesCount = 0

    private val bucketListObserver = object : BucketList.Observer {
        override fun onIngredientAdded(ingredient: Ingredient) {
            onBucketListChanged(bucketList.getList())
        }
        override fun onIngredientRemoved(ingredient: Ingredient) {
            onBucketListChanged(bucketList.getList())
        }
    }

    override fun modeId() = MainScreenMode.ID.RECIPE

    override fun initialize(snackbarController: SelectedFoodstuffsSnackbar) {
        this.snackbarController = snackbarController
        bucketList.addObserver(bucketListObserver)
        onBucketListChanged(bucketList.getList())

        this.snackbarController.setOnBasketClickRunnable {
            BucketListActivity.start(
                    fragment,
                    RequestCodes.MAIN_SCREEN_BUCKET_LIST_CREATE_FOODSTUFF)
        }
        this.snackbarController.setOnDismissListener {
            val dismissedRecipe = bucketList.getRecipe()
            bucketList.clear()
            val snackbar = Snackbar.make(
                    fragment.requireView(),
                    R.string.work_with_recipe_canceled, Snackbar.LENGTH_LONG)
            snackbar.setAction(R.string.undo) {
                bucketList.setRecipe(dismissedRecipe)
                if (modesSwitcher.modeId() != MainScreenMode.ID.RECIPE) {
                    modesSwitcher.switchModeTo(
                            MainScreenRecipeMode(
                                    modesSwitcher, fragment, bucketList, recipesRepository))
                }
            }
            snackbar.show()
            modesSwitcher.switchModeTo(MainScreenDefaultMode(recipesRepository, fragment))
        }

        val title = if (bucketList.getRecipe().isFromDB) {
            context.getString(R.string.selected_foodstuffs_snackbar_title_recipe_editing)
        } else {
            context.getString(R.string.selected_foodstuffs_snackbar_title_recipe_creation)
        }
        this.snackbarController.show(title)
    }

    override fun deinitialize() {
        bucketList.removeObserver(bucketListObserver)
        snackbarController.setOnBasketClickRunnable(null)
        snackbarController.setOnDismissListener(null)
        snackbarController.hide()
        bucketList.clear()
    }

    override fun setupCardButton2(foodstuff: Foodstuff)
            : Single<Optional<MainScreenMode.CardButtonSettings>> {
        val listener = Card.OnMainButtonSimpleClickListener { foodstuff ->
            bucketList.add(create(foodstuff, ""))
            var totalWeight = 0f
            for (ingredient in bucketList.getList()) {
                totalWeight += ingredient.weight
            }
            bucketList.setTotalWeight(totalWeight)
        }
        return Single.just(Optional.of(
                MainScreenMode.CardButtonSettings(
                        listener, ADD_FOODSTUFF_TO_RECIPE_CARD_TEXT)))
    }

    // Nothing to save, state is stored in a singleton (BucketList).
    override fun saveInstanceState(): Bundle = Bundle()

    private fun onBucketListChanged(ingredients: List<Ingredient>) {
        bucketListChangesCount += 1
        snackbarController.updateSelectedFoodstuffsCounter(ingredients.size)
        if (ingredients.isEmpty() && bucketListChangesCount > 1) {
            // BucketList was emptied
            modesSwitcher.switchModeTo(MainScreenDefaultMode(recipesRepository, fragment))
        }
    }
}