package com.example.smartmarathonrunningapp_project;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/*
    Collects and summarizes the runner’s activities (primarily runs) into a nested structure convenient for UI and analytics:
    data["Run"][weekLabel]["yyyy-MM-dd <weekday>"] -> metric map where the metric map contains raw Strava metrics (distance, moving_time,average_speed, HR, etc.) plus derived values like pace (sec/km) and TRIMP.
 */

public class PerformanceData {

    private final Map<String, Map<String, Map<String, Map<String, Float>>>> data = new HashMap<>();
    private static final String TAG = "PerformanceData";

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final ThreadLocal<SimpleDateFormat> YMD =
            ThreadLocal.withInitial(() -> {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                f.setTimeZone(UTC);
                return f;
            });

    public void addActivity(Activity activity) {
        try {
            if (activity == null) return;

            String sportType = safe(activity.getSport_type());
            String type      = safe(activity.getType());
            boolean isRun = "run".equalsIgnoreCase(sportType) || "run".equalsIgnoreCase(type);
            if (!isRun) return;

            String iso = safe(activity.getStart_date());
            if (iso.length() < 10) return;

            String week = DateUtils.getWeekOfYear(iso);
            String dayKey = DateUtils.buildRunKey(iso);
            String dateOnly = DateUtils.getDateOnly(iso);

            Map<String, Float> m = new HashMap<>();

            // Raw Strava metrics
            m.put("distance", activity.getDistance());
            m.put("moving_time", (float) activity.getMoving_time());
            m.put("average_speed", activity.getAverage_speed());
            m.put("heart_rate", activity.getAverage_heartrate());
            m.put("max_heartrate", activity.getMax_heartrate());
            m.put("elevation", activity.getTotal_elevation_gain());

            Float pace = null;
            if (activity.getAverage_speed() > 0f)
            {
                pace = 1000f / activity.getAverage_speed();
            }
            else if (activity.getMoving_time() > 0 && activity.getDistance() > 0f)
            {
                pace = (float) activity.getMoving_time() / (activity.getDistance() / 1000f);
            }
            else if (activity.getPaceInSeconds() > 0f)
            {
                pace = activity.getPaceInSeconds();
            }
            if (pace != null && pace > 0f && !pace.isInfinite() && !pace.isNaN())
            {
                m.put("pace", pace);
            }

            float trimp = TRIMP.calculate(
                    activity.getMoving_time() / 60f,
                    activity.getAverage_heartrate(),
                    activity.getResting_heartrate(),
                    activity.getMax_heartrate(),
                    activity.isMale()
            );
            m.put("trimp", trimp);

            data
                    .computeIfAbsent("Run", k -> new TreeMap<>(Collections.reverseOrder()))
                    .computeIfAbsent(week, k -> new HashMap<>())
                    .put(dayKey, m);

        } catch (Exception e) {
            Log.e(TAG, "Error adding activity", e);
        }
    }

    public Map<String, Map<String, Map<String, Float>>> getRunData() {
        return data.getOrDefault("Run", new HashMap<>());
    }

    public boolean isEmpty() {
        Map<String, Map<String, Map<String, Float>>> runs = data.get("Run");
        return runs == null || runs.isEmpty();
    }

    public void clear() {
        data.clear();
    }

    public String toJson() {
        return new Gson().toJson(data);
    }

    public static PerformanceData fromJson(String json) {
        PerformanceData result = new PerformanceData();
        try {
            Map<String, Map<String, Map<String, Map<String, Float>>>> parsed =
                    new Gson().fromJson(json,
                            new TypeToken<Map<String, Map<String, Map<String, Map<String, Float>>>>>(){}.getType());
            if (parsed != null) result.data.putAll(parsed);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error parsing JSON", e);
        }
        return result;
    }


    public float getWeeklyDistance(String week) {
        Map<String, Map<String, Float>> weekData = getRunData().get(week);
        if (weekData == null) return 0f;
        float total = 0f;
        for (Map<String, Float> run : weekData.values()) {
            total += run.getOrDefault("distance", 0f);
        }
        return total;
    }

    public float getWeeklyAvgPace(String week) {
        Map<String, Map<String, Float>> weekData = getRunData().get(week);
        if (weekData == null || weekData.isEmpty()) return 0f;
        float total = 0f; int n = 0;
        for (Map<String, Float> run : weekData.values()) {
            Float p = run.get("pace");
            if (p != null && p > 0f) { total += p; n++; }
        }
        return n > 0 ? total / n : 0f;
    }

    public float getWeeklyAvgHR(String week) {
        Map<String, Map<String, Float>> weekData = getRunData().get(week);
        if (weekData == null || weekData.isEmpty()) return 0f;
        float total = 0f; int n = 0;
        for (Map<String, Float> run : weekData.values()) {
            Float hr = run.get("heart_rate");
            if (hr != null && hr > 0f) { total += hr; n++; }
        }
        return n > 0 ? total / n : 0f;
    }


    public static class PerformanceMetrics {
        public float acuteLoad;
        public float chronicLoad;
    }

    public PerformanceMetrics calculatePerformanceMetrics() {
        PerformanceMetrics m = new PerformanceMetrics();
        Date latest = getLatestRunDate();
        if (latest == null) return m;

        Date d7  = addDays(latest, -6);
        Date d28 = addDays(latest, -27);

        for (Map<String, Map<String, Float>> weekData : getRunData().values()) {
            for (String key : weekData.keySet()) {
                Date d = keyToDate(key);
                if (d == null) continue;
                Float trimp = Objects.requireNonNull(weekData.get(key)).get("trimp");
                if (trimp == null) continue;

                if (!d.before(d7))  m.acuteLoad   += trimp;
                if (!d.before(d28)) m.chronicLoad += trimp;
            }
        }
        return m;
    }

    public String getRelativeEffortLevel(float trimp) {
        if (trimp < 50)  return "easy";
        if (trimp < 100) return "moderate";
        return "hard";
    }

    public Map<String, Map<String, Map<String, Map<String, Float>>>> getData() {
        return data;
    }


    private static String safe(String s) { return s == null ? "" : s.trim(); }

    private static String keyYmd(String dayKey) {
        if (dayKey == null || dayKey.length() < 10) return null;
        String ymd = dayKey.substring(0, 10);
        return ymd.matches("\\d{4}-\\d{2}-\\d{2}") ? ymd : null;
    }

    private static Date keyToDate(String dayKey) {
        String ymd = keyYmd(dayKey);
        if (ymd == null) return null;
        try { return YMD.get().parse(ymd); }
        catch (ParseException e) { return null; }
    }

    private Date getLatestRunDate() {
        Date latest = null;
        for (Map<String, Map<String, Float>> week : getRunData().values()) {
            for (String key : week.keySet()) {
                Date d = keyToDate(key);
                if (d != null && (latest == null || d.after(latest))) latest = d;
            }
        }
        return latest;
    }

    private static Date addDays(Date base, int delta) {
        Calendar c = Calendar.getInstance(UTC, Locale.US);
        c.setTime(base);
        c.add(Calendar.DAY_OF_MONTH, delta);
        return c.getTime();
    }
}
