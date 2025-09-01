package com.example.smartmarathonrunningapp_project;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
/*
    Displays feedback UI for the athlete’s most recent run.
    Shows metrics: distance, pace, TRIMP effort, HR, streak, achievements.
*/
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
            if (performanceData != null && !performanceData.isEmpty()) {
                updateFeedbackUI(performanceData);
            } else {
                ((TextView) findViewById(R.id.feedbackText)).setText("No running data available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating feedback", e);
            ((TextView) findViewById(R.id.feedbackText)).setText("Error generating feedback");
        }

        closeButton.setOnClickListener(v -> finish());
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateFeedbackUI(PerformanceData performanceData) {
        TextView feedbackText    = findViewById(R.id.feedbackText);
        TextView distanceView    = findViewById(R.id.distanceValue);
        TextView paceView        = findViewById(R.id.paceValue);
        TextView effortView      = findViewById(R.id.effortValue);
        TextView streakView      = findViewById(R.id.streakValue);
        TextView achievementView = findViewById(R.id.achievementText);
        TextView avgHRView       = findViewById(R.id.avgHRValue);
        TextView maxHRView       = findViewById(R.id.maxHRValue);

        Map<String, Float> currentRun = getMostRecentRun(performanceData);
        if (currentRun.isEmpty()) {
            feedbackText.setText("No recent runs found");
            distanceView.setText("—");
            paceView.setText("—");
            effortView.setText("—");
            avgHRView.setText("—");
            maxHRView.setText("—");
            streakView.setText("0-day streak");
            achievementView.setText("Keep going!");
            return;
        }

        String feedback = generateFeedback(currentRun, performanceData);
        feedbackText.setText(feedback);

        Float distance = currentRun.get("distance");
        Float paceSec  = currentRun.get("pace");
        Float trimp    = currentRun.get("trimp");
        Float avgHR    = currentRun.get("heart_rate");
        Float maxHR    = currentRun.get("max_heartrate");

        distanceView.setText(distance != null && distance > 0
                ? String.format(Locale.getDefault(), "%.1f km", distance / 1000f)
                : "—");

        paceView.setText(formatPaceOrDash(paceSec));

        avgHRView.setText(avgHR != null && avgHR > 0
                ? String.format(Locale.getDefault(), "%.0f bpm", avgHR)
                : "—");

        maxHRView.setText(maxHR != null && maxHR > 0
                ? String.format(Locale.getDefault(), "%.0f bpm", maxHR)
                : "—");

        if (trimp != null && trimp > 0) {
            String level = performanceData.getRelativeEffortLevel(trimp);
            effortView.setText(String.format(Locale.getDefault(), "%.0f (%s)", trimp, level));
        } else {
            effortView.setText("—");
        }

        int streak = computeStreakFromPerformanceData(performanceData);
        streakView.setText(String.format(Locale.getDefault(), "%d-day streak", streak));

        if (distance != null && distance >= 20000f) {
            achievementView.setText("Long run achievement!");
        } else if (paceSec != null && paceSec > 0 && paceSec < 300f) {
            achievementView.setText("Speed achievement!");
        } else {
            achievementView.setText("Keep going!");
        }
    }

    private Map<String, Float> getMostRecentRun(PerformanceData performanceData) {
        Map<String, Map<String, Map<String, Float>>> runData = performanceData.getRunData();
        if (runData == null || runData.isEmpty()) return Collections.emptyMap();

        String latestWeek = Collections.max(runData.keySet());
        Map<String, Map<String, Float>> weekRuns = runData.get(latestWeek);
        if (weekRuns == null || weekRuns.isEmpty()) return Collections.emptyMap();

        String latestKey = Collections.max(weekRuns.keySet());
        Map<String, Float> run = weekRuns.get(latestKey);
        if (run == null) return Collections.emptyMap();

        Float pace = run.get("pace");
        if (pace == null || pace <= 0f) {
            Float avgSpeed = run.get("average_speed");
            if (avgSpeed != null && avgSpeed > 0f) {
                run.put("pace", 1000f / avgSpeed);
            } else {
                Float moving = run.get("moving_time");
                Float dist   = run.get("distance");
                if (moving != null && moving > 0f && dist != null && dist > 0f) {
                    run.put("pace", moving / (dist / 1000f));
                }
            }
        }
        return run;
    }

    private String generateFeedback(Map<String, Float> currentRun, PerformanceData performanceData) {
        StringBuilder out = new StringBuilder();

        Float distanceM = currentRun.get("distance");
        Float paceSec   = currentRun.get("pace");
        Float trimp     = currentRun.get("trimp");

        float km = distanceM != null ? (distanceM / 1000f) : 0f;
        out.append(String.format(Locale.getDefault(), "Nice steady %.1f km run! ", km));

        if (paceSec != null && paceSec > 0) {
            out.append(String.format(Locale.getDefault(),
                    "You maintained a pace of %s/km.\n\n", formatPace(paceSec)));
        } else {
            out.append("\n");
        }

        if (trimp != null && trimp > 0) {
            String level = performanceData.getRelativeEffortLevel(trimp);
            out.append(String.format(Locale.getDefault(),
                    "Relative Effort: %.0f (%s)\n", trimp, level));
        }

        int streak = computeStreakFromPerformanceData(performanceData);
        out.append(String.format(Locale.getDefault(), "%d-day running streak\n\n", streak));

        PerformanceData.PerformanceMetrics m = performanceData.calculatePerformanceMetrics();
        if (m != null && m.chronicLoad > 0 && (m.acuteLoad / m.chronicLoad) > 1.2f) {
            out.append("Great effort! Consider a recovery day soon.");
        } else {
            out.append("Keep up the consistency!");
        }
        return out.toString();
    }

    private String formatPaceOrDash(Float secPerKm) {
        if (secPerKm == null || secPerKm <= 0f || secPerKm.isNaN() || secPerKm.isInfinite()) return "—";
        return formatPace(secPerKm) + "/km";
    }

    private String formatPace(float seconds) {
        int m = (int) (seconds / 60f);
        int s = Math.max(0, Math.min(59, Math.round(seconds - m * 60f)));
        return String.format(Locale.getDefault(), "%d:%02d", m, s);
    }

    private int computeStreakFromPerformanceData(PerformanceData pd) {
        Map<String, Map<String, Map<String, Float>>> runData = pd.getRunData();
        if (runData == null || runData.isEmpty()) return 0;

        Set<String> runDays = new HashSet<>();
        for (Map<String, Map<String, Float>> week : runData.values()) {
            if (week == null) continue;
            for (String key : week.keySet()) {
                if (key != null && key.length() >= 10) {
                    runDays.add(key.substring(0, 10));
                }
            }
        }
        if (runDays.isEmpty()) return 0;

        String cur = DateUtils.maxYmd(runDays);
        int streak = 0;
        while (cur != null && runDays.contains(cur)) {
            streak++;
            cur = DateUtils.addDays(cur, -1);
        }
        return streak;
    }
}
