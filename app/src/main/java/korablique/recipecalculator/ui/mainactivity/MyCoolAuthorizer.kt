package korablique.recipecalculator.ui.mainactivity

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import korablique.recipecalculator.RequestCodes
import korablique.recipecalculator.base.ActivityCallbacks
import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.dagger.ActivityScope
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task


@ActivityScope
class MyCoolAuthorizer @Inject constructor(
        private val context: BaseActivity,
        private val activityCallbacks: ActivityCallbacks) {
    init {
        activityCallbacks.addObserver(object : ActivityCallbacks.Observer {
            override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
                this@MyCoolAuthorizer.onActivityResult(requestCode, resultCode, data)
            }
        })
    }

    fun auth() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("560504820389-e0pvlp32fn3kn10ud6md0fp533f0170f.apps.googleusercontent.com")
                .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(context, gso)

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            // already signed in
            return
        }

        val intent = mGoogleSignInClient.getSignInIntent()
        context.startActivityForResult(intent, RequestCodes.GOOGLE_SIGN_IN)

//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
//            // Permission is not granted
//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(context,
//                            Manifest.permission.GET_ACCOUNTS)) {
//                // Show an explanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//                // TODO: show explanation
//                throw Error("todo: show explanation")
//            } else {
//                // No explanation needed, we can request the permission.
//                ActivityCompat.requestPermissions(context,
//                        arrayOf(Manifest.permission.GET_ACCOUNTS),
//                        RequestCodes.PERMISSION_GET_ACCOUNTS)
//            }
//            return
//        }
//
//        authContinue()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != RequestCodes.GOOGLE_SIGN_IN) {
            return
        }
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        // The Task returned from this call is always completed, no need to attach
        // a listener.
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        handleSignInResult(task)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            android.util.Log.e("DANIL", account!!.idToken)
            // Signed in successfully, show authenticated UI.
//            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            throw Error("unhandled", e)
        }

    }

    private fun authContinue() {
//        val am: AccountManager = AccountManager.get(context)
//        val options = Bundle()
//
//        // TODO: use newChooseAccountIntent instead of GET_ACCOUNTS on O and above
//        // TODO: use account specified by user when newChooseAccountIntent used
//        // TODO: list accounts to user and ask which one to use when GET_ACCOUNTS used
//        val accounts = am.getAccountsByType("com.google")
//        val account = accounts[0]
//
//        am.getAuthToken(
//                account,                     // Account retrieved using getAccountsByType()
//                "Manage your tasks",            // Auth scope
//                options,                        // Authenticator-specific options
//                this,                           // Your activity
//                OnTokenAcquired(),              // Callback called when a token is successfully acquired
//                Handler(OnError())              // Callback called if an error occurs
//        )

    }
}