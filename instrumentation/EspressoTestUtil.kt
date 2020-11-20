package com.locus.handy.app.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import com.google.android.gms.common.internal.Preconditions.checkArgument
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.locus.handy.R
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.fail

object EspressoTestUtils {
    /**
     * Check for the error expected string in an edit text field.
     *
     * @param activity   Activity that contains the edit text.
     * @param resourceId Id of the edit text field.
     */
    fun checkRequiredFieldOnEditTextWithId(activity: Activity,
                                           resourceId: Int,
                                           expected: String) {

        val view = activity.findViewById<View>(resourceId)
        if (view is EditText) {
            val error = view.error

            if (error != null) {
                val actual = error.toString()

                assertEquals(expected, actual)
            } else {
                fail("The view has no errors")
            }
        } else {
            fail("The view is not instance of EditText")
        }
    }

    /**
     * Check for the error expected string in an text input layout.
     *
     * @param activity   Activity that contains the edit text.
     * @param resourceId Id of the edit text field.
     */
    fun checkIfErrorOnTextInputLayout(activity: Activity,
                                      resourceId: Int,
                                      expected: String) {

        val view = activity.findViewById<View>(resourceId)
        if (view is TextInputLayout) {
            val error = view.error

            if (error != null) {
                val actual = error.toString()

                assertEquals(expected, actual)
            } else {
                fail("The view has no errors")
            }
        } else {
            fail("The view is not instance of TextInputLayout")
        }
    }

    /**
     * from: https://codelabs.developers.google.com/codelabs/android-testing
     * A custom [Matcher] which matches an item in a [RecyclerView] by its text.
     *
     *
     *
     * View constraints:
     *
     *  * View must be a child of a [RecyclerView]
     *
     *
     * @param itemText the text to match
     * @return Matcher that matches text in the given view
     */
    fun withItemText(itemText: String): Matcher<View> {
        checkArgument(!TextUtils.isEmpty(itemText), "itemText cannot be null or empty")

        return object : TypeSafeMatcher<View>() {
            override fun matchesSafely(item: View): Boolean {
                return allOf(
                        isDescendantOfA(isAssignableFrom(RecyclerView::class.java)),
                        withText(itemText)).matches(item)
            }

            override fun describeTo(description: Description) {
                description.appendText("is isDescendantOfA RV with text $itemText")
            }
        }
    }

