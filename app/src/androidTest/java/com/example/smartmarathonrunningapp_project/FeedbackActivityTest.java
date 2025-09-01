//package com.example.smartmarathonrunningapp_project;
//
//import android.graphics.drawable.ColorDrawable;
//import android.graphics.drawable.Drawable;
//import android.view.View;
//
//import androidx.annotation.ColorRes;
//import androidx.core.content.ContextCompat;
//import androidx.test.espresso.matcher.BoundedMatcher;
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
//import static androidx.test.espresso.matcher.ViewMatchers.withId;
//import static androidx.test.espresso.assertion.ViewAssertions.matches;
//
//@RunWith(AndroidJUnit4.class)
//public class FeedbackActivityTest {
//
//    @Rule
//    public ActivityScenarioRule<MainActivity> activityRule =
//            new ActivityScenarioRule<>(MainActivity.class); // or FeedbackActivity.class
//
//    @Test
//    public void testTrafficLightColorGreenIsDisplayed() {
//        onView(withId(R.id.trafficLightView))
//                .check(matches(hasColor(R.color.traffic_light_green)));
//    }
//
//    public static Matcher<View> hasColor(@ColorRes int colorRes) {
//        return new BoundedMatcher<View, View>(View.class) {
//            @Override
//            protected boolean matchesSafely(View view) {
//                int expectedColor = ContextCompat.getColor(view.getContext(), colorRes);
//                Drawable background = view.getBackground();
//                if (background instanceof ColorDrawable) {
//                    int actualColor = ((ColorDrawable) background).getColor();
//                    return actualColor == expectedColor;
//                }
//                return false;
//            }
//
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("has background color: " + colorRes);
//            }
//        };
//    }
//}
//
