package com.example.smartmarathonrunningapp_project;

import android.util.Log;
import java.util.Locale;

public class AutoAdjuster {
    private static final String TAG = "AutoAdjuster";
    private static final float PACE_TOLERANCE = 0.05f;
    private static final float DISTANCE_TOLERANCE = 0.15f;
    private static final float HR_THRESHOLD = 150f;
    private static final float MAX_DISTANCE = 26.2f;


    public TrainingPlan adjustPlan(TrainingPlan plan, Activity latestRun) {
        if (!"Run".equals(latestRun.getType())) return plan;

        try {
            TrainingPlan.TrainingWeek currentWeek = plan.getTraining_weeks().get(0);
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
        adjustDay(days.getWednesday(), latestRun);
        adjustDay(days.getThursday(), latestRun);
        adjustDay(days.getFriday(), latestRun);
        adjustDay(days.getSaturday(), latestRun);
        adjustDay(days.getSunday(), latestRun);
    }

    private void adjustDay(TrainingPlan.Day day, Activity latestRun) {
        if (day == null || day.getPace() == null) return;

        try {
            // Reset adjustment note
            day.setAdjustmentNote(null);

            String workoutType = classifyWorkout(day.getExercise());
            float actualPace = latestRun.getPaceInSeconds();
            float actualDistance = latestRun.getDistance();

            // Apply sensible bounds checking
            actualDistance = Math.min(actualDistance, MAX_DISTANCE * 1609.34f); // Convert miles to meters

            switch (workoutType) {
                case "RECOVERY":
                    adjustRecoveryDay(day, actualPace, actualDistance);
                    break;
                case "INTERVAL":
                    adjustIntervalDay(day, actualPace, actualDistance);
                    break;
                case "LONG_RUN":
                    adjustLongRunDay(day, actualPace, actualDistance);
                    break;
                default:
                    adjustGeneralDay(day, actualPace, actualDistance);
            }

            // Apply HR-based recovery if needed (only once)
            if (latestRun.getAverage_heartrate() > HR_THRESHOLD) {
                reduceDayIntensity(day, 0.8f);
                appendAdjustmentNote(day, "Reduced intensity after high HR effort");
            }

            // Final sanity checks
            enforceSaneValues(day);
        } catch (Exception e) {
            Log.e(TAG, "Day adjustment failed", e);
        }
    }

    private void appendAdjustmentNote(TrainingPlan.Day day, String note) {
        if (day.getAdjustmentNote() == null) {
            day.setAdjustmentNote(note);
        } else if (!day.getAdjustmentNote().contains(note)) {
            day.setAdjustmentNote(day.getAdjustmentNote() + "; " + note);
        }
    }
    private void enforceSaneValues(TrainingPlan.Day day) {
        try {
            if (day.getDistance() != null && day.getDistance().contains("mi")) {
                float dist = parseDistance(day.getDistance());
                dist = Math.max(0.5f, Math.min(dist, MAX_DISTANCE)); // Keep between 0.5mi and marathon distance
                day.setDistance(String.format(Locale.US, "%.1f mi", dist));
            }
        } catch (Exception e) {
            Log.e(TAG, "Sanity check failed", e);
            day.setDistance("5.0 mi"); // Fallback value
        }
    }

    private String classifyWorkout(String exercise) {
        if (exercise == null) return "GENERAL";
        String lcExercise = exercise.toLowerCase();

        if (lcExercise.contains("recovery")) return "RECOVERY";
        if (lcExercise.contains("speed") || lcExercise.contains("strides") ||
                lcExercise.contains("interval")) return "INTERVAL";
        if (lcExercise.contains("marathon") || lcExercise.contains("long run"))
            return "LONG_RUN";
        return "GENERAL";
    }

    private void adjustRecoveryDay(TrainingPlan.Day day, float actualPace, float actualDistance) {
        // More conservative adjustments
        adjustPace(day, actualPace, 0.2f);
        adjustDistance(day, actualDistance, 0.3f);
        appendAdjustmentNote(day, "Recovery day adjustments");
    }

    private void adjustIntervalDay(TrainingPlan.Day day, float actualPace, float actualDistance) {
        // Maintain interval structure
        if (day.getPace().contains("-")) {
            String[] paceParts = day.getPace().split(" - ");
            float basePace = parsePace(paceParts[0]);
            float adjustedBase = calculateAdjustedPace(basePace, actualPace, 0.3f);
            float range = parsePace(paceParts[0]) - parsePace(paceParts[1]);
            day.setPace(formatPace(adjustedBase) + " - " + formatPace(adjustedBase - range));
        }
        adjustDistance(day, actualDistance, 0.1f);
        appendAdjustmentNote(day, "Interval workout adjustments");
    }

    private void adjustLongRunDay(TrainingPlan.Day day, float actualPace, float actualDistance) {
        if (day.getExercise().toLowerCase().contains("marathon-pace")) {
            String[] parts = day.getDistance().split(" w/ ");
            float totalDist = Math.min(parseDistance(parts[0]), MAX_DISTANCE);
            float mpDist = parts.length > 1 ?
                    Math.min(parseDistance(parts[1].split(" @")[0]), totalDist) : totalDist * 0.6f;

            float newTotal = Math.max(8.0f, totalDist * 0.9f); // Never below 8 miles
            float newMp = Math.min(mpDist * 0.9f, newTotal * 0.7f); // MP portion <= 70% of total
            day.setDistance(String.format(Locale.US, "%.1f mi w/ %.1f @ marathon race pace", newTotal, newMp));
        } else {
            adjustPace(day, actualPace, 0.3f);
            adjustDistance(day, actualDistance, 0.2f);
        }
        appendAdjustmentNote(day, "Long run adjustments");
    }

    private void adjustGeneralDay(TrainingPlan.Day day, float actualPace, float actualDistance) {
        adjustPace(day, actualPace, 0.3f);
        adjustDistance(day, actualDistance, 0.15f);
        appendAdjustmentNote(day, "General workout adjustments");
    }

    private void adjustPace(TrainingPlan.Day day, float actualPace, float adjustmentFactor) {
        float targetPace = parsePace(day.getPace().split(" - ")[0]);
        float paceDiff = Math.abs(actualPace - targetPace) / targetPace;

        if (paceDiff > PACE_TOLERANCE) {
            float newPace = targetPace + (actualPace - targetPace) * adjustmentFactor;
            day.setPace(formatPace(newPace));
        }
    }

    private void adjustDistance(TrainingPlan.Day day, float actualDistance, float adjustmentFactor) {
        if (day.getDistance() != null && day.getDistance().contains("mi")) {
            float targetDist = parseDistance(day.getDistance());
            float actualDist = actualDistance / 1609.34f;
            float distRatio = actualDist / targetDist;

            if (Math.abs(1 - distRatio) > DISTANCE_TOLERANCE) {
                float newDist = targetDist * (0.9f + adjustmentFactor);
                day.setDistance(String.format(Locale.US, "%.1f mi", newDist));
            }
        }
    }

    private void reduceDayIntensity(TrainingPlan.Day day, float factor) {
        try {
            if (day.getDistance() != null && day.getDistance().contains("mi")) {
                float dist = parseDistance(day.getDistance());
                day.setDistance(String.format(Locale.US, "%.1f mi", dist * factor));
            }
            if (day.getPace() != null) {
                float pace = parsePace(day.getPace().split(" - ")[0]);
                day.setPace(formatPace(pace * 1.05f));
            }
        } catch (Exception e) {
            Log.e(TAG, "Intensity reduction failed", e);
        }
    }

    private float parsePace(String paceStr) {
        try {
            String[] parts = paceStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse pace: " + paceStr, e);
            return 8 * 60; // Default to 8:00/mile
        }
    }

    private float parseDistance(String distanceStr) {
        try {
            return Float.parseFloat(distanceStr.split(" ")[0]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse distance: " + distanceStr, e);
            return 5.0f; // Default to 5 miles
        }
    }

    private String formatPace(float seconds) {
        int minutes = (int)(seconds / 60);
        int secs = (int)(seconds % 60);
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }

    private float calculateAdjustedPace(float targetPace, float actualPace, float factor) {
        return targetPace + (actualPace - targetPace) * factor;
    }
}