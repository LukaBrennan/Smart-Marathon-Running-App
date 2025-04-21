package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
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
import com.example.smartmarathonrunningapp_project.utils.DateUtils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private StravaRepository stravaRepository;
    private TrainingPlanManager planManager;
    private AutoAdjuster autoAdjuster;

    private final PerformanceData performanceData = new PerformanceData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stravaRepository = new StravaRepository();
        initializeDependencies();
        setupUI();
        fetchAndCheckActivities();
        autoAdjuster = new AutoAdjuster();
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
        Intent intent = new Intent(this, FeedbackActivity.class);
        intent.putExtra("performanceData", performanceData.toJson());
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
            // Load either adjusted plan or fallback to original
            TrainingPlan trainingPlan = planManager.loadAdjustedPlan();
            if (trainingPlan == null) {
                trainingPlan = planManager.loadTrainingPlanFromAssets();
            }

            performanceData.clear();
            for (Activity activity : activities) {
                if (isValidActivity(activity)) {
                    performanceData.addActivity(activity);
                    // Auto-adjust after each activity
                    trainingPlan = autoAdjuster.adjustPlan(trainingPlan, activity);
                }
            }

            // Save adjusted plan
            planManager.saveAdjustedPlan(trainingPlan);
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
                String text = dayName + ": " + day.getExercise() + " - " +
                        day.getDistance() + " @ " + day.getPace();

                // Show adjustment note if exists
                if (day.getAdjustmentNote() != null) {
                    text += "\n[Adjusted: " + day.getAdjustmentNote() + "]";
                    dayTextView.setTextColor(Color.BLUE);
                }

                dayTextView.setText(text);
                dayTextView.setTextSize(16);
                dayTextView.setPadding(16, 8, 16, 8);
                container.addView(dayTextView);
            }
        }

        private boolean isValidActivity(Activity activity) {
            return activity.getDistance() > 100 && activity.getMoving_time() > 60;
        }
    }
}




