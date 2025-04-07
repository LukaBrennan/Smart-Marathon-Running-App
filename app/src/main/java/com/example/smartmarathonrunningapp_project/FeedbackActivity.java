package com.example.smartmarathonrunningapp_project;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartmarathonrunningapp_project.processors.ProgressiveFitnessAnalyzer;
import com.example.smartmarathonrunningapp_project.processors.WeeklyReport;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {
    private StravaRepository stravaRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_feedback); // Using your existing dialog layout

        stravaRepository = new StravaRepository(); // Initialize repository
        TextView feedbackTextView = findViewById(R.id.dialogFeedbackTextView);

        // Handle both performance data and progressive reports
        StringBuilder combinedFeedback = new StringBuilder();

        // 1. Add performance data analysis if available
        String performanceDataJson = getIntent().getStringExtra("performanceData");
        if (performanceDataJson != null) {
            combinedFeedback.append(analyzePerformanceData(performanceDataJson));
            combinedFeedback.append("\n\n——————————————————————\n\n");
        }

        // 2. Add progressive fitness reports
        combinedFeedback.append(generateProgressiveReports());


        feedbackTextView.setText(combinedFeedback.toString());
    }

    private String analyzePerformanceData(String performanceDataJson) {
        StringBuilder analysis = new StringBuilder("Weekly Performance Analysis:\n\n");

        try {
            Type type = new TypeToken<Map<String, Map<String, Float>>>() {}.getType();
            Map<String, Map<String, Float>> weeklyData = new Gson().fromJson(performanceDataJson, type);

            if (weeklyData != null && !weeklyData.isEmpty()) {
                List<String> weeks = new ArrayList<>(weeklyData.keySet());
                Collections.sort(weeks);

                for (String week : weeks) {
                    analysis.append(week).append(":\n");
                    for (Map.Entry<String, Float> entry : weeklyData.get(week).entrySet()) {
                        String key = entry.getKey();
                        float value = entry.getValue();

                        analysis.append("  ").append(key).append(": ");

                        if (key.toLowerCase().contains("pace")) {
                            analysis.append(formatPace(value)).append("\n");
                        } else if (key.toLowerCase().contains("distance")) {
                            analysis.append(String.format(Locale.getDefault(), "%.2f km", value / 1000)).append("\n");
                        } else if (key.toLowerCase().contains("heart rate") || key.toLowerCase().contains("hr")) {
                            analysis.append(String.format(Locale.getDefault(), "%.1f bpm", value)).append("\n");
                        } else {
                            analysis.append(String.format(Locale.getDefault(), "%.1f", value)).append("\n");
                        }
                    }

                    analysis.append("\n");
                }
            }
        } catch (Exception e) {
            Log.e("FeedbackActivity", "Error parsing performance data", e);
            return "Could not analyze performance data";
        }

        return analysis.toString();
    }

    private String generateProgressiveReports() {
        StringBuilder reports = new StringBuilder("Progressive Fitness Report:\n\n");

        ProgressiveFitnessAnalyzer analyzer = new ProgressiveFitnessAnalyzer();
        List<WeeklyReport> weeklyReports = analyzer.generateWeeklyReports(
                stravaRepository.getCachedActivities() // Now using instance method
        );

        if (weeklyReports.isEmpty()) {
            return "No training data available for progressive analysis";
        }

        for (WeeklyReport report : weeklyReports) {
            reports.append(report.content).append("\n\n");
        }

        return reports.toString();
    }

    private String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d min/mile", minutes, secs);
    }
}