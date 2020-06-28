package korablique.recipecalculator.ui.mainactivity.mainscreen.modes

import android.os.Bundle
import androidx.annotation.StringRes
import io.reactivex.Single
import korablique.recipecalculator.R
import korablique.recipecalculator.RequestCodes.MAIN_SCREEN_BUCKET_LIST_OPEN_RECIPE
import korablique.recipecalculator.base.BaseFragment
import korablique.recipecalculator.base.Optional
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.ui.bucketlist.BucketListActivity
import korablique.recipecalculator.ui.card.Card
import korablique.recipecalculator.ui.mainactivity.mainscreen.SelectedFoodstuffsSnackbar

@StringRes
private val OPEN_FOODSTUFF_RECIPE_CARD_TEXT = R.string.open_foodstuff_recipe

class MainScreenDefaultMode(
        val recipesRepository: RecipesRepository,
        val fragment: BaseFragment) : MainScreenMode {
    override fun initialize(snackbarController: SelectedFoodstuffsSnackbar) = Unit
    override fun deinitialize() = Unit
    override fun saveInstanceState(): Bundle = Bundle()
    override fun modeId() = MainScreenMode.ID.DEFAULT

    override fun setupCardButton2(foodstuff: Foodstuff)
            : Single<Optional<MainScreenMode.CardButtonSettings>> {
        // If card is opened for recipe, display 'Open recipe' button.
        // If card is opened not for recipe, don't display the button.
        return recipesRepository.getRecipeOfFoodstuffRx(foodstuff)
                .map {
                    if (it.isPresent) {
                        Optional.of(recipeToButton2Settings(it.get()))
                    } else {
                        Optional.empty()
                    }
                }
    }

    private fun recipeToButton2Settings(recipe: Recipe): MainScreenMode.CardButtonSettings {
        val clickListener = Card.OnMainButtonSimpleClickListener {
            BucketListActivity.startForRecipe(
                    fragment,
                    MAIN_SCREEN_BUCKET_LIST_OPEN_RECIPE,
                    recipe,
                    editRecipe = false)
        }
        return MainScreenMode.CardButtonSettings(
                clickListener,
                OPEN_FOODSTUFF_RECIPE_CARD_TEXT,
                closeCardOnButtonClick = false,
                disableButtonWhenWeight0 = false)
    }
}