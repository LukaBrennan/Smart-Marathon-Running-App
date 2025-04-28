package com.example.smartmarathonrunningapp_project;

public class VO2MaxEstimator {
    //  A VO2Max estimator, which was given by CHATGPT, this code uses two different validated methods to determine the fitness level of the runner
    public static float fromRacePerformance(float distanceMeters, float timeSeconds) {
        if (distanceMeters <= 0 || timeSeconds <= 0) return 0;

        float distanceKm = distanceMeters / 1000f;
        float timeMinutes = timeSeconds / 60f;
        float velocity = distanceKm / (timeMinutes / 60f); // km/h

        float result = (float)(-4.60 + 0.182258 * velocity + 0.000104 * Math.pow(velocity, 2));
        return Math.max(0, result); // Ensure non-negative
    }
}