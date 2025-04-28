package com.example.smartmarathonrunningapp_project;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.google.gson.Gson;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
    //  This class dynamically updates the training plan based on recent workout performance metrics
public class AutoAdjuster
{
    private static final String TAG = "AutoAdjuster";

    // Updated thresholds with more sophisticated analysis
    private static final float HIGH_ACUTE_TRIMP_THRESHOLD = 350f;   //  TRIMP score beyond which workouts are considered too intense
    private static final float FATIGUE_RATIO_THRESHOLD = 1.15f; //  Acute an Chronic ratio

    // New constants for data analysis
    private static final int MIN_RUNS_FOR_ACCURACY = 5; //  Min runs needed
    private static final int DAYS_FOR_ACUTE_LOAD = 7;   //  7 day training load window
    private static final int DAYS_FOR_CHRONIC_LOAD = 28;    //  28 day Window for baseline fitness

    //  Main adjustment algorithm that modifies training plans based on recent performance
    public TrainingPlan adjustPlan(TrainingPlan currentPlan, List<Activity> recentRuns, TrainingPlan originalPlan)
    {
        if (recentRuns == null || recentRuns.isEmpty())
        {
            return currentPlan; //  No adjustments can be made without data
        }

        //  Filter runs to relevant time windows
        List<Activity> filteredRuns = filterRecentRuns(recentRuns, DAYS_FOR_ACUTE_LOAD + DAYS_FOR_CHRONIC_LOAD);

        if (filteredRuns.size() < MIN_RUNS_FOR_ACCURACY)
        {
            Log.w(TAG, "Insufficient data - only " + filteredRuns.size() + " runs available");
            // Fall back to simpler adjustment if needed
            return basicAdjustment(currentPlan, recentRuns, originalPlan);
        }

        // Create fresh copy to modify
        TrainingPlan adjustedPlan = deepCopyPlan(originalPlan);

        // Calculate training loads with proper time windows
        float acuteTRIMP = calculateTRIMPForPeriod(filteredRuns, DAYS_FOR_ACUTE_LOAD);
        float chronicTRIMP = calculateTRIMPForPeriod(filteredRuns, DAYS_FOR_CHRONIC_LOAD);
        float fatigueRatio = acuteTRIMP / chronicTRIMP;

        // More robust fitness estimation
        float vo2max = estimateCurrentFitness(filteredRuns);

        // Apply intensity reduction if thresholds exceeded
        if (fatigueRatio > FATIGUE_RATIO_THRESHOLD || acuteTRIMP > HIGH_ACUTE_TRIMP_THRESHOLD)
        {
            reduceWeekIntensity(adjustedPlan, 0.85f);   //  Reduce by 15%
            adjustedPlan.setAdjustmentNote(String.format(Locale.US, "Recovery week: Acute TRIMP=%.1f, Chronic TRIMP=%.1f, Ratio=%.2f", acuteTRIMP, chronicTRIMP, fatigueRatio));
        }
        return adjustedPlan;
    }
    //  Calculates TRIMP load for a specific time period
    private float calculateTRIMPForPeriod(List<Activity> runs, int days)
    {
        List<Activity> periodRuns = filterRecentRuns(runs, days);
        return periodRuns.stream().map(r -> TRIMP.calculate(r.getMoving_time()/60f, r.getAverage_heartrate(), r.getResting_heartrate(), r.getMax_heartrate(), r.isMale())).reduce(0f, Float::sum);
    }
    //  Filters runs to only those within the specified days
    private List<Activity> filterRecentRuns(List<Activity> runs, int maxDays)
    {
        long cutoff = System.currentTimeMillis() - (maxDays * 24 * 60 * 60 * 1000L);
        return runs.stream().filter(run -> run.getStart_date() != null && DateUtils.parseDate(run.getStart_date()).getTime() > cutoff).collect(Collectors.toList());
    }
    //  Creates a deep copy of a training plan using Gson serialization
    private TrainingPlan deepCopyPlan(TrainingPlan plan)
    {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(plan), TrainingPlan.class);
    }
    //  Fallback adjustment when insufficient data is available
    private TrainingPlan basicAdjustment(TrainingPlan ignoredCurrentPlan, List<Activity> recentRuns, TrainingPlan originalPlan)
    {
        TrainingPlan adjustedPlan = deepCopyPlan(originalPlan);
        float weeklyTRIMP = calculateWeeklyTRIMP(recentRuns);
        float fatigueRatio = calculateFatigueRatio(recentRuns);

        if (fatigueRatio > FATIGUE_RATIO_THRESHOLD || weeklyTRIMP > HIGH_ACUTE_TRIMP_THRESHOLD)
        {
            reduceWeekIntensity(adjustedPlan, 0.85f);
            adjustedPlan.setAdjustmentNote("Basic adjustment: High fatigue (TRIMP=" + weeklyTRIMP + ")");
        }
        return adjustedPlan;
    }
    //  Simplified weekly TRIMP calculation (uses all runs)
    private float calculateWeeklyTRIMP(List<Activity> runs)
    {
        return runs.stream().map(r -> TRIMP.calculate(r.getMoving_time()/60f, r.getAverage_heartrate(), r.getResting_heartrate(), r.getMax_heartrate(), r.isMale())).reduce(0f, Float::sum);
    }
    //  Calculates fatigue ratio comparing recent performance to baseline
    private float calculateFatigueRatio(List<Activity> runs)
    {
        if (runs.size() < 5)
        {
            return 1.0f;    //  Default ratio with  insufficient data
        }
        Activity lastRun = runs.get(0);
        List<Activity> recentRuns = runs.stream().limit(5).collect(Collectors.toList());
        //  Calculate average pace and HR from recent runs
        float avgPace = recentRuns.stream().map(Activity::getPaceInSeconds).reduce(0f, Float::sum) / recentRuns.size();

        float avgHR = recentRuns.stream().map(Activity::getAverage_heartrate).reduce(0f, Float::sum) / recentRuns.size();
        //  Weighted ratio (70% pace, 30% HR)
        return (lastRun.getPaceInSeconds() / avgPace) * 0.7f + (lastRun.getAverage_heartrate() / avgHR) * 0.3f;
    }
    //  Estimates current VO2Max fitness level from recent runs
    private float estimateCurrentFitness(List<Activity> runs)
    {
        // Take median of top 3 runs instead of just the best
        List<Float> vo2Estimates = runs.stream().sorted(Comparator.comparing(Activity::getPaceInSeconds)).limit(3).map(r -> VO2MaxEstimator.fromRacePerformance(r.getDistance(), r.getMoving_time())).sorted().collect(Collectors.toList());
        if (vo2Estimates.size() > 1)
        {
            return vo2Estimates.get(1);
        }
        else
        {
            if (vo2Estimates.isEmpty()) return 0;
            return vo2Estimates.get(0);
        }
    }
    //  Utility methods for parsing and formatting
    private float parsePace(String paceStr)
    {
        try
        {
            String[] parts = paceStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to parse pace: " + paceStr, e);
            return 8 * 60; // fallback 8:00/mile
        }
    }

    private float parseDistance(String distanceStr)
    {
        try
        {
            // Handle empty or null strings
            if (distanceStr == null || distanceStr.trim().isEmpty())
            {
                return 0f;
            }

            // Remove "mi" if present
            distanceStr = distanceStr.replace("mi", "").trim();

            // Check if it's a range (contains "-")
            if (distanceStr.contains("-"))
            {
                String[] parts = distanceStr.split("-");
                // Take the first value in the range (or average if preferred)
                return Float.parseFloat(parts[0].trim());
            }

            // Check if it's a complex description (e.g., "13 mi w/ 8 @ marathon race pace")
            if (distanceStr.contains("w/"))
            {
                String[] parts = distanceStr.split("w/");
                return Float.parseFloat(parts[0].trim());
            }

            // Default case - simple number
            return Float.parseFloat(distanceStr.split(" ")[0]);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to parse distance: " + distanceStr, e);
            return 5.0f; // Default fallback value
        }
    }
    private String formatPace(float seconds)
    {
        int minutes = (int)(seconds / 60);
        int secs = (int)(seconds % 60);
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }
    //  Reduces intensity across all workouts in a plan
    private void reduceWeekIntensity(TrainingPlan plan, float factor)
    {
        for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks())
        {
            adjustDayIntensity(week.getTraining_plan().getMonday(), factor);
            adjustDayIntensity(week.getTraining_plan().getTuesday(), factor);
            adjustDayIntensity(week.getTraining_plan().getWednesday(), factor);
            adjustDayIntensity(week.getTraining_plan().getThursday(), factor);
            adjustDayIntensity(week.getTraining_plan().getFriday(), factor);
            adjustDayIntensity(week.getTraining_plan().getSaturday(), factor);
            adjustDayIntensity(week.getTraining_plan().getSunday(), factor);

        }
    }
    //  Adjusts an individual day's workout intensity
    private void adjustDayIntensity(TrainingPlan.Day day, float factor)
    {
        if (day == null) return;

        try
        {
            if (day.getDistance() != null && day.getDistance().contains("mi"))
            {
                // Skip adjustment for special workout types
                if (day.getExercise().toLowerCase().contains("tune-up") || day.getExercise().toLowerCase().contains("race"))
                {
                    return;
                }

                float dist = parseDistance(day.getDistance());
                day.setDistance(String.format(Locale.US, "%.1f mi", dist * factor));
            }
            if
            (day.getPace() != null)
            {
                float pace = parsePace(day.getPace().split(" - ")[0]);
                day.setPace(formatPace(pace * 1.05f));
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Intensity reduction failed", e);
        }
    }

}
