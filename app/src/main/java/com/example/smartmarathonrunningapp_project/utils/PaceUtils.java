package com.example.smartmarathonrunningapp_project.utils;
import android.util.Log;
import java.util.Locale;

    // Utility class for converting between different pace representations
    // Handles conversions between "mm:ss" format and total seconds
public class PaceUtils
{
    // TAG for error
    private static final String TAG = "PaceUtils";
    // Code that complies with SonarQube+
    private PaceUtils()
    {
        throw new IllegalStateException("Utility class - do not instantiate");
    }
    // Converts a pace string (e.g., "8:30") to total seconds
    public static int convertPaceToSeconds(String pace)
    {
        // Validate the input
        if (pace == null || pace.isEmpty() || !pace.matches("\\d+:\\d{2}"))
        {
            Log.e(TAG, "Invalid pace format: " + pace);
            return 0;
        }
        String[] parts = pace.split(":");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        return minutes * 60 + seconds;
    }
    // Formats total seconds into a "mm:ss" pace string
    public static String convertSecondsToPace(int timeInSeconds)
    {
        int minutes = timeInSeconds / 60;
        int seconds = timeInSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}