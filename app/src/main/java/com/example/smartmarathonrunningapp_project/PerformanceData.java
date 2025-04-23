package com.example.smartmarathonrunningapp_project;

import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PerformanceData {
    // Structure: ActivityType -> Week -> Day -> Metrics
    private final Map<String, Map<String, Map<String, Map<String, Float>>>> data = new HashMap<>();

    public void addActivity(Activity activity) {
        try {
            String type = activity.getType();
            String week = DateUtils.getWeekOfYear(activity.getStart_date());
            String day = DateUtils.getDayOfWeek(activity.getStart_date());

            Map<String, Float> metrics = new HashMap<>();
            metrics.put("distance", activity.getDistance());
            metrics.put("pace", activity.getPaceInSeconds());
            metrics.put("heart_rate", activity.getAverage_heartrate());
            metrics.put("elevation", activity.getTotal_elevation_gain());

            // Calculate and add TRIMP
            float trimp = TRIMP.calculate(
                    activity.getMoving_time()/60f,
                    activity.getAverage_heartrate(),
                    activity.getResting_heartrate(),
                    activity.getMax_heartrate(),
                    activity.isMale()
            );
            metrics.put("trimp", trimp);

            data.computeIfAbsent(type, k -> new TreeMap<>(Collections.reverseOrder()))
                    .computeIfAbsent(week, k -> new HashMap<>())
                    .put(day, metrics);
        } catch (Exception e) {
            Log.e("PerformanceData", "Error adding activity", e);
        }
    }
    public Map<String, Map<String, Map<String, Float>>> getRunData() {
        return data.getOrDefault("Run", new HashMap<>());
    }

    public boolean isEmpty() {
        return data.isEmpty();
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
                    new Gson().fromJson(json, new TypeToken<Map<String, Map<String, Map<String, Map<String, Float>>>>>(){}.getType());
            if (parsed != null) {
                result.data.putAll(parsed);
            }
        } catch (Exception e) {
            Log.e("PerformanceData", "Error parsing JSON", e);
        }
        return result;
    }

    public List<String> getActivityTypes() {
        return new ArrayList<>(data.keySet());
    }

    public List<String> getWeeks(String activityType) {
        if (data.containsKey(activityType)) {
            return new ArrayList<>(data.get(activityType).keySet());
        }
        return new ArrayList<>();
    }

    public Map<String, Map<String, Float>> getWeeklyData(String activityType, String week) {
        try {
            return data.get(activityType).get(week);
        } catch (NullPointerException e) {
            return null;
        }
    }
}