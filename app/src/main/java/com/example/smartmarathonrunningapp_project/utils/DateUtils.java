package com.example.smartmarathonrunningapp_project.utils;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.util.TimeZone;
/*
    Provides helper methods for parsing and formatting ISO date/time strings.
    Centralizes logic for Strava’s UTC timestamps and converting to week/day labels.
*/
public class DateUtils {
    private static final String TAG = "DateUtils";
    private static final String ERROR_LOG = "failed to parse date: ";
    private DateUtils() { throw new IllegalStateException("Utility class - do not instantiate"); }

    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return tryParseWithFormats(dateStr);
        } catch (ParseException e) {
            Log.e(TAG, ERROR_LOG + dateStr, e);
            return null;
        }
    }

    private static Date tryParseWithFormats(String dateStr) throws ParseException {
        SimpleDateFormat dateTimeZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        dateTimeZ.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat dateOnly  = new SimpleDateFormat("yyyy-MM-dd",                Locale.getDefault());
        try {
            return dateTimeZ.parse(dateStr);
        } catch (ParseException e) {
            return dateOnly.parse(dateStr);
        }
    }

    public static String buildRunKey(String isoDateTime) {
        Date d = parseDate(isoDateTime);
        if (d == null) return isoDateTime + " " + getDayName(isoDateTime);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
    public static String getDayName(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Unknown";
        try {
            Date date = parseDate(dateStr);
            if (date == null) {
                if (dateStr.equalsIgnoreCase("Monday")    ||
                        dateStr.equalsIgnoreCase("Tuesday")   ||
                        dateStr.equalsIgnoreCase("Wednesday") ||
                        dateStr.equalsIgnoreCase("Thursday")  ||
                        dateStr.equalsIgnoreCase("Friday")    ||
                        dateStr.equalsIgnoreCase("Saturday")  ||
                        dateStr.equalsIgnoreCase("Sunday")) {
                    return dateStr;
                }
                return "Unknown";
            }
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
            cal.setTime(date);
            switch (cal.get(Calendar.DAY_OF_WEEK)) {
                case Calendar.MONDAY:    return "Monday";
                case Calendar.TUESDAY:   return "Tuesday";
                case Calendar.WEDNESDAY: return "Wednesday";
                case Calendar.THURSDAY:  return "Thursday";
                case Calendar.FRIDAY:    return "Friday";
                case Calendar.SATURDAY:  return "Saturday";
                case Calendar.SUNDAY:    return "Sunday";
                default: return "Unknown";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting day name", e);
            return "Unknown";
        }
    }

    public static String getDateOnly(String iso) {
        if (iso == null) return null;
        int t = iso.indexOf('T');
        if (t >= 0) {
            String ymd = iso.substring(0, t);
            if (ymd.matches("\\d{4}-\\d{2}-\\d{2}")) return ymd;
        }
        try {
            SimpleDateFormat in  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            out.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = in.parse(iso);
            return out.format(d);
        } catch (Exception e) {
            if (iso.length() >= 10 && iso.substring(0,10).matches("\\d{4}-\\d{2}-\\d{2}")) {
                return iso.substring(0, 10);
            }
            return null;
        }
    }
    public static String localDateOnly(String isoLocal) {
        if (isoLocal == null) return null;
        int t = isoLocal.indexOf('T');
        if (t > 0) return isoLocal.substring(0, t);
        if (isoLocal.length() >= 10 && isoLocal.substring(0,10).matches("\\d{4}-\\d{2}-\\d{2}")) {
            return isoLocal.substring(0, 10);
        }
        return null;
    }

    public static String utcIsoToLocalDateOnly(String isoUtc) {
        if (isoUtc == null) return null;
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            in.setTimeZone(TimeZone.getTimeZone("UTC"));

            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            out.setTimeZone(TimeZone.getDefault());

            Date d = in.parse(isoUtc);
            return d == null ? null : out.format(d);
        } catch (Exception e) {
            return getDateOnly(isoUtc); // best-effort fallback
        }
    }
    public static String addDays(String ymd, int delta) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = f.parse(ymd);
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
            c.setTime(d);
            c.add(Calendar.DAY_OF_MONTH, delta);
            return f.format(c.getTime());
        } catch (Exception e) { return ymd; }
    }
    public static String maxYmd(Collection<String> ymds) {
        String max = null;
        for (String s : ymds) if (s != null && s.length() >= 10)
            if (max == null || s.compareTo(max) > 0) max = s;
        return max;
    }
}
