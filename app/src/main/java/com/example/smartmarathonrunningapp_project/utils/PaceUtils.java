package com.example.smartmarathonrunningapp_project.utils;
import android.util.Log;

import java.util.Arrays;
import java.util.Locale;

    // Utility class for converting between different pace representations
    // Handles conversions between "mm:ss" format and total seconds
public class PaceUtils
{
    // TAG for error
    private static final String TAG = "PaceUtils";
    // Code that complies with SonarQube
    private PaceUtils()
    {
        throw new IllegalStateException("Utility class - do not instantiate");
    }
    // Converts a pace string (e.g., "8:30") to total seconds
    public static int convertPaceToSeconds(String pace)
    {
        if (pace == null || pace.isEmpty())
        {
            Log.e(TAG, "Pace string is null or empty");
            return 0;
        }
        Log.d(TAG, "Processing pace string: " + pace);
        // Handle ranges by taking the first pace value
        String[] parts = pace.split(" - ");
        String actualPace = parts[0].trim();
        // Split minutes and seconds
        String[] timeParts = actualPace.split(":");
        if (timeParts.length != 2)
        {
            Log.e(TAG, "Invalid pace format: " + pace);
            return 0;
        }
        Log.d(TAG, "Extracted time parts: " + Arrays.toString(timeParts));
        try
        {
            int minutes = Integer.parseInt(timeParts[0]);
            int seconds = Integer.parseInt(timeParts[1]);
            return minutes * 60 + seconds;
        }
        catch (NumberFormatException e)
        {
            Log.e(TAG, "Failed to parse pace: " + pace, e);
            return 0;
        }

    }
    // Formats total seconds into a "mm:ss" pace string
    public static String convertSecondsToPace(int timeInSeconds)
    {
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}