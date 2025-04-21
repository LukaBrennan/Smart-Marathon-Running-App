package com.example.smartmarathonrunningapp_project.utils;

import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class DateUtils {
    private static final String TAG = "DateUtils";
    private static final String ERROR_LOG = "failed to parse date";

    private DateUtils() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    public static Date parseDate(String dateStr) {
        try {
            return tryParseWithFormats(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, ERROR_LOG + dateStr, e);
            return null;
        }
    }

    private static Date tryParseWithFormats(String dateStr) throws ParseException {
        SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        SimpleDateFormat dateFormatWithDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            return dateFormatWithTime.parse(dateStr);
        } catch (ParseException e) {
            return dateFormatWithDateOnly.parse(dateStr);
        }
    }

    public static String getWeekOfYear(String dateStr) {
        try {
            Date date = parseDate(dateStr);
            if (date == null) return "Unknown Week";

            Calendar cal = Calendar.getInstance(Locale.US);
            cal.setTime(date);
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setMinimalDaysInFirstWeek(4);

            int year = cal.get(Calendar.YEAR);
            int week = cal.get(Calendar.WEEK_OF_YEAR);

            return String.format(Locale.US, "Week %02d-%d", week, year);
        } catch (Exception e) {
            Log.e(TAG, "Error getting week for: " + dateStr, e);
            return "Unknown Week";
        }
    }

    public static String getDayOfWeek(String dateStr) {
        try {
            Date date = parseDate(dateStr);
            if (date == null) return "Unknown Day";

            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            return dayFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error getting day for: " + dateStr, e);
            return "Unknown Day";
        }
    }
}