package korablique.recipecalculator.ui.mainactivity.partners.pairing

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.Keep
import androidx.lifecycle.lifecycleScope
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.R
import korablique.recipecalculator.base.FragmentCallbacks
import korablique.recipecalculator.base.TimeProvider
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.dagger.FragmentScope
import korablique.recipecalculator.outside.STATUS_PARTNER_USER_NOT_FOUND
import korablique.recipecalculator.outside.http.*
import korablique.recipecalculator.outside.partners.Partner
import korablique.recipecalculator.outside.partners.PartnersRegistry
import korablique.recipecalculator.outside.serverAddr
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import korablique.recipecalculator.ui.KeyboardHandler
import kotlinx.coroutines.launch
import javax.inject.Inject

@FragmentScope
class PairingFragmentController @Inject constructor(
        fragmentCallbacks: FragmentCallbacks,
        private val fragment: PairingFragment,
        private val httpContext: BroccalcHttpContext,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val partnersRegistry: PartnersRegistry,
        private val timeProvider: TimeProvider,
        private val mainThreadExecutor: MainThreadExecutor)
    : FragmentCallbacks.Observer, PartnersRegistry.Observer {
    private lateinit var fragmentView: View
    private var lastCountDownTimer: CountDownTimer? = null

    init {
        fragmentCallbacks.addObserver(this)
        partnersRegistry.addObserver(this)
    }

    override fun onFragmentDestroy() {
        partnersRegistry.removeObserver(this)
    }

    override fun onFragmentViewCreated(fragmentView: View, savedInstanceState: Bundle?) {
        this.fragmentView = fragmentView
        fragment.lifecycleScope.launch(mainThreadExecutor) {
            initViews()
        }
        fragmentView.findViewById<View>(R.id.request_code_button).setOnClickListener {
            // Reinit
            fragment.lifecycleScope.launch(mainThreadExecutor) {
                initViews()
            }
        }
    }

    private suspend fun initViews() {
        val userParams = userParamsRegistry.getUserParams()
        if (userParams == null) {
            fragment.close()
            return
        }

        initMainState(isActive = true)
        setProgressBarVisibility(true)
        deinitPairingRequestSending()

        val context = fragment.requireContext()
        val url = ("${serverAddr(context)}/v1/user/start_pairing?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}")
        val response = httpContext.run { httpRequest(url, StartPairingResponse::class) }
        val pairingCode = when (response) {
            is BroccalcNetJobResult.Ok -> response.item.pairing_code
            is BroccalcNetJobResult.Error.NetError -> {
                showSnackbarMsg(R.string.possibly_no_network_connection)
                setProgressBarVisibility(false)
                initMainState(isActive = false)
                return
            }
            is BroccalcNetJobResult.Error -> {
                showSnackbarMsg(R.string.something_went_wrong)
                fragment.close()
                return
            }
        }

        fragmentView.findViewById<TextView>(
                R.id.your_pairing_code_text).text = pairingCode.toString()
        setProgressBarVisibility(false)
        initPairingRequestSending(userParams, response.item.pairing_code_expiration_date)
    }

    private fun initMainState(isActive: Boolean) {
        if (isActive) {
            fragmentView.findViewById<View>(R.id.request_code_button).visibility = View.INVISIBLE
            fragmentView.findViewById<View>(R.id.your_pairing_code_text).visibility = View.VISIBLE
        } else {
            fragmentView.findViewById<View>(R.id.request_code_button).visibility = View.VISIBLE
            fragmentView.findViewById<View>(R.id.your_pairing_code_text).visibility = View.INVISIBLE
        }
    }

    private fun setProgressBarVisibility(isVisible: Boolean) {
        val progressBar = fragmentView.findViewById<View>(R.id.progress_bar_layout)

        if (isVisible) {
            progressBar.visibility = View.VISIBLE
        }

        val alphaEnd = if (isVisible) 1f else 0f
        val animator = ValueAnimator.ofFloat(progressBar.alpha, alphaEnd)
        animator.addUpdateListener {
            progressBar.alpha = it.animatedValue as Float
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                if (!isVisible) {
                    progressBar.visibility = View.GONE
                }
            }
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {}
        })
        animator.duration = 500
        animator.start()
    }

    private fun initPairingRequestSending(userParams: ServerUserParams, expirationDate: Long) {
        fragmentView.findViewById<View>(R.id.partner_pairing_code_edittext).isEnabled = true
        fragmentView.findViewById<View>(R.id.countdown_seconds_title).visibility = View.VISIBLE
        fragmentView.findViewById<View>(R.id.countdown_seconds_value_text).visibility = View.VISIBLE
        initCountDown(expirationDate)

        val partnerPairingCodeView = fragmentView.findViewById<EditText>(R.id.partner_pairing_code_edittext)
        partnerPairingCodeView.addTextChangedListener(object : TextWatcherAdapter() {
            override fun afterTextChanged(view: Editable) {
                if (view.toString().length == 4) {
                    fragment.lifecycleScope.launch(mainThreadExecutor) {
                        sendPairingRequest(to = view.toString(), from = userParams)
                    }
                }
            }
        })
    }

    private fun initCountDown(expirationDate: Long) {
        val secsUntilExpiration = expirationDate - timeProvider.now().millis / 1000
        val timer = object : CountDownTimer(secsUntilExpiration * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val text = (millisUntilFinished / 1000).toString()
                fragmentView.findViewById<TextView>(R.id.countdown_seconds_value_text).text = text
            }
            override fun onFinish() {
                initMainState(isActive = false)
                deinitPairingRequestSending()
                lastCountDownTimer = null
            }
        }
        timer.start()
        lastCountDownTimer = timer
    }

    private fun deinitPairingRequestSending() {
        fragmentView.findViewById<View>(R.id.partner_pairing_code_edittext).isEnabled = false
        fragmentView.findViewById<View>(R.id.countdown_seconds_title).visibility = View.INVISIBLE
        fragmentView.findViewById<View>(R.id.countdown_seconds_value_text).visibility = View.INVISIBLE
        lastCountDownTimer?.cancel()
        lastCountDownTimer = null
    }

    private suspend fun sendPairingRequest(to: String, from: ServerUserParams) {
        val context = fragment.requireContext()
        val url = ("${serverAddr(context)}/v1/user/pairing_request?"
                + "client_token=${from.token}&user_id=${from.uid}&partner_pairing_code=$to")

        val response = httpContext.run {
            httpRequest(url, PairingRequestResponse::class)
        }
        if (STATUS_PARTNER_USER_NOT_FOUND == response.tryGetServerErrorStatus()) {
            showSnackbarMsg(
                    context.getString(R.string.partner_with_code_not_found, to),
                    Snackbar.LENGTH_LONG)
            KeyboardHandler(fragment.requireActivity()).hideKeyBoard()
        } else if (response is BroccalcNetJobResult.Error) {
            if (response is BroccalcNetJobResult.Error.NetError) {
                showSnackbarMsg(R.string.possibly_no_network_connection)
                KeyboardHandler(fragment.requireActivity()).hideKeyBoard()
            } else {
                showSnackbarMsg(R.string.something_went_wrong)
                fragment.close()
            }
        } else {
            KeyboardHandler(fragment.requireActivity()).hideKeyBoard()
            showSnackbarMsg(R.string.pairing_request_sent)
        }
    }

    private fun showSnackbarMsg(stringId: Int, length: Int = Snackbar.LENGTH_SHORT) {
        showSnackbarMsg(fragment.getString(stringId), length)
    }

    private fun showSnackbarMsg(string: String, length: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(
                fragmentView,
                string,
                length).show()
    }

    override fun onPartnersChanged(
            partners: List<Partner>, newPartners: List<Partner>, removedPartners: List<Partner>) {
        if (newPartners.isEmpty()) {
            return
        } else if (newPartners.size == 1) {
            val msg = fragment.getString(R.string.successfully_paired_with, newPartners[0].name)
            showSnackbarMsg(msg)
            fragment.close()
        } else {
            showSnackbarMsg(R.string.successfully_paired_with_multiple_partners)
            fragment.close()
        }
    }
}

@Keep
@JsonClass(generateAdapter = true)
private data class StartPairingResponse(
        val pairing_code: Int,
        val pairing_code_expiration_date: Long
)

@Keep
@JsonClass(generateAdapter = true)
private data class PairingRequestResponse(
        val status: String
)
