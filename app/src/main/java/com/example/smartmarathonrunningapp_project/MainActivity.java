package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.smartmarathonrunningapp_project.managers.TrainingPlanManager;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import java.util.*;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private StravaRepository stravaRepository;
    private TrainingPlanManager planManager;
    private AutoAdjuster autoAdjuster;
    private final PerformanceData performanceData = new PerformanceData();

    private int COLOR_GREEN, COLOR_YELLOW, COLOR_RED, COLOR_GRAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initColors();
        initManagers();
        setupUI();
        fetchActivities();
    }

    private void initColors() {
        COLOR_GREEN = ContextCompat.getColor(this, R.color.traffic_light_green);
        COLOR_YELLOW = ContextCompat.getColor(this, R.color.traffic_light_yellow);
        COLOR_RED = ContextCompat.getColor(this, R.color.traffic_light_red);
        COLOR_GRAY = ContextCompat.getColor(this, R.color.traffic_light_gray);
    }

    private void initManagers() {
        stravaRepository = new StravaRepository();
        planManager = new TrainingPlanManager(this);
        autoAdjuster = new AutoAdjuster();
    }

    private void setupUI() {
        findViewById(R.id.feedbackButton).setOnClickListener(v -> {
            if (performanceData.isEmpty()) {
                Toast.makeText(this, "Fetching data. Please wait...", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, FeedbackActivity.class);
                intent.putExtra("performanceData", performanceData.toJson());
                startActivity(intent);
            }
        });
    }

    private void fetchActivities() {
        stravaRepository.refreshAccessToken(new Callback<TokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    stravaRepository.fetchActivities(response.body().getAccessToken(), 1, 30, new ActivityFetchCallback());
                } else {
                    Log.e(TAG, "Token refresh failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Token refresh failed", t);
            }
        });
    }

    private class ActivityFetchCallback implements Callback<List<Activity>> {
        @Override
        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
            if (response.isSuccessful() && response.body() != null) {
                handleActivities(response.body());
            } else {
                Log.e(TAG, "Failed to fetch activities. Code: " + response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
            Log.e(TAG, "Failed to fetch activities", t);
        }

        private void handleActivities(List<Activity> activities) {
            TrainingPlan currentPlan = planManager.loadAdjustedPlan();
            performanceData.clear();

            List<Activity> validActivities = activities.stream()
                    .filter(a -> a.getDistance() > 100 && a.getMoving_time() > 60)
                    .peek(performanceData::addActivity)
                    .collect(Collectors.toList());

            if (!validActivities.isEmpty()) {
                Map<TrainingPlan.Day, String> lights = calculateTrafficLights(currentPlan, validActivities);
                TrainingPlan adjusted = autoAdjuster.adjustPlan(currentPlan, validActivities, lights);
                planManager.saveAdjustedPlan(adjusted);
                currentPlan = adjusted;
            }

            updateUI(currentPlan);
        }
    }

    private void updateUI(TrainingPlan plan) {
        LinearLayout weekContainer = findViewById(R.id.weekContainer);
        weekContainer.removeAllViews();
        setupWeekStats();

        if (plan.getTraining_weeks() != null) {
            for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks()) {
                addWeekView(weekContainer, week);
                for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                    addDayView(weekContainer, day);
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void addDayView(LinearLayout container, TrainingPlan.Day day) {
        if (day == null) return;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(this, R.drawable.day_item_bg));
        layout.setPadding(16, 12, 16, 12);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Traffic Light
        View trafficLight = new View(this);
        trafficLight.setLayoutParams(new LinearLayout.LayoutParams(24, 24));
        trafficLight.setBackgroundResource(R.drawable.ic_circle_background);
        updateTrafficLightColor(trafficLight, determineTrafficLight(day));
        layout.addView(trafficLight);

        layout.addView(makeText(day.getDayOfWeek(), 16, true));
        layout.addView(makeText(day.getExercise(), 14, false));
        layout.addView(makeText(day.getDistance() + " @ " + day.getPace(), 14, false));

        if (day.getAdjustmentNote() != null) {
            TextView note = makeText("Adjusted: " + day.getAdjustmentNote(), 12, false);
            note.setTextColor(ContextCompat.getColor(this, R.color.strava_orange));
            layout.addView(note);
        }

        container.addView(layout);
    }

    private TextView makeText(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextColor(ContextCompat.getColor(this, R.color.strava_dark_text));
        return view;
    }
    private String determineTrafficLight(TrainingPlan.Day day) {
        if (day == null || day.getDayOfWeek() == null) return "N/A";

        for (Map<String, Map<String, Float>> runs : performanceData.getRunData().values()) {
            for (Map.Entry<String, Map<String, Float>> entry : runs.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.endsWith(day.getDayOfWeek())) {
                    Map<String, Float> run = entry.getValue();
                    if (run == null) continue;

                    Activity act = new Activity();
                    act.setDistance(run.getOrDefault("distance", 0f));
                    act.setMoving_time(run.getOrDefault("moving_time", 0f).intValue());
                    act.setAverage_heartrate(run.getOrDefault("heart_rate", 0f));
                    act.setMax_heartrate(run.getOrDefault("max_heartrate", 0f));
                    return TrainingPlan.getTrafficLightStatus(day, act);
                }
            }
        }

        return "N/A";
    }
    private void updateTrafficLightColor(View view, String status) {
        int color = COLOR_GRAY;
        switch (status) {
            case "GREEN": color = COLOR_GREEN; break;
            case "YELLOW": color = COLOR_YELLOW; break;
            case "RED": color = COLOR_RED; break;
        }
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void setupWeekStats() {
        LinearLayout stats = findViewById(R.id.weekStatsContainer);
        stats.removeAllViews();

        List<String> weeks = new ArrayList<>(performanceData.getRunData().keySet());
        weeks.sort(Collections.reverseOrder());

        if (weeks.isEmpty()) {
            addStat(stats, "0 km", "Distance");
            addStat(stats, "--", "Avg Pace");
            addStat(stats, "--", "Avg HR");
            return;
        }

        String week = weeks.get(0);
        addStat(stats, String.format("%.1f km", performanceData.getWeeklyDistance(week) / 1000), "Distance");
        addStat(stats, DailyFeedbackGenerator.formatPace(performanceData.getWeeklyAvgPace(week)) + "/km", "Avg Pace");
        addStat(stats, String.format("%.0f bpm", performanceData.getWeeklyAvgHR(week)), "Avg HR");
    }

    private void addStat(LinearLayout container, String value, String label) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        layout.addView(makeText(value, 18, true));
        layout.addView(makeText(label, 12, false));
        container.addView(layout);
    }

    private void addWeekView(LinearLayout container, TrainingPlan.TrainingWeek week) {
        TextView title = makeText("Week " + week.getWeek(), 18, true);
        title.setTextColor(ContextCompat.getColor(this, R.color.strava_orange));
        container.addView(title);
    }

    private List<TrainingPlan.Day> getDaysOfWeek(TrainingPlan.TrainingWeek week) {
        return Arrays.asList(
                week.getTraining_plan().getMonday(),
                week.getTraining_plan().getTuesday(),
                week.getTraining_plan().getWednesday(),
                week.getTraining_plan().getThursday(),
                week.getTraining_plan().getFriday(),
                week.getTraining_plan().getSaturday(),
                week.getTraining_plan().getSunday()
        );
    }
    private Map<TrainingPlan.Day, String> calculateTrafficLights(TrainingPlan plan, List<Activity> activities) {
        Map<TrainingPlan.Day, String> statusMap = new HashMap<>();
        if (plan == null || activities == null) return statusMap;

        for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks()) {
            for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                if (day == null || day.getDate() == null) continue;
                String plannedDay = DateUtils.getDayName(day.getDate());

                for (Activity act : activities) {
                    if (act.getStart_date() == null) continue;
                    String actualDay = DateUtils.getDayName(act.getStart_date());
                    if (plannedDay.equalsIgnoreCase(actualDay)) {
                        statusMap.put(day, TrainingPlan.getTrafficLightStatus(day, act));
                        break;
                    }
                }
            }
        }
        return statusMap;
    }
}

