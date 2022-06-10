package com.burrowsapps.example.gif.ui.giflist

import android.Manifest.permission.*
import android.widget.TextView
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.intent.Intents.*
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import com.burrowsapps.example.gif.R
import com.burrowsapps.example.gif.test.TestFileUtils.MOCK_SERVER_PORT
import com.burrowsapps.example.gif.test.TestFileUtils.getMockFileResponse
import com.burrowsapps.example.gif.test.TestFileUtils.getMockResponse
import com.burrowsapps.example.gif.ui.license.LicenseActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.ScreenshotWatcher
import java.net.HttpURLConnection.HTTP_NOT_FOUND


@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class GifActivityTest {
  @get:Rule(order = 0)
  val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  @get:Rule(order = 2)
  val activityScenarioRule: ActivityScenarioRule<GifActivity> =
    ActivityScenarioRule(GifActivity::class.java)

  @get:Rule(order = 3)
  val permissionRule: GrantPermissionRule = GrantPermissionRule
    .grant(INTERNET, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)

  @get:Rule(order = 4)
  val screenshotWatcher = ScreenshotWatcher()

  private val server = MockWebServer()

  @Before
  fun setUp() {
    hiltRule.inject()

    server.apply {
      dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          request.path.orEmpty().apply {
            return when {
              contains("v1/trending") -> getMockResponse("/trending_results.json")
              contains("v1/search") -> getMockResponse("/search_results.json")
              contains("images") -> getMockFileResponse("/ic_launcher.webp")
              else -> MockResponse().setResponseCode(HTTP_NOT_FOUND)
            }
          }
        }
      }

      start(MOCK_SERVER_PORT)
    }
  }

  @After
  fun tearDown() {
    server.shutdown()
  }

  @Test
  fun testMainTitleIsShowing() {
    onView(
      allOf(
        instanceOf(TextView::class.java),
        withParent(withId(R.id.toolbar))
      )
    ).check(matches(withText(containsString("Top Trending Gifs"))))
  }

  @Test
  fun testSearchWidgetIsVisible() {
    onView(withId(R.id.menuSearch)).check(matches(isDisplayed()))
  }

  @Test
  fun testToolbarWidgetIsVisible() {
    onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
  }

  @Test
  fun testMainActivityInLicenseMenuNotVisible() {
    init()

    openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

    onView(withText(R.string.menu_licenses)).perform(click())

    intended(hasComponent(LicenseActivity::class.java.name))
    onView(withId(R.id.recyclerView)).check(doesNotExist())

    release()
  }

  @Test
  fun testSearchIconHidesAfterClick() {
    onView(withId(R.id.menuSearch)).perform(click())

    onView(withText(R.string.menu_licenses)).check(doesNotExist())
  }

  @Test
  fun testUpdatingGiffList() {
    onView(withId(R.id.swipeRefresh)).perform(swipeUp())

    onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
  }

  @Test
  fun testScrollDownGiftList() {
    onView(withId(R.id.swipeRefresh)).perform(swipeDown())

    onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
  }

  @Test
  fun testHiddenMenuAfterGifSelected() {
    onView(withId(R.id.swipeRefresh)).perform(swipeUp())

    onView(withId(R.id.recyclerView))
      .perform(
        RecyclerViewActions.actionOnItemAtPosition<GifAdapter.ViewHolder>(1, longClick())
      )

    onView(withId(R.id.menuSearch)).check(doesNotExist())
    onView(withId(R.id.menuLicenses)).check(doesNotExist())
  }

  @Test
  fun testHiddenRecyclerViewAfterGifSelected() {
    onView(withId(R.id.swipeRefresh)).perform(swipeUp())

    onView(withId(R.id.recyclerView))
      .perform(
        RecyclerViewActions.actionOnItemAtPosition<GifAdapter.ViewHolder>(1, longClick())
    )

    onView(withId(R.id.recyclerView)).check(doesNotExist())
  }

  @Test
  fun testTrendingVisibleAppLaunch() {
    onView(withId(R.id.recyclerView))
      .check(matches(isDisplayed()))
  }

  @Test
  fun testLicenseMenuOpensLicenseActivity() {
    init()

    openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

    onView(withText(R.string.menu_licenses)).perform(click())

    intended(hasComponent(LicenseActivity::class.java.name))

    release()
  }

  @Test
  fun testOpensLicenseActivityAndGoBack() {
    init()

    openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

    onView(withText(R.string.menu_licenses)).perform(click())

    intended(hasComponent(LicenseActivity::class.java.name))

    pressBackUnconditionally()

    onView(
      allOf(
        instanceOf(TextView::class.java),
        withParent(withId(R.id.toolbar))
      )
    ).check(matches(withText(containsString("Top Trending Gifs"))))

    release()
  }


  @Test
  fun testTrendingResultsThenSearchThenBackToTrending() {
    onView(withId(R.id.menuSearch))
      .perform(click())

    onView(withId(androidx.appcompat.R.id.search_src_text))
      .perform(click(), typeText("hello"), closeSoftKeyboard(), pressBack())

    onView(withId(R.id.recyclerView))
      .perform(pressBack())
  }
}
