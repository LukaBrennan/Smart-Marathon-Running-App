package com.example.smartmarathonrunningapp_project;

import android.util.Log;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AutoAdjuster {
    private static final String TAG = "AutoAdjuster";
    private static final float HIGH_ACUTE_TRIMP_THRESHOLD = 350f;
    private static final float FATIGUE_RATIO_THRESHOLD = 1.15f;
    private static final int MIN_RUNS_FOR_ACCURACY = 5;
    private static final int DAYS_FOR_ACUTE_LOAD = 7;
    private static final int DAYS_FOR_CHRONIC_LOAD = 28;

    public TrainingPlan adjustPlan(TrainingPlan currentPlan, List<Activity> recentRuns, Map<TrainingPlan.Day, String> trafficLightStatuses) {
        if (recentRuns == null || recentRuns.isEmpty()) {
            return currentPlan;
        }

        List<Activity> filteredRuns = filterRecentRuns(recentRuns, DAYS_FOR_ACUTE_LOAD + DAYS_FOR_CHRONIC_LOAD);
        float acuteTRIMP = calculateTRIMPForPeriod(filteredRuns, DAYS_FOR_ACUTE_LOAD);
        float chronicTRIMP = calculateTRIMPForPeriod(filteredRuns, DAYS_FOR_CHRONIC_LOAD);
        float fatigueRatio = acuteTRIMP / chronicTRIMP;

        TrainingPlan adjustedPlan;
        if (filteredRuns.size() < MIN_RUNS_FOR_ACCURACY) {
            Log.w(TAG, "Insufficient data - falling back to basic adjustment");
            adjustedPlan = basicAdjustment(currentPlan, recentRuns, trafficLightStatuses);
        } else {
            adjustedPlan = deepCopyPlan(currentPlan);
            boolean needsRecovery = fatigueRatio > FATIGUE_RATIO_THRESHOLD || acuteTRIMP > HIGH_ACUTE_TRIMP_THRESHOLD;

            if (needsRecovery) {
                String note = String.format(Locale.US,
                        "Recovery week: Acute TRIMP=%.1f, Chronic TRIMP=%.1f, Ratio=%.2f",
                        acuteTRIMP, chronicTRIMP, fatigueRatio);
                adjustedPlan.setAdjustmentNote(note);
            }

            for (TrainingPlan.TrainingWeek week : adjustedPlan.getTraining_weeks()) {
                for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                    if (trafficLightStatuses.containsKey(day)) {
                        String status = trafficLightStatuses.get(day);
                        adjustDayIntensity(day, needsRecovery ? "RED" : status);
                    }
                }
            }
        }
        return adjustedPlan;
    }

    private float calculateTRIMPForPeriod(List<Activity> runs, int days) {
        List<Activity> periodRuns = filterRecentRuns(runs, days);
        return periodRuns.stream()
                .map(r -> TRIMP.calculate(r.getMoving_time()/60f, r.getAverage_heartrate(),
                        r.getResting_heartrate(), r.getMax_heartrate(), r.isMale()))
                .reduce(0f, Float::sum);
    }

    private List<Activity> filterRecentRuns(List<Activity> runs, int maxDays) {
        long cutoff = System.currentTimeMillis() - (maxDays * 24 * 60 * 60 * 1000L);
        return runs.stream()
                .filter(run -> run.getStart_date() != null &&
                        DateUtils.parseDate(run.getStart_date()).getTime() > cutoff)
                .collect(Collectors.toList());
    }

    private TrainingPlan deepCopyPlan(TrainingPlan plan) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(plan), TrainingPlan.class);
    }

    private TrainingPlan basicAdjustment(TrainingPlan currentPlan, List<Activity> recentRuns,
                                         Map<TrainingPlan.Day, String> trafficLightStatuses) {
        TrainingPlan adjustedPlan = deepCopyPlan(currentPlan);
        for (TrainingPlan.TrainingWeek week : adjustedPlan.getTraining_weeks()) {
            for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                if (trafficLightStatuses.containsKey(day)) {
                    adjustDayIntensity(day, trafficLightStatuses.get(day));
                }
            }
        }
        adjustedPlan.setAdjustmentNote("Basic adjustment applied");
        return adjustedPlan;
    }

    private float parseDistance(String distanceStr) {
        try {
            // Handle empty or null strings
            if (distanceStr == null || distanceStr.trim().isEmpty()) {
                return 0f;
            }

            // Remove "mi" if present
            distanceStr = distanceStr.replace("mi", "").trim();

            // Check if it's a range (contains "-")
            if (distanceStr.contains("-")) {
                String[] parts = distanceStr.split("-");
                // Take the first value in the range (or average if preferred)
                return Float.parseFloat(parts[0].trim());
            }

            // Check if it's a complex description (e.g., "13 mi w/ 8 @ marathon race pace")
            if (distanceStr.contains("w/")) {
                String[] parts = distanceStr.split("w/");
                return Float.parseFloat(parts[0].trim());
            }

            // Default case - simple number
            return Float.parseFloat(distanceStr.split(" ")[0]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse distance: " + distanceStr, e);
            return 5.0f; // Default fallback value
        }
    }

    private void adjustDayIntensity(TrainingPlan.Day day, String trafficLightStatus) {
        if (day == null || trafficLightStatus == null || trafficLightStatus.equals("N/A")) {
            return;
        }

        try {
            if (day.getExercise().toLowerCase().contains("tune-up") ||
                    day.getExercise().toLowerCase().contains("race")) {
                return;
            }

            // Adjust distance
            if (day.getDistance() != null && day.getDistance().contains("mi")) {
                float dist = parseDistance(day.getDistance());
                float adjustmentFactor = getDistanceAdjustmentFactor(trafficLightStatus);
                dist *= adjustmentFactor;
                day.setDistance(String.format(Locale.US, "%.1f mi", dist));
            }

            // Adjust pace
            if (day.getPace() != null) {
                float[] currentPaceRange = parsePaceRange(day.getPace());
                float adjustmentSecPerKm = getPaceAdjustment(trafficLightStatus);

                float newMinPace = currentPaceRange[0] + adjustmentSecPerKm;
                float newMaxPace = currentPaceRange[1] + adjustmentSecPerKm;

                day.setPace(formatPace(newMinPace) + " - " + formatPace(newMaxPace));
            }
        } catch (Exception e) {
            Log.e(TAG, "Adjustment failed", e);
        }
    }

    private float getDistanceAdjustmentFactor(String status) {
        switch (status) {
            case "GREEN": return 1.0f;
            case "YELLOW": return 0.9f;
            case "RED": return 0.8f;
            default: return 1.0f;
        }
    }

    private float getPaceAdjustment(String trafficLightStatus) {
        switch (trafficLightStatus) {
            case "GREEN": return -5f;
            case "YELLOW": return +5f;
            case "RED": return +10f;
            default: return 0f;
        }
    }

    private float[] parsePaceRange(String paceStr) throws Exception {
        String[] parts = paceStr.split(" - ");
        return new float[] {
                parsePace(parts[0]),
                parts.length > 1 ? parsePace(parts[1]) : parsePace(parts[0])
        };
    }

    private float parsePace(String paceStr) {
        try {
            String[] parts = paceStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse pace: " + paceStr, e);
            return 8 * 60;
        }
    }

    private String formatPace(float seconds) {
        int minutes = (int)(seconds / 60);
        int secs = (int)(seconds % 60);
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }

    private List<TrainingPlan.Day> getDaysOfWeek(TrainingPlan.TrainingWeek week) {
        return Arrays.asList(
                week.getTraining_plan().getMonday(),
                week.getTraining_plan().getTuesday(),
                week.getTraining_plan().getWednesday(),
                week.getTraining_plan().getThursday(),
                week.getTraining_plan().getFriday(),
                week.getTraining_plan().getSaturday(),
                week.getTraining_plan().getSunday()
        );
    }
}
