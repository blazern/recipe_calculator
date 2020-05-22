package korablique.recipecalculator.ui.mainactivity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.R
import korablique.recipecalculator.outside.STATUS_INVALID_CLIENT_TOKEN
import korablique.recipecalculator.outside.STATUS_USER_NOT_FOUND
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.outside.userparams.ObtainResult
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import korablique.recipecalculator.util.FakeGPAuthorizer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception

private const val UID = "123e4567-e89b-12d3-a456-426655440000"
private const val TOKEN = "123e4567-e89b-12d3-a456-426655440001"

@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthTest : MainActivityTestsBase() {
    @Test
    fun noAccMoveServAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "$UID",
                    "client_token": "$TOKEN"
                }
            """.trimIndent()))
        }
        mActivityRule.launchActivity(null)


        val result = interactiveServerUserParamsObtainer.obtainUserParams()
        assertTrue(result is ObtainResult.Success)
        val success = result as ObtainResult.Success
        assertEquals(ServerUserParams(UID, TOKEN), success.params)
    }

    @Test
    fun accMoveServAuth() {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "already_registered",
                    "error_description": "wow such error"
                }
            """))
        }
        fakeHttpClient.setResponse(".*move_device_account.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "$UID",
                    "client_token": "$TOKEN",
                    "user_name": "general kenobi"
                }
            """))
        }
        fakeHttpClient.setResponse(".*update_user_name.*") {
            RequestResult.Success(Response("""{"status": "ok"}"""))
        }

        mActivityRule.launchActivity(null)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*update_user_name.*").size)

        val resultFuture = GlobalScope.async(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }
        onView(withId(R.id.positive_button)).perform(click())

        val result = runBlocking { resultFuture.await() }
        assertEquals("$result", ObtainResult.Success::class, result::class)
        val success = result as ObtainResult.Success
        assertEquals(ServerUserParams(UID, TOKEN), success.params)

        // Account move should always be accompanied by user name update
        assertEquals(1, fakeHttpClient.getRequestsMatching(".*update_user_name.*").size)
    }

    @Test
    fun cancelledByUserAccMoveServAuth() {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "already_registered",
                    "error_description": "wow such error"
                }
            """.trimIndent()))
        }
        mActivityRule.launchActivity(null)


        val resultFuture = GlobalScope.async(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }
        onView(withId(R.id.negative_button)).perform(click())

        val result = runBlocking { resultFuture.await() }
        assertEquals("$result", ObtainResult.CanceledByUser::class, result::class)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*move_device_account.*").size)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*update_user_name.*").size)
    }

    @Test
    fun cancelledByUserGPAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.CanceledByUser
        mActivityRule.launchActivity(null)
        val result = interactiveServerUserParamsObtainer.obtainUserParams()
        assertEquals("$result", ObtainResult.CanceledByUser::class, result::class)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*register.*").size)
    }

    @Test
    fun failedGPAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.Failure(Exception("such exception wow"))
        mActivityRule.launchActivity(null)
        val result = interactiveServerUserParamsObtainer.obtainUserParams()
        assertEquals("$result", ObtainResult.Failure::class, result::class)
        assertEquals(0, fakeHttpClient.getRequestsMatching(".*register.*").size)
    }

    @Test
    fun failedServAuth() = runBlocking {
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*") {
            RequestResult.Success(Response("""
                {
                    "status": "internal_error",
                    "error_description": "wow such error"
                }
            """.trimIndent()))
        }
        mActivityRule.launchActivity(null)


        val result = interactiveServerUserParamsObtainer.obtainUserParams()
        assertEquals("$result", ObtainResult.Failure::class, result::class)
    }

    @Test
    fun savedAuthErasedOnServerInvalidTokenError() {
        testSavedUserParamsReactionOnServerErrors(STATUS_INVALID_CLIENT_TOKEN, true)
    }

    @Test
    fun savedAuthErasedOnServerUserNotFoundError() = runBlocking {
        testSavedUserParamsReactionOnServerErrors(STATUS_USER_NOT_FOUND, true)
    }

    @Test
    fun savedAuthNotErasedOnServerInternalError() = runBlocking {
        testSavedUserParamsReactionOnServerErrors("internal_error", false)
    }

    private fun testSavedUserParamsReactionOnServerErrors(
            errorStatus: String,
            paramsExpectedToBeErased: Boolean) = runBlocking {
        var observedUser: ServerUserParams? = null
        val observer = object : ServerUserParamsRegistry.Observer {
            override fun onUserParamsChange(userParams: ServerUserParams?) {
                observedUser = userParams
            }
        }
        serverUserParamsRegistry.addObserver(observer)

        // Successful registration
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "$UID",
                    "client_token": "$TOKEN"
                }
            """))
        }
        mActivityRule.launchActivity(null)
        val result = interactiveServerUserParamsObtainer.obtainUserParams()
        assertTrue(result is ObtainResult.Success)

        // Assert user params exist at first
        assertEquals(ServerUserParams(UID, TOKEN), observedUser)
        assertEquals(ServerUserParams(UID, TOKEN), serverUserParamsRegistry.getUserParams())

        // Set up failed response and make request so that the response would be received
        fakeHttpClient.setResponse(".*my_cool_request.*") {
            RequestResult.Success(Response("""
                {
                    "status": "$errorStatus",
                    "error_description": "wow"
                }
            """))
        }
        httpContext.httpRequest("my_cool_request", String::class)

        if (paramsExpectedToBeErased) {
            // Assert user params are gone
            assertEquals(null, observedUser)
            assertEquals(null, serverUserParamsRegistry.getUserParams())

            // Register again
            val result2 = interactiveServerUserParamsObtainer.obtainUserParams()
            assertTrue(result2 is ObtainResult.Success)

            // Assert user params exist again
            assertEquals(ServerUserParams(UID, TOKEN), observedUser)
            assertEquals(ServerUserParams(UID, TOKEN), serverUserParamsRegistry.getUserParams())
        } else {
            // Assert user params still exist
            assertEquals(ServerUserParams(UID, TOKEN), observedUser)
            assertEquals(ServerUserParams(UID, TOKEN), serverUserParamsRegistry.getUserParams())
        }
    }

    @Test
    fun serverUserParamsNotStored_whenUidIsNotUUID() = runBlocking {
        // Successful registration with given invalid uid
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "${UID}invalid_part",
                    "client_token": "$TOKEN"
                }
            """))
        }
        mActivityRule.launchActivity(null)
        val result = interactiveServerUserParamsObtainer.obtainUserParams()
        assertTrue(result is ObtainResult.Success)

        assertNull(serverUserParamsRegistry.getUserParams())
    }

    @Test
    fun serverUserParamsNotStored_whenTokenIsNotUUID() = runBlocking {
        // Successful registration with given invalid token
        fakeGPAuthorizer.authResult = GPAuthResult.Success("gptoken")
        fakeHttpClient.setResponse(".*register.*") {
            RequestResult.Success(Response("""
                {
                    "status": "ok",
                    "user_id": "$UID",
                    "client_token": "${TOKEN}invalid_part"
                }
            """))
        }
        mActivityRule.launchActivity(null)
        val result = interactiveServerUserParamsObtainer.obtainUserParams()
        assertTrue(result is ObtainResult.Success)

        assertNull(serverUserParamsRegistry.getUserParams())
    }

    @Test
    fun canSetSavedUserParams_directlyFromTest() = runBlocking {
        val params = ServerUserParams(UID, TOKEN)
        ServerUserParamsRegistry.storeServerUserParamsPersistently(params, prefsManager)
        val otherServerUserParamsRegistry1 =
                ServerUserParamsRegistry(
                        context, mainThreadExecutor, ioExecutor, FakeGPAuthorizer(),
                        userNameProvider, httpContext, prefsManager)
         assertEquals(params, otherServerUserParamsRegistry1.getUserParams())

        ServerUserParamsRegistry.storeServerUserParamsPersistently(null, prefsManager)
        val otherServerUserParamsRegistry2 =
                ServerUserParamsRegistry(
                        context, mainThreadExecutor, ioExecutor, FakeGPAuthorizer(),
                        userNameProvider, httpContext, prefsManager)
        assertEquals(null, otherServerUserParamsRegistry2.getUserParams())
    }

    @Test
    fun erasesSavedServerUserParamsOnStart_whenUidIsNotUUID() = runBlocking  {
        val params = ServerUserParams("${UID}invalid_part", TOKEN)
        ServerUserParamsRegistry.storeServerUserParamsPersistently(params, prefsManager)
        val otherServerUserParamsRegistry =
                ServerUserParamsRegistry(
                        context, mainThreadExecutor, ioExecutor, FakeGPAuthorizer(),
                        userNameProvider, httpContext, prefsManager)
        // Null, even though we stored params
        assertEquals(null, otherServerUserParamsRegistry.getUserParams())
    }

    @Test
    fun erasesSavedServerUserParamsOnStart_whenTokenIsNotUUID() = runBlocking {
        val params = ServerUserParams("UID", "${TOKEN}invalid_part")
        ServerUserParamsRegistry.storeServerUserParamsPersistently(params, prefsManager)
        val otherServerUserParamsRegistry =
                ServerUserParamsRegistry(
                        context, mainThreadExecutor, ioExecutor, FakeGPAuthorizer(),
                        userNameProvider, httpContext, prefsManager)
        // Null, even though we stored params
        assertEquals(null, otherServerUserParamsRegistry.getUserParams())
    }
}
