package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartmarathonrunningapp_project.managers.TrainingPlanManager;
import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private StravaRepository stravaRepository;
    private TrainingPlanManager planManager;
    private final Map<String, Map<String, Float>> performanceData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stravaRepository = new StravaRepository();
        initializeDependencies();
        setupUI();
        fetchAndCheckActivities();
    }

    private void initializeDependencies() {
        planManager = new TrainingPlanManager(this);
    }

    private void setupUI() {
        Button btnFeedback = findViewById(R.id.feedbackButton);
        btnFeedback.setOnClickListener(v -> {
            if (performanceData.isEmpty()) {
                Toast.makeText(this, "Fetching latest data. Please wait a moment.", Toast.LENGTH_SHORT).show();
            } else {
                launchFeedbackActivity();
            }
        });
    }

    private void launchFeedbackActivity() {
        Log.d(TAG, "Sending performance data: " + new Gson().toJson(performanceData));
        Intent intent = new Intent(this, FeedbackActivity.class);
        intent.putExtra("performanceData", new Gson().toJson(performanceData));
        startActivity(intent);
    }

    private void fetchAndCheckActivities() {
        stravaRepository.refreshAccessToken(new TokenRefreshCallback());
    }

    private final class TokenRefreshCallback implements Callback<TokenResponse> {
        @Override
        public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
            if (response.isSuccessful() && response.body() != null) {
                stravaRepository.fetchActivities(response.body().getAccessToken(), 1, 100, new ActivitiesCallback());
            } else {
                Log.e(TAG, "Token refresh failed. Code: " + response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
            Log.e(TAG, "Token refresh failed", t);
        }
    }

    private final class ActivitiesCallback implements Callback<List<Activity>> {
        @Override
        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
            if (response.isSuccessful() && response.body() != null) {
                processFetchedActivities(response.body());
            } else {
                Log.e(TAG, "Failed to fetch activities. Code: " + response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
            Log.e(TAG, "Failed to fetch activities", t);
        }

        private void processFetchedActivities(List<Activity> activities) {
            TrainingPlan trainingPlan = planManager.loadTrainingPlanFromAssets();
            if (trainingPlan == null) {
                Log.e(TAG, "Failed to load training plan");
                return;
            }
            generatePerformanceData(activities);
            updateUI(trainingPlan);
        }

        @SuppressLint("SetTextI18n")
        private void updateUI(TrainingPlan trainingPlan) {
            LinearLayout weekContainer = findViewById(R.id.weekContainer);
            weekContainer.removeAllViews();
            for (TrainingPlan.TrainingWeek week : trainingPlan.getTraining_weeks()) {
                addWeekView(weekContainer, week);
                addDayViews(weekContainer, week);
            }
        }

        @SuppressLint("SetTextI18n")
        private void addWeekView(LinearLayout container, TrainingPlan.TrainingWeek week) {
            TextView weekTextView = new TextView(MainActivity.this);
            weekTextView.setText("Week: " + week.getWeek());
            weekTextView.setTextSize(18);
            weekTextView.setPadding(0, 16, 0, 8);
            container.addView(weekTextView);
        }

        @SuppressLint("SetTextI18n")
        private void addDayViews(LinearLayout container, TrainingPlan.TrainingWeek week) {
            addDayView(container, week.getTraining_plan().getMonday(), "Monday");
            addDayView(container, week.getTraining_plan().getTuesday(), "Tuesday");
            addDayView(container, week.getTraining_plan().getWednesday(), "Wednesday");
            addDayView(container, week.getTraining_plan().getThursday(), "Thursday");
            addDayView(container, week.getTraining_plan().getFriday(), "Friday");
            addDayView(container, week.getTraining_plan().getSaturday(), "Saturday");
            addDayView(container, week.getTraining_plan().getSunday(), "Sunday");
        }

        @SuppressLint("SetTextI18n")
        private void addDayView(LinearLayout container, TrainingPlan.Day day, String dayName) {
            if (day != null) {
                TextView dayTextView = new TextView(MainActivity.this);
                dayTextView.setText(dayName + ": " + day.getExercise() + " - " +
                        day.getDistance() + " @ " + day.getPace());
                dayTextView.setTextSize(16);
                dayTextView.setPadding(16, 8, 16, 8);
                container.addView(dayTextView);
            }
        }

        private void generatePerformanceData(List<Activity> activities) {
            performanceData.clear();
            if (activities == null || activities.isEmpty()) return;

            Map<String, List<Activity>> weeklyActivities = groupActivitiesByWeek(activities);
            calculateWeeklyMetrics(weeklyActivities);
        }

        private Map<String, List<Activity>> groupActivitiesByWeek(List<Activity> activities) {
            Map<String, List<Activity>> weeklyActivities = new TreeMap<>();
            for (Activity activity : activities) {
                try {
                    if (isValidActivity(activity)) {
                        String week = DateUtils.getWeekOfYear(activity.getStart_date());
                        weeklyActivities.computeIfAbsent(week, k -> new ArrayList<>()).add(activity);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing activity: " + activity.getName(), e);
                }
            }
            return weeklyActivities;
        }

        private boolean isValidActivity(Activity activity) {
            return activity.getDistance() > 100 && activity.getMoving_time() > 60;
        }

        private void calculateWeeklyMetrics(Map<String, List<Activity>> weeklyActivities) {
            for (Map.Entry<String, List<Activity>> entry : weeklyActivities.entrySet()) {
                Map<String, Float> weekMetrics = calculateMetricsForWeek(entry.getValue());
                if (!weekMetrics.isEmpty()) {
                    performanceData.put(entry.getKey(), weekMetrics);
                }
            }
        }

        private Map<String, Float> calculateMetricsForWeek(List<Activity> weekRuns) {
            Map<String, Float> weekMetrics = new HashMap<>();
            ActivityMetrics metrics = calculateAggregateMetrics(weekRuns);

            if (metrics.validRuns > 0) {
                float avgPace = metrics.totalTime / UnitConverter.metersToMiles(metrics.totalDistance);
                float avgDistance = metrics.totalDistance / metrics.validRuns;
                float avgHeartRate = metrics.totalHeartRate > 0 ?
                        metrics.totalHeartRate / metrics.validRuns : 0;

                weekMetrics.put("Avg Pace (min/mile)", avgPace);
                weekMetrics.put("Avg Distance (m)", avgDistance);
                weekMetrics.put("Avg Heart Rate", avgHeartRate);
                weekMetrics.put("Total Runs", (float) metrics.validRuns);
            }
            return weekMetrics;
        }

        private ActivityMetrics calculateAggregateMetrics(List<Activity> weekRuns) {
            ActivityMetrics metrics = new ActivityMetrics();
            for (Activity run : weekRuns) {
                if (run.getDistance() > 0 && run.getMoving_time() > 0) {
                    metrics.totalDistance += run.getDistance();
                    metrics.totalTime += run.getMoving_time();
                    if (run.getAverage_heartrate() > 0) {
                        metrics.totalHeartRate += run.getAverage_heartrate();
                    }
                    metrics.validRuns++;
                }
            }
            return metrics;
        }

        private class ActivityMetrics {
            float totalDistance = 0;
            float totalTime = 0;
            float totalHeartRate = 0;
            int validRuns = 0;
        }
    }
}




