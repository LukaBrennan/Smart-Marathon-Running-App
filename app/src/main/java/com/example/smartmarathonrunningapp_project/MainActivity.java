package com.example.smartmarathonrunningapp_project;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// used for parsing JSON data into Java objects
import com.google.gson.Gson;
// Handle file reading from assets folder
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
// Parse and format dates
import java.text.ParseException;
import java.text.SimpleDateFormat;
// Data structures and locale settings
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
// Used to make asynchronous calls (API calls)
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
{
    private StravaRepository stravaRepository; // Manage API calls to Strava
    // Dates for getting STRAVA data
    public static final String START_DATE = "2023-08-07";
    public static final String END_DATE = "2023-10-29";

    private static final String ERROR_LOG = "failed to parse date";  // Compliant
    private TrainingPlan trainingPlan; // Stores the loaded training plan
    private final Map<String, Float> performanceData = new HashMap<>(); // keeps track of training performance

    // Constants for log tags and day names
    private static final String TAG = "MainActivity";
    private static final String MONDAY = "Monday";
    private static final String TUESDAY = "Tuesday";
    private static final String WEDNESDAY = "Wednesday";
    private static final String THURSDAY = "Thursday";
    private static final String FRIDAY = "Friday";
    private static final String SATURDAY = "Saturday";
    private static final String SUNDAY = "Sunday";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stravaRepository = new StravaRepository();

        // Load the training plan from assets
        trainingPlan = loadTrainingPlanFromAssets();

        fetchAndCheckActivities();
    }

    // Fetch activities from Strava and check compliance with the training plan
    private void fetchAndCheckActivities() {
        stravaRepository.refreshAccessToken(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Failed to refresh token. Response code: " + response.code());
                    return;
                }

                String accessToken = response.body().getAccessToken();
                Log.d(TAG, "Access token refreshed: " + accessToken);
                fetchActivities(accessToken);
            }
            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Token refresh failed", t);
            }
        });
    }

    private void fetchActivities(String accessToken) {
        stravaRepository.fetchActivities(accessToken, 1, 100, new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Failed to fetch activities. Response code: " + response.code());
                    return;
                }

                List<Activity> activities = response.body();
                Log.d(TAG, "Fetched activities: " + activities.size());

                processActivities(activities);
            }

            @Override
            public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to fetch activities", t);
            }
        });
    }

    private void processActivities(List<Activity> activities) {
        for (Activity activity : activities) {
            Log.d(TAG, "Activity: " + activity.getName() + ", Date: " + activity.getStart_date());
        }

        List<Activity> filteredActivities = filterActivitiesByDate(activities);
        Log.d(TAG, "Filtered activities: " + filteredActivities.size());

        List<Activity> oneRunPerDay = extractDailyRuns(filteredActivities);
        Log.d(TAG, "One run per day: " + oneRunPerDay.size());

        updateTrainingPlan(oneRunPerDay);
    }

    private void updateTrainingPlan(List<Activity> oneRunPerDay) {
        if (trainingPlan == null) {
            Log.e(TAG, "Failed to load training plan.");
            return;
        }

        processFirstDayAndUpdateNextDay(oneRunPerDay, trainingPlan);
        logUpdatedPlan(trainingPlan);
        updateUI(trainingPlan);
    }


        // Process one run per day
    List<Activity> extractDailyRuns(List<Activity> activities)
        {
        Map<String, Activity> runsByDate = new HashMap<>();
        for (Activity activity : activities)
        {
            String activityDate = activity.getStart_date().split("T")[0]; // Extract date part (e.g., "2023-08-07")
            runsByDate.putIfAbsent(activityDate, activity); // Add the first run for each date if not present
            }
            return new ArrayList<>(runsByDate.values());
        }


    // Load the TrainingPlan.json from the assets folder
    private TrainingPlan loadTrainingPlanFromAssets()
    {
        try
        {
            InputStream inputStream = getAssets().open("TrainingPlan.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            Gson gson = new Gson();
            TrainingPlan plan = gson.fromJson(reader, TrainingPlan.class);
            Log.d(TAG, "Training plan loaded: " + (plan != null));
            return plan;
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed to load training plan", e);
            return null;
        }
    }


    // Get the corresponding day by name
    private TrainingPlan.Day getDayByName(TrainingPlan.TrainingWeek week, String dayName) {
        Map<String, TrainingPlan.Day> dayMap = Map.of(
                MONDAY, week.getTraining_plan().getMonday(),
                TUESDAY, week.getTraining_plan().getTuesday(),
                WEDNESDAY, week.getTraining_plan().getWednesday(),
                THURSDAY, week.getTraining_plan().getThursday(),
                FRIDAY, week.getTraining_plan().getFriday(),
                SATURDAY, week.getTraining_plan().getSaturday(),
                SUNDAY, week.getTraining_plan().getSunday()
        );
        return dayMap.getOrDefault(dayName, null);
    }

    // Get the next training day's name
    private static final List<String> WEEK_DAYS =
            List.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);

    private String getNextDayName(String currentDayName)
    {
        int index = WEEK_DAYS.indexOf(currentDayName);
        return (index == -1 || index == WEEK_DAYS.size() - 1) ? MONDAY : WEEK_DAYS.get(index + 1);
    }


    // MVP - calculate pace algorithm
    // Process the first day and update the next training day's pace
    private void processFirstDayAndUpdateNextDay(List<Activity> activities, TrainingPlan trainingPlan)
    {
        if (activities.isEmpty() || trainingPlan == null)
        {
            Log.e(TAG, "No activities or training plan available.");
            return;
        }

        // Get the first activity
        Activity firstActivity = activities.get(0);
        String firstActivityDay = getDayOfWeek(firstActivity.getStart_date());

        // Find the corresponding day in the training plan
        TrainingPlan.TrainingWeek firstWeek = trainingPlan.getTraining_weeks().get(0);
        assert firstActivityDay != null;
        TrainingPlan.Day firstDay = getDayByName(firstWeek, firstActivityDay);
        if (firstDay != null)
        {
            // Check compliance for the first day
            boolean isCompleted = activityMatchesPlan(firstActivity, firstDay);
            firstDay.setCompleted(isCompleted);
            if (isCompleted)
            {
                Log.d(TAG, "First day (" + firstActivityDay + "): Completed - " + firstDay.getExercise());
                // Track performance for the first day
                trackPerformance(firstActivity, firstDay, firstWeek.getWeek(), firstActivityDay);
                // Adjust pace for the next training day
                adjustNextTrainingDayPace(firstWeek, firstActivityDay, firstActivity);
            }
            else
            {
                Log.d(TAG, "First day (" + firstActivityDay + "): Not completed - " + firstDay.getExercise());
            }
        }
        else
        {
            Log.e(TAG, "No matching day found in the training plan for the first activity.");
        }
    }
    // Adjust pace for the next training day based on the first day's performance
    private void adjustNextTrainingDayPace(TrainingPlan.TrainingWeek week, String currentDayName, Activity activity)
    {
        String nextDayName = getNextDayName(currentDayName);
        assert nextDayName != null;
        TrainingPlan.Day nextDay = getDayByName(week, nextDayName);

        if (nextDay != null && nextDay.getPace() != null)
        {
            float activityDistanceMiles = UnitConverter.metersToMiles(activity.getDistance());
            float activityTime = activity.getMoving_time();

            // Check for NAN values
            if (activityDistanceMiles == 0 || activityTime == 0)
            {
                Log.e(TAG, "Invalid activity data: distance=" + activityDistanceMiles + ", time=" + activityTime);
                return;
            }

            float averagePaceSecPerMile = activityTime / activityDistanceMiles;
            float requiredPace = convertPaceToSeconds(nextDay.getPace());

            if (averagePaceSecPerMile  < requiredPace)
            {
                // Runner is faster than the target pace
                float newPace = requiredPace * 0.95f; // Increase pace by 5%
                nextDay.setPace(convertSecondsToPace((int) newPace));
                Log.d(TAG, "Next day (" + nextDayName + "): Increased pace to " + convertSecondsToPace((int) newPace));
            }
            else if (averagePaceSecPerMile  > requiredPace)
            {
                // Runner is slower than the target pace
                float newPace = requiredPace * 1.05f; // Decrease pace by 5%
                nextDay.setPace(convertSecondsToPace((int) newPace));
                Log.d(TAG, "Next day (" + nextDayName + "): Decreased pace to " + convertSecondsToPace((int) newPace));
            }
        }
        else
        {
            Log.e(TAG, "No valid next training day found.");
        }
    }
    // Update the UI with the updated training plan
    @SuppressLint("SetTextI18n")
    private void updateUI(TrainingPlan trainingPlan)
    {
        LinearLayout weekContainer = findViewById(R.id.weekContainer);
        weekContainer.removeAllViews();

        for (TrainingPlan.TrainingWeek week : trainingPlan.getTraining_weeks())
        {
            // Add a TextView for the week
            TextView weekTextView = new TextView(this);
            weekTextView.setText("Week: " + week.getWeek());
            weekTextView.setTextSize(18);
            weekTextView.setPadding(0, 16, 0, 8);
            weekContainer.addView(weekTextView);

            // Add TextViews for each day
            addDayTextView(week.getTraining_plan().getMonday(), MONDAY, weekContainer);
            addDayTextView(week.getTraining_plan().getTuesday(), TUESDAY, weekContainer);
            addDayTextView(week.getTraining_plan().getWednesday(), WEDNESDAY, weekContainer);
            addDayTextView(week.getTraining_plan().getThursday(), THURSDAY, weekContainer);
            addDayTextView(week.getTraining_plan().getFriday(), FRIDAY, weekContainer);
            addDayTextView(week.getTraining_plan().getSaturday(), SATURDAY, weekContainer);
            addDayTextView(week.getTraining_plan().getSunday(), SUNDAY, weekContainer);
        }
    }

    // Add a TextView for a specific day
    @SuppressLint("SetTextI18n")
    private void addDayTextView(TrainingPlan.Day day, String dayName, LinearLayout container)
    {
        if (day != null)
        {
            TextView dayTextView = new TextView(this);
            String distanceDisplay = day.getDistance();
            dayTextView.setText(dayName + ": " + day.getExercise() + " - " + distanceDisplay + " @ " + day.getPace());
            dayTextView.setTextSize(16);
            dayTextView.setPadding(16, 8, 16, 8);
            container.addView(dayTextView);
        }
    }


    // Parse pace string (e.g., "8:00") to seconds
    private int convertPaceToSeconds(String pace)
    {
        if (pace == null || pace.isEmpty() || !pace.matches("\\d+:\\d{2}"))
        {
            Log.e(TAG, "Invalid pace format: " + pace);
            return 0;
        }
        String[] parts = pace.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    // Format time in seconds to "mm:ss"
    @SuppressLint("DefaultLocale")
    private String convertSecondsToPace(int timeInSeconds)
    {
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // Helper method to get the day of the week from a date string
    private String getDayOfWeek(String date)
    {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            Date activityDate = dateFormat.parse(date);
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(activityDate);
        }
        catch (ParseException e)
        {
            Log.e(TAG, ERROR_LOG + date, e);
            return null;
        }
    }

    private void logUpdatedPlan(TrainingPlan trainingPlan)
    {
        for (TrainingPlan.TrainingWeek week : trainingPlan.getTraining_weeks())
        {
            Log.d(TAG, "Updated Plan - Week: " + week.getWeek());

            TrainingPlan.Days days = week.getTraining_plan();
            logDayPlan(days.getMonday(), MONDAY);
            logDayPlan(days.getTuesday(), TUESDAY);
            logDayPlan(days.getWednesday(), WEDNESDAY);
            logDayPlan(days.getThursday(), THURSDAY);
            logDayPlan(days.getFriday(), FRIDAY);
            logDayPlan(days.getSaturday(), SATURDAY);
            logDayPlan(days.getSunday(), SUNDAY);
        }
    }

    private void logDayPlan(TrainingPlan.Day day, String dayName)
    {
        if (day == null)
        {
            Log.d(TAG, dayName + ": No plan");
            return;
        }
        Log.d(TAG, dayName + ": " + day.getExercise() + " - " + day.getDistance() + " @ " + day.getPace());
    }

    private boolean activityMatchesPlan(Activity activity, TrainingPlan.Day day)
    {
        Log.d(TAG, "Checking activity: " + activity.getType() + ", " + activity.getDistance() + " meters, " + activity.getMoving_time() + "s");
        // Compare activity details with the training plan
        if (!activity.getType().equals("Run"))
        {
            Log.d(TAG, "Activity type does not match: " + activity.getType());
            return false; // Only running activities count
        }

        // Check pace (if applicable)
        if (day.getPace() != null)
        {
            float activityPace = activity.getMoving_time() / activity.getDistance(); // Pace in seconds per unit distance
            float requiredPace = convertPaceToSeconds(day.getPace());
            if (activityPace > requiredPace)
            {
                Log.d(TAG, "Activity pace is too slow: " + activityPace + " > " + requiredPace);
                return false; // Pace is too slow
            }
        }

        Log.d(TAG, "Activity matches the plan");
        return true;
    }

    private void trackPerformance(Activity activity, TrainingPlan.Day day, String week, String dayName)
    {

        float activityDistanceMiles = UnitConverter.metersToMiles(activity.getDistance());
        float activityTime = activity.getMoving_time();
        float averagePaceSecPerMile = activityTime / activityDistanceMiles;
        performanceData.put(week + " - " + dayName, averagePaceSecPerMile);
        for (Map.Entry<String, Float> entry : performanceData.entrySet())
        {
            Log.d(TAG, "Performance Data for " + entry.getKey() + ": " + entry.getValue());
        }


        // Log performance data
        Log.d(TAG,  week + ", " + dayName + ": Average Pace = " + convertSecondsToPace((int) averagePaceSecPerMile) + " min/mile");
        Log.d(TAG, "Distance: " + activityDistanceMiles + " miles (" + activity.getDistance() + " meters)");
        Log.d(TAG, week + ", " + dayName + ": Average Heart Rate = " + activity.getAverage_heartrate());

        // Adjust pace based on heart rate
        if (isHeartRateTooHigh(activity))
        {
            float requiredPace = convertPaceToSeconds(day.getPace());
            float newPace = requiredPace * 1.05f; // Decrease pace by 5%
            day.setPace(convertSecondsToPace((int) newPace));
            Log.d(TAG, "Week " + week + ", " + dayName + ": Decreased pace to " + convertSecondsToPace((int) newPace) + " due to high heart rate");
        }
    }

    private boolean isHeartRateTooHigh(Activity activity)
    {
        float averageHeartRate = activity.getAverage_heartrate();
        int targetMaxHeartRate = 160; // Need to refine this part
        return averageHeartRate > targetMaxHeartRate;
    }

    private Date parseDate(String dateStr)
    {
        try
        {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            return dateFormat.parse(dateStr);
        }
        catch (ParseException e)
        {
            Log.e(TAG, ERROR_LOG + dateStr, e);
            return null;
        }
    }

    private List<Activity> filterActivitiesByDate(List<Activity> activities)
    {
        List<Activity> filteredActivities = new ArrayList<>();

        Date startDate = parseDate(MainActivity.START_DATE);
        Date endDate = parseDate(MainActivity.END_DATE);
        // Filtering null dates
        if (startDate == null || endDate == null) {
            Log.e(TAG, "Invalid date range. Skipping filtering.");
            return filteredActivities;
        }

        for (Activity activity : activities)
        {
            Date activityDate = parseDate(activity.getStart_date());
            if (activityDate != null && !activityDate.before(startDate) && !activityDate.after(endDate))
            {
                filteredActivities.add(activity);
            }
        }
        return filteredActivities;
    }
}