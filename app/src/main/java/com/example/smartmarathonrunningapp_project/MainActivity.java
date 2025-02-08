package com.example.smartmarathonrunningapp_project;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
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
    private TextView activityTextView;
    private TextView mondayDetailsTextView;
    private TextView tuesdayDetailsTextView;
    private TextView wednesdayDetailsTextView;
    private TextView thursdayDetailsTextView;
    private TextView fridayrunDetailsTextView;
    private TextView saturdayrunDetailsTextView;
    private TextView sundayrunDetailsTextView;
    private Activity lastRun;

    @SuppressLint("SetTextI18n")
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

        // Load the JSON data
        TrainingPlan trainingPlan = loadTrainingPlanFromAssets();

        fetchLatestActivity();

        // Update the JSON ui with the new pace calculated by V02max TODO - Not fully working
        if (trainingPlan != null && lastRun != null)
        {
            float vo2Max = calculateVO2Max(lastRun.getDistance(), lastRun.getMoving_time(), lastRun.getAverage_heartrate());
            updateUiWithTrainingPlan(trainingPlan, vo2Max);
        } else {
            activityTextView.setText("Failed to load training plan or fetch latest run.");
        }
    }

    // Method to load the contents of the TrainingPlan.json TODO - Need to add more than just week 11
    private TrainingPlan loadTrainingPlanFromAssets()
    {
        try
        {
            InputStream inputStream = getAssets().open("TrainingPlan.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            Gson gson = new Gson();

            // Debugging
            StringBuilder jsonString = new StringBuilder();
            int data;
            while ((data = reader.read()) != -1)
            {
                jsonString.append((char) data);
            }
            Log.d("JSON_DEBUG", "Raw JSON: " + jsonString);

            return gson.fromJson(jsonString.toString(), TrainingPlan.class);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    // Fetch the latest activity from Strava
    private void fetchLatestActivity()
    {
        stravaRepository.refreshAccessToken(new Callback<>()
        {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    String newAccessToken = response.body().getAccessToken();
                    Log.d("StravaAPI", "Access token refreshed: " + newAccessToken);

                    stravaRepository.fetchActivities(newAccessToken, 1, 10, new Callback<>()
                    {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
                        {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty())
                            {
                                List<Activity> runs = filterRuns(response.body());
                                Log.d("StravaAPI", "Runs fetched: " + runs.size());
                                if (!runs.isEmpty())
                                {
                                    lastRun = runs.get(0);

                                    runOnUiThread(() -> // Updates the UI after a new run is found TODO - need to make sure that it will be properly working with more weeks and to not replace previous weeks
                                    {
                                        TrainingPlan trainingPlan = loadTrainingPlanFromAssets();
                                        if (trainingPlan != null && lastRun != null)
                                        {
                                            float vo2Max = calculateVO2Max(lastRun.getDistance(), lastRun.getMoving_time(), lastRun.getAverage_heartrate());
                                            updateUiWithTrainingPlan(trainingPlan, vo2Max);
                                        }
                                        else
                                        {
                                            Log.d("StravaAPI", "No runs available in response.");
                                            activityTextView.setText("Failed to load training plan or fetch latest run.");
                                        }
                                    });
                                }
                                else
                                {
                                    Log.d("StravaAPI", "No runs available in response.");
                                    runOnUiThread(() -> activityTextView.setText("No recent runs found."));
                                }
                            }
                            else
                            {
                                Log.e("StravaAPI", "Failed to fetch activities. Response code: " + response.code());
                                runOnUiThread(() -> activityTextView.setText("Error fetching activities."));
                            }
                        }

                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t)
                        {
                            Log.e("StravaAPI", "API call failed: ", t);
                            runOnUiThread(() -> activityTextView.setText("Failed to connect to Strava API."));
                        }
                    });
                }
                else
                {
                    Log.e("StravaAPI", "Failed to refresh token. Response code: " + response.code());
                    runOnUiThread(() -> activityTextView.setText("Error refreshing access token."));
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t)
            {
                Log.e("StravaAPI", "Token refresh failed: ", t);
                runOnUiThread(() -> activityTextView.setText("Failed to refresh access token."));
            }
        });
    }


    private List<Activity> filterRuns(List<Activity> activities)
    {
        List<Activity> runs = new ArrayList<>();
        for (Activity activity : activities)
        {
            if ("Run".equals(activity.getType()))
            {
                runs.add(activity);
            }
        }
        return runs;
    }

    // Updated version that uses the JSON file instead of hardcoded values from XML
    private void updateUiWithTrainingPlan(TrainingPlan trainingPlan, float vo2Max)
    {
        TrainingPlan.Days days = trainingPlan.getTraining_plan();
        mondayDetailsTextView.setText(formatDay(days.getMonday(), vo2Max));
        tuesdayDetailsTextView.setText(formatDay(days.getTuesday(), vo2Max));
        wednesdayDetailsTextView.setText(formatDay(days.getWednesday(), vo2Max));
        thursdayDetailsTextView.setText(formatDay(days.getThursday(), vo2Max));
        fridayrunDetailsTextView.setText(formatDay(days.getFriday(), vo2Max));
        saturdayrunDetailsTextView.setText(formatDay(days.getSaturday(), vo2Max));
        sundayrunDetailsTextView.setText(formatDay(days.getSunday(), vo2Max));
    }

    // Format the different days for the Training plan with updated pace goals
    private String formatDay(TrainingPlan.Day day, float vo2Max)
    {
        String pace = day.getPace();
        if (pace != null)
        {
            pace = adjustPaceBasedOnVO2Max(pace, vo2Max);
        }
        return "Exercise: " + day.getExercise() + "\nDistance: " + day.getDistance() + "\nPace: " + pace;
    }

    // Two methods below are generate by AI to try to try and do V02Max calculations, not working properly TODO - need to fix
    @SuppressLint("DefaultLocale")
    private String adjustPaceBasedOnVO2Max(String pace, float vo2Max)
    {
        String[] parts = pace.split(":");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        int totalSeconds = minutes * 60 + seconds;

        // Adjust totalSeconds based on VO2Max (example logic)
        totalSeconds -= (int) (vo2Max / 10); // Example adjustment

        // Convert back to "mm:ss" format
        int adjustedMinutes = totalSeconds / 60;
        int adjustedSeconds = totalSeconds % 60;
        return String.format("%d:%02d", adjustedMinutes, adjustedSeconds);
    }

    // Calculate VO2Max
    private float calculateVO2Max(float distance, int movingTime, float avgHeartRate)
    {
        if (movingTime <= 0 || distance <= 0 || avgHeartRate <= 0) return 0;
        float speed = distance / movingTime;
        return (speed * 1000) / avgHeartRate;
    }

}