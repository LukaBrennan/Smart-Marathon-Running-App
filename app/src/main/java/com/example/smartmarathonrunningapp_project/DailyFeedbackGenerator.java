package com.example.smartmarathonrunningapp_project;
import java.util.Locale;
    //  This is legacy code that is currently only used for 1 section in the MainActivity class
public class DailyFeedbackGenerator
{
    public static String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }
}