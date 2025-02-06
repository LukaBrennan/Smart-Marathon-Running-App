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
    private TextView week11Monday;
    private TextView week11Tuesday;
    private TextView week11Wednesday;
    private TextView week11Thursday;
    private TextView week11Friday;
    private TextView week11Saturday;
    private TextView week11Sunday;
    private float goalPace = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityTextView = findViewById(R.id.trainingPlanTitle);
        week11Monday = findViewById(R.id.week11Monday);
        week11Tuesday = findViewById(R.id.week11Tuesday);
        week11Wednesday = findViewById(R.id.week11Wednesday);
        week11Thursday = findViewById(R.id.week11Thursday);
        week11Friday = findViewById(R.id.week11Friday);
        week11Saturday = findViewById(R.id.week11Saturday);
        week11Sunday = findViewById(R.id.week11Sunday);

        stravaRepository = new StravaRepository();

        fetchLatestActivity();
    }

    private void fetchLatestActivity() {
        stravaRepository.refreshAccessToken(new Callback<TokenResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newAccessToken = response.body().getAccessToken();
                    Log.d("StravaAPI", "Access token refreshed: " + newAccessToken);

                    stravaRepository.fetchActivities(newAccessToken, 1, 10, new Callback<List<Activity>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                List<Activity> runs = filterRuns(response.body());
                                Log.d("StravaAPI", "Runs fetched: " + runs.size());

                                if (runs.size() >= 2) {
                                    processRuns(runs.get(1));
                                } else if (runs.size() == 1) {
                                    displayLastRunStats(runs.get(0));
                                } else {
                                    Log.d("StravaAPI", "No runs available in response.");
                                    activityTextView.setText("No recent runs found.");
                                }
                            } else {
                                Log.e("StravaAPI", "Failed to fetch activities. Response code: " + response.code());
                                activityTextView.setText("Error fetching activities. Please check your access token.");
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
                            Log.e("StravaAPI", "API call failed: ", t);
                            activityTextView.setText("Failed to connect to Strava API.");
                        }
                    });
                } else {
                    Log.e("StravaAPI", "Failed to refresh token. Response code: " + response.code());
                    activityTextView.setText("Error refreshing access token.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
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

        goalPace = determineGoalPace(lastVO2Max);

        updateTrainingPlan();
    }

    @SuppressLint("SetTextI18n")
    private void updateTrainingPlan() {
        week11Monday.setText("Monday: Rest or cross-training");
        week11Tuesday.setText("Tuesday: Easy run, maintain a pace of " + formatPace(goalPace + 10));
        week11Wednesday.setText("Wednesday: Tempo run at " + formatPace(goalPace - 2));
        week11Thursday.setText("Thursday: Recovery run, maintain a pace of " + formatPace(goalPace + 2));
        week11Friday.setText("Friday: Interval training (e.g., 4x800m at " + formatPace(goalPace - 5) + ")");
        week11Saturday.setText("Saturday: Long run, maintain a consistent pace around " + formatPace(goalPace + 4));
        week11Sunday.setText("Sunday: Recovery run, maintain a pace of " + formatPace(goalPace + 2));

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

    private float calculateVO2Max(float distance, int movingTime, float avgHeartRate) {
        if (movingTime <= 0 || distance <= 0 || avgHeartRate <= 0) return 0;
        float speed = distance / movingTime;
        return (speed * 1000) / avgHeartRate;
    }

    private float determineGoalPace(float vo2Max) {
        if (vo2Max <= 0) {
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
        // Format pace in minutes and seconds per kilometer
        int minutes = (int) (paceInSeconds / 60);
        int seconds = (int) (paceInSeconds % 60);
        return String.format("%d:%02d min/km", minutes, seconds);
    }
}
private void updateTrainingPlan() {
    week11Monday.setText("Monday: Rest or cross-training");
    week11Tuesday.setText("Tuesday: Easy run, maintain a pace of " + formatPace(goalPace + 10));
    week11Wednesday.setText("Wednesday: Tempo run at " + formatPace(goalPace - 2));
    week11Thursday.setText("Thursday: Recovery run, maintain a pace of " + formatPace(goalPace + 2));
    week11Friday.setText("Friday: Interval training (e.g., 4x800m at " + formatPace(goalPace - 5) + ")");
    week11Saturday.setText("Saturday: Long run, maintain a consistent pace around " + formatPace(goalPace + 4));
    week11Sunday.setText("Sunday: Recovery run, maintain a pace of " + formatPace(goalPace + 2));

    activityTextView.setText("Training Plan Updated Based on Recent Runs");
}