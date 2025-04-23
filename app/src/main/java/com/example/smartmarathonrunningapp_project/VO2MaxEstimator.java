package com.example.smartmarathonrunningapp_project;

public class VO2MaxEstimator {
    // Jack Daniels' VDOT formula
    public static float fromRacePerformance(float distanceMeters, float timeSeconds) {
        float distanceKm = distanceMeters / 1000f;
        float timeMinutes = timeSeconds / 60f;
        float velocity = distanceKm / (timeMinutes / 60f); // km/h

        return (float)(-4.60 + 0.182258 * velocity + 0.000104 * Math.pow(velocity, 2));
    }

    // Uth-Sørensen estimation
    public static float fromHeartRate(float restingHR, float maxHR) {
        return 15.3f * (maxHR / restingHR);
    }
}