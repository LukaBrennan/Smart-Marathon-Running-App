package com.example.smartmarathonrunningapp_project.processors;

import android.annotation.SuppressLint;

import com.example.smartmarathonrunningapp_project.Activity;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.example.smartmarathonrunningapp_project.utils.PaceUtils;
import com.example.smartmarathonrunningapp_project.UnitConverter;

import java.util.*;
import java.util.stream.Collectors;

public class ProgressiveFitnessAnalyzer {

    public List<WeeklyReport> generateWeeklyReports(List<Activity> allActivities) {
        List<WeeklyReport> reports = new ArrayList<>();
        ActivityProcessor processor = new ActivityProcessor();

        // Filter and sort activities
// Debugging: Log the total number of activities received
        System.out.println("DEBUG: Total activities received: " + allActivities.size());

        List<Activity> validRuns = allActivities.stream()
                .filter(a -> a.getDistance() > 500 && a.getMoving_time() > 180) // Lowered threshold for debugging
                .sorted(Comparator.comparing(Activity::getStart_date))
                .collect(Collectors.toList());

// Debugging: Log how many activities passed the filter
        System.out.println("DEBUG: Valid runs after filtering: " + validRuns.size());


        if (validRuns.isEmpty()) {
            reports.add(new WeeklyReport(0, "No valid runs available for analysis"));
            return reports;
        }

        // Group by week
        Map<String, List<Activity>> weeklyRuns = new TreeMap<>();
        for (Activity run : validRuns) {
            String week = DateUtils.getWeekOfYear(run.getStart_date());
            weeklyRuns.computeIfAbsent(week, k -> new ArrayList<>()).add(run);
        }

        // Calculate metrics for each week
        List<WeekMetrics> weeklyMetrics = new ArrayList<>();
        for (Map.Entry<String, List<Activity>> entry : weeklyRuns.entrySet()) {
            WeekMetrics metrics = calculateWeekMetrics(entry.getValue(), entry.getKey());
            if (metrics != null) {
                weeklyMetrics.add(metrics);
            }
        }

        // Generate reports
        if (weeklyMetrics.size() >= 2) {
            WeekMetrics baseline = weeklyMetrics.get(0);

            for (int i = 1; i < weeklyMetrics.size(); i++) {
                reports.add(createWeeklyReport(
                        i,
                        weeklyMetrics.get(i),
                        i == 1 ? baseline : weeklyMetrics.get(i-1),
                        baseline
                ));
            }

            // Add final summary if we have enough data
            if (weeklyMetrics.size() > 2) {
                reports.add(createFinalReport(
                        baseline,
                        weeklyMetrics.get(weeklyMetrics.size()-1),
                        weeklyMetrics
                ));
            }
        } else {
            reports.add(new WeeklyReport(0, "Need at least 2 weeks of data for analysis"));
        }

        return reports;
    }

    private WeekMetrics calculateWeekMetrics(List<Activity> weekRuns, String weekLabel) {
        if (weekRuns == null || weekRuns.isEmpty()) return null;

        float totalDistance = 0;
        float totalPace = 0;
        float totalHeartRate = 0;
        int count = 0;

        for (Activity run : weekRuns) {
            if (run.getDistance() > 0) {
                float distance = run.getDistance(); // Distance in meters
                float pace = run.getMoving_time() / UnitConverter.metersToMiles(distance); // Pace in sec/mile

                totalDistance += distance;
                totalPace += pace;

                if (run.getAverage_heartrate() > 0) {
                    totalHeartRate += run.getAverage_heartrate();
                }
                count++;
            }
        }

        if (count == 0) return null;

        return new WeekMetrics(
                weekLabel,
                totalDistance / count,  // Average distance
                totalPace / count,      // Average pace
                totalHeartRate > 0 ? totalHeartRate / count : 0,  // Average HR
                weekRuns.size()         // Number of runs
        );
    }


    @SuppressLint("DefaultLocale")
    private WeeklyReport createWeeklyReport(
            int weekNumber,
            WeekMetrics currentWeek,
            WeekMetrics previousWeek,
            WeekMetrics baseline
    ) {
        StringBuilder content = new StringBuilder();
        content.append("WEEK ").append(weekNumber).append(" (").append(currentWeek.weekLabel).append(")\n\n");
        content.append("Weekly Averages:\n");
        content.append(String.format("- Runs: %d\n", currentWeek.numberOfRuns));
        content.append(String.format("- Distance: %.1f km\n", currentWeek.distance / 1000));
        content.append(String.format("- Pace: %s/mile\n", PaceUtils.convertSecondsToPace((int) currentWeek.pace)));

        if (currentWeek.heartRate > 0) {
            content.append(String.format("- Avg HR: %.1f bpm\n", currentWeek.heartRate));
        }

        content.append("\nTrend Analysis:\n");
        content.append(generateTrendAnalysis(currentWeek, previousWeek, baseline));


        return new WeeklyReport(weekNumber, content.toString());
    }

