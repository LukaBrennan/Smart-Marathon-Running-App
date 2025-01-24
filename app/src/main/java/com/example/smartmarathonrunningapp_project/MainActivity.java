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
    private float goalPace = 1.0f; // Stores the calculated goal pace for the next run

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityTextView = findViewById(R.id.activityTextView);
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
                                    processRuns(runs.get(0), runs.get(1));
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

    private void processRuns(Activity currentRun, Activity lastRun) {
        float currentPace = calculatePace(currentRun.getMoving_time(), currentRun.getDistance());
        float lastPace = calculatePace(lastRun.getMoving_time(), lastRun.getDistance());

        goalPace = currentPace < lastPace ? currentPace - 0.025F : currentPace + 0.025F;

        displayTrainingPlan(currentRun, lastRun, goalPace);
    }

    private void displayTrainingPlan(Activity currentRun, Activity lastRun, float goalPace) {
        String plan = "Training Plan:\n\n" +
                "Completed Run:\n" +
                "Name: " + currentRun.getName() +
                "\nDistance: " + formatDistance(currentRun.getDistance()) +
                "\nTime: " + formatTime(currentRun.getMoving_time()) +
                "\nPace: " + formatPace(calculatePace(currentRun.getMoving_time(), currentRun.getDistance())) +
                "\nHeart Rate (Avg/Max): " + currentRun.getAverage_heartrate() + "/" + currentRun.getMax_heartrate() +
                "\n\nPrevious Run:\n" +
                "Name: " + lastRun.getName() +
                "\nDistance: " + formatDistance(lastRun.getDistance()) +
                "\nTime: " + formatTime(lastRun.getMoving_time()) +
                "\nPace: " + formatPace(calculatePace(lastRun.getMoving_time(), lastRun.getDistance())) +
                "\nHeart Rate (Avg/Max): " + lastRun.getAverage_heartrate() + "/" + lastRun.getMax_heartrate() +
                "\n\nNext Goal:\n" +
                "Goal Pace: " + formatPace(goalPace) + "\n\n" +
                "Day 1: Easy run, maintain a pace of " + formatPace(goalPace + 0.3F) + "\n" +
                "Day 2: Rest or cross-training\n" +
                "Day 3: Tempo run at " + formatPace(goalPace - 0.2F) + "\n" +
                "Day 4: Rest\n" +
                "Day 5: Interval training (e.g., 4x800m at " + formatPace(goalPace - 0.1F) + ")\n" +
                "Day 6: Long run, maintain a consistent pace\n" +
                "Day 7: Recovery run, maintain a pace of " + formatPace(goalPace + 0.4F);

        activityTextView.setText(plan);
    }

    private void displayLastRunStats(Activity activity) {
        String stats = "Name: " + activity.getName() +
                "\nDistance: " + formatDistance(activity.getDistance()) +
                "\nTime: " + formatTime(activity.getMoving_time()) +
                "\nType: " + activity.getType() +
                "\nDate: " + activity.getStart_date();

        activityTextView.setText(stats);
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

    private float calculatePace(int movingTime, float distance) {
        if (distance <= 0) return 0;
        return movingTime / (distance / 1000);
    }

    @SuppressLint("DefaultLocale")
    private String formatPace(float paceInSeconds) {
        int minutes = (int) (paceInSeconds / 60);
        int seconds = (int) (paceInSeconds % 60);
        return String.format("%d:%02d min/km", minutes, seconds);
    }
}
