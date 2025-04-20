package com.example.smartmarathonrunningapp_project.processors;
import android.annotation.SuppressLint;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.Activity;
import com.example.smartmarathonrunningapp_project.Feedback;
import com.example.smartmarathonrunningapp_project.FeedbackGenerator;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.example.smartmarathonrunningapp_project.UnitConverter;

import java.util.*;
import java.util.stream.Collectors;

public class ProgressiveFitnessAnalyzer {

    private static final String TAG = "ProgressiveFitnessAnalyzer";
    private final FeedbackGenerator feedbackGenerator = new FeedbackGenerator();

    public List<WeeklyReport> generateWeeklyReports(List<Activity> allActivities) {
        Log.d(TAG, "Generating weekly reports for " + (allActivities != null ? allActivities.size() : "null") + " activities");
        List<WeeklyReport> reports = new ArrayList<>();

        if (allActivities == null || allActivities.isEmpty()) {
            reports.add(new WeeklyReport(0, "No activities available for analysis."));
            return reports;
        }

        List<Activity> validRuns = allActivities.stream()
                .filter(a -> a.getDistance() > 500 && a.getMoving_time() > 180)
                .sorted(Comparator.comparing(Activity::getStart_date))
                .collect(Collectors.toList());

        if (validRuns.isEmpty()) {
            reports.add(new WeeklyReport(0, "No valid runs available for analysis."));
            return reports;
        }

        // Group by week
        Map<String, List<Activity>> weeklyRuns = new TreeMap<>();
        for (Activity run : validRuns) {
            String week = DateUtils.getWeekOfYear(run.getStart_date());
            weeklyRuns.computeIfAbsent(week, k -> new ArrayList<>()).add(run);
        }

        List<WeekMetrics> weeklyMetrics = weeklyRuns.entrySet().stream()
                .map(entry -> calculateWeekMetrics(entry.getValue(), entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (weeklyMetrics.size() < 2) {
            reports.add(new WeeklyReport(0, "Need at least 2 weeks of data for meaningful progressive analysis."));
            return reports;
        }

        WeekMetrics baseline = weeklyMetrics.get(0);

        for (int i = 1; i < weeklyMetrics.size(); i++) {
            WeekMetrics current = weeklyMetrics.get(i);
            WeekMetrics previous = weeklyMetrics.get(i - 1);
            String reportText = feedbackGenerator.generateWeeklyFeedback(i, current, previous, baseline);
            reports.add(new WeeklyReport(i, reportText));
        }

        if (weeklyMetrics.size() > 2) {
            WeekMetrics finalWeek = weeklyMetrics.get(weeklyMetrics.size() - 1);
            String summaryText = feedbackGenerator.generateFinalSummary(baseline, finalWeek, weeklyMetrics);
            reports.add(new WeeklyReport(-1, summaryText));
        }

        return reports;
    }

    private WeekMetrics calculateWeekMetrics(List<Activity> runs, String weekLabel) {
        if (runs == null || runs.isEmpty()) return null;

        float totalDistance = 0f;
        float totalPace = 0f;
        float totalHR = 0f;
        int count = 0;

        for (Activity run : runs) {
            if (run.getDistance() <= 0) continue;

            float distance = run.getDistance();
            float pace = run.getMoving_time() / UnitConverter.metersToMiles(distance);
            totalDistance += distance;
            totalPace += pace;

            if (run.getAverage_heartrate() > 0) {
                totalHR += run.getAverage_heartrate();
            }

            count++;
        }

        if (count == 0) return null;

        return new WeekMetrics(
                weekLabel,
                totalDistance / count,
                totalPace / count,
                totalHR > 0 ? totalHR / count : 0,
                runs.size()
        );
    }

    private float calculateTotalDistance(List<WeekMetrics> metrics) {
        return metrics.stream()
                .map(m -> m.distance * m.numberOfRuns)
                .reduce(0f, Float::sum);
    }

    public static class WeekMetrics {
        public final String weekLabel;
        public final float distance;  // average per run
        public final float pace;      // in seconds/mile
        public final float heartRate;
        public final int numberOfRuns;

        public WeekMetrics(String weekLabel, float distance, float pace, float heartRate, int numberOfRuns) {
            this.weekLabel = weekLabel;
            this.distance = distance;
            this.pace = pace;
            this.heartRate = heartRate;
            this.numberOfRuns = numberOfRuns;
        }
    }

    public Feedback generateDailyFeedback(List<Activity> activities) {
        if (activities == null || activities.size() < 2) return null;

        activities.sort(Comparator.comparing(Activity::getStart_date)); // this assumes correct ISO string sorting

        Activity lastRun = activities.get(activities.size() - 2);
        Activity todayRun = activities.get(activities.size() - 1);

        double paceDiff = lastRun.getPace() - todayRun.getPace(); // positive = faster today
        int heartRateDiff = lastRun.getHeartRate() - todayRun.getHeartRate(); // positive = better efficiency

        return new Feedback(paceDiff, heartRateDiff, todayRun.getPace(), todayRun.getHeartRate());
    }


}