    @SuppressLint("DefaultLocale")
    private String generateTrendAnalysis(WeekMetrics current, WeekMetrics previous, WeekMetrics baseline) {
        StringBuilder analysis = new StringBuilder();

        // Pace trend
        float paceChange = previous.pace - current.pace;
        analysis.append("- Pace: ");
        analysis.append(paceChange > 0 ? "Improving" : paceChange < 0 ? "Declining" : "Stable");
        analysis.append(String.format(" (%s/mile)\n",
                PaceUtils.formatPaceDifference(Math.abs(paceChange))));

        // Distance trend
        float distanceChange = current.distance - previous.distance;
        analysis.append("- Distance: ");
        analysis.append(distanceChange > 0 ? "Increasing" : distanceChange < 0 ? "Decreasing" : "Stable");
        analysis.append(String.format(" (%.1f km)\n", Math.abs(distanceChange)/1000));

        // HR trend if available
        if (current.heartRate > 0 && previous.heartRate > 0) {
            float hrChange = previous.heartRate - current.heartRate;
            analysis.append("- Heart Rate: ");
            analysis.append(hrChange > 0 ? "Improving (-" : "Declining (+");
            analysis.append(String.format("%.1f bpm)\n", Math.abs(hrChange)));
        }


        // Compare to baseline
        analysis.append("\nSince Starting Week:\n");
        analysis.append(String.format("- Pace: %s/mile %s\n",
                PaceUtils.formatPaceDifference(baseline.pace - current.pace),
                baseline.pace > current.pace ? "faster" : "slower"));

        analysis.append(String.format("- Distance: %.1f km %s\n",
                Math.abs(current.distance - baseline.distance)/1000,
                current.distance > baseline.distance ? "longer" : "shorter"));

        return analysis.toString();
    }

    private WeeklyReport createFinalReport(
            WeekMetrics firstWeek,
            WeekMetrics lastWeek,
            List<WeekMetrics> allMetrics
    ) {
        StringBuilder content = new StringBuilder();
        content.append("TRAINING PERIOD SUMMARY\n\n");
        content.append(String.format("Duration: %d weeks\n", allMetrics.size()));

        // Calculate totals
        float totalDistance = calculateTotalDistance(allMetrics);
        float averageWeeklyDistance = totalDistance / allMetrics.size();

        // Calculate improvements
        float paceImprovement = (firstWeek.pace - lastWeek.pace) / firstWeek.pace * 100;
        float hrImprovement = firstWeek.heartRate > 0 && lastWeek.heartRate > 0 ?
                (firstWeek.heartRate - lastWeek.heartRate) / firstWeek.heartRate * 100 : 0;
        float distImprovement = (lastWeek.distance - firstWeek.distance) / firstWeek.distance * 100;

        // Performance changes
        content.append("\nPerformance Changes:\n");
        content.append(String.format("- Pace: %.1f%% %s\n",
                Math.abs(paceImprovement),
                paceImprovement > 0 ? "faster" : "slower"));

        content.append(String.format("- Distance: %.1f%% %s\n",
                Math.abs(distImprovement),
                distImprovement > 0 ? "longer" : "shorter"));

        if (hrImprovement != 0) {
            content.append(String.format("- Heart Rate: %.1f%% %s\n",
                    Math.abs(hrImprovement),
                    hrImprovement > 0 ? "lower" : "higher"));
        }

        // Training load
        content.append("\nTraining Load:\n");
        content.append(String.format("- Total Distance: %.1f km\n", totalDistance/1000));
        content.append(String.format("- Average Weekly: %.1f km\n", averageWeeklyDistance/1000));

        // Recommendations
        content.append("\nRecommendations:\n");
        if (paceImprovement > 5 && distImprovement > 10) {
            content.append("- Excellent progress! Consider adding speed work\n");
        } else if (hrImprovement > 3) {
            content.append("- Good cardiovascular improvement detected\n");
        } else if (distImprovement > 5) {
            content.append("- Maintain current training load\n");
        } else {
            content.append("- Consider increasing volume or intensity\n");
        }

        return new WeeklyReport(-1, content.toString());
    }

    private float calculateTotalDistance(List<WeekMetrics> metrics) {
        float total = 0;
        for (WeekMetrics week : metrics) {
            total += week.distance * week.numberOfRuns;
        }
        return total;
    }

    private static class WeekMetrics {
        final String weekLabel;
        final float distance; // meters
        final float pace; // sec/mile
        final float heartRate; // bpm
        final int numberOfRuns;

        WeekMetrics(String weekLabel, float distance, float pace, float heartRate, int numberOfRuns) {
            this.weekLabel = weekLabel;
            this.distance = distance;
            this.pace = pace;
            this.heartRate = heartRate;
            this.numberOfRuns = numberOfRuns;
        }
    }
}