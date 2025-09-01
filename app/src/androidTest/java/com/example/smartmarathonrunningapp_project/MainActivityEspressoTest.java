//package com.example.smartmarathonrunningapp_project;
//
//import android.view.View;
//import android.graphics.drawable.ColorDrawable;
//import android.graphics.drawable.Drawable;
//
//import androidx.annotation.ColorRes;
//import androidx.core.content.ContextCompat;
//import androidx.test.ext.junit.rules.ActivityScenarioRule;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
//
//import org.hamcrest.Description;
//import org.hamcrest.Matcher;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import static androidx.test.espresso.Espresso.onView;
//import static androidx.test.espresso.action.ViewActions.click;
//import static androidx.test.espresso.action.ViewActions.scrollTo;
//import static androidx.test.espresso.matcher.ViewMatchers.withId;
//import static androidx.test.espresso.assertion.ViewAssertions.matches;
//
//import androidx.test.espresso.matcher.BoundedMatcher;
//
//@RunWith(AndroidJUnit4.class)
//public class MainActivityEspressoTest {
//
//    @Rule
//    public ActivityScenarioRule<MainActivity> activityRule =
//            new ActivityScenarioRule<>(MainActivity.class);
//
//    @Test
//    public void testGreenTrafficLightDisplayed() {
//        onView(withId(R.id.btnTestGreen)).perform(scrollTo(), click());
//        onView(withId(R.id.trafficLightView)).check(matches(hasBackgroundColor(R.color.traffic_light_green)));
//    }
//
//    @Test
//    public void testYellowTrafficLightDisplayed() {
//        onView(withId(R.id.btnTestYellow)).perform(scrollTo(), click());
//        onView(withId(R.id.trafficLightView)).check(matches(hasBackgroundColor(R.color.traffic_light_yellow)));
//    }
//
//    @Test
//    public void testRedTrafficLightDisplayed() {
//        onView(withId(R.id.btnTestRed)).perform(scrollTo(), click());
//        onView(withId(R.id.trafficLightView)).check(matches(hasBackgroundColor(R.color.traffic_light_red)));
//    }
//
//    // Custom matcher to check view background color
//    public static Matcher<View> hasBackgroundColor(@ColorRes int expectedColorRes) {
//        return new BoundedMatcher<View, View>(View.class) {
//            @Override
//            protected boolean matchesSafely(View view) {
//                Drawable background = view.getBackground();
//                if (background instanceof ColorDrawable) {
//                    int expectedColor = ContextCompat.getColor(view.getContext(), expectedColorRes);
//                    int actualColor = ((ColorDrawable) background).getColor();
//                    return actualColor == expectedColor;
//                }
//                return false;
//            }
//
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("has background color matching resolved color resource.");
//            }
//        };
//    }
//
//}
