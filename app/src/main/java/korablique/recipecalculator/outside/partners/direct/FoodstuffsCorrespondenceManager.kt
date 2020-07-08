package korablique.recipecalculator.outside.partners.direct

import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import korablique.recipecalculator.R
import korablique.recipecalculator.base.CurrentActivityProvider
import korablique.recipecalculator.base.RxGlobalSubscriptions
import korablique.recipecalculator.base.logging.Log
import korablique.recipecalculator.database.FoodstuffsList
import korablique.recipecalculator.model.Foodstuff
import korablique.recipecalculator.model.Recipe
import korablique.recipecalculator.model.proto.FoodstuffsMsgProtos
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.http.extractException
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.ui.mainactivity.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

private const val DIRECT_MSG_TYPE_FOODSTUFF = "foodstuff"

@Singleton
class FoodstuffsCorrespondenceManager @Inject constructor(
        private val directMsgsManager: DirectMsgsManager,
        private val foodstuffsList: FoodstuffsList,
        private val currentActivityProvider: CurrentActivityProvider,
        private val globalSubscriptions: RxGlobalSubscriptions
) : DirectMsgsManager.DirectMessageReceiver {
    private val observers = mutableListOf<Observer>()

    interface Observer {
        fun onFoodstuffsSentToPartner(foodstuffs: List<Foodstuff>, recipes: List<Recipe>) = Unit
        fun onFoodstuffsSendingToPartnerFailed(error: Exception?) = Unit
    }

    companion object {
        @VisibleForTesting
        fun createFoodstuffsDirectMsg(
                foodstuffs: List<Foodstuff>)
                : Pair<String, String> {
            val proto = FoodstuffsMsgProtos.FoodstuffsMsg.newBuilder()
                    .addAllPlainFoodstuffs(foodstuffs.map { it.toProto() })
                    .build()

            val foodstuffEncoded = Base64.encodeToString(proto.toByteArray(), Base64.DEFAULT)
            return Pair(DIRECT_MSG_TYPE_FOODSTUFF, foodstuffEncoded)
        }
    }

    init {
        directMsgsManager.registerReceiver(DIRECT_MSG_TYPE_FOODSTUFF, this)
    }

    suspend fun sendFooodstuffsToPartner(foodstuffs: List<Foodstuff>, partner: Partner): BroccalcNetJobResult<Unit> {
        val msg = createFoodstuffsDirectMsg(foodstuffs)
        val result = directMsgsManager.sendDirectMSGToPartner(msg.first, msg.second, partner)

        val view = currentActivityProvider.currentActivity?.contentView
        when (result) {
            is BroccalcNetJobResult.Ok -> {
                observers.forEach { it.onFoodstuffsSentToPartner(foodstuffs, emptyList()) }
                if (view != null) {
                    Snackbar.make(view, R.string.foodstuff_is_sent, Snackbar.LENGTH_SHORT).show()
                }
            }
            is BroccalcNetJobResult.Error -> {
                observers.forEach { it.onFoodstuffsSendingToPartnerFailed(result.extractException()) }
                if (view != null) {
                    Snackbar.make(view, R.string.something_went_wrong, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        return result
    }

    override fun onNewDirectMessage(msg: String) {
        Log.i("FoodstuffsCorrespondenceManager.onNewDirectMessage start")
        val foodstuffMsg = try {
            FoodstuffsMsgProtos.FoodstuffsMsg.parseFrom(Base64.decode(msg, Base64.DEFAULT))
        } catch (e: Exception) {
            Log.w(e, "FoodstuffsCorrespondenceManager.onNewDirectMessage error1")
            null
        }
        if (foodstuffMsg == null) {
            return
        }

        // Get foodstuffs and recipes.
        // Erase their ids, because the ids are local to another application.
        val foodstuffs = foodstuffMsg.getPlainFoodstuffsList()
                .map { Foodstuff.fromProto(it) }
                .map { it.recreateWithId(0) }

        val d = Observable
                .fromIterable(foodstuffs)
                .map { foodstuffsList.saveFoodstuff(it) }
                .flatMapSingle { it }
                .toList()
                .subscribe(
                        {
                            if (it.size == 1) {
                                tryShowReceivedFoodstuffSnackbar(it[0])
                            }
                            Log.i("FoodstuffsCorrespondenceManager.onNewDirectMessage " +
                                    "foodstuffs saved: $it")
                        },
                        {
                            Log.e(it, "FoodstuffsCorrespondenceManager.onNewDirectMessage " +
                                    "couldn't save foodstuff")
                            // Couldn't save foodstuff, nothing to do
                        })
        globalSubscriptions.add(d)
    }

    private fun tryShowReceivedFoodstuffSnackbar(foodstuff: Foodstuff) {
        val activity = currentActivityProvider.currentActivity ?: return
        val view = activity.contentView ?: return
        val msg = activity.getString(R.string.foodstuff_is_received, foodstuff.name)
        val snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG)
        if (activity is MainActivity) {
            snackbar.setAction(R.string.show_received_foodstuff) {
                activity.openFoodstuffCard(foodstuff)
            }
        }
        snackbar.show()
    }

    fun addObserver(observer: Observer) {
        observers += observer
    }

    fun removeObserver(observer: Observer) {
        observers -= observer
    }
}
