package com.example.smartmarathonrunningapp_project;
import com.example.smartmarathonrunningapp_project.data.DataSource;
import com.example.smartmarathonrunningapp_project.data.MockDataSource;
import com.example.smartmarathonrunningapp_project.data.StravaDataSource;
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
import android.util.TypedValue;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.smartmarathonrunningapp_project.dating.AnchorManager;
import com.example.smartmarathonrunningapp_project.dating.PlanDateUtils;
import com.example.smartmarathonrunningapp_project.managers.TrainingPlanManager;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import java.util.*;
import java.util.stream.Collectors;
/*
    Entry point of the app: manages UI, data fetching, and plan adjustment.
Responsibilities:
    Fetch activities from Strava (live) or assets (mock simulation).
    Maintain PerformanceData (runs history).
    Anchor training plan with dates and apply auto adjustments.
    Render weekly plan and traffic light statuses in dynamic UI.
*/
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final float MIN_VALID_RUN_METERS = (4f - 0.10f) * 1609.344f;
    private final Map<String, String> latestMatchInfo = new HashMap<>();
    private final Map<String, String> latestLights = new HashMap<>();
    private final PerformanceData performanceData = new PerformanceData();

    private DataSource liveSource;
    private DataSource mockSource;

    private TrainingPlanManager planManager;
    private AutoAdjuster autoAdjuster;
    private int COLOR_GREEN, COLOR_YELLOW, COLOR_RED, COLOR_GRAY;

    private int dp(int dps) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dps, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initColors();
        initManagers();
        setupUI();

        TrainingPlan initial = planManager.loadAdjustedPlan();
        if (initial == null) initial = planManager.loadBasePlan();

        String existingAnchor = AnchorManager.getAnchor(this);
        if (existingAnchor != null && initial != null) {
            PlanDateUtils.applyDatesFromAnchor(initial, existingAnchor);
        }
        updateUI(initial);
        fetchActivities();
    }

    private void initColors() {
        COLOR_GREEN = ContextCompat.getColor(this, R.color.traffic_light_green);
        COLOR_YELLOW = ContextCompat.getColor(this, R.color.traffic_light_yellow);
        COLOR_RED    = ContextCompat.getColor(this, R.color.traffic_light_red);
        COLOR_GRAY   = ContextCompat.getColor(this, R.color.traffic_light_gray);
    }

    private void initManagers() {
        planManager = new TrainingPlanManager(this);
        autoAdjuster = new AutoAdjuster();
        liveSource = new StravaDataSource(new StravaRepository());
        mockSource = new MockDataSource(this, "MockRun.json");
    }

    private void setupUI() {
        Button simulateButton = findViewById(R.id.btnSimulateRun);
        if (simulateButton != null) {
            simulateButton.setOnClickListener(v -> onSimulateClicked());
        }

        View feedback = findViewById(R.id.feedbackButton);
        if (feedback != null) {
            feedback.setOnClickListener(v -> {
                if (performanceData.isEmpty()) {
                    Toast.makeText(this, "Fetching data. Please wait...", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, FeedbackActivity.class);
                    intent.putExtra("performanceData", performanceData.toJson());
                    startActivity(intent);
                }
            });
        }
    }

    private void fetchActivities() {
        liveSource.fetchActivities(new DataSource.ActivityListCallback() {
            @Override public void onResult(List<Activity> data) {
                handleActivities("Strava", data);
            }
            @Override public void onError(Throwable t) {
                Log.e(TAG, "Strava fetch failed", t);
                Toast.makeText(MainActivity.this, "Strava fetch failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onSimulateClicked() {
        mockSource.fetchActivities(new DataSource.ActivityListCallback() {
            @Override public void onResult(List<Activity> data) {
                handleActivities("Mock", data);
                Toast.makeText(MainActivity.this, "Simulated runs loaded", Toast.LENGTH_SHORT).show();
            }
            @Override public void onError(Throwable t) {
                Log.e(TAG, "Mock load failed", t);
                Toast.makeText(MainActivity.this, "Failed to load MockRun.json", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleActivities(String source, List<Activity> activities) {
        Log.d(TAG, "handleActivities: source=" + source + " runs=" + (activities == null ? 0 : activities.size()));

        TextView badge = findViewById(R.id.dataSourceBadge);
        if (badge != null) badge.setText("Mock".equalsIgnoreCase(source) ? "Simulated" : "Live (Strava)");

        TrainingPlan currentPlan = planManager.loadBasePlan();
        if (currentPlan == null) return;

        // >>> Only let STRAVA seed the permanent anchor
        String anchorIso = AnchorManager.getAnchor(this);
        if (anchorIso == null && "Strava".equalsIgnoreCase(source)) {
            anchorIso = AnchorManager.ensureAnchor(this, activities);
        }
        if (anchorIso != null) {
            PlanDateUtils.applyDatesFromAnchor(currentPlan, anchorIso);
            planManager.saveAdjustedPlan(currentPlan);
        }

        performanceData.clear();

        List<Activity> validActivities = activities == null ? Collections.emptyList()
                : activities.stream()
                .filter(Objects::nonNull)
                .filter(a -> {
                    String t = a.getSport_type() != null ? a.getSport_type() : a.getType();
                    return t != null && t.equalsIgnoreCase("Run");
                })
                .filter(a -> a.getDistance() >= MIN_VALID_RUN_METERS && a.getMoving_time() >= 600)
                .peek(performanceData::addActivity)
                .collect(Collectors.toList());

        Log.d(TAG, "filter: keptRuns=" + validActivities.size());

        Map<String, String> lights = calculateTrafficLights(currentPlan, validActivities);
        latestLights.clear();
        latestLights.putAll(lights);

        TrainingPlan adjusted = autoAdjuster.adjustPlan(currentPlan, validActivities, lights);

        if ("Strava".equalsIgnoreCase(source)) {
            planManager.saveAdjustedPlan(adjusted);
        }

        updateUI(adjusted);
    }

    private void updateUI(TrainingPlan plan) {
        LinearLayout weekContainer = findViewById(R.id.weekContainer);
        if (weekContainer == null) return;

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
        String distPace = prettyPlanLine(day);
        if (!distPace.isEmpty()) {
            layout.addView(makeText(distPace, 14, false));
        }

        layout.addView(makeText(prettyDate(day.getDate()), 12, false));

        if (day.getAdjustmentNote() != null) {
            TextView note = makeText("Adjusted: " + day.getAdjustmentNote(), 12, false);
            note.setTextColor(ContextCompat.getColor(this, R.color.strava_orange));
            layout.addView(note);
        }

        TextView statusTv = makeText("Status: " + status, 12, false);
        String dOnly = DateUtils.getDateOnly(day.getDate());
        if (dOnly != null) {
            String matched = latestMatchInfo.get(dOnly);
            if (matched != null) {
                TextView mt = makeText(matched, 12, false);
                mt.setTextColor(ContextCompat.getColor(this, R.color.strava_dark_text));
                layout.addView(mt);
            }
        }

        statusTv.setTextColor(ContextCompat.getColor(this, R.color.strava_orange));
        layout.addView(statusTv);

        container.addView(layout);
    }

    private String prettyPlanLine(TrainingPlan.Day day) {
        boolean restLike = false;
        String ex = day.getExercise() == null ? "" : day.getExercise().toLowerCase(Locale.US);
        if (ex.contains("rest") || ex.contains("cross")) restLike = true;

        float meters = TrainingPlan.parseDistanceToMeters(day.getDistance());
        if (meters <= 0.001f) restLike = true;

        if (restLike) return "";

        float miles = meters / 1609.344f;
        String distText = String.format(Locale.getDefault(), "%.0f mi", miles);

        String pace = day.getPace();
        if (pace == null || pace.trim().isEmpty() || "null".equalsIgnoreCase(pace.trim())) {
            return distText;
        }

        String nicePace = TrainingPlan.normalizeWeirdPaceString(pace.trim());
        return distText + " @ " + nicePace;
    }

    private String prettyDate(String isoUtc) {
        if (isoUtc == null) return "";
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date d = in.parse(isoUtc);

            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
            out.setTimeZone(java.util.TimeZone.getDefault());
            return out.format(d);
        } catch (Exception e) {
            return com.example.smartmarathonrunningapp_project.utils.DateUtils.getDateOnly(isoUtc);
        }
    }

    private TextView makeText(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setTextColor(ContextCompat.getColor(this, R.color.strava_dark_text));
        return view;
    }

    private void updateTrafficLightColor(View view, String status) {
        int color = COLOR_GRAY;
        switch (status) {
            case "GREEN":  color = COLOR_GREEN;  break;
            case "YELLOW": color = COLOR_YELLOW; break;
            case "RED":    color = COLOR_RED;    break;
        }
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(dp(18), dp(18));
        dot.setColor(color);
        dot.setStroke(dp(2), 0x33000000);
        view.setBackground(dot);
    }

    private void setupWeekStats() {
        LinearLayout stats = findViewById(R.id.weekStatsContainer);
        if (stats == null) return;
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
        latestMatchInfo.clear();
        if (plan == null || activities == null) return statusMap;

        Map<String, Activity> longestByDate = new HashMap<>();
        for (Activity a : activities) {
            if (a == null) continue;

            String t = a.getSport_type() != null ? a.getSport_type() : a.getType();
            if (t == null || !t.equalsIgnoreCase("Run")) continue;

            String dayLocal = (a.getStart_date_local() != null)
                    ? DateUtils.localDateOnly(a.getStart_date_local())
                    : DateUtils.utcIsoToLocalDateOnly(a.getStart_date());
            if (dayLocal == null) continue;

            Activity prev = longestByDate.get(dayLocal);
            if (prev == null || a.getMoving_time() > prev.getMoving_time()) {
                longestByDate.put(dayLocal, a);
            }
        }

        for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks()) {
            for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                if (day == null || day.getDate() == null) continue;
                final String plannedDateOnly = DateUtils.getDateOnly(day.getDate());

                Activity act = longestByDate.get(plannedDateOnly);
                if (act == null) continue;

                String color = TrainingPlan.getTrafficLightStatus(day, act);
                if (!"N/A".equals(color)) {
                    statusMap.put(plannedDateOnly, color);

                    float miles = act.getDistance() / 1609.344f;
                    float secPerMi = act.getMoving_time() / Math.max(miles, 0.001f);
                    int m = (int)(secPerMi / 60f);
                    int s = (int)(secPerMi % 60f);
                    latestMatchInfo.put(plannedDateOnly,
                            String.format(Locale.getDefault(), "Matched: %.1f mi @ %d:%02d/mi", miles, m, s));
                }
            }
        }
        return statusMap;
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
}
