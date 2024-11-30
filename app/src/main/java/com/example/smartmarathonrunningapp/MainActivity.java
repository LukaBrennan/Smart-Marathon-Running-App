package com.example.smartmarathonrunningapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private StravaRepository stravaRepository; // Repository for Strava API interactions
    private TextView activityTextView; // TextView to display the latest activity stats

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Set the activity's layout

        // Initialize TextView
        activityTextView = findViewById(R.id.activityTextView);

        // Initialize StravaRepository
        stravaRepository = new StravaRepository();

        // Fetch and display the latest activity
        fetchLatestActivity();
    }

    // Fetches the latest activity using the repository
    private void fetchLatestActivity() {
        String accessToken = "cee39cc9d13b2afa4c36ad12772d0876723149a9"; // Replace with a dynamically retrieved token
        stravaRepository.fetchActivities(accessToken, 1, 1, new Callback<List<Activity>>() {
            @Override
            public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Activity lastActivity = response.body().get(0); // Get the latest activity
                    displayLastRunStats(lastActivity); // Display its stats
                } else {
                    Log.e("StravaAPI", "No activities found or failed to fetch: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
                Log.e("StravaAPI", "API call failed: ", t);
            }
        });
    }

    // Display stats for the latest run
    private void displayLastRunStats(Activity activity) {
        String stats = "Name: " + activity.getName() +
                "\nDistance: " + formatDistance(activity.getDistance()) +
                "\nTime: " + formatTime(activity.getMoving_time()) +
                "\nType: " + activity.getType() +
                "\nDate: " + activity.getStart_date();

        activityTextView.setText(stats); // Update the TextView with the stats
    }

    // Convert meters to kilometers
    @SuppressLint("DefaultLocale")
    private String formatDistance(float meters) {
        return String.format("%.2f km", meters / 1000);
    }

    // Format time in hours, minutes, and seconds
    @SuppressLint("DefaultLocale")
    private String formatTime(int seconds)
    {
        int minutes = seconds / 60;
        int hours = minutes / 60;
        minutes %= 60;
        seconds %= 60;
        return hours > 0 ? String.format("%d hr %d min", hours, minutes) : String.format("%d min %d sec", minutes, seconds);
    }
}
