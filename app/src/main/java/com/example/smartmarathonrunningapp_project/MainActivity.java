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
        String accessToken = "48d6c0e58c17fcc01d1ea4a0abe79aceb8d823cf";
        // The code below will filter the runs that is on the runner's strava account so that it shows the first page of data taken from the activities
        stravaRepository.fetchActivities(accessToken, 1, 10, new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
            {
                // Checking if valid - true path
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty())
                {
                    List<Activity> runs = filterRuns(response.body());
                    if(runs.size() >= 2)
                    {
                        Compare(runs.get(0), runs.get(1));
                    }
                    else if (runs.size() == 1)
                    {
                        displayLastRunStats(runs.get(0));
                    }
                }
                // Not valid - false path
                else
                {
                    Log.e("StravaAPI", "No activities found or failed to fetch: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t)
            {
                Log.e("StravaAPI", "API call failed: ", t);
            }
        });
    }

    // This will filter out all activities from the runners Strava account to only show activities of type "Run"
    private List<Activity> filterRuns(List<Activity> activities)
    {
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

    // To show the completed run and the last run.
    private void Compare(Activity CurrentRun, Activity LastRun)
    {
        // These two float variables will calculate the pace for both runs (Current completed and previous)
        float currentPace = calculatePace(CurrentRun.getMoving_time(), CurrentRun.getDistance());
        float lastPace = calculatePace(LastRun.getMoving_time(), LastRun.getDistance());
        // The "goalpace will determine   the increase or decrease for the next run
        float goalpace = currentPace;
        if (currentPace < lastPace)
        {
            goalpace -= 0.025F; // improve slightly by 0.025 pace min/km
        }
        else{
            goalpace += 0.025F;// recover slightly by 0.025 pace min/km
        }
        String comparison = "Completed Run:" +
                "Name: " + CurrentRun.getName() +
                "\nDistance: " + formatDistance(CurrentRun.getDistance()) +
                "\nTime: " + formatTime(CurrentRun.getMoving_time()) +
                "\nPace: " + formatPace(currentPace)  +
                "\nHeart Rate (Avg/Max): " + CurrentRun.getAverage_heartrate() + "/" + CurrentRun.getMax_heartrate() + "\n\n" +

                "\n\nPrevious Run:" +
                "Name: " + LastRun.getName() +
                "\nDistance: " + formatDistance(LastRun.getDistance()) +
                "\nTime: " + formatTime(LastRun.getMoving_time()) +
                 "\nPace: " + formatPace(lastPace) + "\n" +
                "Heart Rate (Avg/Max): " + LastRun.getAverage_heartrate() + "/" + LastRun.getMax_heartrate() + "\n\n" +

                "Next Goal:\n" +
                "Pace: " + formatPace(goalpace);
        activityTextView.setText(comparison);
    }
    // Display stats for the latest run
    private void displayLastRunStats(Activity activity)
    {
        String stats = "Name: " + activity.getName() +
                "\nDistance: " + formatDistance(activity.getDistance()) +
                "\nTime: " + formatTime(activity.getMoving_time()) +
                "\nType: " + activity.getType() +
                "\nDate: " + activity.getStart_date();

        activityTextView.setText(stats);
  }

    // Convert meters to kilometers
    @SuppressLint("DefaultLocale")
    private String formatDistance(float meters)
    {
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

    // used to calculate the pace in seconds per km
    private float calculatePace(int movingTime, float distance)
    {
        return movingTime / (distance / 1000); // returns pace in seconds per km
    }

    // Format pace to min/km
    @SuppressLint("DefaultLocale")
    private String formatPace(float paceInSeconds)
    {
        int minutes = (int) (paceInSeconds / 60);
        int seconds = (int) (paceInSeconds % 60);
        return String.format("%d:%02d min/km", minutes, seconds);
    }
}
