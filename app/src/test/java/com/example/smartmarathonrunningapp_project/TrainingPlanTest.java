package com.example.smartmarathonrunningapp_project;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
//  Redundant test cases
public class TrainingPlanTest {
    @Test
    public void testTrafficLight_Green() {
        TrainingPlan.Day plannedDay = new TrainingPlan.Day();
        plannedDay.setDistance("5 mi");
        plannedDay.setPace("8:00");

        Activity activity = new Activity();
        activity.setDistance(5 * 1609.34f); // 5 miles in meters
        activity.setMoving_time(40 * 60); // 8:00 min/mile pace

        assertEquals("GREEN", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }

    @Test
    public void testTrafficLight_RedDistance() {
        TrainingPlan.Day plannedDay = new TrainingPlan.Day();
        plannedDay.setDistance("5 mi");
        plannedDay.setPace("8:00");

        Activity activity = new Activity();
        activity.setDistance(2 * 1609.34f); // Only ran 2 miles
        activity.setMoving_time(16 * 60); // Correct pace

        assertEquals("RED", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }

    @Test
    public void testTrafficLight_YellowPace() {
        TrainingPlan.Day plannedDay = new TrainingPlan.Day();
        plannedDay.setDistance("5 mi");
        plannedDay.setPace("8:00 - 7:00");

        Activity activity = new Activity();
        activity.setDistance(5 * 1609.34f);
        activity.setMoving_time(35 * 60); // 7:00 min/mile (15% faster than max)

        assertEquals("YELLOW", TrainingPlan.getTrafficLightStatus(plannedDay, activity));
    }
}