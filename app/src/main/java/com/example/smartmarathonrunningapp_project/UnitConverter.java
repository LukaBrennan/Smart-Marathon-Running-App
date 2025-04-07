package com.example.smartmarathonrunningapp_project;

public class UnitConverter
{
    // This code section was given by chatGPT to get an accurate conversion of distances.

    // More precise conversion factors
    private static final float METERS_TO_MILES = 0.000621371192f;
    private static final float MILES_TO_METERS = 1609.344f;
    private static final float SECONDS_PER_MINUTE = 60f;

    // Distance conversions
    public static float metersToMiles(float meters) {
        return meters * METERS_TO_MILES;
    }

    public static float milesToMeters(float miles) {
        return miles / MILES_TO_METERS;
    }

    // Pace conversions
    public static float secondsPerMeterToMinPerMile(float secondsPerMeter) {
        return (secondsPerMeter * MILES_TO_METERS) / SECONDS_PER_MINUTE;
    }

    public static float minPerMileToSecondsPerMeter(float minPerMile) {
        return (minPerMile * SECONDS_PER_MINUTE) / MILES_TO_METERS;
    }

    // Convert Strava speed (m/s) to pace (min/mile)
    public static float stravaSpeedToMinPerMile(float speedMetersPerSecond) {
        if (speedMetersPerSecond == 0) return 0;
        float secondsPerMeter = 1 / speedMetersPerSecond;
        return secondsPerMeterToMinPerMile(secondsPerMeter);
    }
}