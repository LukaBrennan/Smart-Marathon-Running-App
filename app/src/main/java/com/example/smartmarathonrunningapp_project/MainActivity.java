package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private StravaRepository stravaRepository;
    private LinearLayout weekContainer; // Container for dynamically added weeks
    private Activity lastRun;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.activityTextView);
        weekContainer = findViewById(R.id.weekContainer); // Initialize the week container
        stravaRepository = new StravaRepository();

        // Load the JSON data
        TrainingPlan trainingPlan = loadTrainingPlanFromAssets();

        fetchLatestActivity();

        // Update the JSON UI with the new pace calculated by VO2Max
        if (trainingPlan != null && lastRun != null) {
            float vo2Max = calculateVO2Max(lastRun.getDistance(), lastRun.getMoving_time(), lastRun.getAverage_heartrate());
            updateUiWithTrainingPlan(trainingPlan, vo2Max);
        } else {
            Log.e("StravaAPI", "Failed to load training plan or fetch latest run.");
        }
    }

    // Method to load the contents of the TrainingPlan.json
    private TrainingPlan loadTrainingPlanFromAssets() {
        try {
            InputStream inputStream = getAssets().open("TrainingPlan.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            Gson gson = new Gson();

            // Debugging
            StringBuilder jsonString = new StringBuilder();
            int data;
            while ((data = reader.read()) != -1) {
                jsonString.append((char) data);
            }
            Log.d("JSON_DEBUG", "Raw JSON: " + jsonString);

            return gson.fromJson(jsonString.toString(), TrainingPlan.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Fetch the latest activity from Strava
    private void fetchLatestActivity() {
        stravaRepository.refreshAccessToken(new Callback<>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newAccessToken = response.body().getAccessToken();
                    Log.d("StravaAPI", "Access token refreshed: " + newAccessToken);

                    stravaRepository.fetchActivities(newAccessToken, 1, 10, new Callback<>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                List<Activity> runs = filterRuns(response.body());
                                Log.d("StravaAPI", "Runs fetched: " + runs.size());
                                if (!runs.isEmpty()) {
                                    lastRun = runs.get(0);

                                    runOnUiThread(() -> {
                                        TrainingPlan trainingPlan = loadTrainingPlanFromAssets();
                                        if (trainingPlan != null && lastRun != null) {
                                            float vo2Max = calculateVO2Max(lastRun.getDistance(), lastRun.getMoving_time(), lastRun.getAverage_heartrate());
                                            updateUiWithTrainingPlan(trainingPlan, vo2Max);
                                        } else {
                                            Log.d("StravaAPI", "No runs available in response.");
                                        }
                                    });
                                } else {
                                    Log.d("StravaAPI", "No runs available in response.");
                                }
                            } else {
                                Log.e("StravaAPI", "Failed to fetch activities. Response code: " + response.code());
                            }
                        }

                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
                            Log.e("StravaAPI", "API call failed: ", t);
                        }
                    });
                } else {
                    Log.e("StravaAPI", "Failed to refresh token. Response code: " + response.code());
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                Log.e("StravaAPI", "Token refresh failed: ", t);
            }
        });
    }

    private List<Activity> filterRuns(List<Activity> activities) {
        List<Activity> runs = new ArrayList<>();
        for (Activity activity : activities) {
            if ("Run".equals(activity.getType())) {
                runs.add(activity);
            }
        }
        return runs;
    }

    // Updated version that dynamically creates UI elements for each week
    @SuppressLint("SetTextI18n")
    private void updateUiWithTrainingPlan(TrainingPlan trainingPlan, float vo2Max) {
        weekContainer.removeAllViews(); // Clear existing views

        if (trainingPlan == null || trainingPlan.getTraining_weeks() == null) {
            Log.e("MainActivity", "Training plan or weeks is null");
            return;
        }

        for (TrainingPlan.TrainingWeek week : trainingPlan.getTraining_weeks()) {
            if (week == null || week.getTraining_plan() == null) {
                Log.e("MainActivity", "Week or training plan is null");
                continue;
            }

            // Create a new LinearLayout for the week
            LinearLayout weekLayout = new LinearLayout(this);
            weekLayout.setOrientation(LinearLayout.VERTICAL);
            weekLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            weekLayout.setPadding(16, 16, 16, 16);
            weekLayout.setBackgroundColor(Color.WHITE);
            weekLayout.setElevation(2);

            // Add a TextView for the week number
            TextView weekTextView = new TextView(this);
            weekTextView.setText("Week " + week.getWeek());
            weekTextView.setTextSize(20);
            weekTextView.setTextColor(Color.BLACK);
            weekTextView.setTypeface(null, Typeface.BOLD);
            weekTextView.setPadding(0, 0, 0, 16);
            weekLayout.addView(weekTextView);

            // Add TextViews for each day
            TrainingPlan.Days days = week.getTraining_plan();
            addDayView(weekLayout, "Monday", days.getMonday(), vo2Max);
            addDayView(weekLayout, "Tuesday", days.getTuesday(), vo2Max);
            addDayView(weekLayout, "Wednesday", days.getWednesday(), vo2Max);
            addDayView(weekLayout, "Thursday", days.getThursday(), vo2Max);
            addDayView(weekLayout, "Friday", days.getFriday(), vo2Max);
            addDayView(weekLayout, "Saturday", days.getSaturday(), vo2Max);
            addDayView(weekLayout, "Sunday", days.getSunday(), vo2Max);

            // Add the week layout to the container
            weekContainer.addView(weekLayout);
        }
    }

    // Helper method to add a day's details to the week layout
    @SuppressLint("SetTextI18n")
    private void addDayView(LinearLayout parent, String dayName, TrainingPlan.Day day, float vo2Max) {
        if (day == null) {
            Log.e("MainActivity", dayName + " is null");
            return;
        }

        // Create a TextView for the day
        TextView dayTextView = new TextView(this);
        dayTextView.setText(dayName + ": " + formatDay(day, vo2Max));
        dayTextView.setTextSize(16);
        dayTextView.setTextColor(Color.DKGRAY);
        dayTextView.setPadding(0, 8, 0, 8);
        parent.addView(dayTextView);
    }

    // Format the different days for the Training plan with updated pace goals
    private String formatDay(TrainingPlan.Day day, float vo2Max) {
        String pace = day.getPace();
        if (pace != null) {
            pace = adjustPaceBasedOnVO2Max(pace, vo2Max);
        }
        return "Exercise: " + day.getExercise() + "\nDistance: " + day.getDistance() + "\nPace: " + pace;
    }

    // Adjust pace based on VO2Max
    @SuppressLint("DefaultLocale")
    private String adjustPaceBasedOnVO2Max(String pace, float vo2Max) {
        String[] paceArray = pace.split(" - ");
        List<String> adjustedPaces = new ArrayList<>();

        // Baseline trained VO2Max
        float baselineVO2Max = 40.0f;
        float adjustmentFactor = 1 - ((vo2Max - baselineVO2Max) / 100);

        for (String p : paceArray) {
            String[] parts = p.split(":");
            if (parts.length < 2) continue;

            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            int totalSeconds = minutes * 60 + seconds;

            // Adjust totalSeconds based on VO2Max
            totalSeconds *= (int) adjustmentFactor;

            // Convert back to "mm:ss"
            int adjustedMinutes = totalSeconds / 60;
            int adjustedSeconds = totalSeconds % 60;
            adjustedPaces.add(String.format("%d:%02d", adjustedMinutes, adjustedSeconds));
        }

        return String.join(" - ", adjustedPaces);
    }

    // Calculate VO2Max
    private float calculateVO2Max(float distance, int movingTime, float avgHeartRate) {
        if (movingTime <= 0 || distance <= 0 || avgHeartRate <= 0) return 0;
        float speed = distance / movingTime;
        return (speed * 1000) / avgHeartRate;
    }
}