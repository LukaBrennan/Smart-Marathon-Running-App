package com.example.smartmarathonrunningapp_project;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AutoAdjuster {

    private static final String TAG = "AutoAdjuster";
    private static final float HIGH_ACUTE_TRIMP_THRESHOLD = 350f;
    private static final float FATIGUE_RATIO_THRESHOLD = 1.15f;
    private static final int MIN_RUNS_FOR_ACCURACY = 3;
    private static final int DAYS_FOR_ACUTE_LOAD = 7;
    private static final int DAYS_FOR_CHRONIC_LOAD = 28;
    private static final float RECOVERY_DISTANCE_FACTOR = 0.8f;
    private static final float RECOVERY_PACE_ADJUSTMENT_SEC_KM = 45f;
    private static final float RECOVERY_DISTANCE_REDUCTION = 0.8f;
    private static final float RECOVERY_PACE_SLOWDOWN = 30f;

    private static class TrainingLoadMetrics {
        float acuteTRIMP;
        float chronicTRIMP;
        float fatigueRatio;
    }

    public TrainingPlan adjustPlan(TrainingPlan currentPlan, List<Activity> recentRuns, Map<String, String> trafficLightStatuses){
        if (recentRuns == null || recentRuns.isEmpty()) return currentPlan;

        TrainingLoadMetrics metrics = calculateTrainingLoadMetrics(recentRuns);
        boolean needsRecovery = shouldTriggerRecovery(metrics);
        TrainingPlan adjustedPlan = deepCopyPlan(currentPlan);
        adjustedPlan.setAdjustmentNote(createAdjustmentNote(metrics, needsRecovery));

        for (TrainingPlan.TrainingWeek week : adjustedPlan.getTraining_weeks()) {
            TrainingPlan.Days days = week.getTraining_plan();

            Map<String, TrainingPlan.Day> dayMap = Map.of(
                    "Monday", days.getMonday(),
                    "Tuesday", days.getTuesday(),
                    "Wednesday", days.getWednesday(),
                    "Thursday", days.getThursday(),
                    "Friday", days.getFriday(),
                    "Saturday", days.getSaturday(),
                    "Sunday", days.getSunday()
            );

            for (Map.Entry<String, TrainingPlan.Day> entry : dayMap.entrySet()) {
                String dayName = entry.getKey();
                TrainingPlan.Day day = entry.getValue();

                if (shouldAdjustDay(day)) {
                    if (needsRecovery) {
                        adjustForRecovery(day);
                    } else if (trafficLightStatuses.containsKey(dayName)) {
                        adjustForTrafficLight(day, trafficLightStatuses.get(dayName));
                    }

                    Activity match = findMatchingActivityForDay(day, recentRuns);
                    if (match != null) {
                        adjustBasedOnFitness(match, day);
                    }
                }
            }
        }

        return adjustedPlan;
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

    private Activity findMatchingActivityForDay(TrainingPlan.Day planDay, List<Activity> recentRuns) {
        return recentRuns.stream()
                .filter(run -> run.getDayOfWeek() != null
                        && planDay.getDayOfWeek() != null
                        && run.getDayOfWeek().equalsIgnoreCase(planDay.getDayOfWeek()))
                .findFirst()
                .orElse(null);
    }


    private TrainingLoadMetrics calculateTrainingLoadMetrics(List<Activity> runs) {
        TrainingLoadMetrics metrics = new TrainingLoadMetrics();
        List<Activity> filteredRuns = filterRecentRuns(runs, DAYS_FOR_ACUTE_LOAD + DAYS_FOR_CHRONIC_LOAD);

        metrics.acuteTRIMP = calculateTRIMPForPeriod(filteredRuns, DAYS_FOR_ACUTE_LOAD);
        metrics.chronicTRIMP = calculateTRIMPForPeriod(filteredRuns, DAYS_FOR_CHRONIC_LOAD);
        metrics.fatigueRatio = metrics.acuteTRIMP / metrics.chronicTRIMP;

        return metrics;
    }

    private String createAdjustmentNote(TrainingLoadMetrics metrics, boolean needsRecovery) {
        return String.format(Locale.US,
                "Acute TRIMP: %.1f, Chronic TRIMP: %.1f, Ratio: %.2f. %s",
                metrics.acuteTRIMP, metrics.chronicTRIMP, metrics.fatigueRatio,
                needsRecovery ? "Recovery week activated" : "Normal training load");
    }

    private boolean shouldTriggerRecovery(TrainingLoadMetrics metrics) {
        return metrics.fatigueRatio > FATIGUE_RATIO_THRESHOLD || metrics.acuteTRIMP > HIGH_ACUTE_TRIMP_THRESHOLD;
    }

    private boolean shouldAdjustDay(TrainingPlan.Day day) {
        return day != null &&
                day.getDistance() != null &&
                !day.getDistance().equals("0 mi") &&
                !day.getExercise().toLowerCase().contains("race") &&
                !day.getExercise().toLowerCase().contains("tune-up");
    }

    private void adjustForRecovery(TrainingPlan.Day day) {
        if (day.getDistance() != null && day.getDistance().contains("mi")) {
            float dist = parseDistance(day.getDistance());
            dist *= RECOVERY_DISTANCE_FACTOR;
            day.setDistance(String.format(Locale.US, "%.1f mi", dist));
        }

        if (day.getPace() != null) {
            float[] currentPace = parsePaceRange(day.getPace());
            float newMinPace = currentPace[0] + RECOVERY_PACE_ADJUSTMENT_SEC_KM;
            float newMaxPace = currentPace[1] + RECOVERY_PACE_ADJUSTMENT_SEC_KM;
            day.setPace(formatPace(newMinPace) + " - " + formatPace(newMaxPace));
        }
    }

    private void adjustForTrafficLight(TrainingPlan.Day day, String status) {
        if (day == null || status == null) return;

        if (day.getDistance() != null && day.getDistance().contains("mi")) {
            float dist = parseDistance(day.getDistance());
            dist *= getDistanceAdjustmentFactor(status);
            day.setDistance(String.format(Locale.US, "%.1f mi", dist));
        }

        if (day.getPace() != null) {
            float[] currentPace = parsePaceRange(day.getPace());
            float adjustment = getPaceAdjustment(status);
            float newMinPace = currentPace[0] + adjustment;
            float newMaxPace = currentPace[1] + adjustment;
            day.setPace(formatPace(newMinPace) + " - " + formatPace(newMaxPace));
        }
    }

    private float calculateTRIMPForPeriod(List<Activity> runs, int days) {
        return filterRecentRuns(runs, days).stream()
                .map(r -> TRIMP.calculate(r.getMoving_time() / 60f, r.getAverage_heartrate(),
                        r.getResting_heartrate(), r.getMax_heartrate(), r.isMale()))
                .reduce(0f, Float::sum);
    }

    private List<Activity> filterRecentRuns(List<Activity> runs, int maxDays) {
        long cutoff = System.currentTimeMillis() - (maxDays * 24L * 60 * 60 * 1000);
        return runs.stream()
                .filter(run -> run.getStart_date() != null &&
                        DateUtils.parseDate(run.getStart_date()).getTime() > cutoff)
                .collect(Collectors.toList());
    }

    private TrainingPlan deepCopyPlan(TrainingPlan plan) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(plan), TrainingPlan.class);
    }

    private float parseDistance(String distanceStr) {
        try {
            if (distanceStr == null || distanceStr.trim().isEmpty()) return 0f;
            distanceStr = distanceStr.replace("mi", "").trim();

            if (distanceStr.contains("-")) return Float.parseFloat(distanceStr.split("-")[0].trim());
            if (distanceStr.contains("w/")) return Float.parseFloat(distanceStr.split("w/")[0].trim());

            return Float.parseFloat(distanceStr.split(" ")[0]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse distance: " + distanceStr, e);
            return 5.0f;
        }
    }

    private float[] parsePaceRange(String paceStr) {
        try {
            String[] parts = paceStr.split(" - ");
            return new float[] {
                    parsePace(parts[0]),
                    parts.length > 1 ? parsePace(parts[1]) : parsePace(parts[0])
            };
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse pace range: " + paceStr, e);
            return new float[]{480f, 480f};
        }
    }

    private float parsePace(String paceStr) {
        try {
            String[] parts = paceStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse pace: " + paceStr, e);
            return 480f;
        }
    }

    private String formatPace(float seconds) {
        int minutes = (int)(seconds / 60);
        int secs = (int)(seconds % 60);
        return String.format(Locale.US, "%d:%02d", minutes, secs);
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
            case "YELLOW": return 5f;
            case "RED": return 10f;
            default: return 0f;
        }
    }

    public void adjustBasedOnFitness(Activity activity, TrainingPlan.Day day) {
        if (activity == null || day == null || activity.getAverage_heartrate() == 0) return;

        float avgHR = activity.getAverage_heartrate();
        float actualPace = activity.getPaceInSeconds();
        float predictedPace = PerformanceEvaluator.predictPaceFromHR(avgHR);

        int delta = PerformanceEvaluator.calculatePaceDelta(actualPace, predictedPace);
        if (delta == 0) return;

        float[] paceRange = parsePaceRange(day.getPace());
        float newMin = paceRange[0] + delta;
        float newMax = paceRange[1] + delta;
        day.setPace(formatPace(newMin) + " - " + formatPace(newMax));

        String feedback = PerformanceEvaluator.generateFeedback(actualPace, predictedPace,
                PerformanceEvaluator.getTrafficLightColor(paceRange[0], actualPace,
                        parseDistance(day.getDistance()), activity.getDistance()));
        activity.setFeedback(feedback);
    }
}
