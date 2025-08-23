package com.example.smartmarathonrunningapp_project.utils;

import android.os.Build;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.util.TimeZone;

public class DateUtils {
    private static final String TAG = "DateUtils";
    private static final String ERROR_LOG = "failed to parse date";

    public static String getCurrentIsoDateTime() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new java.util.Date());
    }

    // Returns the next occurrence of the requested weekday as an ISO UTC string
    public static String getUpcomingWeekdayIsoUtc(int calendarDayConstant) {
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        int today = c.get(java.util.Calendar.DAY_OF_WEEK);
        int delta = (calendarDayConstant - today + 7) % 7;
        if (delta == 0) delta = 7; // move to next week’s same day
        c.add(java.util.Calendar.DAY_OF_YEAR, delta);

        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(c.getTime());
    }



    private DateUtils() {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

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

    public static String buildRunKey(String isoDateTime) {
        // Produces keys like "2025-07-16 Wednesday" so determineTrafficLight(...endsWith(day)) can match
        Date d = parseDate(isoDateTime);
        if (d == null) return isoDateTime + " " + getDayName(isoDateTime);

        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return df.format(d) + " " + getDayName(isoDateTime);
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

    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }



    public static String getDayName(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Unknown";

        try {
            Date date = parseDate(dateStr);
            if (date == null) {
                // If already a weekday label, accept it
                if (dateStr.equalsIgnoreCase("Monday") || dateStr.equalsIgnoreCase("Tuesday")
                        || dateStr.equalsIgnoreCase("Wednesday") || dateStr.equalsIgnoreCase("Thursday")
                        || dateStr.equalsIgnoreCase("Friday") || dateStr.equalsIgnoreCase("Saturday")
                        || dateStr.equalsIgnoreCase("Sunday")) {
                    return dateStr;
                }
                return "Unknown";
            }

            // >>> Use UTC to avoid local-timezone day rollovers <<<
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
            cal.setTime(date);

            switch (cal.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.MONDAY: return "Monday";
                case Calendar.TUESDAY: return "Tuesday";
                case Calendar.WEDNESDAY: return "Wednesday";
                case Calendar.THURSDAY: return "Thursday";
                case Calendar.FRIDAY: return "Friday";
                case Calendar.SATURDAY: return "Saturday";
                case Calendar.SUNDAY: return "Sunday";
                default: return "Unknown";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting day name", e);
            return "Unknown";
        }
    }


    public static String getDateOnly(String iso) {
        Date d = parseDate(iso);
        if (d == null) return iso;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(d);
    }

}