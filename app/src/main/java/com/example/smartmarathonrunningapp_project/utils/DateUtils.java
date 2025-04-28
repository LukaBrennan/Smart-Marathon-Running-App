package com.example.smartmarathonrunningapp_project.utils;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
    //  Utility class for date parsing and formatting operations, providing methods to handel different date formats.
public class DateUtils
    {
        //  Logging TAGS
    private static final String TAG = "DateUtils";
    private static final String ERROR_LOG = "failed to parse date";

    //  Private constructor to prevent instantiation.
    private DateUtils()
    {
        throw new IllegalStateException("Utility class - do not instantiate");
    }

    //  Parse the date string using multiple possible formats
    public static Date parseDate(String dateStr)
    {
        try
        {
            return tryParseWithFormats(dateStr);
        }
        catch (ParseException e)
        {
            Log.e(TAG, ERROR_LOG + dateStr, e);
            return null;
        }
    }

    //  Method that tries to parse date formats using multiple formats
    private static Date tryParseWithFormats(String dateStr) throws ParseException
    {
        SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        SimpleDateFormat dateFormatWithDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try
        {
            return dateFormatWithTime.parse(dateStr);
        }
        catch (ParseException e)
        {
            return dateFormatWithDateOnly.parse(dateStr);
        }
    }

    //  Converts a date string into a standardized week identifying string
    public static String getWeekOfYear(String dateStr)
    {
        try
        {
            Date date = parseDate(dateStr);
            if (date == null) return "Unknown Week";    //  Early return for unparseable dates

            Calendar cal = Calendar.getInstance(Locale.US);
            cal.setTime(date);
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setMinimalDaysInFirstWeek(4);

            int year = cal.get(Calendar.YEAR);
            int week = cal.get(Calendar.WEEK_OF_YEAR);

            return String.format(Locale.US, "Week %02d-%d", week, year);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Error getting week for: " + dateStr, e);
            return "Unknown Week";
        }
    }
}