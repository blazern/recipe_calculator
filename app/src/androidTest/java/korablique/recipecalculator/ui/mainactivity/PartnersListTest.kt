package korablique.recipecalculator.ui.mainactivity

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyBelow
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import korablique.recipecalculator.R
import korablique.recipecalculator.outside.http.RequestResult
import korablique.recipecalculator.outside.http.Response
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.util.EspressoUtils.isNotDisplayed
import korablique.recipecalculator.util.EspressoUtils.matches
import korablique.recipecalculator.util.checkNetworkChangesSnackbarReaction
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

private const val UID = "123e4567-e89b-12d3-a456-426655440000"
private const val TOKEN = "123e4567-e89b-12d3-a456-426655440001"

@RunWith(AndroidJUnit4::class)
@LargeTest
class PartnersListTest : MainActivityTestsBase() {
    @Test
    fun partnersListOpening_whenNotRegistered() {
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

        onView(withId(R.id.menu_item_profile)).perform(click())
        onView(withId(R.id.layout_button_partners)).perform(click())

        onView(withId(R.id.button_google_sign_in)).check(matches(isDisplayed()))
        onView(withId(R.id.add_fab)).check(isNotDisplayed())
        onView(withId(R.id.no_partners_layout)).check(isNotDisplayed())

        onView(withId(R.id.button_google_sign_in)).perform(click())

        onView(withId(R.id.button_google_sign_in)).check(isNotDisplayed())
        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
        onView(withId(R.id.no_partners_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun partnersListOpening_whenRegistered() {
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

        // Interactive params obtainer will register through GP
        GlobalScope.launch(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }

        onView(withId(R.id.menu_item_profile)).perform(click())
        onView(withId(R.id.layout_button_partners)).perform(click())

        onView(withId(R.id.button_google_sign_in)).check(isNotDisplayed())
        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
        onView(withId(R.id.no_partners_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun partnersListOpening_whenNotRegistered_andHavePartners() {
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
        fakeHttpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid1",
                            "partner_name": "partner name1"
                        },
                        {
                            "partner_user_id": "uid2",
                            "partner_name": "partner name2"
                        }
                    ]
                }
            """.trimIndent()
            RequestResult.Success(Response(body))
        }
        mActivityRule.launchActivity(null)

        onView(withId(R.id.menu_item_profile)).perform(click())
        onView(withId(R.id.layout_button_partners)).perform(click())

        onView(withId(R.id.button_google_sign_in)).check(matches(isDisplayed()))
        onView(withId(R.id.add_fab)).check(isNotDisplayed())
        onView(withId(R.id.no_partners_layout)).check(isNotDisplayed())

        onView(withId(R.id.button_google_sign_in)).perform(click())

        onView(withId(R.id.button_google_sign_in)).check(isNotDisplayed())
        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
        onView(withId(R.id.no_partners_layout)).check(isNotDisplayed())

        onView(withText("partner name1")).check(matches(isDisplayed()))
        onView(allOf(
                withText("partner name2"),
                matches(isCompletelyBelow(withText("partner name1")))))
                    .check(matches(isDisplayed()))
    }

    @Test
    fun partnersListOpening_whenRegistered_andHavePartners() {
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
        fakeHttpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid1",
                            "partner_name": "partner name1"
                        },
                        {
                            "partner_user_id": "uid2",
                            "partner_name": "partner name2"
                        }
                    ]
                }
            """.trimIndent()
            RequestResult.Success(Response(body))
        }
        mActivityRule.launchActivity(null)

        // Interactive params obtainer will register through GP
        GlobalScope.launch(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }

        onView(withId(R.id.menu_item_profile)).perform(click())
        onView(withId(R.id.layout_button_partners)).perform(click())

        onView(withId(R.id.button_google_sign_in)).check(isNotDisplayed())
        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
        onView(withId(R.id.no_partners_layout)).check(isNotDisplayed())

        onView(withText("partner name1")).check(matches(isDisplayed()))
        onView(allOf(
                withText("partner name2"),
                matches(isCompletelyBelow(withText("partner name1")))))
                    .check(matches(isDisplayed()))
    }

    @Test
    fun networkUnavailableMessage() {
        mActivityRule.launchActivity(null)

        onView(withId(R.id.menu_item_profile)).perform(click())
        onView(withId(R.id.layout_button_partners)).perform(click())

        checkNetworkChangesSnackbarReaction(fakeNetworkStateDispatcher)
    }

    @Test
    fun deletePartner() {
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
        fakeHttpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid1",
                            "partner_name": "partner name1"
                        },
                        {
                            "partner_user_id": "uid2",
                            "partner_name": "partner name2"
                        }
                    ]
                }
            """.trimIndent()
            RequestResult.Success(Response(body))
        }
        mActivityRule.launchActivity(null)

        // Interactive params obtainer will register through GP
        GlobalScope.launch(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }


        onView(withId(R.id.menu_item_profile)).perform(click())
        onView(withId(R.id.layout_button_partners)).perform(click())

        onView(withText("partner name1")).check(matches(isDisplayed()))
        onView(withText("partner name2")).check(matches(isDisplayed()))

        // Prepare deletion response
        fakeHttpClient.setResponse(".*unpair.*") {
            RequestResult.Success(Response("""{"status":"ok"}"""))
        }
        // Prepare a response without the deleted user
        fakeHttpClient.setResponse(".*list_partners.*") {
            val body = """
                {
                    "status": "ok",
                    "partners": [
                        {
                            "partner_user_id": "uid1",
                            "partner_name": "partner name1"
                        }
                    ]
                }
            """
            RequestResult.Success(Response(body))
        }

        onView(withText("partner name2")).perform(longClick())
        onView(withText(R.string.delete_partner)).perform(click())

        onView(withText("partner name1")).check(matches(isDisplayed()))
        onView(withText("partner name2")).check(isNotDisplayed())
    }

    @Test
    fun partnerListOpening_fromMainScreen() {
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

        // Interactive params obtainer will register through GP
        GlobalScope.launch(mainThreadExecutor) {
            interactiveServerUserParamsObtainer.obtainUserParams()
        }

        // No partners list yet
        onView(withId(R.id.partners_list_fragment)).check(isNotDisplayed())

        onView(withId(R.id.mode_fab)).perform(click())
        onView(withId(R.id.partners_menu_button)).perform(click())

        // Partners list opened
        onView(withId(R.id.partners_list_fragment)).check(matches(isDisplayed()))
        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
        onView(withId(R.id.no_partners_layout)).check(matches(isDisplayed()))
    }
}
