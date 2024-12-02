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
        String accessToken = "34e906ba61956135f7adc2651509800339b0e3a6";
        stravaRepository.fetchActivities(accessToken, 1, 10, new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Activity> runs = filterRuns(response.body());
                    if(runs.size() >= 2){
                        Compare(runs.get(0), runs.get(1));
                        displayLastRunStats(runs.get(0));

                    }
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
    private List<Activity> filterRuns(List<Activity> activities) {
        List<Activity> runs = new ArrayList<>();
        for(Activity activity : activities)
        {
            if("Run".equals(activity.getType()))
            {
                runs.add(activity);
            }
        }
        return runs;
    }


    private void Compare(Activity CurrentRun, Activity LastRun) {
        String comparison = "Latest Run:" +
                "Name: " + CurrentRun.getName() +
                "Distance: " + formatDistance(CurrentRun.getDistance()) +
                "Time: " + formatTime(CurrentRun.getMoving_time()) +
                "Previous Run:" +
                "Name: " + LastRun.getName() +
                "Distance: " + formatDistance(LastRun.getDistance()) +
                "Time: " + formatTime(LastRun.getMoving_time());

        activityTextView.setText(comparison);
    }



    // Display stats for the latest run
    private void displayLastRunStats(Activity activity) {
        String stats = "Name: " + activity.getName() +
                "\nDistance: " + formatDistance(activity.getDistance()) +
                "\nTime: " + formatTime(activity.getMoving_time()) +
                "\nType: " + activity.getType() +
                "\nDate: " + activity.getStart_date();

        activityTextView.setText(stats);
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
