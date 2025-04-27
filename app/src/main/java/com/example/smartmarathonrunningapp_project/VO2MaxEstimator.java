package com.example.smartmarathonrunningapp_project;

public class VO2MaxEstimator {
    // Add validation to prevent negative values
    public static float fromRacePerformance(float distanceMeters, float timeSeconds) {
        if (distanceMeters <= 0 || timeSeconds <= 0) return 0;

        float distanceKm = distanceMeters / 1000f;
        float timeMinutes = timeSeconds / 60f;
        float velocity = distanceKm / (timeMinutes / 60f); // km/h

        float result = (float)(-4.60 + 0.182258 * velocity + 0.000104 * Math.pow(velocity, 2));
        return Math.max(0, result); // Ensure non-negative
    }

    public static float fromHeartRate(float restingHR, float maxHR) {
        if (restingHR <= 0 || maxHR <= 0 || restingHR >= maxHR) return 0;
        return Math.max(0, 15.3f * (maxHR / restingHR));
    }
}