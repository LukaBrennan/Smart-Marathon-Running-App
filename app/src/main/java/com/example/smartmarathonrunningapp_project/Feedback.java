package com.example.smartmarathonrunningapp_project;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Feedback {
    private static final String TAG = "Feedback";

    public static class FeedbackResult {
        private final String feedback;
        private final float paceSlope;
        private final float heartRateSlope;
        private final float distanceSlope;

        public FeedbackResult(String feedback, float paceSlope, float heartRateSlope, float distanceSlope) {
            this.feedback = feedback;
            this.paceSlope = paceSlope;
            this.heartRateSlope = heartRateSlope;
            this.distanceSlope = distanceSlope;
        }

        public String getFeedback() {
            return feedback;
        }

        public float getPaceSlope() {
            return paceSlope;
        }

        public float getHeartRateSlope() {
            return heartRateSlope;
        }

        public float getDistanceSlope() {
            return distanceSlope;
        }
    }

    public static FeedbackResult getFitnessFeedback(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return new FeedbackResult("No activities found to analyze fitness trends.", 0, 0, 0);
        }

        Map<String, Float> weeklyAveragePace = new HashMap<>();
        Map<String, Float> weeklyHeartRate = new HashMap<>();
        Map<String, Float> weeklyDistance = new HashMap<>();

        for (Activity activity : activities) {
            String week = getWeekOfYear(activity.getStart_date());
            float pace = activity.getMoving_time() / activity.getDistance();
            float heartRate = activity.getAverage_heartrate();
            float distance = activity.getDistance();

            weeklyAveragePace.put(week, weeklyAveragePace.containsKey(week) ? (weeklyAveragePace.get(week) + pace) / 2 : pace);
            weeklyHeartRate.put(week, weeklyHeartRate.containsKey(week) ? (weeklyHeartRate.get(week) + heartRate) / 2 : heartRate);
            weeklyDistance.put(week, weeklyDistance.containsKey(week) ? weeklyDistance.get(week) + distance : distance);
        }

        List<Float> paceTrends = new ArrayList<>(weeklyAveragePace.values());
        List<Float> heartRateTrends = new ArrayList<>(weeklyHeartRate.values());
        List<Float> distanceTrends = new ArrayList<>(weeklyDistance.values());

        float paceSlope = calculateTrend(paceTrends);
        float heartRateSlope = calculateTrend(heartRateTrends);
        float distanceSlope = calculateTrend(distanceTrends);

        StringBuilder feedback = new StringBuilder();
        feedback.append("Fitness Trends:\n");
        feedback.append("Pace Trend: ").append(paceSlope < 0 ? "Improving" : "Stable/Declining").append("\n");
        feedback.append("Heart Rate Trend: ").append(heartRateSlope < 0 ? "Improving" : "Stable/Declining").append("\n");
        feedback.append("Distance Trend: ").append(distanceSlope > 0 ? "Improving" : "Stable/Declining").append("\n");

        Log.d(TAG, feedback.toString());
        return new FeedbackResult(feedback.toString(), paceSlope, heartRateSlope, distanceSlope);
    }

    private static float calculateTrend(List<Float> values) {
        if (values == null || values.size() < 2) {
            return 0;
        }

        float sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = values.size();

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }

    private static String getWeekOfYear(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date activityDate = dateFormat.parse(date);
            SimpleDateFormat weekFormat = new SimpleDateFormat("w");
            return weekFormat.format(activityDate);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + date, e);
            return null;
        }
    }
}