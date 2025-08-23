package com.example.smartmarathonrunningapp_project;

import android.util.Log;

import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.google.gson.Gson;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AutoAdjuster {

    private static final String TAG = "AutoAdjuster";

    private static final float HIGH_ACUTE_TRIMP_THRESHOLD = 350f;
    private static final float FATIGUE_RATIO_THRESHOLD    = 1.15f;
    private static final int   DAYS_FOR_ACUTE_LOAD        = 7;
    private static final int   DAYS_FOR_CHRONIC_LOAD      = 28;

    private static final float RECOVERY_DISTANCE_FACTOR          = 0.8f; // -20% distance
    private static final int   RECOVERY_PACE_ADJUSTMENT_SEC      = 30;   // +30s to all paces

    private static final float FACTOR_GREEN = 1.00f;
    private static final float FACTOR_YELLOW = 0.90f;
    private static final float FACTOR_RED = 0.80f;

    private static final int DELTA_PACE_GREEN = -5;
    private static final int DELTA_PACE_YELLOW = 5;
    private static final int DELTA_PACE_RED = 10;


    private static class TrainingLoadMetrics {
        float acuteTRIMP;
        float chronicTRIMP;
        float fatigueRatio;
    }

    public TrainingPlan adjustPlan(TrainingPlan currentPlan,
                                   List<Activity> recentRuns,
                                   Map<String, String> trafficLightStatuses) {
        if (currentPlan == null) return null;
        TrainingPlan adjustedPlan = deepCopyPlan(currentPlan);

        annotateDaysWithNames(adjustedPlan);

        if (recentRuns == null || recentRuns.isEmpty()) return adjustedPlan;

        TrainingLoadMetrics metrics = calculateTrainingLoadMetrics(recentRuns);
        boolean needsRecovery = shouldTriggerRecovery(metrics);
        adjustedPlan.setAdjustmentNote(createAdjustmentNote(metrics, needsRecovery));

        for (TrainingPlan.TrainingWeek week : adjustedPlan.getTraining_weeks()) {
            for (TrainingPlan.Day day : daysOf(week)) {
                if (day == null) continue;

                if (!shouldAdjustDay(day)) continue;

                if (needsRecovery) {
                    adjustForRecovery(day);
                } else {
                    String keyName = day.getDayOfWeek();
                    String light = (trafficLightStatuses != null && keyName != null)
                            ? trafficLightStatuses.get(keyName)
                            : null;
                    if (light != null) {
                        adjustForTrafficLight(day, light);
                    }
                }

                Activity match = findMatchingActivityForDay(day, recentRuns);
                if (match != null) {
                    adjustBasedOnFitness(match, day);
                }
            }
        }

        return adjustedPlan;
    }


    private List<TrainingPlan.Day> daysOf(TrainingPlan.TrainingWeek w) {
        List<TrainingPlan.Day> out = new ArrayList<>(7);
        if (w == null || w.getTraining_plan() == null) return out;
        out.add(w.getTraining_plan().getMonday());
        out.add(w.getTraining_plan().getTuesday());
        out.add(w.getTraining_plan().getWednesday());
        out.add(w.getTraining_plan().getThursday());
        out.add(w.getTraining_plan().getFriday());
        out.add(w.getTraining_plan().getSaturday());
        out.add(w.getTraining_plan().getSunday());
        return out;
    }

    private void annotateDaysWithNames(TrainingPlan plan) {
        if (plan == null || plan.getTraining_weeks() == null) return;
        for (TrainingPlan.TrainingWeek w : plan.getTraining_weeks()) {
            for (TrainingPlan.Day d : daysOf(w)) {
                if (d == null) continue;
                if (d.getDayOfWeek() == null || d.getDayOfWeek().trim().isEmpty()) {
                    if (d.getDate() != null) {
                        d.setDayOfWeek(DateUtils.getDayName(d.getDate()));
                    }
                }
            }
        }
    }


    private Activity findMatchingActivityForDay(TrainingPlan.Day planDay, List<Activity> runs) {
        if (planDay == null || planDay.getDayOfWeek() == null) return null;
        String want = planDay.getDayOfWeek();
        return runs.stream()
                .filter(a -> a != null && a.getStart_date() != null)
                .filter(a -> {
                    String have = DateUtils.getDayName(a.getStart_date());
                    return have != null && have.equalsIgnoreCase(want);
                })
                .findFirst()
                .orElse(null);
    }


    private TrainingLoadMetrics calculateTrainingLoadMetrics(List<Activity> runs) {
        TrainingLoadMetrics m = new TrainingLoadMetrics();
        List<Activity> recent = filterRecentRuns(runs, DAYS_FOR_ACUTE_LOAD + DAYS_FOR_CHRONIC_LOAD);
        m.acuteTRIMP   = calculateTRIMPForPeriod(recent, DAYS_FOR_ACUTE_LOAD);
        m.chronicTRIMP = calculateTRIMPForPeriod(recent, DAYS_FOR_CHRONIC_LOAD);
        m.fatigueRatio = (m.chronicTRIMP <= 0f) ? 0f : (m.acuteTRIMP / m.chronicTRIMP);
        return m;
    }

    private String createAdjustmentNote(TrainingLoadMetrics m, boolean recovery) {
        return String.format(
                Locale.US,
                "Acute TRIMP: %.1f, Chronic TRIMP: %.1f, Ratio: %.2f. %s",
                m.acuteTRIMP, m.chronicTRIMP, m.fatigueRatio,
                recovery ? "Recovery week activated." : "Normal training load."
        );
    }

    private boolean shouldTriggerRecovery(TrainingLoadMetrics m) {
        return (m.fatigueRatio > FATIGUE_RATIO_THRESHOLD) || (m.acuteTRIMP > HIGH_ACUTE_TRIMP_THRESHOLD);
    }

    private float calculateTRIMPForPeriod(List<Activity> runs, int days) {
        long cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000;
        return runs.stream()
                .filter(a -> {
                    if (a == null || a.getStart_date() == null) return false;
                    Date d = DateUtils.parseDate(a.getStart_date());
                    return d != null && d.getTime() >= cutoff;
                })
                .map(a -> TRIMP.calculate(
                        a.getMoving_time() / 60f,
                        a.getAverage_heartrate(),
                        a.getResting_heartrate(),
                        a.getMax_heartrate(),
                        a.isMale()
                ))
                .reduce(0f, Float::sum);
    }

    private List<Activity> filterRecentRuns(List<Activity> runs, int maxDays) {
        long cutoff = System.currentTimeMillis() - (maxDays * 24L * 60 * 60 * 1000);
        return runs.stream()
                .filter(run -> {
                    if (run == null || run.getStart_date() == null) return false;
                    Date d = DateUtils.parseDate(run.getStart_date());
                    return d != null && d.getTime() > cutoff;
                })
                .collect(Collectors.toList());
    }


    private boolean shouldAdjustDay(TrainingPlan.Day day) {
        if (day == null) return false;
        String dist = day.getDistance();
        String ex   = (day.getExercise() == null) ? "" : day.getExercise().toLowerCase(Locale.US);

        boolean isRest = (dist == null) || "0 mi".equalsIgnoreCase(dist.trim());
        boolean isRace = ex.contains("race") || ex.contains("tune-up");
        return !isRest && !isRace;
    }

    private void adjustForRecovery(TrainingPlan.Day day) {
        if (day.getDistance() != null && day.getDistance().contains("mi")) {
            float dist = parseDistance(day.getDistance());
            dist *= RECOVERY_DISTANCE_FACTOR;
            day.setDistance(String.format(Locale.US, "%.1f mi", dist));
        }
        day.setPace(shiftAllPaces(day.getPace(), RECOVERY_PACE_ADJUSTMENT_SEC));
        day.setAdjustmentNote("Recovery: distance -20%, pace +30\".");
    }

    private void adjustForTrafficLight(TrainingPlan.Day day, String status) {
        if (day == null || status == null) return;

        if (day.getDistance() != null && day.getDistance().contains("mi")) {
            float dist  = parseDistance(day.getDistance());
            float factor = getDistanceAdjustmentFactor(status);
            dist *= factor;
            day.setDistance(String.format(Locale.US, "%.1f mi", dist));
        }

        int delta = getPaceAdjustment(status);
        if (delta != 0) {
            day.setPace(shiftAllPaces(day.getPace(), delta));
        }

        if ("RED".equals(status)) {
            day.setAdjustmentNote("Traffic light RED: pace +10\" and distance reduced.");
        } else if ("YELLOW".equals(status)) {
            day.setAdjustmentNote("Traffic light YELLOW: slight pace +5\" and distance reduced.");
        } else if ("GREEN".equals(status)) {
            day.setAdjustmentNote("Traffic light GREEN: pace -5\" (nice!).");
        }
    }


    public void adjustBasedOnFitness(Activity activity, TrainingPlan.Day day) {
        if (activity == null || day == null || activity.getAverage_heartrate() == 0) return;

        float avgHR        = activity.getAverage_heartrate();
        float actualPace   = activity.getPaceInSeconds();
        float predictedPace= PerformanceEvaluator.predictPaceFromHR(avgHR);

        int delta = PerformanceEvaluator.calculatePaceDelta(actualPace, predictedPace);
        if (delta == 0) return;

        day.setPace(shiftAllPaces(day.getPace(), delta));

        String feedback = PerformanceEvaluator.generateFeedback(
                actualPace,
                predictedPace,
                PerformanceEvaluator.getTrafficLightColor(
                        extractRepresentativePace(day.getPace()), // baseline
                        actualPace,
                        parseDistance(day.getDistance()),
                        activity.getDistance()
                )
        );
        activity.setFeedback(feedback);
    }


    private float parseDistance(String distanceStr) {
        try {
            if (distanceStr == null || distanceStr.trim().isEmpty()) return 0f;
            String s = distanceStr.toLowerCase(Locale.US);

            if (s.contains("w/")) s = s.substring(0, s.indexOf("w/"));
            s = s.replace("mi", " ").trim();

            String firstNumber = s.split("-| ")[0].trim();
            return Float.parseFloat(firstNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse distance: " + distanceStr, e);
            return 5.0f;
        }
    }

    private float extractRepresentativePace(String paceStr) {
        if (paceStr == null) return 480f; // default 8:00
        Matcher m = MMSS.matcher(paceStr);
        if (m.find()) {
            return mmssToSeconds(m.group());
        }
        return 480f;
    }


    private static final Pattern MMSS = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");

    private String shiftAllPaces(String pace, int deltaSec) {
        if (pace == null || pace.trim().isEmpty()) return pace;
        StringBuffer sb = new StringBuffer();
        Matcher m = MMSS.matcher(pace);
        while (m.find()) {
            String original = m.group();
            String shifted  = secondsToMmss(mmssToSeconds(original) + deltaSec);
            m.appendReplacement(sb, Matcher.quoteReplacement(shifted));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private int mmssToSeconds(String mmss) {
        try {
            String[] ps = mmss.split(":");
            int m = Integer.parseInt(ps[0]);
            int s = Integer.parseInt(ps[1]);
            return Math.max(0, m * 60 + s);
        } catch (Exception e) {
            return 480; // fallback
        }
    }

    private String secondsToMmss(int total) {
        if (total < 0) total = 0;
        int m = total / 60;
        int s = total % 60;
        return String.format(Locale.getDefault(), "%d:%02d", m, s);
    }


    private float getDistanceAdjustmentFactor(String status) {
        switch (status) {
            case "GREEN":  return FACTOR_GREEN;
            case "YELLOW": return FACTOR_YELLOW;
            case "RED":    return FACTOR_RED;
            default:       return 1.0f;
        }
    }

    private int getPaceAdjustment(String status) {
        switch (status) {
            case "GREEN":  return DELTA_PACE_GREEN;
            case "YELLOW": return DELTA_PACE_YELLOW;
            case "RED":    return DELTA_PACE_RED;
            default:       return 0;
        }
    }


    private TrainingPlan deepCopyPlan(TrainingPlan plan) {
        if (plan == null) return null;
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(plan), TrainingPlan.class);
    }
}
