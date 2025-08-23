package com.example.smartmarathonrunningapp_project;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.multidex.BuildConfig;
import com.example.smartmarathonrunningapp_project.managers.TrainingPlanManager;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import java.util.*;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Map<String, String> latestLights = new HashMap<>();

    private StravaRepository stravaRepository;
    private TrainingPlanManager planManager;
    private AutoAdjuster autoAdjuster;
    private final List<Activity> latestActivities = new ArrayList<>();
    private int dp(int dps) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dps, getResources().getDisplayMetrics());
    }
    private final PerformanceData performanceData = new PerformanceData();

    private int COLOR_GREEN, COLOR_YELLOW, COLOR_RED, COLOR_GRAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initColors();
        initManagers();
        setupUI();
        fetchActivities();
    }

    private void initColors() {
        COLOR_GREEN = ContextCompat.getColor(this, R.color.traffic_light_green);
        COLOR_YELLOW = ContextCompat.getColor(this, R.color.traffic_light_yellow);
        COLOR_RED = ContextCompat.getColor(this, R.color.traffic_light_red);
        COLOR_GRAY = ContextCompat.getColor(this, R.color.traffic_light_gray);
    }

    private void initManagers() {
        stravaRepository = new StravaRepository();
        planManager = new TrainingPlanManager(this);
        autoAdjuster = new AutoAdjuster();
    }

    private void setupUI() {
        // show test buttons in debug mode
        if (BuildConfig.DEBUG) {
            findViewById(R.id.debugButtonsContainer).setVisibility(View.VISIBLE);
        }

        findViewById(R.id.feedbackButton).setOnClickListener(v -> {
            if (performanceData.isEmpty()) {
                Toast.makeText(this, "Fetching data. Please wait...", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, FeedbackActivity.class);
                intent.putExtra("performanceData", performanceData.toJson());
                startActivity(intent);
            }
        });

        // Espresso helpers
        View trafficLightView = findViewById(R.id.trafficLightView);
        View btnGreen = findViewById(R.id.btnTestGreen);
        View btnYellow = findViewById(R.id.btnTestYellow);
        View btnRed = findViewById(R.id.btnTestRed);

        if (btnGreen != null) btnGreen.setOnClickListener(v -> trafficLightView.setBackgroundColor(COLOR_GREEN));
        if (btnYellow != null) btnYellow.setOnClickListener(v -> trafficLightView.setBackgroundColor(COLOR_YELLOW));
        if (btnRed != null) btnRed.setOnClickListener(v -> trafficLightView.setBackgroundColor(COLOR_RED));

        Button simulateButton = findViewById(R.id.btnSimulateRun);
        if (simulateButton != null) {
            simulateButton.setOnClickListener(v -> {
                Activity fake = createSimulatedRedRunForPlannedDay("Tuesday"); // Testing for Tuesday
                simulateActivity(fake);
            });
        }

    }

    public void simulateActivity(Activity fakeActivity) {
        TrainingPlan currentPlan = planManager.loadAdjustedPlan();
        if (currentPlan == null) {
            Toast.makeText(this, "No training plan loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPlan.getTraining_weeks() != null) {
            for (TrainingPlan.TrainingWeek w : currentPlan.getTraining_weeks()) {
                annotateDaysWithNames(w);
            }
        }

        performanceData.addActivity(fakeActivity);

        List<Activity> fakeActivities = Collections.singletonList(fakeActivity);
        Map<String, String> lights = calculateTrafficLights(currentPlan, fakeActivities);

        latestLights.clear();
        latestLights.putAll(lights);
        Log.d(TAG, "lights=" + lights);  // Expecting light to be RED, for testing
        Toast.makeText(this, "Lights: " + lights, Toast.LENGTH_LONG).show();


        TrainingPlan adjusted = autoAdjuster.adjustPlan(currentPlan, fakeActivities, lights);
        planManager.saveAdjustedPlan(adjusted);
        updateUI(adjusted);

        Toast.makeText(this, "Simulation complete. Check the training plan view.", Toast.LENGTH_SHORT).show();
    }


    private Activity createSimulatedRedRunForPlannedDay(String weekday) {
        TrainingPlan plan = planManager.loadAdjustedPlan();
        String plannedIso = null;

        if (plan != null && plan.getTraining_weeks() != null) {
            for (TrainingPlan.TrainingWeek w : plan.getTraining_weeks()) {
                TrainingPlan.Day d = null;
                String dayKey = weekday.toUpperCase(Locale.ROOT);

                switch (dayKey) {
                    case "MONDAY":
                        d = w.getTraining_plan() != null ? w.getTraining_plan().getMonday() : null;
                        break;
                    case "TUESDAY":
                        d = w.getTraining_plan() != null ? w.getTraining_plan().getTuesday() : null;
                        break;
                    case "WEDNESDAY":
                        d = w.getTraining_plan() != null ? w.getTraining_plan().getWednesday() : null;
                        break;
                    case "THURSDAY":
                        d = w.getTraining_plan() != null ? w.getTraining_plan().getThursday() : null;
                        break;
                    case "FRIDAY":
                        d = w.getTraining_plan() != null ? w.getTraining_plan().getFriday() : null;
                        break;
                    case "SATURDAY":
                        d = w.getTraining_plan() != null ? w.getTraining_plan().getSaturday() : null;
                        break;
                    case "SUNDAY":
                        d = w.getTraining_plan() != null ? w.getTraining_plan().getSunday() : null;
                        break;
                }

                if (d != null && d.getDate() != null) {
                    plannedIso = d.getDate();   // Gettting the ISO date that is in the JSON file, i.e the Tuesday on week 11
                    break;
                }
            }
        }

        if (plannedIso == null) {
            plannedIso = DateUtils.getUpcomingWeekdayIsoUtc(java.util.Calendar.TUESDAY);
        }


        Activity a = new Activity();
        a.setDistance(5000f);
        a.setMoving_time(3600);
        a.setStart_date(plannedIso);
        a.setAverage_heartrate(175);
        a.setMax_heartrate(190);
        return a;
    }
    private void fetchActivities() {
        stravaRepository.refreshAccessToken(new Callback<TokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    stravaRepository.fetchActivities(response.body().getAccessToken(), 1, 30, new ActivityFetchCallback());
                } else {
                    Log.e(TAG, "Token refresh failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Token refresh failed", t);
            }
        });
    }

    private class ActivityFetchCallback implements Callback<List<Activity>> {
        @Override
        public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
            if (response.isSuccessful() && response.body() != null) {
                handleActivities(response.body());
            } else {
                Log.e(TAG, "Failed to fetch activities. Code: " + response.code());
            }
        }

        @Override
        public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
            Log.e(TAG, "Failed to fetch activities", t);
        }

        private void handleActivities(List<Activity> activities) {
            TrainingPlan currentPlan = planManager.loadAdjustedPlan();
            performanceData.clear();

            if (currentPlan != null && currentPlan.getTraining_weeks() != null) {
                for (TrainingPlan.TrainingWeek w : currentPlan.getTraining_weeks()) {
                    annotateDaysWithNames(w);
                }
            }

            List<Activity> validActivities = activities.stream()
                    .filter(a -> a.getDistance() > 100 && a.getMoving_time() > 60)
                    .peek(performanceData::addActivity)
                    .collect(Collectors.toList());

            if (!validActivities.isEmpty()) {
                Map<String, String> lights = calculateTrafficLights(currentPlan, validActivities);

                latestLights.clear();
                latestLights.putAll(lights);

                TrainingPlan adjusted = autoAdjuster.adjustPlan(currentPlan, validActivities, lights);
                planManager.saveAdjustedPlan(adjusted);
                currentPlan = adjusted;
            }

            updateUI(currentPlan);
        }

    }

    private void updateUI(TrainingPlan plan) {
        LinearLayout weekContainer = findViewById(R.id.weekContainer);
        weekContainer.removeAllViews();
        setupWeekStats();

        if (plan != null && plan.getTraining_weeks() != null) {
            for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks()) {
                annotateDaysWithNames(week);
                addWeekView(weekContainer, week);
                for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                    addDayView(weekContainer, day);
                }
            }
        }
    }

    private void annotateDaysWithNames(TrainingPlan.TrainingWeek week) {
        if (week == null || week.getTraining_plan() == null) return;
        if (week.getTraining_plan().getMonday()    != null) week.getTraining_plan().getMonday().setDayOfWeek("Monday");
        if (week.getTraining_plan().getTuesday()   != null) week.getTraining_plan().getTuesday().setDayOfWeek("Tuesday");
        if (week.getTraining_plan().getWednesday() != null) week.getTraining_plan().getWednesday().setDayOfWeek("Wednesday");
        if (week.getTraining_plan().getThursday()  != null) week.getTraining_plan().getThursday().setDayOfWeek("Thursday");
        if (week.getTraining_plan().getFriday()    != null) week.getTraining_plan().getFriday().setDayOfWeek("Friday");
        if (week.getTraining_plan().getSaturday()  != null) week.getTraining_plan().getSaturday().setDayOfWeek("Saturday");
        if (week.getTraining_plan().getSunday()    != null) week.getTraining_plan().getSunday().setDayOfWeek("Sunday");
    }


    @SuppressLint("SetTextI18n")
    private void addDayView(LinearLayout container, TrainingPlan.Day day) {
        if (day == null) return;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(ContextCompat.getDrawable(this, R.drawable.day_item_bg));
        layout.setPadding(dp(16), dp(12), dp(16), dp(12));
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        header.setGravity(Gravity.CENTER_VERTICAL);

        View trafficLight = new View(this);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        dotLp.rightMargin = dp(8);
        trafficLight.setLayoutParams(dotLp);

        String status = determineTrafficLight(day);
        updateTrafficLightColor(trafficLight, status);

        TextView dayTv = makeText(day.getDayOfWeek(), 16, true);

        header.addView(trafficLight);
        header.addView(dayTv);
        layout.addView(header);

        layout.addView(makeText(day.getExercise(), 14, false));
        layout.addView(makeText(day.getDistance() + " @ " + day.getPace(), 14, false));

        if (day.getAdjustmentNote() != null) {
            TextView note = makeText("Adjusted: " + day.getAdjustmentNote(), 12, false);
            note.setTextColor(ContextCompat.getColor(this, R.color.strava_orange));
            layout.addView(note);
        }

        TextView statusTv = makeText("Status: " + status, 12, false);
        statusTv.setTextColor(ContextCompat.getColor(this, R.color.strava_orange));
        layout.addView(statusTv);

        container.addView(layout);
    }


    private TextView makeText(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextColor(ContextCompat.getColor(this, R.color.strava_dark_text));
        return view;
    }

    private String determineTrafficLight(TrainingPlan.Day day) {
        if (day == null || day.getDate() == null) return "N/A";

        final String dateOnly = DateUtils.getDateOnly(day.getDate());

        String v = latestLights.get(dateOnly);
        if (v != null) return v;

        for (Map<String, Map<String, Float>> runsByWeek : performanceData.getRunData().values()) {
            for (Map.Entry<String, Map<String, Float>> entry : runsByWeek.entrySet()) {
                String key = entry.getKey();
                if (key != null && key.startsWith(dateOnly)) {
                    Map<String, Float> run = entry.getValue();
                    if (run == null) continue;

                    Activity act = new Activity();
                    act.setDistance(run.getOrDefault("distance", 0f));
                    act.setMoving_time(run.getOrDefault("moving_time", 0f).intValue());
                    act.setAverage_heartrate(run.getOrDefault("heart_rate", 0f));
                    act.setMax_heartrate(run.getOrDefault("max_heartrate", 0f));

                    return TrainingPlan.getTrafficLightStatus(day, act);
                }
            }
        }

        return "N/A";
    }



    private void updateTrafficLightColor(View view, String status) {
        int color = COLOR_GRAY;
        switch (status) {
            case "GREEN":  color = COLOR_GREEN;  break;
            case "YELLOW": color = COLOR_YELLOW; break;
            case "RED":    color = COLOR_RED;    break;
        }
        Log.d("TL", "status=" + status + " colorInt=#" + Integer.toHexString(color));

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(dp(18), dp(18));
        dot.setColor(color);
        dot.setStroke(dp(2), 0x33000000);
        view.setBackground(dot);
    }



    private void setupWeekStats() {
        LinearLayout stats = findViewById(R.id.weekStatsContainer);
        stats.removeAllViews();

        List<String> weeks = new ArrayList<>(performanceData.getRunData().keySet());
        weeks.sort(Collections.reverseOrder());

        if (weeks.isEmpty()) {
            addStat(stats, "0 km", "Distance");
            addStat(stats, "--", "Avg Pace");
            addStat(stats, "--", "Avg HR");
            return;
        }

        String week = weeks.get(0);
        addStat(stats, String.format(Locale.getDefault(), "%.1f km", performanceData.getWeeklyDistance(week) / 1000), "Distance");
        addStat(stats, DailyFeedbackGenerator.formatPace(performanceData.getWeeklyAvgPace(week)) + "/km", "Avg Pace");
        addStat(stats, String.format(Locale.getDefault(), "%.0f bpm", performanceData.getWeeklyAvgHR(week)), "Avg HR");
    }

    private void addStat(LinearLayout container, String value, String label) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        layout.addView(makeText(value, 18, true));
        layout.addView(makeText(label, 12, false));
        container.addView(layout);
    }

    private void addWeekView(LinearLayout container, TrainingPlan.TrainingWeek week) {
        TextView title = makeText("Week " + week.getWeek(), 18, true);
        title.setTextColor(ContextCompat.getColor(this, R.color.strava_orange));
        container.addView(title);
    }

    private List<TrainingPlan.Day> getDaysOfWeek(TrainingPlan.TrainingWeek week) {
        return Arrays.asList(
                week.getTraining_plan().getMonday(),
                week.getTraining_plan().getTuesday(),
                week.getTraining_plan().getWednesday(),
                week.getTraining_plan().getThursday(),
                week.getTraining_plan().getFriday(),
                week.getTraining_plan().getSaturday(),
                week.getTraining_plan().getSunday()
        );
    }


    private Map<String, String> calculateTrafficLights(TrainingPlan plan, List<Activity> activities) {
        Map<String, String> statusMap = new HashMap<>();
        if (plan == null || activities == null) return statusMap;

        for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks()) {
            for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                if (day == null || day.getDate() == null) continue;

                final String plannedDateOnly = DateUtils.getDateOnly(day.getDate());

                for (Activity act : activities) {
                    if (act == null || act.getStart_date() == null) continue;

                    String actualDateOnly = DateUtils.getDateOnly(act.getStart_date());
                    if (plannedDateOnly.equalsIgnoreCase(actualDateOnly)) {
                        statusMap.put(plannedDateOnly, TrainingPlan.getTrafficLightStatus(day, act));
                        break;
                    }
                }
            }
        }
        return statusMap;
    }


}
