package com.example.smartmarathonrunningapp_project;
import com.google.gson.Gson;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import java.util.*;
/*
    Dynamically adjusts a training plan based on past performances.
    Uses traffic light statuses to modify future days’ pace bands.
*/
public class AutoAdjuster {

    private static final String TAG = "AutoAdjuster";

    private static final int RED_D1 = 12, RED_D2 = 7, RED_D3 = 3;
    private static final int YEL_D1 = 6,  YEL_D2 = 3, YEL_D3 = 2;
    private static final int GRN_D1 = -4, GRN_D2 = -2, GRN_D3 = 0;

    private static final int SAME_DAY_RED   = 10;
    private static final int SAME_DAY_YELOW = 5;

    private static final int DOWNGRADE_HARD_THRESHOLD = 10; // sec/km

    private static final double RECOVERY_DECAY = 0.60;

    public TrainingPlan adjustPlan(
            TrainingPlan base,
            List<Activity> validActivities,
            Map<String, String> lightsByDate
    ) {
        if (base == null) return null;

        TrainingPlan adjusted = deepCopy(base);

        applySameDayAdjustments(adjusted, lightsByDate);

        applyCarryForward(adjusted, lightsByDate);

        return adjusted;
    }

    private void applySameDayAdjustments(TrainingPlan plan, Map<String,String> lightsByDate) {
        if (plan == null || plan.getTraining_weeks() == null) return;

        for (TrainingPlan.TrainingWeek week : plan.getTraining_weeks()) {
            if (week == null || week.getTraining_plan() == null) continue;
            for (TrainingPlan.Day day : getDaysOfWeek(week)) {
                if (day == null || day.getDate() == null) continue;

                if (isNoTrainingDay(day)) continue;

                String key = DateUtils.getDateOnly(day.getDate());
                String status = lightsByDate.getOrDefault(key, "N/A");

                switch (safe(status)) {
                    case "RED":
                        bumpAllPaces(day, +SAME_DAY_RED);
                        addNote(day, "Adjusted for fatigue (+10s).");
                        break;
                    case "YELLOW":
                        bumpAllPaces(day, +SAME_DAY_YELOW);
                        addNote(day, "Slightly easier today (+5s).");
                        break;
                    case "GREEN":
                        addNote(day, "Met target.");
                        break;
                    default:
                        break;
                }
            }
        }
    }


    private void applyCarryForward(TrainingPlan plan, Map<String,String> lightsByDate) {
        if (plan == null || plan.getTraining_weeks() == null) return;

        List<TrainingPlan.Day> days = new ArrayList<>();
        for (TrainingPlan.TrainingWeek w : plan.getTraining_weeks()) {
            if (w == null || w.getTraining_plan() == null) continue;
            Collections.addAll(days,
                    w.getTraining_plan().getMonday(),
                    w.getTraining_plan().getTuesday(),
                    w.getTraining_plan().getWednesday(),
                    w.getTraining_plan().getThursday(),
                    w.getTraining_plan().getFriday(),
                    w.getTraining_plan().getSaturday(),
                    w.getTraining_plan().getSunday());
        }
        days.removeIf(d -> d == null || d.getDate() == null);
        days.sort(Comparator.comparing(d -> DateUtils.getDateOnly(d.getDate())));

        Map<String,Integer> idxByDate = new HashMap<>();
        for (int i = 0; i < days.size(); i++) {
            String key = DateUtils.getDateOnly(days.get(i).getDate());
            if (key != null) idxByDate.put(key, i);
        }

        for (Map.Entry<String,String> e : lightsByDate.entrySet()) {
            Integer i = idxByDate.get(e.getKey());
            if (i == null) continue;

            int d1 = 0, d2 = 0, d3 = 0;
            switch (safe(e.getValue())) {
                case "RED":    d1 = RED_D1; d2 = RED_D2; d3 = RED_D3; break;
                case "YELLOW": d1 = YEL_D1; d2 = YEL_D2; d3 = YEL_D3; break;
                case "GREEN":  d1 = GRN_D1; d2 = GRN_D2; d3 = GRN_D3; break;
                default: break;
            }

            applyPaceOffsetWithRecoveryAwareDecay(days, i+1, d1);
            applyPaceOffsetWithRecoveryAwareDecay(days, i+2, d2);
            applyPaceOffsetWithRecoveryAwareDecay(days, i+3, d3);
        }
    }
    private void applyPaceOffsetWithRecoveryAwareDecay(List<TrainingPlan.Day> days, int idx, int secondsPerKm) {
        if (idx < 0 || idx >= days.size() || secondsPerKm == 0) return;

        TrainingPlan.Day target = days.get(idx);
        if (target == null) return;

        if (isNoTrainingDay(target)) return;

        boolean prevIsRecovery = false;
        if (idx - 1 >= 0 && days.get(idx - 1) != null) {
            String ex = safe(days.get(idx - 1).getExercise()).toLowerCase(Locale.US);
            prevIsRecovery = ex.contains("rest") || ex.contains("recovery") || ex.contains("cross");
        }

        int applied = secondsPerKm;
        if (prevIsRecovery) {
            applied = (int) Math.round(applied * RECOVERY_DECAY);
            if (applied == 0 && secondsPerKm != 0) {
                applied = (secondsPerKm > 0) ? 1 : -1;
            }
        }
        if (applied == 0) return;

        if (isHardWorkout(target) && Math.abs(applied) >= DOWNGRADE_HARD_THRESHOLD && applied > 0) {
            downgradeToAerobic(target);
            addNote(target, "Downgraded due to fatigue (+" + applied + "s/km).");
            bumpAllPaces(target, applied);
        } else {
            bumpAllPaces(target, applied);
            if (applied > 0) {
                addNote(target, "Carry-forward fatigue: pace +" + applied + "s/km.");
            } else {
                addNote(target, "Carry-forward GREEN: pace " + applied + "s/km.");
            }
        }
    }


