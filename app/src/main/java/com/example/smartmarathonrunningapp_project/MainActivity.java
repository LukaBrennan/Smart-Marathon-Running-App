package com.example.smartmarathonrunningapp_project;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.smartmarathonrunningapp_project.managers.TrainingPlanManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
    //  This class is responsible for managing STRAVA API connections, Handling training plan adjustments and reliving the feedback ui
public class MainActivity extends AppCompatActivity
    {
        //  LOGS and initialising Key dependencies
    private static final String TAG = "MainActivity";
    private StravaRepository stravaRepository;
    private TrainingPlanManager planManager;
    private AutoAdjuster autoAdjuster;
    private final PerformanceData performanceData = new PerformanceData();

    @Override
        //  This runs the initial setup of the app when ran
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stravaRepository = new StravaRepository();
        initializeDependencies();
        setupUI();
        fetchAndCheckActivities();
        autoAdjuster = new AutoAdjuster();
    }
        //  Initializes required managers and services
    private void initializeDependencies()
    {
        planManager = new TrainingPlanManager(this);
    }
        //  Sets up UI components and event listeners
    private void setupUI()
    {
        Button btnFeedback = findViewById(R.id.feedbackButton);
        btnFeedback.setOnClickListener(v -> {
            if (performanceData.isEmpty())
            {
                Toast.makeText(this, "Fetching latest data. Please wait a moment.", Toast.LENGTH_SHORT).show();
            }
            else
            {
                launchFeedbackActivity();
            }
        });
    }
        //  Launches the feedback activity with current performance data
    private void launchFeedbackActivity()
    {
        Intent intent = new Intent(this, FeedbackActivity.class);
        intent.putExtra("performanceData", performanceData.toJson());
        startActivity(intent);
    }
        //  Initiates the process of fetching Strava activities
    private void fetchAndCheckActivities()
    {   //  Refresh the access token if needed then fetch the activities
        stravaRepository.refreshAccessToken(new TokenRefreshCallback());
    }
        //  Callback handler for token refresh operation
    private final class TokenRefreshCallback implements Callback<TokenResponse>
        {
        @Override
        public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response)
        {   //  If successful fetch the activities
            if (response.isSuccessful() && response.body() != null)
            {   //  Data gotten is the last 30 activates
                stravaRepository.fetchActivities(response.body().getAccessToken(), 1, 30, new ActivitiesCallback());
            }
            else
            {
                Log.e(TAG, "Token refresh failed. Code: " + response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
            Log.e(TAG, "Token refresh failed", t);
        }
    }
        //  Callback handler for Strava activities fetch operation
    private final class ActivitiesCallback implements Callback<List<Activity>>
        {
        @Override
        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
        {
            if (response.isSuccessful() && response.body() != null)
            {
                processFetchedActivities(response.body());
            }
            else
            {
                Log.e(TAG, "Failed to fetch activities. Code: " + response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t)
        {
            Log.e(TAG, "Failed to fetch activities", t);
        }
        //  Processes fetched activities and updates training plan
        private void processFetchedActivities(List<Activity> activities)
        {
            TrainingPlan originalPlan = planManager.loadAdjustedPlan();
            TrainingPlan currentPlan = planManager.loadAdjustedPlan();

            Log.d(TAG, "Original plan loaded: " + (originalPlan != null));
            Log.d(TAG, "Adjusted plan loaded: " + (currentPlan != null));
            Log.d(TAG, "Original and adjusted same: " + (originalPlan == currentPlan));
            //  Clear previous data and filter valid activities
            performanceData.clear();
            List<Activity> validActivities = new ArrayList<>();

            for (Activity activity : activities)
            {
                if (isValidActivity(activity))
                {
                    validActivities.add(activity);
                    performanceData.addActivity(activity);  //  Add to performance metrics
                }
            }

            Log.d(TAG, "Valid activities count: " + validActivities.size());
            //  Adjust training plan if we have valid activities
            if (!validActivities.isEmpty())
            {
                TrainingPlan newPlan = autoAdjuster.adjustPlan(currentPlan, validActivities, originalPlan);
                Log.d(TAG, "Plan after adjustment: " + newPlan.getAdjustmentNote());
                planManager.saveAdjustedPlan(newPlan);
                currentPlan = newPlan;
            }

            assert currentPlan != null;
            updateUI(currentPlan);
        }
        //  Updates all UI components with current training plan
        @SuppressLint("SetTextI18n")
        private void updateUI(TrainingPlan trainingPlan)
        {
            Log.d(TAG, "Updating UI with plan: " + trainingPlan.getAdjustmentNote());
            LinearLayout weekContainer = findViewById(R.id.weekContainer);
            weekContainer.removeAllViews();

            setupWeekStats();   //  Update weekly statistics
            //  Add all weeks and days from the training plan
            if (trainingPlan.getTraining_weeks() != null)
            {
                Log.d(TAG, "Number of weeks in plan: " + trainingPlan.getTraining_weeks().size());
                for (TrainingPlan.TrainingWeek week : trainingPlan.getTraining_weeks())
                {
                    addWeekView(weekContainer, week);
                    addDayViews(weekContainer, week);
                }
            }
            else
            {
                Log.e(TAG, "Plan or training weeks is null");
            }
        }
        //  Sets up the weekly statistics view
        private void setupWeekStats()
        {
            LinearLayout statsContainer = findViewById(R.id.weekStatsContainer);
            statsContainer.removeAllViews();

            // Get most recent week's data
            List<String> weeks = new ArrayList<>(performanceData.getRunData().keySet());
            weeks.sort(Collections.reverseOrder());

            if (weeks.isEmpty())
            {
                // Placeholders for when there is no data
                addStatView(statsContainer, "0 km", "Distance");
                addStatView(statsContainer, "--", "Avg Pace");
                addStatView(statsContainer, "--", "Avg HR");
                return;
            }
                //  Calculate and display metrics for most recent week
            String currentWeek = weeks.get(0);
            float totalDistance = performanceData.getWeeklyDistance(currentWeek);
            float avgPace = performanceData.getWeeklyAvgPace(currentWeek);
            float avgHR = performanceData.getWeeklyAvgHR(currentWeek);

            // Format and display values
            String distanceText = String.format(Locale.getDefault(), "%.1f km", totalDistance / 1000);
            String paceText = DailyFeedbackGenerator.formatPace(avgPace) + "/km";
            String hrText = String.format(Locale.getDefault(), "%.0f bpm", avgHR);
            addStatView(statsContainer, distanceText, "Distance");
            addStatView(statsContainer, paceText, "Avg Pace");
            addStatView(statsContainer, hrText, "Avg HR");
        }
        //  Creates a statistic view with value and label
        private void addStatView(LinearLayout container, String value, String label)
        {
            //  Create container layout
            LinearLayout statLayout = new LinearLayout(MainActivity.this);
            statLayout.setOrientation(LinearLayout.VERTICAL);
            statLayout.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            statLayout.setGravity(Gravity.CENTER);
            statLayout.setPadding(8, 8, 8, 8);
            //  Value text
            TextView valueView = new TextView(MainActivity.this);
            valueView.setText(value);
            valueView.setTextSize(18);
            valueView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.strava_dark_text));
            valueView.setTypeface(null, Typeface.BOLD);
            valueView.setGravity(Gravity.CENTER);
            //  Label text
            TextView labelView = new TextView(MainActivity.this);
            labelView.setText(label);
            labelView.setTextSize(12);
            labelView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.strava_dark_text));
            labelView.setAlpha(0.7f);
            labelView.setGravity(Gravity.CENTER);
            //  Add views to layout
            statLayout.addView(valueView);
            statLayout.addView(labelView);
            container.addView(statLayout);
        }
        //  Adds a week header to the training plan view
        @SuppressLint("SetTextI18n")
        private void addWeekView(LinearLayout container, TrainingPlan.TrainingWeek week)
        {
            TextView weekTextView = new TextView(MainActivity.this);
            weekTextView.setText("Week " + week.getWeek());
            weekTextView.setTextSize(18);
            weekTextView.setTypeface(null, Typeface.BOLD);
            weekTextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.strava_orange));
            weekTextView.setPadding(0, 16, 0, 8);
            container.addView(weekTextView);
        }
        //  Adds all days for a week to the training plan view
        @SuppressLint("SetTextI18n")
        private void addDayViews(LinearLayout container, TrainingPlan.TrainingWeek week)
        {
            addDayView(container, week.getTraining_plan().getMonday(), "Monday");
            addDayView(container, week.getTraining_plan().getTuesday(), "Tuesday");
            addDayView(container, week.getTraining_plan().getWednesday(), "Wednesday");
            addDayView(container, week.getTraining_plan().getThursday(), "Thursday");
            addDayView(container, week.getTraining_plan().getFriday(), "Friday");
            addDayView(container, week.getTraining_plan().getSaturday(), "Saturday");
            addDayView(container, week.getTraining_plan().getSunday(), "Sunday");
        }
        //  Creates a view for a single training day
        @SuppressLint("SetTextI18n")
        private void addDayView(LinearLayout container, TrainingPlan.Day day, String dayName)
        {
            if (day != null)
            {
                //  Create day container
                Log.d(TAG, "Adding day: " + dayName + " - " + day.getExercise());
                LinearLayout dayLayout = new LinearLayout(MainActivity.this);
                dayLayout.setOrientation(LinearLayout.VERTICAL);
                dayLayout.setBackground(ContextCompat.getDrawable(MainActivity.this, R.drawable.day_item_bg));
                dayLayout.setPadding(16, 12, 16, 12);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 8);
                dayLayout.setLayoutParams(params);
                //  Day name
                TextView dayNameView = new TextView(MainActivity.this);
                dayNameView.setText(dayName);
                dayNameView.setTextSize(16);
                dayNameView.setTypeface(null, Typeface.BOLD);
                dayNameView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.strava_dark_text));
                dayLayout.addView(dayNameView);
                //  Exercise details
                TextView exerciseView = new TextView(MainActivity.this);
                exerciseView.setText(day.getExercise());
                exerciseView.setTextSize(14);
                exerciseView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.strava_dark_text));
                dayLayout.addView(exerciseView);
                //   Distance and pace
                TextView detailsView = new TextView(MainActivity.this);
                detailsView.setText(day.getDistance() + " @ " + day.getPace());
                detailsView.setTextSize(14);
                detailsView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.strava_dark_text));
                dayLayout.addView(detailsView);
                //  Add adjustment note if present
                if (day.getAdjustmentNote() != null)
                {
                    TextView noteView = new TextView(MainActivity.this);
                    noteView.setText("Adjusted: " + day.getAdjustmentNote());
                    noteView.setTextSize(12);
                    noteView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.strava_orange));
                    noteView.setPadding(0, 4, 0, 0);
                    dayLayout.addView(noteView);
                }
                container.addView(dayLayout);
            }
        }
        //  Validates if an activity should be included in metrics
        private boolean isValidActivity(Activity activity)
        {
            return activity.getDistance() > 100 && activity.getMoving_time() > 60;
        }
    }
}




