package korablique.recipecalculator.ui.mainactivity.mainscreen.modes

import android.os.Bundle
import androidx.annotation.StringRes
import io.reactivex.Single
import korablique.recipecalculator.base.NTuple2
import korablique.recipecalculator.base.Optional
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.ui.card.Card
import korablique.recipecalculator.ui.card.CardDialog
import korablique.recipecalculator.ui.mainactivity.mainscreen.SelectedFoodstuffsSnackbar

interface MainScreenMode {
    enum class ID {
        DEFAULT,
        RECIPE,
        SEND_FOODSTUFFS,
    }

    interface ModesSwitcher {
        fun switchModeTo(mode: MainScreenMode)
        fun modeId(): ID
    }

    data class CardButtonSettings(
            val clickListener: Card.OnMainButtonSimpleClickListener,
            val btnTitleStrId: Int,
            val closeCardOnButtonClick: Boolean = true,
            val disableButtonWhenWeight0: Boolean = true)

    fun initialize(snackbarController: SelectedFoodstuffsSnackbar)
    fun deinitialize()
    fun saveInstanceState(): Bundle
    fun setupCardButton1(foodstuff: Foodstuff): Single<Optional<CardButtonSettings>> =
            Single.just(Optional.empty())
    fun setupCardButton2(foodstuff: Foodstuff): Single<Optional<CardButtonSettings>> =
            Single.just(Optional.empty())
    fun modeId(): ID
}

enum class FinishAttemptResult {
    DONE,
    USER_CANCELLED
}