    class RecyclerViewItemCountAssertion(private val expectedCount: Int) : ViewAssertion {
        override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
            if (noViewFoundException != null) {
                throw noViewFoundException
            }
            val recyclerView = view as RecyclerView
            val adapter = recyclerView.adapter
            assertThat(adapter!!.itemCount, `is`(expectedCount))
        }

    }

    /**
     * Find and click on certain element position in a RecyclerView
     *
     *
     *
     * @param position of the element in the list
     * @param resourceId of the RecyclerView to be clicked
     */
    fun clickOnRecyclerViewItem(position: Int, resourceId: Int) {
        Espresso.onView(withId(resourceId)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
        Espresso.onView(withId(resourceId)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, ViewActions.click()))
    }

    /**
     * Find and click on a child element
     *
     *
     * @param resourceId child element to be clicked
     */
    fun clickChildViewWithId(resourceId: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View>? {
                return null
            }

            override fun getDescription(): String {
                return "Click on a child view with specified id."
            }

            override fun perform(uiController: UiController, view: View) {
                val v = view.findViewById<View>(resourceId)
                v.performClick()
            }
        }
    }

    /**
     * A custom [Matcher] which matches an item in a [RecyclerView] by its position.
     *
     *
     * @param position of the element in the list
     * @param itemMatcher to compare with
     */
    fun withViewAtPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
        return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                val viewHolderForAdapterPosition = recyclerView.findViewHolderForAdapterPosition(position)
                return viewHolderForAdapterPosition != null && itemMatcher.matches(viewHolderForAdapterPosition.itemView)
            }
        }
    }

    fun selectMainMenuTabAtPosition(tabIndex: Int): ViewAction {
        return object : ViewAction {
            override fun getDescription() = "with tab at index $tabIndex"

            override fun getConstraints() = CoreMatchers.allOf(isDisplayed(), isAssignableFrom(TabLayout::class.java))

            override fun perform(uiController: UiController, view: View) {
                val tabLayout = view as TabLayout
                val tabAtIndex: TabLayout.Tab = tabLayout.getTabAt(tabIndex)
                        ?: throw PerformException.Builder()
                                .withCause(Throwable("No tab at index $tabIndex"))
                                .build()

                tabAtIndex.select()
            }
        }
    }

    /**
     * Allow Automatically all the needed permissions so the activity can be tested.
     */
    fun allowPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val device = UiDevice.getInstance(getInstrumentation())

        var acceptString = context.getString(R.string.marshmallow_permission_dialog_accept)
        var acceptButton = device.findObject(UiSelector().text(acceptString))

        if (!acceptButton.exists()) {
            acceptString = context.getString(R.string.marshmallow_permission_dialog_accept_uppercase)
            acceptButton = device.findObject(UiSelector().text(acceptString))
        }

        if (acceptButton.exists()) {
            try {
                acceptButton.click()
            } catch (e: UiObjectNotFoundException) {
                e.printStackTrace()
            }
        }

        var allowString = context.getString(R.string.marshmallow_permission_dialog_allow)
        var allowPermissions = device.findObject(UiSelector().text(allowString))

        if (!allowPermissions.exists()) {
            allowString = context.getString(R.string.marshmallow_permission_dialog_allow_uppercase)
            allowPermissions = device.findObject(UiSelector().text(allowString))
        }

        while (allowPermissions.exists()) {
            try {
                allowPermissions.click()
            } catch (e: UiObjectNotFoundException) {
                e.printStackTrace()
            }

        }
    }

    fun activateOnPhoneTimeDateAutomaticSettingIfNeeded() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val okString = context.getString(R.string.action_ok)
        val device = UiDevice.getInstance(getInstrumentation())
        val dialogOkButton = device.findObject(UiSelector().text(okString))

        while (dialogOkButton.exists()) {
            try {
                dialogOkButton.click()
            } catch (e: UiObjectNotFoundException) {
                e.printStackTrace()
            }

        }

        val timeDateSwitch = device.findObject(UiSelector()
                .text(context.getString(R.string.automatic_date_time_setting)))
        val zoneSwitch = device.findObject(UiSelector()
                .text(context.getString(R.string.automatic_time_zone_setting)))

        if (timeDateSwitch.exists() && zoneSwitch.exists()) {
            try {
                timeDateSwitch.click()
                zoneSwitch.click()
                device.pressBack()
            } catch (e: UiObjectNotFoundException) {
                e.printStackTrace()
            }

        }
    }

    fun enableLocationIfNeeded() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val okString = context.getString(R.string.location_service__enable)
        val device = UiDevice.getInstance(getInstrumentation())
        val dialogOkButton = device.findObject(UiSelector().text(okString))

        while (dialogOkButton.exists()) {
            try {
                dialogOkButton.click()
            } catch (e: UiObjectNotFoundException) {
                e.printStackTrace()
            }
        }

        val enabledSwitch = device.findObject(UiSelector()
                .text(context.getString(R.string.location_disabled_switch)))

        if (enabledSwitch.exists()) {
            try {
                enabledSwitch.click()
                device.pressBack()
            } catch (e: UiObjectNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    fun typeSearchViewText(text: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                //Ensure that only apply if it is a SearchView and if it is visible.
                return CoreMatchers.allOf(isDisplayed(), isAssignableFrom(SearchView::class.java))
            }

            override fun getDescription(): String {
                return "Change view text"
            }

            override fun perform(uiController: UiController, view: View) {
                (view as SearchView).setQuery(text, false)
            }
        }
    }

    fun openActionBarMenu() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        openActionBarOverflowOrOptionsMenu(context)
    }
  
    fun getResourceString(id: Int): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.resources.getString(id)
    }
  
    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun isKeyboardShown(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return inputMethodManager.isAcceptingText
    }
}
