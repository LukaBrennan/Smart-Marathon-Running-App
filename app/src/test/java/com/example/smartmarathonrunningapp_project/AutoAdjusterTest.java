package com.example.smartmarathonrunningapp_project;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import android.util.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RunWith(MockitoJUnitRunner.class)
public class AutoAdjusterTest {

    private AutoAdjuster autoAdjuster;
    private MockedStatic<Log> mockedLog;

    @Before
    public void setUp() {
        mockedLog = mockStatic(Log.class);
        autoAdjuster = new AutoAdjuster();
    }

    @Test
    public void testAdjustPlan_RecoveryWeek() {
        mockedLog.when(() -> Log.w(anyString(), anyString())).thenReturn(0);

        TrainingPlan plan = createTestPlan();
        List<Activity> highLoadActivities = createHighLoadActivities();

        TrainingPlan adjustedPlan = autoAdjuster.adjustPlan(plan, highLoadActivities, new HashMap<>());

        System.out.println("Adjustment Note: " + adjustedPlan.getAdjustmentNote());

        assertNotNull(adjustedPlan);
        assertTrue(adjustedPlan.getAdjustmentNote().contains("Recovery week activated"));
    }


    @Test
    public void testAdjustPlan_NormalLoad() {
        TrainingPlan plan = createTestPlan();
        List<Activity> moderateLoadActivities = createModerateLoadActivities();

        TrainingPlan adjustedPlan = autoAdjuster.adjustPlan(plan, moderateLoadActivities, new HashMap<>());

        assertNotNull(adjustedPlan);
        assertFalse(adjustedPlan.getAdjustmentNote().contains("Recovery"));
    }

    @Test
    public void testAdjustPlan_WithTrafficLights() {
        TrainingPlan plan = createTestPlan();
        List<Activity> runs = createModerateLoadActivities();

        TrainingPlan.Day originalTuesday = plan.getTraining_weeks().get(0).getTraining_plan().getTuesday();

        Map<String, String> lights = new HashMap<>();
        lights.put("Tuesday", "YELLOW");

        TrainingPlan adjustedPlan = autoAdjuster.adjustPlan(plan, runs, lights);
        TrainingPlan.Day updatedTuesday = adjustedPlan.getTraining_weeks().get(0).getTraining_plan().getTuesday();

        System.out.println("Adjusted Tuesday pace: " + updatedTuesday.getPace());

        assertNotEquals("8:00", updatedTuesday.getPace());
    }


    @Test
    public void testAdjustPlan_DayWithoutPace() {
        TrainingPlan plan = createTestPlan();
        plan.getTraining_weeks().get(0).getTraining_plan().getTuesday().setPace(null); // No pace

        List<Activity> runs = createModerateLoadActivities();
        Map<String, String> lights = new HashMap<>();
        lights.put("Tuesday", "YELLOW");

        try {
            TrainingPlan adjustedPlan = autoAdjuster.adjustPlan(plan, runs, lights);
            assertNotNull(adjustedPlan); // No crash
        } catch (Exception e) {
            fail("Should not throw exception with null pace: " + e.getMessage());
        }
    }


    @Test
    public void testAdjustPlan_EmptyInputs() {
        TrainingPlan plan = new TrainingPlan();  // empty plan
        TrainingPlan result = autoAdjuster.adjustPlan(plan, new ArrayList<>(), new HashMap<>());
        assertNotNull(result);
    }

    @Test
    public void testAdjustPlan_UnknownTrafficLightColor() {
        TrainingPlan plan = createTestPlan();
        List<Activity> runs = createModerateLoadActivities();

        Map<String, String> lights = new HashMap<>();
        lights.put("Tuesday", "BLUE");

        TrainingPlan adjustedPlan = autoAdjuster.adjustPlan(plan, runs, lights);
        String pace = adjustedPlan.getTraining_weeks().get(0).getTraining_plan().getTuesday().getPace();

        assertTrue(pace.contains("8:00"));
    }

    private List<Activity> createModerateLoadActivities() {
        List<Activity> activities = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String date = java.time.LocalDate.now().minusDays(i * 2).toString(); // Spread out
            activities.add(createActivity(10000, 3600, 140, 160, date)); // Moderate run
        }
        return activities;
    }


    @After
    public void tearDown() {
        mockedLog.close();
    }

    private TrainingPlan createTestPlan() {
        TrainingPlan plan = new TrainingPlan();
        List<TrainingPlan.TrainingWeek> weeks = new ArrayList<>();

        // Week 11
        TrainingPlan.TrainingWeek week = new TrainingPlan.TrainingWeek();
        week.setWeek("11");

        TrainingPlan.Days days = new TrainingPlan.Days();
        days.setMonday(createDay("Rest", "0 mi", null));
        days.setTuesday(createDay("General aerobic", "5 mi", "8:00"));
        days.setWednesday(createDay("Rest", "0 mi", null));
        days.setThursday(createDay("General aerobic", "5 mi", "8:00"));
        days.setFriday(createDay("Rest", "0 mi", null));
        days.setSaturday(createDay("Recovery", "5 mi", "8:00"));
        days.setSunday(createDay("Long run", "10 mi", "7:00"));

        week.setTraining_plan(days);
        weeks.add(week);
        plan.setTraining_weeks(weeks);

        return plan;
    }

    private TrainingPlan.Day createDay(String exercise, String distance, String pace) {
        TrainingPlan.Day day = new TrainingPlan.Day();
        day.setExercise(exercise);
        day.setDistance(distance);
        day.setPace(pace);
        return day;
    }

    private List<Activity> createHighLoadActivities() {
        List<Activity> activities = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String date = java.time.LocalDate.now().minusDays(i).toString(); // Last 7 days
            activities.add(createActivity(15000, 5400, 160, 180, date)); // Hard run
        }
        return activities;
    }


    private Activity createActivity(float distance, int movingTime, float avgHR, float maxHR, String startDate) {
        Activity activity = new Activity();
        activity.setDistance(distance);
        activity.setMoving_time(movingTime);
        activity.setAverage_heartrate(avgHR);
        activity.setMax_heartrate(maxHR);
        activity.setType("Run");
        activity.setStart_date(startDate);
        return activity;
    }
}