package com.example.smartmarathonrunningapp_project.dating;
import android.content.Context;
import android.content.SharedPreferences;
import com.example.smartmarathonrunningapp_project.Activity;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
/*
    Computes and persists the “Week-11 Monday” anchor date that the training plan is stamped from. This anchor is derived from the *oldest Strava run.
How it works:
    1) Look through activities to find the oldest RUN (by UTC yyyy-MM-dd).
    2) Snap that date back to the Monday of that week.
    3) Persist that Monday (yyyy-MM-dd in UTC) in SharedPreferences.
 */
public final class AnchorManager {
    private static final String PREFS = "plan_prefs";
    private static final String KEY   = "anchor_week11_monday";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final ThreadLocal<SimpleDateFormat> ISO_DATE =
            ThreadLocal.withInitial(() -> {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                f.setTimeZone(UTC);
                return f;
            });
    private AnchorManager() {}
    public static String ensureAnchor(Context ctx, List<Activity> activities) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String iso = p.getString(KEY, null);
        if (iso != null) return iso;
        if (activities == null || activities.isEmpty()) return null;
        Date oldestDate = null;
        for (Activity a : activities) {
            if (a == null) continue;
            String sportType = safe(a.getSport_type());
            String type      = safe(a.getType());
            String start     = safe(a.getStart_date());
            boolean isRun = "run".equalsIgnoreCase(sportType) || "run".equalsIgnoreCase(type);
            if (!isRun || start.length() < 10) continue;
            String yyyyMmDd = start.substring(0, 10); // YYYY-MM-DD from Strava UTC
            Date d = parseIsoDate(yyyyMmDd);
            if (d == null) continue;
            if (oldestDate == null || d.before(oldestDate)) {
                oldestDate = d;
            }
        }
        if (oldestDate == null) return null;
        Calendar cal = Calendar.getInstance(UTC, Locale.US);
        cal.setTime(oldestDate);
        zeroTime(cal);
        int dow = cal.get(Calendar.DAY_OF_WEEK); // Sun=1, Mon=2...
        int back = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -back);
        String week11MondayIso = ISO_DATE.get().format(cal.getTime());
        p.edit().putString(KEY, week11MondayIso).apply();
        return week11MondayIso;
    }
    public static String getAnchor(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
    }
    private static void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }
    private static Date parseIsoDate(String s) {
        try { return ISO_DATE.get().parse(s); }
        catch (ParseException e) { return null; }
    }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
