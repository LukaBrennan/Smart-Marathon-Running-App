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
import com.example.smartmarathonrunningapp_project.processors.ActivityProcessor;
import com.google.gson.Gson;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.example.smartmarathonrunningapp_project.utils.PaceUtils;

    // Main activity that coordinates training plan tracking and Strava integration
public class MainActivity extends AppCompatActivity {
        private static final String TAG = "MainActivity";
        // Dependencies
        private StravaRepository stravaRepository;
        private TrainingPlanManager planManager;
        private ActivityProcessor activityProcessor;
        private final Map<String, Map<String, Float>> performanceData = new HashMap<>();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            stravaRepository = new StravaRepository(this);
            initializeDependencies();
            setupUI();
            fetchAndCheckActivities();
        }

        private void initializeDependencies() {
            //stravaRepository = new StravaRepository(this);
            planManager = new TrainingPlanManager(this);
            activityProcessor = new ActivityProcessor();
            stravaRepository.loadLocalActivities(this);
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
            Log.d(TAG, "Sending performance data: " + performanceData);
            Log.d("MainActivity", "Sending performance data: " + new Gson().toJson(performanceData));
            Intent intent = new Intent(this, FeedbackActivity.class);
            intent.putExtra("performanceData", new Gson().toJson(performanceData));
            startActivity(intent);
        }

        private void fetchAndCheckActivities() {
            stravaRepository.refreshAccessToken(new TokenRefreshCallback());
        }

        private void processActivity(Activity activity, TrainingPlan trainingPlan) {
            String dayOfWeek = DateUtils.getDayOfWeek(activity.getStart_date());
            TrainingPlan.TrainingWeek firstWeek = trainingPlan.getTraining_weeks().get(0);
            TrainingPlan.Day day = TrainingPlanManager.getDayByName(firstWeek, dayOfWeek);

            if (day != null && activityProcessor.activityMatchesPlan(activity, day)) {
                day.setCompleted(true);
                trackPerformance(activity, day, firstWeek.getWeek(), dayOfWeek);
            }
        }

        private void trackPerformance(Activity activity, TrainingPlan.Day day, String week, String dayName) {
            float activityDistanceMiles = UnitConverter.metersToMiles(activity.getDistance());
            float activityTime = activity.getMoving_time();
            float averagePaceSecPerMile = activityTime / activityDistanceMiles;
            performanceData
                    .computeIfAbsent("Week " + week, k -> new HashMap<>())
                    .put(dayName, averagePaceSecPerMile);
            adjustPaceForHeartRate(activity, day);
        }

        private void adjustPaceForHeartRate(Activity activity, TrainingPlan.Day day) {
            if (activity.getAverage_heartrate() > 160) {
                float requiredPace = PaceUtils.convertPaceToSeconds(day.getPace());
                float newPace = requiredPace * 1.05f;
                day.setPace(PaceUtils.convertSecondsToPace((int) newPace));
            }
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
            TextView weekTextView = new TextView(this);
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
                TextView dayTextView = new TextView(this);
                dayTextView.setText(dayName + ": " + day.getExercise() + " - " +
                        day.getDistance() + " @ " + day.getPace());
                dayTextView.setTextSize(16);
                dayTextView.setPadding(16, 8, 16, 8);
                container.addView(dayTextView);
            }
        }

        //Handles token refresh response and initiates activities fetch
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

        // Handles activities fetch response and processes the data
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

                // Generate performance data
                generatePerformanceData(activities);

                updateUI(trainingPlan);
            }

            private void generatePerformanceData(List<Activity> activities) {
                performanceData.clear();
                if (activities == null || activities.isEmpty()) return;

                // Group by week
                Map<String, List<Activity>> weeklyActivities = new TreeMap<>(); // TreeMap for sorted weeks

                for (Activity activity : activities) {
                    try {
                        // Skip invalid activities
                        if (activity.getDistance() <= 100 || activity.getMoving_time() <= 60) {
                            continue;
                        }

                        String week = DateUtils.getWeekOfYear(activity.getStart_date());
                        weeklyActivities.computeIfAbsent(week, k -> new ArrayList<>()).add(activity);

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing activity: " + activity.getName(), e);
                    }
                }

                // Calculate metrics per week
                for (Map.Entry<String, List<Activity>> entry : weeklyActivities.entrySet()) {
                    Map<String, Float> weekMetrics = new HashMap<>();
                    List<Activity> weekRuns = entry.getValue();

                    // Calculate averages
                    float totalDistance = 0;
                    float totalTime = 0;
                    float totalHeartRate = 0;
                    int validRuns = 0;

                    for (Activity run : weekRuns) {
                        if (run.getDistance() > 0 && run.getMoving_time() > 0) {
                            totalDistance += run.getDistance();
                            totalTime += run.getMoving_time();

                            if (run.getAverage_heartrate() > 0) {
                                totalHeartRate += run.getAverage_heartrate();
                            }
                            validRuns++;
                        }
                    }

                    if (validRuns > 0) {
                        // Calculate metrics
                        float avgPace = totalTime / UnitConverter.metersToMiles(totalDistance);
                        float avgDistance = totalDistance / validRuns;
                        float avgHeartRate = totalHeartRate > 0 ? totalHeartRate / validRuns : 0;

                        // Store metrics
                        weekMetrics.put("Avg Pace (min/mile)", avgPace);
                        weekMetrics.put("Avg Distance (m)", avgDistance);
                        weekMetrics.put("Avg Heart Rate", avgHeartRate);
                        weekMetrics.put("Total Runs", (float) validRuns);

                        performanceData.put(entry.getKey(), weekMetrics);
                    }
                }
            }
        }
    }







