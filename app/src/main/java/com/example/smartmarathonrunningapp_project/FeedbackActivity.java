package com.example.smartmarathonrunningapp_project;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_feedback);

        TextView feedbackTextView = findViewById(R.id.dialogFeedbackTextView);
        String performanceDataJson = getIntent().getStringExtra("performanceData");

        Log.d("FeedbackActivity", "Raw JSON: " + performanceDataJson); // Add this line

        if (performanceDataJson != null && !performanceDataJson.isEmpty()) {
            try {
                String feedback = buildFeedbackString(performanceDataJson);
                feedbackTextView.setText(feedback);
                Log.d("FeedbackActivity", "Feedback displayed successfully");
            } catch (Exception e) {
                Log.e("FeedbackActivity", "Error building feedback", e);
                feedbackTextView.setText("Error displaying feedback\n" + e.getMessage());
            }
        } else {
            Log.e("FeedbackActivity", "No performance data received");
            feedbackTextView.setText("No performance data available");
        }
    }

    private String buildFeedbackString(String json) throws Exception {
        StringBuilder sb = new StringBuilder();

        // 1. Parse the JSON
        Type type = new TypeToken<Map<String, Map<String, Float>>>(){}.getType();
        Map<String, Map<String, Float>> data = new Gson().fromJson(json, type);

        // 2. Weekly Analysis
        sb.append("Weekly Performance Analysis\n\n");
        List<String> weeks = new ArrayList<>(data.keySet());
        Collections.sort(weeks);

        for (String week : weeks) {
            Map<String, Float> metrics = data.get(week);
            sb.append(week).append(":\n")
                    .append("• Pace: ").append(formatPace(metrics.get("Avg Pace (min/mile)"))).append("\n")
                    .append("• Distance: ").append(String.format(Locale.US, "%.1f km", metrics.get("Avg Distance (m)") / 1000)).append("\n")
                    .append("• Heart Rate: ").append(String.format(Locale.US, "%.0f bpm", metrics.get("Avg Heart Rate"))).append("\n")
                    .append("• Runs: ").append(metrics.get("Total Runs").intValue()).append("\n\n");
        }

        // 3. Trend Analysis (if enough data)
        if (weeks.size() >= 2) {
            sb.append("Trend Analysis\n\n");
            Map<String, Float> first = data.get(weeks.get(0));
            Map<String, Float> last = data.get(weeks.get(weeks.size()-1));

            float paceDiff = last.get("Avg Pace (min/mile)") - first.get("Avg Pace (min/mile)");
            float distDiff = (last.get("Avg Distance (m)") - first.get("Avg Distance (m)")) / 1000;

            sb.append(String.format(Locale.US,
                    "From %s to %s:\n" +
                            "• Pace %s by %s\n" +
                            "• Distance %s by %.1f km",
                    weeks.get(0), weeks.get(weeks.size()-1),
                    paceDiff < 0 ? "improved" : "declined",
                    formatPace(Math.abs(paceDiff)),
                    distDiff > 0 ? "increased" : "decreased",
                    Math.abs(distDiff)
            ));
        }

        return sb.toString();
    }

    private String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }
}