package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {
    private static final String TAG = "FeedbackActivity";
    private static final String KEY_AVG_PACE = "Avg Pace (min/mile)";
    private static final String KEY_AVG_HEART_RATE = "Avg Heart Rate";
    private static final float DEFAULT_PACE = 8f * 60f; // 8 min/mile as default (float multiplication)
    private static final float DEFAULT_HEART_RATE = 150f; // bpm as default

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_feedback);

        TextView feedbackTextView = findViewById(R.id.dialogFeedbackTextView);
        String performanceDataJson = getIntent().getStringExtra("performanceData");

        try {
            String feedback = generatePersonalizedFeedback(performanceDataJson);
            feedbackTextView.setText(feedback);
        } catch (Exception e) {
            feedbackTextView.setText("Error generating feedback");
            Log.e(TAG, "Error generating feedback", e);
        }
    }

    private String generatePersonalizedFeedback(String json) {
        if (json == null || json.isEmpty()) {
            return "No performance data available";
        }

        try {
            PerformanceData performanceData = parseAndValidateData(json);
            if (!performanceData.isValid()) {
                return performanceData.getErrorMessage();
            }

            return buildFeedbackMessage(performanceData);
        } catch (Exception e) {
            Log.e(TAG, "Error processing performance data", e);
            return "Error processing your performance data";
        }
    }

    private PerformanceData parseAndValidateData(String json) {
        Type type = new TypeToken<Map<String, Map<String, Float>>>(){}.getType();
        Map<String, Map<String, Float>> data = new Gson().fromJson(json, type);

        if (data == null || data.isEmpty()) {
            return new PerformanceData("No valid performance data found");
        }

        List<String> dates = new ArrayList<>(data.keySet());
        Collections.sort(dates);

        if (dates.size() < 2) {
            return new PerformanceData("Need at least 2 runs to compare");
        }

        String latestDate = dates.get(dates.size()-1);
        String previousDate = dates.get(dates.size()-2);

        Map<String, Float> latestRun = data.get(latestDate);
        Map<String, Float> previousRun = data.get(previousDate);

        if (latestRun == null || previousRun == null) {
            return new PerformanceData("Incomplete run data for comparison");
        }

        return new PerformanceData(data, dates, latestRun, previousRun);
    }

    private String buildFeedbackMessage(PerformanceData data) {
        StringBuilder feedback = new StringBuilder();
        addGreeting(feedback);
        addPaceComparison(feedback, data);
        addHeartRateAnalysis(feedback, data);
        addWeeklySummary(feedback, data);
        return feedback.toString().replace("%n", System.lineSeparator());
    }

    private void addGreeting(StringBuilder feedback) {
        feedback.append("🏃 Great job on your run!%n%n");
    }

    private void addPaceComparison(StringBuilder feedback, PerformanceData data) {
        float paceDiff = data.getPreviousPace() - data.getLatestPace();

        if (paceDiff > 0) {
            feedback.append(String.format(Locale.getDefault(),
                    "⭐ Your pace improved by %s compared to last run!%n",
                    formatPace(paceDiff)));
        } else if (paceDiff < 0) {
            feedback.append(String.format(Locale.getDefault(),
                    "🔹 Your pace was %s slower this time - maybe try some intervals next run?%n",
                    formatPace(Math.abs(paceDiff))));
        } else {
            feedback.append("↔ Your pace was consistent with your last run%n");
        }
    }

    private void addHeartRateAnalysis(StringBuilder feedback, PerformanceData data) {
        float hrDiff = data.getPreviousHeartRate() - data.getLatestHeartRate();

        if (hrDiff > 5) {
            feedback.append("❤️ Your heart rate dropped significantly - great cardio efficiency!%n");
        } else if (hrDiff > 0) {
            feedback.append("❤️ Your heart rate was slightly lower - good endurance building%n");
        } else if (hrDiff < -5) {
            feedback.append("⚠️ Your heart rate was higher - consider more recovery time%n");
        } else {
            feedback.append("❤️ Your heart rate was stable%n");
        }
    }

    private void addWeeklySummary(StringBuilder feedback, PerformanceData data) {
        if (data.getDates().size() >= 7) {
            feedback.append("%n📊 Weekly Summary:%n");

            Map<String, Float> firstWeek = data.getData().get(data.getDates().get(0));
            Map<String, Float> lastWeek = data.getData().get(data.getDates().get(data.getDates().size()-1));

            if (firstWeek != null && lastWeek != null) {
                float firstWeekPace = getSafeFloat(firstWeek);
                float lastWeekPace = getSafeFloat(lastWeek);
                float weeklyPaceChange = firstWeekPace - lastWeekPace;

                if (weeklyPaceChange > 0) {
                    feedback.append(String.format("You've improved your pace by %s this week!",
                            formatPace(weeklyPaceChange)));
                }
            }
        }
    }

    private float getSafeFloat(Map<String, Float> map) {
        if (map == null) {
            return FeedbackActivity.DEFAULT_PACE;
        }
        Float value = map.get(FeedbackActivity.KEY_AVG_PACE);
        return value != null ? value : FeedbackActivity.DEFAULT_PACE;
    }

    private String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }

    private static class PerformanceData {
        private final Map<String, Map<String, Float>> data;
        private final List<String> dates;
        private final Map<String, Float> latestRun;
        private final Map<String, Float> previousRun;
        private final String errorMessage;

        public PerformanceData(String errorMessage) {
            this.data = null;
            this.dates = null;
            this.latestRun = null;
            this.previousRun = null;
            this.errorMessage = errorMessage;
        }

        public PerformanceData(Map<String, Map<String, Float>> data,
                               List<String> dates,
                               Map<String, Float> latestRun,
                               Map<String, Float> previousRun) {
            this.data = data;
            this.dates = dates;
            this.latestRun = latestRun;
            this.previousRun = previousRun;
            this.errorMessage = null;
        }

        public boolean isValid() {
            return errorMessage == null;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Map<String, Map<String, Float>> getData() {
            return data;
        }

        public List<String> getDates() {
            return dates;
        }

        public float getLatestPace() {
            return getSafeFloat(latestRun, KEY_AVG_PACE, DEFAULT_PACE);
        }

        public float getPreviousPace() {
            return getSafeFloat(previousRun, KEY_AVG_PACE, DEFAULT_PACE);
        }

        public float getLatestHeartRate() {
            return getSafeFloat(latestRun, KEY_AVG_HEART_RATE, DEFAULT_HEART_RATE);
        }

        public float getPreviousHeartRate() {
            return getSafeFloat(previousRun, KEY_AVG_HEART_RATE, DEFAULT_HEART_RATE);
        }

        private float getSafeFloat(Map<String, Float> map, String key, float defaultValue) {
            if (map == null || key == null) {
                return defaultValue;
            }
            Float value = map.get(key);
            return value != null ? value : defaultValue;
        }
    }
}