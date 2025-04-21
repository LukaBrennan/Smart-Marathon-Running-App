package com.example.smartmarathonrunningapp_project;

import android.util.Log;
import java.util.Locale;

public class AutoAdjuster {
    private static final String TAG = "AutoAdjuster";
    private static final float PACE_TOLERANCE = 0.05f; // 5% threshold
    private static final float DISTANCE_TOLERANCE = 0.15f; // 15% threshold
    private static final float HR_THRESHOLD = 150f; // bpm

    public TrainingPlan adjustPlan(TrainingPlan plan, Activity latestRun) {
        if (!"Run".equals(latestRun.getType())) return plan;

        try {
            TrainingPlan.TrainingWeek currentWeek = plan.getTraining_weeks().get(0); // Adjust upcoming week
            adjustWeek(currentWeek, latestRun);
        } catch (Exception e) {
            Log.e(TAG, "Adjustment failed", e);
        }
        return plan;
    }

    private void adjustWeek(TrainingPlan.TrainingWeek week, Activity latestRun) {
        if (week == null || week.getTraining_plan() == null) return;

        TrainingPlan.Days days = week.getTraining_plan();
        adjustDay(days.getMonday(), latestRun);
        adjustDay(days.getTuesday(), latestRun);
        adjustDay(days.getWednesday(), latestRun); // Fixed spelling
        adjustDay(days.getThursday(), latestRun);
        adjustDay(days.getFriday(), latestRun);
        adjustDay(days.getSaturday(), latestRun); // Fixed capitalization
        adjustDay(days.getSunday(), latestRun);
    }

    private void adjustDay(TrainingPlan.Day day, Activity latestRun) {
        if (day == null || day.getPace() == null) return;

        try {
            // Pace adjustment
            float targetPace = parsePace(day.getPace());
            float actualPace = latestRun.getPaceInSeconds();
            float paceDiff = Math.abs(actualPace - targetPace) / targetPace;

            if (paceDiff > PACE_TOLERANCE) {
                float newPace = targetPace * (1 + (actualPace - targetPace)/targetPace * 0.5f);
                day.setPace(formatPace(newPace));
            }

            // Distance adjustment
            if (day.getDistance() != null && day.getDistance().contains("mi")) {
                float targetDist = Float.parseFloat(day.getDistance().split(" ")[0]);
                float actualDist = latestRun.getDistance() / 1609.34f; // meters to miles
                float distRatio = actualDist / targetDist;

                if (Math.abs(1 - distRatio) > DISTANCE_TOLERANCE) {
                    float newDist = targetDist * (0.9f + 0.1f * distRatio); // Conservative adjustment
                    day.setDistance(String.format(Locale.US, "%.1f mi", newDist));
                }
            }

            // Recovery check
            if (latestRun.getAverage_heartrate() > HR_THRESHOLD) {
                reduceDayIntensity(day, 0.8f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Day adjustment failed", e);
        }
    }

    private void reduceDayIntensity(TrainingPlan.Day day, float factor) {
        try {
            if (day.getDistance() != null && day.getDistance().contains("mi")) {
                float dist = Float.parseFloat(day.getDistance().split(" ")[0]);
                day.setDistance(String.format(Locale.US, "%.1f mi", dist * factor));
            }
            if (day.getPace() != null) {
                float pace = parsePace(day.getPace());
                day.setPace(formatPace(pace * 1.05f)); // 5% slower
            }
        } catch (Exception e) {
            Log.e(TAG, "Intensity reduction failed", e);
        }
    }

    private float parsePace(String paceStr) {
        try {
            String[] parts = paceStr.split("[: -]")[0].split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse pace: " + paceStr, e);
            return 8 * 60; // Default to 8:00/mile if parsing fails
        }
    }

    private String formatPace(float seconds) {
        int minutes = (int)(seconds / 60);
        int secs = (int)(seconds % 60);
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }
}