package com.example.smartmarathonrunningapp_project;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoAdjuster {
    private static final String TAG = "AutoAdjuster";
    private static final float PACE_TOLERANCE = 0.05f;
    private static final float DISTANCE_TOLERANCE = 0.15f;
    private static final float HR_THRESHOLD = 150f;
    private static final float MAX_DISTANCE = 26.2f;

    private static final float HIGH_TRIMP_THRESHOLD = 350f; // Weekly TRIMP threshold
    private static final float FATIGUE_RATIO_THRESHOLD = 1.15f;

    // Entry point — now supports "structured" or "free" mode
    public TrainingPlan adjustPlan(TrainingPlan plan, List<Activity> recentRuns) {
        if (recentRuns == null || recentRuns.isEmpty()) return plan;

        // Calculate weekly TRIMP load
        float weeklyTRIMP = calculateWeeklyTRIMP(recentRuns);

        // Calculate fatigue ratio
        float fatigueRatio = calculateFatigueRatio(recentRuns);

        // Estimate VO2Max from best recent run
        float vo2max = estimateCurrentFitness(recentRuns);

        // Apply adjustments based on metrics
        if (fatigueRatio > FATIGUE_RATIO_THRESHOLD || weeklyTRIMP > HIGH_TRIMP_THRESHOLD) {
            reduceWeekIntensity(plan, 0.85f);
            plan.setAdjustmentNote("Recovery week: High fatigue (TRIMP=" + weeklyTRIMP + ")");
        }

        return plan;
    }

    private float calculateWeeklyTRIMP(List<Activity> runs) {
        return runs.stream()
                .map(r -> TRIMP.calculate(
                        r.getMoving_time()/60f,
                        r.getAverage_heartrate(),
                        r.getResting_heartrate(),
                        r.getMax_heartrate(),
                        r.isMale()
                ))
                .reduce(0f, Float::sum);
    }


    private float calculateFatigueRatio(List<Activity> runs) {
        if (runs.size() < 3) return 1.0f;

        Activity lastRun = runs.get(0);
        float avgPace = runs.stream()
                .limit(3)
                .map(Activity::getPaceInSeconds)
                .reduce(0f, Float::sum) / 3;

        float avgHR = runs.stream()
                .limit(3)
                .map(Activity::getAverage_heartrate)
                .reduce(0f, Float::sum) / 3;

        return (lastRun.getPaceInSeconds() / avgPace) * 0.7f +
                (lastRun.getAverage_heartrate() / avgHR) * 0.3f;
    }


    private float estimateCurrentFitness(List<Activity> runs) {
        Activity bestRun = runs.stream()
                .min((a,b) -> Float.compare(a.getPaceInSeconds(), b.getPaceInSeconds()))
                .orElse(null);

        return bestRun != null ?
                VO2MaxEstimator.fromRacePerformance(bestRun.getDistance(), bestRun.getMoving_time()) :
                0;
    }


    // STRUCTURED mode — adjust just today's matching plan day
    private void adjustNextScheduledDay(TrainingPlan plan, Activity latestRun) {
        TrainingPlan.TrainingWeek currentWeek = plan.getTraining_weeks().get(0);
        if (currentWeek == null || currentWeek.getTraining_plan() == null) return;

        TrainingPlan.Days days = currentWeek.getTraining_plan();
        String today = new SimpleDateFormat("EEEE", Locale.US).format(new Date()).toLowerCase();
        TrainingPlan.Day todayDay = getDayByName(days, today);
        if (todayDay != null) adjustDay(todayDay, latestRun);
    }

    // FREE mode — adjust all days in the current week
    private void adjustAllDays(TrainingPlan plan, Activity latestRun) {
        TrainingPlan.TrainingWeek currentWeek = plan.getTraining_weeks().get(0);
        if (currentWeek == null || currentWeek.getTraining_plan() == null) return;

        TrainingPlan.Days days = currentWeek.getTraining_plan();
        adjustDay(days.getMonday(), latestRun);
        adjustDay(days.getTuesday(), latestRun);
        adjustDay(days.getWednesday(), latestRun);
        adjustDay(days.getThursday(), latestRun);
        adjustDay(days.getFriday(), latestRun);
        adjustDay(days.getSaturday(), latestRun);
        adjustDay(days.getSunday(), latestRun);
    }

    // Utility — map String to corresponding day
    private TrainingPlan.Day getDayByName(TrainingPlan.Days days, String dayName) {
        switch (dayName) {
            case "monday": return days.getMonday();
            case "tuesday": return days.getTuesday();
            case "wednesday": return days.getWednesday();
            case "thursday": return days.getThursday();
            case "friday": return days.getFriday();
            case "saturday": return days.getSaturday();
            case "sunday": return days.getSunday();
            default: return null;
        }
    }

    // Core day adjustment logic — unchanged but polished
    private void adjustDay(TrainingPlan.Day day, Activity latestRun) {
        if (day == null || day.getPace() == null) return;

        try {
            day.setAdjustmentNote(null);
            String workoutType = classifyWorkout(day.getExercise());
            float actualPace = latestRun.getPaceInSeconds();
            float actualDistance = Math.min(latestRun.getDistance(), MAX_DISTANCE * 1609.34f); // meters

            switch (workoutType) {
                case "RECOVERY": adjustRecoveryDay(day, actualPace, actualDistance); break;
                case "INTERVAL": adjustIntervalDay(day, actualPace, actualDistance); break;
                case "LONG_RUN": adjustLongRunDay(day, actualPace, actualDistance); break;
                default: adjustGeneralDay(day, actualPace, actualDistance); break;
            }

            if (latestRun.getAverage_heartrate() > HR_THRESHOLD) {
                reduceDayIntensity(day, 0.8f);
                appendAdjustmentNote(day, "Reduced intensity after high HR effort");
            }

            enforceSaneValues(day);
        } catch (Exception e) {
            Log.e(TAG, "Day adjustment failed", e);
        }
    }

    // --- Supporting logic (same as before, just grouped nicely) ---

    private void adjustRecoveryDay(TrainingPlan.Day day, float pace, float distance) {
        adjustPace(day, pace, 0.2f);
        adjustDistance(day, distance, 0.3f);
        appendAdjustmentNote(day, "Recovery day adjustments");
    }

    private void adjustIntervalDay(TrainingPlan.Day day, float pace, float distance) {
        if (day.getPace().contains("-")) {
            String[] parts = day.getPace().split(" - ");
            float base = parsePace(parts[0]);
            float range = parsePace(parts[0]) - parsePace(parts[1]);
            float adjusted = calculateAdjustedPace(base, pace, 0.3f);
            day.setPace(formatPace(adjusted) + " - " + formatPace(adjusted - range));
        }
        adjustDistance(day, distance, 0.1f);
        appendAdjustmentNote(day, "Interval workout adjustments");
    }

    private void adjustLongRunDay(TrainingPlan.Day day, float pace, float distance) {
        if (day.getExercise().toLowerCase().contains("marathon-pace")) {
            String[] parts = day.getDistance().split(" w/ ");
            float total = Math.min(parseDistance(parts[0]), MAX_DISTANCE);
            float mp = (parts.length > 1) ? Math.min(parseDistance(parts[1].split(" @")[0]), total) : total * 0.6f;

            float newTotal = Math.max(8.0f, total * 0.9f);
            float newMp = Math.min(mp * 0.9f, newTotal * 0.7f);

            day.setDistance(String.format(Locale.US, "%.1f mi w/ %.1f @ marathon race pace", newTotal, newMp));
        } else {
            adjustPace(day, pace, 0.3f);
            adjustDistance(day, distance, 0.2f);
        }
        appendAdjustmentNote(day, "Long run adjustments");
    }

    private void adjustGeneralDay(TrainingPlan.Day day, float pace, float distance) {
        adjustPace(day, pace, 0.3f);
        adjustDistance(day, distance, 0.15f);
        appendAdjustmentNote(day, "General workout adjustments");
    }

    private void adjustPace(TrainingPlan.Day day, float actual, float factor) {
        float target = parsePace(day.getPace().split(" - ")[0]);
        float diff = Math.abs(actual - target) / target;
        if (diff > PACE_TOLERANCE) {
            float newPace = target + (actual - target) * factor;
            day.setPace(formatPace(newPace));
        }
    }

    private void adjustDistance(TrainingPlan.Day day, float actualMeters, float factor) {
        if (day.getDistance() != null && day.getDistance().contains("mi")) {
            float target = parseDistance(day.getDistance());
            float actualMiles = actualMeters / 1609.34f;
            float ratio = actualMiles / target;

            if (Math.abs(1 - ratio) > DISTANCE_TOLERANCE) {
                float newDist = target * (0.9f + factor);
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

    private void enforceSaneValues(TrainingPlan.Day day) {
        try {
            if (day.getDistance() != null && day.getDistance().contains("mi")) {
                float dist = parseDistance(day.getDistance());
                dist = Math.max(0.5f, Math.min(dist, MAX_DISTANCE));
                day.setDistance(String.format(Locale.US, "%.1f mi", dist));
            }
        } catch (Exception e) {
            Log.e(TAG, "Sanity check failed", e);
            day.setDistance("5.0 mi");
        }
    }

    private void appendAdjustmentNote(TrainingPlan.Day day, String note) {
        if (day.getAdjustmentNote() == null) {
            day.setAdjustmentNote(note);
        } else if (!day.getAdjustmentNote().contains(note)) {
            day.setAdjustmentNote(day.getAdjustmentNote() + "; " + note);
        }
    }

    private String classifyWorkout(String exercise) {
        if (exercise == null) return "GENERAL";
        String e = exercise.toLowerCase();
        if (e.contains("recovery")) return "RECOVERY";
        if (e.contains("interval") || e.contains("strides") || e.contains("speed")) return "INTERVAL";
        if (e.contains("long run") || e.contains("marathon")) return "LONG_RUN";
        return "GENERAL";
    }

    private float parsePace(String paceStr) {
        try {
            String[] parts = paceStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse pace: " + paceStr, e);
            return 8 * 60; // fallback 8:00/mile
        }
    }

    private float parseDistance(String distanceStr) {
        try {
            return Float.parseFloat(distanceStr.split(" ")[0]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse distance: " + distanceStr, e);
            return 5.0f;
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

    private void reduceWeekIntensity(TrainingPlan plan, float factor) {
        for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks()) {
            adjustDayIntensity(week.getTraining_plan().getMonday(), factor);
            adjustDayIntensity(week.getTraining_plan().getTuesday(), factor);
            adjustDayIntensity(week.getTraining_plan().getWednesday(), factor);
            adjustDayIntensity(week.getTraining_plan().getThursday(), factor);
            adjustDayIntensity(week.getTraining_plan().getFriday(), factor);
            adjustDayIntensity(week.getTraining_plan().getSaturday(), factor);
            adjustDayIntensity(week.getTraining_plan().getSunday(), factor);

        }
    }

    private void adjustDayIntensity(TrainingPlan.Day day, float factor) {
        if (day == null) return;

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

}
