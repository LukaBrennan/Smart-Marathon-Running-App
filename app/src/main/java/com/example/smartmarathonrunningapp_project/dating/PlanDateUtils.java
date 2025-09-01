package com.example.smartmarathonrunningapp_project.dating;
import com.example.smartmarathonrunningapp_project.TrainingPlan;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
/*
    Stamps concrete calendar dates onto the TrainingPlan using a Week-11 Monday anchor.
Mechanism:
    Each week in the JSON has a label (e.g., "11", "10", ..., "Race week").
    We convert that label into an offset (in weeks) relative to Week-11.
    For each day (Mon..Sun) we assign yyyy-MM-dd (UTC) and a day-of-week label.
*/
public final class PlanDateUtils {
    private PlanDateUtils() {}
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final ThreadLocal<SimpleDateFormat> DATE_FMT =
            ThreadLocal.withInitial(() -> {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                f.setTimeZone(UTC);
                return f;
            });
    public static void applyDatesFromAnchor(TrainingPlan plan, String week11MondayIso) {
        if (plan == null || plan.getTraining_weeks() == null || week11MondayIso == null) return;

        Calendar base;
        try {
            base = Calendar.getInstance(UTC, Locale.US);
            base.setTime(DATE_FMT.get().parse(week11MondayIso));
            zeroTime(base);
        } catch (Exception e) {
            return;
        }
        for (TrainingPlan.TrainingWeek w : plan.getTraining_weeks()) {
            if (w == null || w.getTraining_plan() == null) continue;
            int offWeeks = offsetForWeekLabel(safe(w.getWeek()));
            if (offWeeks < 0) continue;
            Calendar monday = (Calendar) base.clone();
            monday.add(Calendar.DATE, offWeeks * 7);
            setDay(w.getTraining_plan().getMonday(),    monday, 0);
            setDay(w.getTraining_plan().getTuesday(),   monday, 1);
            setDay(w.getTraining_plan().getWednesday(), monday, 2);
            setDay(w.getTraining_plan().getThursday(),  monday, 3);
            setDay(w.getTraining_plan().getFriday(),    monday, 4);
            setDay(w.getTraining_plan().getSaturday(),  monday, 5);
            setDay(w.getTraining_plan().getSunday(),    monday, 6);
        }
    }
    private static void setDay(TrainingPlan.Day d, Calendar monday, int plusDays) {
        if (d == null) return;
        Calendar c = (Calendar) monday.clone();
        c.add(Calendar.DATE, plusDays);
        d.setDate(DATE_FMT.get().format(c.getTime()));
        d.setDayOfWeek(dayOfWeekString(c.get(Calendar.DAY_OF_WEEK)));
    }
    private static int offsetForWeekLabel(String label) {
        switch (label) {
            case "11": return 0;
            case "10": return 1;
            case "9":  return 2;
            case "8":  return 3;
            case "7":  return 4;
            case "6":  return 5;
            case "5":  return 6;
            case "4":  return 7;
            case "3":  return 8;
            case "2":  return 9;
            case "1":  return 10;
            case "Race week": return 11;
            default: return -1;
        }
    }
    private static String dayOfWeekString(int calDow) {
        switch (calDow) {
            case Calendar.MONDAY:    return "MONDAY";
            case Calendar.TUESDAY:   return "TUESDAY";
            case Calendar.WEDNESDAY: return "WEDNESDAY";
            case Calendar.THURSDAY:  return "THURSDAY";
            case Calendar.FRIDAY:    return "FRIDAY";
            case Calendar.SATURDAY:  return "SATURDAY";
            case Calendar.SUNDAY:    return "SUNDAY";
            default: return "";
        }
    }
    private static void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
