package com.example.smartmarathonrunningapp_project;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Map;
public class FeedbackActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView feedbackTextView = findViewById(R.id.feedbackTextView);
        // Get performance data from the Intent
        String performanceDataJson = getIntent().getStringExtra("performanceData");
        if (performanceDataJson != null)
        {
            // Convert JSON string back to a Map of performance data
            Type type = new TypeToken<Map<String, Float>>() {}.getType();
            Map<String, Float> performanceData = new Gson().fromJson(performanceDataJson, type);
            // Create feedback text to display
            StringBuilder feedbackText = new StringBuilder("Fitness Trends:\n");
            for (Map.Entry<String, Float> entry : performanceData.entrySet())
            {
                feedbackText.append(entry.getKey()).append(": ")
                        .append(formatTrend(entry.getValue())).append("\n");
            }
            // Set the feedback text to the TextView
            feedbackTextView.setText(feedbackText.toString());
        }
    }
    // Format the trend value into a human-readable form
    private String formatTrend(float value)
    {
        if (value < 0) return "Improving";
        if (value > 0) return "Stable/Declining";
        return "Neutral";
    }
}
