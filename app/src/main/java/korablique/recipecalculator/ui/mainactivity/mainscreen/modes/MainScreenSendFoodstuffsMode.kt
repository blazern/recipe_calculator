package korablique.recipecalculator.ui.mainactivity.mainscreen.modes

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Single
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.BaseFragment
import korablique.recipecalculator.base.Optional
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.outside.partners.direct.FoodstuffsCorrespondenceManager
import korablique.recipecalculator.ui.card.Card
import korablique.recipecalculator.ui.mainactivity.mainscreen.SelectedFoodstuffsSnackbar
import korablique.recipecalculator.ui.mainactivity.partners.PartnersListFragment

@StringRes
private const val INCLUDE_TO_SENT_FOODSTUFFS_CARD_TEXT = R.string.include_to_sent_foodstuffs

private const val EXTRA_FOODSTUFFS = "EXTRA_FOODSTUFFS"

class MainScreenSendFoodstuffsMode(
        private val modesSwitcher: MainScreenMode.ModesSwitcher,
        private val fragment: BaseFragment,
        private val recipesRepository: RecipesRepository,
        private val foodstuffsCorrespondenceManager: FoodstuffsCorrespondenceManager,
        private val foodstuffs: MutableList<Foodstuff> = emptyList<Foodstuff>().toMutableList(),
        private val savedState: Bundle? = null)
    : MainScreenMode, FoodstuffsCorrespondenceManager.Observer {
    private lateinit var snackbarController: SelectedFoodstuffsSnackbar

    init {
        if (savedState != null) {
            foodstuffs += savedState.getParcelableArrayList(EXTRA_FOODSTUFFS)!!
        }
    }

    override fun setupCardButton2(foodstuff: Foodstuff): Single<Optional<MainScreenMode.CardButtonSettings>> {
        val listener = Card.OnMainButtonSimpleClickListener {
            addFoodstuffClicked(it.withoutWeight())
        }
        val result = MainScreenMode.CardButtonSettings(
                listener,
                INCLUDE_TO_SENT_FOODSTUFFS_CARD_TEXT,
                disableButtonWhenWeight0 = false)
        return Single.just(Optional.of(result))
    }

    override fun initialize(snackbarController: SelectedFoodstuffsSnackbar) {
        this.snackbarController = snackbarController
        this.snackbarController.setOnDismissListener {
            val snackbar = Snackbar.make(
                    fragment.requireView(),
                    R.string.work_with_foodstuffs_canceled, Snackbar.LENGTH_LONG)
            snackbar.setAction(R.string.undo) {
                modesSwitcher.switchModeTo(
                        MainScreenSendFoodstuffsMode(
                                modesSwitcher, fragment, recipesRepository,
                                foodstuffsCorrespondenceManager, foodstuffs))
            }
            snackbar.show()
            modesSwitcher.switchModeTo(MainScreenDefaultMode(recipesRepository, fragment))
        }
        this.snackbarController.setOnBasketClickRunnable {
            if (foodstuffs.isEmpty()) {
                Toast.makeText(
                        fragment.requireContext(),
                        R.string.foodstuffs_are_not_selected,
                        Toast.LENGTH_LONG).show()
            } else {
                PartnersListFragment.startToSendFoodstuff(
                        fragment.activity as BaseActivity,
                        foodstuffs)
            }
        }
        this.snackbarController.updateSelectedFoodstuffsCounter(0)
        this.snackbarController.show(
                fragment.getString(R.string.send_selected_foodstuffs_to_partner))

        foodstuffsCorrespondenceManager.addObserver(this)
    }

    override fun deinitialize() {
        snackbarController.hide()
        foodstuffsCorrespondenceManager.removeObserver(this)
    }

    override fun saveInstanceState(): Bundle {
        val bundle = Bundle()
        bundle.putParcelableArrayList(EXTRA_FOODSTUFFS, ArrayList(foodstuffs))
        return bundle
    }

    override fun modeId() = MainScreenMode.ID.SEND_FOODSTUFFS

    private fun addFoodstuffClicked(foodstuff: Foodstuff) {
        foodstuffs += foodstuff
        snackbarController.updateSelectedFoodstuffsCounter(foodstuffs.size)
    }

    override fun onFoodstuffsSentToPartner(foodstuffs: List<Foodstuff>, recipes: List<Recipe>) {
        // The foodstuffs were probably sent by us
        modesSwitcher.switchModeTo(MainScreenDefaultMode(recipesRepository, fragment))
    }
}