    private boolean isHardWorkout(TrainingPlan.Day d) {
        String ex = safe(d.getExercise()).toLowerCase(Locale.US);
        return ex.contains("vo2") || ex.contains("lactate") || ex.contains("threshold")
                || ex.contains("marathon pace") || ex.contains("interval") || ex.contains("repeat");
    }

    private void downgradeToAerobic(TrainingPlan.Day d) {
        d.setExercise("General aerobic");
    }

    private void bumpAllPaces(TrainingPlan.Day d, int sec) {
        if (isNoTrainingDay(d)) return;
        String p = safe(d.getPace());
        if (p.isEmpty() || "null".equalsIgnoreCase(p)) return;

        String[] parts = p.split("-");
        List<String> bumped = new ArrayList<>();
        for (String raw : parts) {
            String s = raw.trim();
            String mmss = addSecondsToMmSs(s, sec);
            bumped.add(mmss == null ? s : mmss);
        }
        d.setPace(String.join(" - ", bumped));
    }

    private String addSecondsToMmSs(String mmss, int add) {
        try {
            String[] t = mmss.split(":");
            int m = Integer.parseInt(t[0].trim());
            int s = Integer.parseInt(t[1].trim());
            int total = Math.max(0, m * 60 + s + add);
            return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60);
        } catch (Exception e)
        {
            return null;
        }
    }

    private void addNote(TrainingPlan.Day d, String note) {
        if (d.getAdjustmentNote() == null || d.getAdjustmentNote().isEmpty()) {
            d.setAdjustmentNote(note);
        } else if (!d.getAdjustmentNote().contains(note)) {
            d.setAdjustmentNote(d.getAdjustmentNote() + " " + note);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private List<TrainingPlan.Day> getDaysOfWeek(TrainingPlan.TrainingWeek week) {
        return Arrays.asList(
                week.getTraining_plan().getMonday(),
                week.getTraining_plan().getTuesday(),
                week.getTraining_plan().getWednesday(),
                week.getTraining_plan().getThursday(),
                week.getTraining_plan().getFriday(),
                week.getTraining_plan().getSaturday(),
                week.getTraining_plan().getSunday()
        );
    }

    private static final Gson GSON = new Gson();

    private TrainingPlan deepCopy(TrainingPlan plan) {
        if (plan == null) return null;
        String json = GSON.toJson(plan);
        return GSON.fromJson(json, TrainingPlan.class);
    }

    private boolean isNoTrainingDay(TrainingPlan.Day d) {
        if (d == null) return true;
        String ex = safe(d.getExercise()).toLowerCase(Locale.US);
        boolean textRest = ex.contains("rest") || ex.contains("cross");
        float meters = TrainingPlan.parseDistanceToMeters(d.getDistance());
        return textRest || meters <= 0f;
    }



}
