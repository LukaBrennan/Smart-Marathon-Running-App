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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView feedbackTextView = findViewById(R.id.feedbackTextView);

        String performanceDataJson = getIntent().getStringExtra("performanceData");
        Log.d("FeedbackActivity", "Received JSON: " + performanceDataJson);
        if (performanceDataJson != null)
        {
            Type type = new TypeToken<Map<String, Map<String, Float>>>() {}.getType();
            Map<String, Map<String, Float>> weeklyData = new Gson().fromJson(performanceDataJson, type);
            Log.d("FeedbackActivity", "Total weeks: " + weeklyData.size());

            if (weeklyData != null && !weeklyData.isEmpty())
            {
                StringBuilder feedbackText = new StringBuilder("Weekly Running Performance Analysis:\n\n");

                // Sort weeks chronologically
                List<String> weeks = new ArrayList<>(weeklyData.keySet());
                Collections.sort(weeks);

                Map<String, Float> weeklyAverages = new LinkedHashMap<>();

                // Calculate weekly averages
                for (String week : weeks)
                {
                    Map<String, Float> weekData = weeklyData.get(week);
                    Log.d("FeedbackActivity", "Processing week: " + week);
                    float totalPace = 0;
                    int count = 0;

                    feedbackText.append(week).append(":\n");
                    for (Map.Entry<String, Float> entry : weekData.entrySet())
                    {
                        feedbackText.append("  ")
                                .append(entry.getKey())
                                .append(": ")
                                .append(formatPace(entry.getValue()))
                                .append("\n");
                        totalPace += entry.getValue();
                        count++;
                    }
                    float avgPace = totalPace / count;
                    weeklyAverages.put(week, avgPace);
                    feedbackText.append("  Weekly Average: ")
                            .append(formatPace(avgPace))
                            .append("\n\n");
                }

                // Add progressive trend analysis
                feedbackText.append("\nWeekly Trend Analysis:\n");
                for (int i = 1; i < weeks.size(); i++)
                {
                    String currentWeek = weeks.get(i);
                    String prevWeek = weeks.get(i-1);

                    float currentAvg = weeklyAverages.get(currentWeek);
                    float prevAvg = weeklyAverages.get(prevWeek);
                    float difference = prevAvg - currentAvg; // positive means improvement

                    feedbackText.append(currentWeek).append(" vs ").append(prevWeek).append(": ");

                    if (difference > 0)
                    {
                        feedbackText.append("Improved by ")
                                .append(formatPaceDifference(difference))
                                .append("\n");
                    }
                    else if (difference < 0)
                    {
                        feedbackText.append("Declined by ")
                                .append(formatPaceDifference(-difference))
                                .append("\n");
                    }
                    else
                    {
                        feedbackText.append("No change\n");
                    }
                }

                // Add overall trend if we have enough data
                if (weeks.size() > 1)
                {
                    float firstWeekAvg = weeklyAverages.get(weeks.get(0));
                    float lastWeekAvg = weeklyAverages.get(weeks.get(weeks.size()-1));
                    float overallDifference = firstWeekAvg - lastWeekAvg;

                    feedbackText.append("\nOverall Trend: ");
                    if (overallDifference > 0)
                    {
                        feedbackText.append("You've improved by ")
                                .append(formatPaceDifference(overallDifference))
                                .append(" since ").append(weeks.get(0));
                    }
                    else if (overallDifference < 0)
                    {
                        feedbackText.append("You've declined by ")
                                .append(formatPaceDifference(-overallDifference))
                                .append(" since ").append(weeks.get(0));
                    }
                    else
                    {
                        feedbackText.append("No overall change since ").append(weeks.get(0));
                    }
                }

                feedbackTextView.setText(feedbackText.toString());
            }
        }
    }

    private String formatPace(float seconds)
    {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d min/mile", minutes, secs);
    }

    private String formatPaceDifference(float difference)
    {
        int minutes = (int) (difference / 60);
        int secs = (int) (difference % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }
    // Code needs to be redone, Need to better the filtering and classifying of runs for better trend analysis.
//    private String classifyRun(Activity activity) {
//        // Check workout type first
//        if (activity.getName().toLowerCase().contains("strides") activity.getName().toLowerCase().contains("plyo"))
//        {
//            return "Speedwork";
//        }
//        if (activity.getName().toLowerCase().contains("x") || // e.g. "4 x 1200"
//                activity.getName().toLowerCase().contains("interval"))
//                {
//            return "Interval";
//        }
//        // Then check distance
//        float distanceKm = activity.getDistance()/1000;
//        if (distanceKm < 5) return "Recovery";
//        else if (distanceKm < 10) return "Medium";
//        else if (distanceKm < 15) return "Long";
//        else return "VeryLong";
//    }
}