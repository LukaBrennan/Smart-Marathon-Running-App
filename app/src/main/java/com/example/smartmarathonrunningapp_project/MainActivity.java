package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private StravaRepository stravaRepository;
    private TextView activityTextView;
    private TextView mondayDetailsTextView;
    private TextView tuesdayDetailsTextView;
    private TextView wednesdayDetailsTextView;
    private TextView thursdayDetailsTextView;
    private TextView fridayrunDetailsTextView;
    private TextView saturdayrunDetailsTextView;
    private TextView sundayrunDetailsTextView;
    private float goalPace = 1.0f; // Stores the calculated goal pace for the next run

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityTextView = findViewById(R.id.activityTextView);
        mondayDetailsTextView = findViewById(R.id.mondayDetailsTextView);
        tuesdayDetailsTextView = findViewById(R.id.tuesdayDetailsTextView);
        wednesdayDetailsTextView = findViewById(R.id.wednesdayDetailsTextView);
        thursdayDetailsTextView = findViewById(R.id.thursdayDetailsTextView);
        fridayrunDetailsTextView = findViewById(R.id.fridayrunDetailsTextView);
        saturdayrunDetailsTextView = findViewById(R.id.saturdayrunDetailsTextView);
        sundayrunDetailsTextView = findViewById(R.id.sundayrunDetailsTextView);
        stravaRepository = new StravaRepository();

        fetchLatestActivity();
    }

    private void fetchLatestActivity()
    {
        // All Logs are used to get understand any potential problems with api access or displaying data.
        stravaRepository.refreshAccessToken(new Callback<>()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    // Checking if the access_token needs to be acquired, log request is used for debugging and viewing potential errors in the LogCat
                    String newAccessToken = response.body().getAccessToken();
                    Log.d("StravaAPI", "Access token refreshed: " + newAccessToken);

                    stravaRepository.fetchActivities(newAccessToken, 1, 10, new Callback<>()
                    {
                        @Override
                        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
                        {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty())
                            {
                                List<Activity> runs = filterRuns(response.body());
                                Log.d("StravaAPI", "Runs fetched: " + runs.size());
                                if (runs.size() >= 2) {
                                    processRuns(runs.get(1));
                                }
                                else if (runs.size() == 1)
                                {
                                    displayLastRunStats(runs.get(0));
                                }
                                else
                                {
                                    Log.d("StravaAPI", "No runs available in response.");
                                    activityTextView.setText("No recent runs found.");
                                }
                            }
                            else
                            {
                                Log.e("StravaAPI", "Failed to fetch activities. Response code: " + response.code());
                                activityTextView.setText("Error fetching activities. Please check your access token.");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t)
                        {
                            Log.e("StravaAPI", "API call failed: ", t);
                            activityTextView.setText("Failed to connect to Strava API.");
                        }
                    });
                }
                else
                {
                    Log.e("StravaAPI", "Failed to refresh token. Response code: " + response.code());
                    activityTextView.setText("Error refreshing access token.");
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t)
            {
                Log.e("StravaAPI", "Token refresh failed: ", t);
                activityTextView.setText("Failed to refresh access token.");
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

    private void processRuns(Activity lastRun) {

        float lastVO2Max = calculateVO2Max(lastRun.getDistance(), lastRun.getMoving_time(), lastRun.getAverage_heartrate());

        // Adjust goal pace based on VO₂max
        goalPace = determineGoalPace(lastVO2Max);

        // Update the training plan in the UI
        updateTrainingPlan();
    }

    @SuppressLint("SetTextI18n")
    private void updateTrainingPlan()
    {
        // Update each TextView with dynamically calculated paces
        mondayDetailsTextView.setText("Rest or cross-training");
        tuesdayDetailsTextView.setText("Easy run, maintain a pace of " + formatPace(goalPace + 2));
        wednesdayDetailsTextView.setText("Tempo run at " + formatPace(goalPace - 2));
        thursdayDetailsTextView.setText("Recovery run, maintain a pace of " + formatPace(goalPace + 2));
        fridayrunDetailsTextView.setText("Interval training (e.g., 4x800m at " + formatPace(goalPace - 5) + ")");
        saturdayrunDetailsTextView.setText("Long run, maintain a consistent pace around " + formatPace(goalPace + 4));
        sundayrunDetailsTextView.setText("Recovery run, maintain a pace of " + formatPace(goalPace + 2));
        activityTextView.setText("Training Plan Updated Based on Recent Runs");
    }

    private void displayLastRunStats(Activity activity) {
        String stats = "Name: " + activity.getName() +
                "\nDistance: " + formatDistance(activity.getDistance()) +
                "\nTime: " + formatTime(activity.getMoving_time()) +
                "\nType: " + activity.getType() +
                "\nDate: " + activity.getStart_date();

        activityTextView.setText(stats);
    }

    private float calculateVO2Max(float distance, int movingTime, float avgHeartRate)
    {
        if (movingTime <= 0 || distance <= 0 || avgHeartRate <= 0) return 0;
        float speed = distance / movingTime;
        return (speed * 1000) / avgHeartRate;
    }

    private float determineGoalPace(float vo2Max)
    {
        if (vo2Max <= 0)
        {
            return 0;
        }

        float basePaceInSeconds = 300;
        float adjustmentFactor = 50;
        float paceInSeconds = basePaceInSeconds - (vo2Max - adjustmentFactor) * 2;

        return Math.max(paceInSeconds, 180);
    }
    @SuppressLint("DefaultLocale")
    private String formatDistance(float meters) {
        return String.format("%.2f km", meters / 1000);
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int hours = minutes / 60;
        minutes %= 60;
        seconds %= 60;
        return hours > 0 ? String.format("%d hr %d min", hours, minutes) : String.format("%d min %d sec", minutes, seconds);
    }

    @SuppressLint("DefaultLocale")
    private String formatPace(float paceInSeconds) {
        int minutes = (int) (paceInSeconds / 60);
        int seconds = (int) (paceInSeconds % 60);
        return String.format("%d:%02d min/km", minutes, seconds);
    }
}