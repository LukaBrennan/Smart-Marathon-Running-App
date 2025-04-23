package com.example.smartmarathonrunningapp_project;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {
    private static final String TAG = "FeedbackActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_feedback);

        TextView feedbackTextView = findViewById(R.id.dialogFeedbackTextView);
        String performanceDataJson = getIntent().getStringExtra("performanceData");

        try {
            PerformanceData performanceData = PerformanceData.fromJson(performanceDataJson);
            String feedback = generateFeedback(performanceData);
            feedbackTextView.setText(feedback);
        } catch (Exception e) {
            Log.e(TAG, "Error generating feedback", e);
            feedbackTextView.setText("Error generating feedback");
        }
    }

    private String generateFeedback(PerformanceData performanceData) {
        if (performanceData == null || performanceData.isEmpty()) {
            return "No running data available";
        }

        StringBuilder feedback = new StringBuilder();
        Map<String, Map<String, Map<String, Float>>> runData = performanceData.getRunData();

        // Get most recent run
        List<String> weeks = new ArrayList<>(runData.keySet());
        Collections.sort(weeks, Collections.reverseOrder());

        if (!weeks.isEmpty()) {
            String currentWeek = weeks.get(0);
            Map<String, Map<String, Float>> weekRuns = runData.get(currentWeek);

            if (weekRuns != null) {
                List<String> days = new ArrayList<>(weekRuns.keySet());
                Collections.sort(days, Collections.reverseOrder());

                if (!days.isEmpty()) {
                    Map<String, Float> currentRun = weekRuns.get(days.get(0));

                    if (currentRun != null) {
                        // Add basic run info
                        feedback.append("🏃 Run Analysis:\n");

                        // Safely get run metrics with defaults
                        Float distance = currentRun.get("distance");
                        Float pace = currentRun.get("pace");
                        Float heartRate = currentRun.get("heart_rate");
                        Float movingTime = currentRun.get("moving_time");

                        if (distance != null) {
                            feedback.append(String.format(Locale.getDefault(),
                                    "  Distance: %.2f km\n", distance / 1000));
                        }

                        if (pace != null) {
                            feedback.append(String.format(Locale.getDefault(),
                                    "  Pace: %s/km\n", formatPace(pace)));
                        }

                        if (heartRate != null) {
                            feedback.append(String.format(Locale.getDefault(),
                                    "  Avg HR: %.0f bpm\n", heartRate));
                        }

                        // Calculate TRIMP only if we have all required data
                        if (movingTime != null && heartRate != null) {
                            try {
                                float trimp = TRIMP.calculate(
                                        movingTime/60f,
                                        heartRate,
                                        50f, // Default resting HR - should be configurable
                                        190f, // Default max HR - should be configurable
                                        true // Default gender - should be configurable
                                );
                                feedback.append(String.format(Locale.getDefault(),
                                        "  Training Load: %.1f TRIMP\n", trimp));
                            } catch (Exception e) {
                                Log.e(TAG, "TRIMP calculation failed", e);
                            }
                        }

                        // Find and compare with previous run
                        Map<String, Float> previousRun = findPreviousRun(runData, currentWeek, days.get(0));
                        if (previousRun != null) {
                            feedback.append("\nCompared to previous run:\n");

                            // Add comparison logic here if needed
                        }
                    }
                }
            }
        }

        return feedback.length() > 0 ? feedback.toString() : "No valid running data available";
    }

    private float calculateWeeklyTRIMP(Map<String, Map<String, Float>> weekData) {
        if (weekData == null) return 0;

        // This assumes you've added TRIMP to the performance data storage
        return weekData.values().stream()
                .map(run -> run.getOrDefault("trimp", 0f))
                .reduce(0f, Float::sum);
    }

    private Map<String, Float> findPreviousRun(Map<String, Map<String, Map<String, Float>>> runData,
                                               String currentWeek, String currentDay) {
        List<String> weeks = new ArrayList<>(runData.keySet());
        Collections.sort(weeks);

        for (String week : weeks) {
            Map<String, Map<String, Float>> weekData = runData.get(week);
            if (weekData == null) continue;

            List<String> days = new ArrayList<>(weekData.keySet());
            Collections.sort(days);

            for (String day : days) {
                if (week.equals(currentWeek) && day.equals(currentDay)) continue;
                return weekData.get(day);
            }
        }
        return null;
    }

    private String generateWeeklySummary(Map<String, Map<String, Map<String, Float>>> runData,
                                         List<String> weeks) {
        if (weeks.size() < 2) return "";

        String currentWeek = weeks.get(0);
        String previousWeek = weeks.get(1);

        WeeklyMetrics current = calculateWeeklyMetrics(runData.get(currentWeek));
        WeeklyMetrics previous = calculateWeeklyMetrics(runData.get(previousWeek));

        if (current == null || previous == null) return "";

        StringBuilder summary = new StringBuilder("📊 Weekly Summary:\n");

        float paceDiff = previous.avgPace - current.avgPace;
        summary.append(String.format(Locale.getDefault(),
                "• Pace: %s (%s)\n",
                formatPace(current.avgPace),
                paceDiff > 0 ? "improved by " + formatPace(paceDiff) :
                        "slowed by " + formatPace(Math.abs(paceDiff))));

        float distDiff = current.totalDistance - previous.totalDistance;
        float distPercent = (distDiff / previous.totalDistance) * 100;
        summary.append(String.format(Locale.getDefault(),
                "• Distance: %.1f km (%+.1f%%)\n",
                current.totalDistance / 1000, distPercent));

        float hrDiff = previous.avgHeartRate - current.avgHeartRate;
        if (Math.abs(hrDiff) > 1) {
            summary.append(String.format(Locale.getDefault(),
                    "• Heart Rate: %.1f bpm (%+.1f)\n",
                    current.avgHeartRate, -hrDiff));
        }

        return summary.toString();
    }

    private WeeklyMetrics calculateWeeklyMetrics(Map<String, Map<String, Float>> weekData) {
        if (weekData == null) return null;

        WeeklyMetrics metrics = new WeeklyMetrics();
        int runCount = 0;

        for (Map<String, Float> run : weekData.values()) {
            metrics.totalDistance += run.getOrDefault("distance", 0f);
            metrics.avgPace += run.getOrDefault("pace", 0f);
            metrics.avgHeartRate += run.getOrDefault("heart_rate", 0f);
            runCount++;
        }

        if (runCount > 0) {
            metrics.avgPace /= runCount;
            metrics.avgHeartRate /= runCount;
        }

        return metrics;
    }

    private static class WeeklyMetrics {
        float totalDistance = 0;
        float avgPace = 0;
        float avgHeartRate = 0;
    }

    private String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }
}