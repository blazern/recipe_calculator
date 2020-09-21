package korablique.recipecalculator.ui.mainactivity.mainscreen

import android.os.Bundle
import android.view.View
import korablique.recipecalculator.R
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.base.TimeProvider
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.database.HistoryWorker
import korablique.recipecalculator.model.WeightedFoodstuff
import korablique.recipecalculator.ui.TwoOptionsDialog
import korablique.recipecalculator.ui.TwoOptionsDialog.ButtonName
import korablique.recipecalculator.ui.mainactivity.MainActivitySelectedDateStorage
import javax.inject.Inject

private const val ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG = "ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG"

@FragmentScope
class MainScreenHistoryAdditionController @Inject constructor(
        private val context: BaseActivity,
        private val timeProvider: TimeProvider,
        private val selectedDateStorage: MainActivitySelectedDateStorage,
        private val historyWorker: HistoryWorker,
        private val fragmentCallbacks: FragmentCallbacks): FragmentCallbacks.Observer {
    init {
        fragmentCallbacks.addObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        TwoOptionsDialog.findDialog(
                context.supportFragmentManager, ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG)
                ?.dismiss()
    }

    @JvmOverloads
    fun addToHistory(foodstuff: WeightedFoodstuff, finishCallback: ((added: Boolean)->Unit)? = null) {
        val selectedDate = selectedDateStorage.selectedDate
        val now = timeProvider.now()
        val selectedDateStr = selectedDate.toString("dd.MM.yy")
        val nowStr = now.toLocalDate().toString("dd.MM.yy")
        if (nowStr == selectedDateStr) {
            historyWorker.saveFoodstuffToHistory(
                    timeProvider.now().toDate(), foodstuff.id, foodstuff.weight)
            finishCallback?.invoke(true)
        } else {
            var finished = false

            val dialog: TwoOptionsDialog = TwoOptionsDialog.showDialog(
                    context.supportFragmentManager,
                    ADD_FOODSTUFF_TO_ANOTHER_DATE_DIALOG_TAG,
                    context.getString(R.string.add_foodstuff_to_other_date_dialog_title, selectedDateStr),
                    context.getString(R.string.add_foodstuff_to_other_date_dialog_other_date_response, selectedDateStr),
                    context.getString(R.string.add_foodstuff_to_other_date_dialog_current_day_response))
            dialog.setOnButtonsClickListener { buttonName: ButtonName ->
                if (buttonName == ButtonName.POSITIVE) {
                    historyWorker.saveFoodstuffToHistoryAfterAllOther(
                            selectedDate.toDate(), foodstuff.id, foodstuff.getWeight())

                    finished = true
                    finishCallback?.invoke(true)
                } else if (buttonName == ButtonName.NEGATIVE) {
                    historyWorker.saveFoodstuffToHistory(
                            now.toDate(), foodstuff.id, foodstuff.getWeight())
                    selectedDateStorage.selectedDate = now.toLocalDate()

                    finished = true
                    finishCallback?.invoke(true)
                } else {
                    throw IllegalStateException("Unknown button: $buttonName")
                }
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                if (!finished) {
                    finishCallback?.invoke(false)
                    finished = true
                }
            }
        }
    }
}