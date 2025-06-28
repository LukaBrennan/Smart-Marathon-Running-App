package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.BuildConfig;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {
    private static final String TAG = "FeedbackActivity";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_feedback);

        Button closeButton = findViewById(R.id.closeButton);
        String performanceDataJson = getIntent().getStringExtra("performanceData");

        try {
            PerformanceData performanceData = PerformanceData.fromJson(performanceDataJson);
            if (!performanceData.isEmpty()) {
                updateFeedbackUI(performanceData);
            } else {
                TextView feedbackText = findViewById(R.id.feedbackText);
                feedbackText.setText("No running data available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating feedback", e);
            TextView feedbackText = findViewById(R.id.feedbackText);
            feedbackText.setText("Error generating feedback");
        }

        closeButton.setOnClickListener(v -> finish());
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateFeedbackUI(PerformanceData performanceData) {
        TextView feedbackText = findViewById(R.id.feedbackText);
        TextView distanceView = findViewById(R.id.distanceValue);
        TextView paceView = findViewById(R.id.paceValue);
        TextView effortView = findViewById(R.id.effortValue);
        TextView streakView = findViewById(R.id.streakValue);
        TextView achievementView = findViewById(R.id.achievementText);
        TextView avgHRView = findViewById(R.id.avgHRValue);
        TextView maxHRView = findViewById(R.id.maxHRValue);

        Map<String, Float> currentRun = getMostRecentRun(performanceData);
        if (currentRun.isEmpty()) {
            feedbackText.setText("No recent runs found");
            return;
        }

        String feedback = generateFeedback(currentRun, performanceData);
        feedbackText.setText(feedback);

        Float distance = currentRun.get("distance");
        Float pace = currentRun.get("pace");
        Float trimp = currentRun.get("trimp");
        Float avgHR = currentRun.get("heart_rate");
        Float maxHR = currentRun.get("max_heartrate");

        distanceView.setText(distance != null ? String.format(Locale.getDefault(), "%.1f km", distance / 1000) : "N/A");
        paceView.setText(pace != null ? formatPace(pace) + "/km" : "N/A");
        avgHRView.setText(avgHR != null ? String.format(Locale.getDefault(), "%.0f bpm", avgHR) : "N/A");
        maxHRView.setText(maxHR != null ? String.format(Locale.getDefault(), "%.0f bpm", maxHR) : "N/A");

        if (trimp != null) {
            String effortLevel = performanceData.getRelativeEffortLevel(trimp);
            effortView.setText(String.format(Locale.getDefault(),"%.0f (%s)", trimp, effortLevel));
        } else {
            effortView.setText("N/A");
        }

        int streak = performanceData.getCurrentStreak();
        streakView.setText(String.format(Locale.getDefault(),"%d-day streak", streak));

        if (distance != null && distance > 20000) {
            achievementView.setText("Long run achievement!");
        } else if (pace != null && pace < 300) {
            achievementView.setText("Speed achievement!");
        } else {
            achievementView.setText("Keep going!");
        }
    }

    private Map<String, Float> getMostRecentRun(PerformanceData performanceData) {
        Map<String, Map<String, Map<String, Float>>> runData = performanceData.getRunData();
        if (runData == null || runData.isEmpty()) {
            return Collections.emptyMap();
        }

        String latestWeek = Collections.max(runData.keySet());
        Map<String, Map<String, Float>> weekRuns = runData.get(latestWeek);
        if (weekRuns == null || weekRuns.isEmpty()) {
            return Collections.emptyMap();
        }

        String latestDay = Collections.max(weekRuns.keySet());
        Map<String, Float> run = weekRuns.get(latestDay);
        if (run == null) {
            return Collections.emptyMap();
        }

        Float avgSpeed = run.get("average_speed");
        if (avgSpeed != null && !run.containsKey("pace")) {
            float paceSecPerKm = 1000 / avgSpeed;
            run.put("pace", paceSecPerKm);
        }
        return run;
    }

    private String generateFeedback(Map<String, Float> currentRun, PerformanceData performanceData) {
        StringBuilder feedback = new StringBuilder();

        // Extract metrics with defaults for missing values
        Float distance = currentRun.get("distance");
        Float trimpValue = currentRun.get("trimp");
        Float pace = currentRun.get("pace");

        float distanceKm = (distance != null ? distance : 0f) / 1000f;
        float trimp = trimpValue != null ? trimpValue : 0f;

        // Build the feedback string
        feedback.append(String.format(Locale.getDefault(),"Nice steady %.1f km run! ", distanceKm));

        if (pace != null) {
            feedback.append(String.format(Locale.getDefault(),"You maintained a pace of %s/km.%n%n", formatPace(pace)));
        }

        // Debug info
        if (BuildConfig.DEBUG) {
            TrainingPlan.Day plannedDay = getMostRecentPlannedDay();
            if (plannedDay != null && distance != null && pace != null) {
                float plannedDistanceMeters = TrainingPlan.parseDistanceToMeters(plannedDay.getDistance());
                float actualDistanceMeters = distance;
                float distDeviation = Math.abs(actualDistanceMeters - plannedDistanceMeters) / plannedDistanceMeters;

                float[] plannedPaceRange = TrainingPlan.parsePaceToSecPerKm(plannedDay.getPace());
                float paceDeviation = calculatePaceDeviation(pace, plannedPaceRange);

                feedback.append("=== DEBUG INFO ===\n");
                feedback.append(String.format(Locale.US, "Planned distance: %.1f mi\n", plannedDistanceMeters / 1609.34f));
                feedback.append(String.format(Locale.US, "Actual distance: %.1f mi\n", actualDistanceMeters / 1609.34f));
                feedback.append(String.format(Locale.US, "Distance deviation: %.1f%%\n", distDeviation * 100));
                feedback.append(String.format("Planned pace: %s/km\n", formatPace(plannedPaceRange[0])));
                feedback.append(String.format("Actual pace: %s/km\n", formatPace(pace)));
                feedback.append(String.format(Locale.US, "Pace deviation: %.1f%%\n", paceDeviation * 100));
                feedback.append("=================\n\n");
            }
        }

        // Add effort information
        String effortLevel = performanceData.getRelativeEffortLevel(trimp);
        feedback.append(String.format(Locale.getDefault(),"Relative Effort: %.0f (%s)%n", trimp, effortLevel));

        // Add streak information
        int streak = performanceData.getCurrentStreak();
        feedback.append(String.format(Locale.getDefault(),"%d-day running streak%n%n", streak));

        // Add training load advice
        PerformanceData.PerformanceMetrics metrics = performanceData.calculatePerformanceMetrics();
        if (metrics.acuteLoad / metrics.chronicLoad > 1.2f) {
            feedback.append("Great effort! Consider a recovery day soon.");
        } else {
            feedback.append("Keep up the consistency!");
        }

        return feedback.toString();
    }

    private float calculatePaceDeviation(float actualPace, float[] plannedRange) {
        if (plannedRange[0] == 0) return 0f;

        if (actualPace < plannedRange[0]) {
            return (plannedRange[0] - actualPace) / plannedRange[0];
        } else if (actualPace > plannedRange[1]) {
            return (actualPace - plannedRange[1]) / plannedRange[1];
        }
        return 0f;
    }

    private TrainingPlan.Day getMostRecentPlannedDay() {
        // TODO: Implement this based on your data structure
        // This should return the planned day that corresponds to the current run
        return null;
    }

    private String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }
}