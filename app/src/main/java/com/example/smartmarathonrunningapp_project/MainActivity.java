package com.example.smartmarathonrunningapp_project;
import android.annotation.SuppressLint;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
{
    private StravaRepository stravaRepository;
    // Dates for getting STRAVA data
    public static final String START_DATE = "2023-08-07";
    public static final String END_DATE = "2023-10-29";
    private TrainingPlan trainingPlan;
    private final Map<String, Float> performanceData = new HashMap<>();

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

        // Fetch activities from Strava and check compliance with the training plan
        fetchAndCheckActivities();
    }

    // Fetch activities from Strava and check compliance with the training plan
    private void fetchAndCheckActivities()
    {
        stravaRepository.refreshAccessToken(new Callback<>()
        {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    String accessToken = response.body().getAccessToken();
                    Log.d(TAG, "Access token refreshed: " + accessToken);

                    // Fetch activities using the access token
                    stravaRepository.fetchActivities(accessToken, 1, 100, new Callback<>()
                    {
                        @Override
                        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
                        {
                            if (response.isSuccessful() && response.body() != null)
                            {
                                List<Activity> activities = response.body();
                                Log.d(TAG, "Fetched activities: " + activities.size());

                                // Log the start dates of the fetched activities
                                for (Activity activity : activities)
                                {
                                    String startDate = activity.getStart_date();
                                    Log.d(TAG, "Activity: " + activity.getName() + ", Date: " + startDate);
                                }

                                // Filter activities locally to ensure they fall within the specified date range
                                List<Activity> filteredActivities = filterActivitiesByDate(activities);
                                Log.d(TAG, "Filtered activities: " + filteredActivities.size());

                                // Process one run per day
                                List<Activity> oneRunPerDay = getOneRunPerDay(filteredActivities);
                                Log.d(TAG, "One run per day: " + oneRunPerDay.size());

                                // Load the training plan
                                if (trainingPlan != null)
                                {
                                    // Process the first day and update the next training day's pace
                                    processFirstDayAndUpdateNextDay(oneRunPerDay, trainingPlan);

                                    // Log the updated training plan
                                    logUpdatedPlan(trainingPlan);

                                    // Display the updated training plan on the screen
                                    updateUI(trainingPlan);
                                }
                                else
                                {
                                    Log.e(TAG, "Failed to load training plan.");
                                }
                            }
                            else
                            {
                                Log.e(TAG, "Failed to fetch activities. Response code: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t)
                        {
                            Log.e(TAG, "Failed to fetch activities", t);
                        }
                    });
                }
                else
                {
                    Log.e(TAG, "Failed to refresh token. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t)
            {
                Log.e(TAG, "Token refresh failed", t);
            }
        });
    }

    //TODO - May remove for now, possible cause for errors
        // Process one run per day
    private List<Activity> getOneRunPerDay(List<Activity> activities)
        {
        Map<String, Activity> runsByDate = new HashMap<>();
        for (Activity activity : activities)
        {
            String activityDate = activity.getStart_date().split("T")[0]; // Extract date part (e.g., "2023-08-07")
            if (!runsByDate.containsKey(activityDate))
            {
                runsByDate.put(activityDate, activity); // Add the first run for each date
            }
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


    // TODO - Might comment this section out, not really needed for now
    // Get the corresponding day by name
    private TrainingPlan.Day getDayByName(TrainingPlan.TrainingWeek week, String dayName)
    {
        switch (dayName)
        {
            case MONDAY:
                return week.getTraining_plan().getMonday();
            case TUESDAY:
                return week.getTraining_plan().getTuesday();
            case WEDNESDAY:
                return week.getTraining_plan().getWednesday();
            case THURSDAY:
                return week.getTraining_plan().getThursday();
            case FRIDAY:
                return week.getTraining_plan().getFriday();
            case SATURDAY:
                return week.getTraining_plan().getSaturday();
            case SUNDAY:
                return week.getTraining_plan().getSunday();
            default:
                return null;
        }
    }

    // Get the next training day's name
    private String getNextDayName(String currentDayName) {
        switch (currentDayName) {
            case MONDAY:
                return TUESDAY;
            case TUESDAY:
                return WEDNESDAY;
            case WEDNESDAY:
                return THURSDAY;
            case THURSDAY:
                return FRIDAY;
            case FRIDAY:
                return SATURDAY;
            case SATURDAY:
                return SUNDAY;
            case SUNDAY:
                return MONDAY; // Assuming the next week starts with Monday
            default:
                return null;
        }
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

        // TODO - Might need to remove or alter, Possible cause for the NAN value errors in the LogCat info
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
            float averagePaceSecPerMile = activityTime / activityDistanceMiles;
            float requiredPace = parseTime(nextDay.getPace());

            // TODO - No check for maintain pace, need improve on this section using a better check
            if (averagePaceSecPerMile  < requiredPace)
            {
                // Runner is faster than the target pace
                float newPace = requiredPace * 0.95f; // Increase pace by 5%
                nextDay.setPace(formatTime((int) newPace));
                Log.d(TAG, "Next day (" + nextDayName + "): Increased pace to " + formatTime((int) newPace));
            }
            else if (averagePaceSecPerMile  > requiredPace)
            {
                // Runner is slower than the target pace
                float newPace = requiredPace * 1.05f; // Decrease pace by 5%
                nextDay.setPace(formatTime((int) newPace));
                Log.d(TAG, "Next day (" + nextDayName + "): Decreased pace to " + formatTime((int) newPace));
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
            addDayTextView(week.getTraining_plan().getMonday(), "Monday", weekContainer);
            addDayTextView(week.getTraining_plan().getTuesday(), "Tuesday", weekContainer);
            addDayTextView(week.getTraining_plan().getWednesday(), "Wednesday", weekContainer);
            addDayTextView(week.getTraining_plan().getThursday(), "Thursday", weekContainer);
            addDayTextView(week.getTraining_plan().getFriday(), "Friday", weekContainer);
            addDayTextView(week.getTraining_plan().getSaturday(), "Saturday", weekContainer);
            addDayTextView(week.getTraining_plan().getSunday(), "Sunday", weekContainer);
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

    // Save the updated training plan to a JSON file
//    private void saveTrainingPlanToFile(TrainingPlan trainingPlan, String filePath) {
//        try (FileWriter writer = new FileWriter(filePath)) {
//            Gson gson = new Gson();
//            gson.toJson(trainingPlan, writer);
//            Log.d(TAG, "Training plan saved to " + filePath);
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to save training plan", e);
//        }
//    }

    // Parse distance string (e.g., "8 mi") to float
//    private float parseDistance(String distance) {
//        if (distance == null || distance.isEmpty()) return 0;
//        return Float.parseFloat(distance.replaceAll("[^0-9.]", ""));
//    }

    // Parse pace string (e.g., "8:00") to seconds
    private int parseTime(String pace)
    {
        if (pace == null || pace.isEmpty())
        {
            return 0;
        }
        // Handle cases where pace contains multiple values (e.g., "8:00 - 7:00 - 8:00")
        String[] paceParts = pace.split(" - ");
        String firstValidPace = paceParts[0]; // Use the first valid pace value

        // Ensure the pace is in the format "mm:ss"
        if (firstValidPace.matches("\\d+:\\d{2}"))
        {
            String[] parts = firstValidPace.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        }
        else
        {
            Log.e(TAG, "Invalid pace format: " + pace);
            return 0;
        }
    }

    // Format time in seconds to "mm:ss"
    @SuppressLint("DefaultLocale")
    private String formatTime(int timeInSeconds)
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
            Log.e(TAG, "Failed to parse date: " + date, e);
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
            float requiredPace = parseTime(day.getPace());
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
        float averageHeartRate = activity.getAverage_heartrate();
        performanceData.put(week + " - " + dayName, averagePaceSecPerMile);

        // Log performance data
        Log.d(TAG, "Week " + week + ", " + dayName + ": Average Pace = " + formatTime((int) averagePaceSecPerMile) + " min/mile");
        Log.d(TAG, "Distance: " + activityDistanceMiles + " miles (" + activity.getDistance() + " meters)");
        Log.d(TAG, "Week " + week + ", " + dayName + ": Average Heart Rate = " + activity.getAverage_heartrate());

        // Adjust pace based on heart rate
        if (isHeartRateTooHigh(activity))
        {
            float requiredPace = parseTime(day.getPace());
            float newPace = requiredPace * 1.05f; // Decrease pace by 5%
            day.setPace(formatTime((int) newPace));
            Log.d(TAG, "Week " + week + ", " + dayName + ": Decreased pace to " + formatTime((int) newPace) + " due to high heart rate");
        }
    }

    private boolean isHeartRateTooHigh(Activity activity)
    {
        float averageHeartRate = activity.getAverage_heartrate();
        int targetMaxHeartRate = 160; // Need to refine this part
        return averageHeartRate > targetMaxHeartRate;
    }

    // Helper method to convert date string to Unix timestamp
//    private long convertDateToUnixTimestamp(String dateStr) {
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        try {
//            Date date = dateFormat.parse(dateStr);
//            return date.getTime() / 1000; // Convert to seconds
//        } catch (ParseException e) {
//            Log.e(TAG, "Failed to parse date: " + dateStr, e);
//            return 0; // Return 0 if parsing fails
//        }
//    }

    // Filter activities by date range
// Filter activities by date range
    private List<Activity> filterActivitiesByDate(List<Activity> activities) {
        List<Activity> filteredActivities = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

        try
        {
            Date startDate = dateFormat.parse(MainActivity.START_DATE + "T00:00:00Z"); // Start of the start date
            Date endDate = dateFormat.parse(MainActivity.END_DATE + "T23:59:59Z"); // End of the end date

            for (Activity activity : activities)
            {
                String activityDateStr = activity.getStart_date();
                Date activityDate = dateFormat.parse(activityDateStr);

                // Check if the activity date falls within the specified range
                if (activityDate != null && !activityDate.before(startDate) && !activityDate.after(endDate))
                {
                    filteredActivities.add(activity);
                }
            }
        }
        catch (ParseException e)
        {
            Log.e(TAG, "Failed to parse date", e);
        }
        return filteredActivities;
    }
}