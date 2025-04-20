package com.example.smartmarathonrunningapp_project;

public class UnitConverter {
    // This code section was given by chatGPT to get an accurate conversion of distances.

    // More precise conversion factors
    private static final float METERS_TO_MILES = 0.000621371192f;

    // Private constructor to prevent instantiation
    private UnitConverter() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    // Distance conversions
    public static float metersToMiles(float meters) {
        return meters * METERS_TO_MILES;
    }
}