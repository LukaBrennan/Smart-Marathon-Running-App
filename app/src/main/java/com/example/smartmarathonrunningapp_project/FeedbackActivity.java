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

        try {
            String feedback = generatePersonalizedFeedback(performanceDataJson);
            feedbackTextView.setText(feedback);
        } catch (Exception e) {
            feedbackTextView.setText("Error generating feedback");
            Log.e("FeedbackActivity", "Error", e);
        }
    }

    private String generatePersonalizedFeedback(String json) {
        // Parse the JSON data
        Type type = new TypeToken<Map<String, Map<String, Float>>>(){}.getType();
        Map<String, Map<String, Float>> data = new Gson().fromJson(json, type);

        // Get the two most recent runs
        List<String> dates = new ArrayList<>(data.keySet());
        Collections.sort(dates);

        if (dates.size() < 2) {
            return "Need at least 2 runs to compare";
        }

        String latestDate = dates.get(dates.size()-1);
        String previousDate = dates.get(dates.size()-2);

        Map<String, Float> latestRun = data.get(latestDate);
        Map<String, Float> previousRun = data.get(previousDate);

        // Calculate differences
        float paceDiff = previousRun.get("Avg Pace (min/mile)") - latestRun.get("Avg Pace (min/mile)");
        float hrDiff = previousRun.get("Avg Heart Rate") - latestRun.get("Avg Heart Rate");

        // Generate feedback message
        StringBuilder feedback = new StringBuilder();

        // 1. Personalized greeting with emoji
        feedback.append("🏃‍♂️ Great job on your run!\n\n");

        // 2. Pace comparison
        if (paceDiff > 0) {
            feedback.append(String.format(Locale.getDefault(),
                    "⭐ Your pace improved by %s compared to last run!\n",
                    formatPace(paceDiff)));
        } else if (paceDiff < 0) {
            feedback.append(String.format(Locale.getDefault(),
                    "🔹 Your pace was %s slower this time - maybe try some intervals next run?\n",
                    formatPace(Math.abs(paceDiff))));
        } else {
            feedback.append("↔ Your pace was consistent with your last run\n");
        }

        // 3. Heart rate analysis
        if (hrDiff > 5) {
            feedback.append("❤️  Your heart rate dropped significantly - great cardio efficiency!\n");
        } else if (hrDiff > 0) {
            feedback.append("❤️  Your heart rate was slightly lower - good endurance building\n");
        } else if (hrDiff < -5) {
            feedback.append("⚠️  Your heart rate was higher - consider more recovery time\n");
        } else {
            feedback.append("❤️  Your heart rate was stable\n");
        }

        // 4. Weekly summary
        if (dates.size() >= 7) {
            feedback.append("\n📊 Weekly Summary:\n");

            float weeklyPaceChange = data.get(dates.get(0)).get("Avg Pace (min/mile)") -
                    data.get(dates.get(dates.size()-1)).get("Avg Pace (min/mile)");

            if (weeklyPaceChange > 0) {
                feedback.append(String.format("You've improved your pace by %s this week!",
                        formatPace(weeklyPaceChange)));
            }
        }

        return feedback.toString();
    }

    private String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }
}