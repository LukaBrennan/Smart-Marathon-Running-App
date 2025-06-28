package com.example.smartmarathonrunningapp_project;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrafficLightTest {

    @Test
    public void testGetTrafficLightStatus_Green() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", "8:00");
        Activity activity = createActivity(8046.72f, 2400); // 5mi in 40:00 (8:00/mi)

        String status = TrainingPlan.getTrafficLightStatus(plannedDay, activity);
        assertEquals("GREEN", status);
    }

    @Test
    public void testGetTrafficLightStatus_Yellow_Pace() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", "8:00");
        Activity activity = createActivity(8046.72f, 2250); // 5 mi in 37:30 (7:30/mi)

        String status = TrainingPlan.getTrafficLightStatus(plannedDay, activity);
        assertEquals("GREEN", status);
    }


    @Test
    public void testGetTrafficLightStatus_Red_Distance() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", "8:00");
        Activity activity = createActivity(4828.03f, 2400); // 3mi in 40:00

        String status = TrainingPlan.getTrafficLightStatus(plannedDay, activity);
        assertEquals("RED", status);
    }

    @Test
    public void testTrafficLight_NullDistance() {
        TrainingPlan.Day plannedDay = new TrainingPlan.Day();
        plannedDay.setDistance(null);
        plannedDay.setPace("8:00");

        Activity activity = new Activity();
        activity.setDistance(8046.72f);
        activity.setMoving_time(2400);

        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }

    @Test
    public void testTrafficLight_GreenAtUpperTolerance() {
        TrainingPlan.Day plannedDay = new TrainingPlan.Day();
        plannedDay.setDistance("5 mi");
        plannedDay.setPace("8:00");

        // 10% over planned distance = still GREEN
        Activity activity = new Activity();
        activity.setDistance(8046.72f * 1.05f); // 10% more
        activity.setMoving_time(2400); // exact pace

        assertEquals("GREEN", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }


    @Test
    public void testTrafficLight_NullActivity() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", "8:00");
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, null));
    }


    @Test
    public void testTrafficLight_NullPace() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", null);
        Activity activity = createActivity(8046.72f, 2400);
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }


    @Test
    public void testTrafficLight_BadPaceFormat() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", "fast");
        Activity activity = createActivity(8046.72f, 2400);
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }


    @Test
    public void testTrafficLight_ZeroTime() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", "8:00");
        Activity activity = createActivity(8046.72f, 0);
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }


    @Test
    public void testTrafficLight_GreenAtLowerTolerance() {
        TrainingPlan.Day plannedDay = new TrainingPlan.Day();
        plannedDay.setDistance("5 mi");
        plannedDay.setPace("8:00");
        float actualDistance = 7242.05f;
        float actualMiles = actualDistance / 1609.34f;
        int movingTime = Math.round(actualMiles * 480); // ≈ 2160 seconds

        Activity activity = new Activity();
        activity.setDistance(actualDistance);
        activity.setMoving_time(movingTime);

        String status = TrainingPlan.getTrafficLightStatus(plannedDay, activity);
        System.out.println("Actual pace: " + (movingTime / actualMiles) + " sec/mi");
        System.out.println("Actual distance: " + actualDistance + ", Expected GREEN but got: " + status);
        assertEquals("GREEN", status);
    }


    @Test
    public void testTrafficLight_NullPlannedDay() {
        Activity activity = createActivity(8046.72f, 2400); // Valid activity
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(null, activity));
    }

    @Test
    public void testTrafficLight_NullDistanceInPlan() {
        TrainingPlan.Day plannedDay = createPlannedDay(null, "8:00");
        Activity activity = createActivity(8046.72f, 2400);
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }


    @Test
    public void testTrafficLight_InvalidDistanceFormat() {
        TrainingPlan.Day plannedDay = createPlannedDay("five miles", "8:00"); // Bad format
        Activity activity = createActivity(8046.72f, 2400);
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }


    @Test
    public void testTrafficLight_ZeroDistance() {
        TrainingPlan.Day plannedDay = createPlannedDay("5 mi", "8:00");
        Activity activity = createActivity(0f, 2400); // Distance is zero
        assertEquals("N/A", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }

    private TrainingPlan.Day createPlannedDay(String distance, String pace) {
        TrainingPlan.Day day = new TrainingPlan.Day();
        day.setDistance(distance);
        day.setPace(pace);
        return day;
    }

    private Activity createActivity(float distanceMeters, int movingTimeSec) {
        Activity activity = new Activity();
        activity.setDistance(distanceMeters);
        activity.setMoving_time(movingTimeSec);
        activity.setAverage_heartrate(150);
        activity.setMax_heartrate(180);
        return activity;
    }



}