package korablique.recipecalculator.ui.mainactivity.mainscreen.modes

import android.os.Bundle
import android.view.View
import korablique.recipecalculator.base.BaseFragment
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.database.RecipesRepository
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Ingredient
import korablique.recipecalculator.ui.bucketlist.BucketList
import korablique.recipecalculator.ui.mainactivity.mainscreen.SelectedFoodstuffsSnackbar
import javax.inject.Inject

private val MODE_SAVED_STATE = "MODE_SAVED_STATE"
private val MODE_ID = "MOD_ID"

@FragmentScope
class MainScreenModesController @Inject constructor(
        private val fragment: BaseFragment,
        private val fragmentCallbacks: FragmentCallbacks,
        private val bucketList: BucketList,
        private val recipesRepository: RecipesRepository)
    : FragmentCallbacks.Observer, MainScreenMode.ModesSwitcher {
    private lateinit var mode: MainScreenMode
    private lateinit var foodstuffsSnackbar: SelectedFoodstuffsSnackbar
    private val context = fragment.requireContext()

    private val bucketListObserver = object : BucketList.Observer {
        override fun onIngredientAdded(ingredient: Ingredient) {
            onBucketListChanged()
        }
    }

    init {
        fragmentCallbacks.addObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        foodstuffsSnackbar = SelectedFoodstuffsSnackbar(fragmentView)

        val modeFromSingletones = tryCreateModeFromSingletonsState()
        mode = when {
            modeFromSingletones != null -> modeFromSingletones
            savedInstanceState != null -> restoreMode(savedInstanceState)
            else -> MainScreenDefaultMode(recipesRepository, fragment)
        }
        mode.initialize(foodstuffsSnackbar)

        bucketList.addObserver(bucketListObserver)
    }

    override fun onFragmentDestroy() {
        bucketList.removeObserver(bucketListObserver)
    }

    private fun restoreMode(savedInstanceState: Bundle): MainScreenMode {
        val id = MainScreenMode.ID.values()[savedInstanceState.getInt(MODE_ID)]
        return when (id) {
            MainScreenMode.ID.DEFAULT -> MainScreenDefaultMode(recipesRepository, fragment)
            MainScreenMode.ID.RECIPE -> MainScreenRecipeMode(this, fragment, bucketList, recipesRepository)
        }
    }

    override fun onFragmentSaveInstanceState(outState: Bundle) {
        outState.putBundle(MODE_SAVED_STATE, mode.saveInstanceState())
        outState.putInt(MODE_ID, mode.modeId().ordinal)
    }

    fun setupCardButton1(foodstuff: Foodstuff) = mode.setupCardButton1(foodstuff)
    fun setupCardButton2(foodstuff: Foodstuff) = mode.setupCardButton2(foodstuff)

    private fun tryCreateModeFromSingletonsState(): MainScreenMode? {
        if (!bucketList.isEmpty()) {
            return MainScreenRecipeMode(this, fragment, bucketList, recipesRepository)
        }
        return null
    }

    override fun switchModeTo(mode: MainScreenMode) {
        this.mode.deinitialize()
        this.mode = mode
        this.mode.initialize(foodstuffsSnackbar)
    }

    override fun modeId() = mode.modeId()

    private fun onBucketListChanged() {
        if (!bucketList.isEmpty() && modeId() != MainScreenMode.ID.RECIPE) {
            switchModeTo(MainScreenRecipeMode(this, fragment, bucketList, recipesRepository))
        }
    }
}
