package com.example.smartmarathonrunningapp_project;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

    //  This class displays the running performance feedback for the runner, showing metrics like distance pace and effort level.
public class FeedbackActivity extends AppCompatActivity
{
    //  Logging TAGS
    private static final String TAG = "FeedbackActivity";
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_feedback);
        //  Initialising the UI components
        Button closeButton = findViewById(R.id.closeButton);
        //  Getting the performance data from the previous activities
        String performanceDataJson = getIntent().getStringExtra("performanceData");
        try
        {
            //  parse JSON into performanceData object
            PerformanceData performanceData = PerformanceData.fromJson(performanceDataJson);
            if (!performanceData.isEmpty())
            {
                updateFeedbackUI(performanceData);  //  Update the UI with valid data
            }
            else
            {
                TextView feedbackText = findViewById(R.id.feedbackText);
                feedbackText.setText("No running data available");
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error generating feedback", e);
            TextView feedbackText = findViewById(R.id.feedbackText);
            feedbackText.setText("Error generating feedback");
        }
        closeButton.setOnClickListener(v -> finish());
    }
    //  Updates all UI elements with performance data
    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateFeedbackUI(PerformanceData performanceData)
    {
        //  Get references to all Textview components
        TextView feedbackText = findViewById(R.id.feedbackText);
        TextView distanceView = findViewById(R.id.distanceValue);
        TextView paceView = findViewById(R.id.paceValue);
        TextView effortView = findViewById(R.id.effortValue);
        TextView streakView = findViewById(R.id.streakValue);
        TextView achievementView = findViewById(R.id.achievementText);
        //  Getting metrics from the most recent run
        Map<String, Float> currentRun = getMostRecentRun(performanceData);
        if (currentRun.isEmpty())
        {
            feedbackText.setText("No recent runs found");
            return;
        }

        // Generate and display the main feedback text
        String feedback = generateFeedback(currentRun, performanceData);
        feedbackText.setText(feedback);

        // Safely extract and display metrics with null checks
        Float distance = currentRun.get("distance");
        Float pace = currentRun.get("pace");
        Float trimp = currentRun.get("trimp");
        //  Displaying logic
        distanceView.setText(distance != null ? String.format(Locale.getDefault(), "%.1f km", distance / 1000) : "N/A");
        paceView.setText(pace != null ? formatPace(pace) + "/km" : "N/A");
        if (trimp != null)
        {
            String effortLevel = performanceData.getRelativeEffortLevel(trimp);
            effortView.setText(String.format(Locale.getDefault(),"%.0f (%s)", trimp, effortLevel));
        }
        else
        {
            effortView.setText("N/A");
        }
        int streak = performanceData.getCurrentStreak();
        streakView.setText(String.format(Locale.getDefault(),"%d-day streak", streak));
        // Achievement detection
        if (distance != null && distance > 20000)
        {
            achievementView.setText("Long run achievement!");
        }
        else if (pace != null && pace < 300)
        {
            achievementView.setText("Speed achievement!");
        }
        else
        {
            achievementView.setText("Keep going!");
        }
    }
    //  retrieves metrics from the most recent runs in the performance data
    private Map<String, Float> getMostRecentRun(PerformanceData performanceData)
    {
        //  Get all run data organised by week
        Map<String, Map<String, Map<String, Float>>> runData = performanceData.getRunData();
        if (runData == null || runData.isEmpty())
        {
            return Collections.emptyMap();
        }
        //  Find the latest week
        String latestWeek = Collections.max(runData.keySet());
        Map<String, Map<String, Float>> weekRuns = runData.get(latestWeek);
        if (weekRuns == null || weekRuns.isEmpty())
        {
            return Collections.emptyMap();
        }
        //  Find the latest day in the week
        String latestDay = Collections.max(weekRuns.keySet());
        Map<String, Float> run = weekRuns.get(latestDay);
        if (run == null)
        {
            return Collections.emptyMap();
        }

        // Calculate pace from average speed
        Float avgSpeed = run.get("average_speed");
        if (avgSpeed != null && !run.containsKey("pace"))
        {
            float paceSecPerKm = 1000 / avgSpeed;
            run.put("pace", paceSecPerKm);
        }
        return run;
    }
    //  Generates the feedback text summarizing the run
    private String generateFeedback(Map<String, Float> currentRun, PerformanceData performanceData)
    {
        StringBuilder feedback = new StringBuilder();
        // extract metrics with defaults for missing values
        Float distance = currentRun.get("distance");
        float distanceKm = (distance != null ? distance : 0f) / 1000f;
        Float pace = currentRun.get("pace");
        float paceSecPerKm = pace != null ? pace : 0f;
        Float trimpValue = currentRun.get("trimp");
        float trimp = trimpValue != null ? trimpValue : 0f;
        //  Build the feedback string
        feedback.append(String.format(Locale.getDefault(),"Nice steady %.1f km run! ", distanceKm));
        feedback.append(String.format(Locale.getDefault(),"You maintained a pace of %s/km.%n%n", formatPace(paceSecPerKm)));
        //  Add effort information
        String effortLevel = performanceData.getRelativeEffortLevel(trimp);
        feedback.append(String.format(Locale.getDefault(),"Relative Effort: %.0f (%s)%n", trimp, effortLevel));
        //  Add streak information
        int streak = performanceData.getCurrentStreak();
        feedback.append(String.format(Locale.getDefault(),"%d-day running streak%n%n", streak));
        //  Add training load advice
        PerformanceData.PerformanceMetrics metrics = performanceData.calculatePerformanceMetrics();
        if (metrics.acuteLoad / metrics.chronicLoad > 1.2f)
        {
            feedback.append("Great effort! Consider a recovery day soon.");
        }
        else
        {
            feedback.append("Keep up the consistency!");
        }
        return feedback.toString();
    }
    //  Formats the pace from seconds per KM to min:sec per KM
    private String formatPace(float seconds)
    {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }
}