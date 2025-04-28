package com.example.smartmarathonrunningapp_project;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.util.Log;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
public class PerformanceData
{
    private final Map<String, Map<String, Map<String, Map<String, Float>>>> data = new HashMap<>();
    public void addActivity(Activity activity)
    {
        try
        {
            String type = activity.getType();
            String week = DateUtils.getWeekOfYear(activity.getStart_date());
            String day = activity.getStart_date();  // Store full date string
            Map<String, Float> metrics = new HashMap<>();
            metrics.put("distance", activity.getDistance());
            metrics.put("pace", activity.getPaceInSeconds());
            metrics.put("heart_rate", activity.getAverage_heartrate());
            metrics.put("elevation", activity.getTotal_elevation_gain());
            metrics.put("max_heartrate", activity.getMax_heartrate());
            // Calculate and add TRIMP
            float trimp = TRIMP.calculate(activity.getMoving_time()/60f, activity.getAverage_heartrate(), activity.getResting_heartrate(), activity.getMax_heartrate(), activity.isMale());
            metrics.put("trimp", trimp);
            data.computeIfAbsent(type, k -> new TreeMap<>(Collections.reverseOrder())).computeIfAbsent(week, k -> new HashMap<>()).put(day, metrics);
        }
        catch (Exception e)
        {
            Log.e("PerformanceData", "Error adding activity", e);
        }
    }
    public Map<String, Map<String, Map<String, Float>>> getRunData()
    {
        return data.getOrDefault("Run", new HashMap<>());
    }
    public boolean isEmpty()
    {
        return data.isEmpty();
    }
    public void clear()
    {
        data.clear();
    }
    public String toJson()
    {
        return new Gson().toJson(data);
    }
    public static PerformanceData fromJson(String json)
    {
        PerformanceData result = new PerformanceData();
        try
        {
            Map<String, Map<String, Map<String, Map<String, Float>>>> parsed = new Gson().fromJson(json, new TypeToken<Map<String, Map<String, Map<String, Map<String, Float>>>>>(){}.getType());
            if (parsed != null)
            {
                result.data.putAll(parsed);
            }
        }
        catch (Exception e)
        {
            Log.e("PerformanceData", "Error parsing JSON", e);
        }
        return result;
    }
    public float getWeeklyDistance(String week)
    {
        Map<String, Map<String, Float>> weekData = getRunData().get(week);
        if (weekData == null) return 0f;

        float totalDistance = 0f;
        for (Map<String, Float> run : weekData.values())
        {
            totalDistance += run.getOrDefault("distance", 0f);
        }
        return totalDistance;
    }
    public float getWeeklyAvgPace(String week)
    {
        Map<String, Map<String, Float>> weekData = getRunData().get(week);
        if (weekData == null || weekData.isEmpty()) return 0f;
        float totalPace = 0f;
        int runCount = 0;
        for (Map<String, Float> run : weekData.values())
        {
            Float pace = run.get("pace");
            if (pace != null)
            {
                totalPace += pace;
                runCount++;
            }
        }
        return runCount > 0 ? totalPace / runCount : 0f;
    }
    public float getWeeklyAvgHR(String week)
    {
        Map<String, Map<String, Float>> weekData = getRunData().get(week);
        if (weekData == null || weekData.isEmpty()) return 0f;
        float totalHR = 0f;
        int runCount = 0;
        for (Map<String, Float> run : weekData.values())
        {
            Float hr = run.get("heart_rate");
            if (hr != null)
            {
                totalHR += hr;
                runCount++;
            }
        }
        return runCount > 0 ? totalHR / runCount : 0f;
    }
    public static class PerformanceMetrics
    {
        public float acuteLoad;  // 7-day load
        public float chronicLoad; // 28-day load
    }
    public PerformanceMetrics calculatePerformanceMetrics()
    {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.acuteLoad = calculateLoadForPeriod();
        metrics.chronicLoad = calculateLoadForPeriod();
        return metrics;
    }
    private float calculateLoadForPeriod()
    {
        float total = 0f;

        for (Map<String, Map<String, Float>> weekData : getRunData().values()) {
            for (Map<String, Float> run : weekData.values()) {
                if (run.containsKey("trimp")) {
                    total += run.get("trimp");
                }
            }
        }
        return total;
    }
    public Map<String, Map<String, Map<String, Map<String, Float>>>> getData()
    {
        return data;
    }
    public int getCurrentStreak()
    {
        int streak = 0;
        for (Map<String, Map<String, Float>> weekData : getRunData().values())
        {
            streak += weekData.size();
        }
        return Math.min(streak, 7);
    }
    public String getRelativeEffortLevel(float trimp)
    {
        if (trimp < 50) return "easy";
        if (trimp < 100) return "moderate";
        return "hard";
    }
}