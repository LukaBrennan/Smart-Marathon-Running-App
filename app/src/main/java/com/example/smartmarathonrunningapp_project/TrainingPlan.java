package com.example.smartmarathonrunningapp_project;
import java.util.List;
import java.util.Locale;
/*
    Represents the marathon training plan loaded from JSON.
    Structured into TrainingWeeks, each with 7 Day objects.
*/
public class TrainingPlan {
    private List<TrainingWeek> training_weeks;
    private String adjustmentNote;

    public List<TrainingWeek> getTraining_weeks() { return training_weeks; }
    public void setTraining_weeks(List<TrainingWeek> training_weeks) { this.training_weeks = training_weeks; }

    public String getAdjustmentNote() { return adjustmentNote; }
    public void setAdjustmentNote(String adjustmentNote) { this.adjustmentNote = adjustmentNote; }

    public static class TrainingWeek {
        private String week;
        private Days training_plan;

        public String getWeek() { return week; }
        public void setWeek(String week) { this.week = week; }

        public Days getTraining_plan() { return training_plan; }
        public void setTraining_plan(Days training_plan) { this.training_plan = training_plan; }
    }

    public static class Days {
        private Day Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday;

        public Day getMonday() { return Monday; }
        public void setMonday(Day monday) { Monday = monday; }

        public Day getTuesday() { return Tuesday; }
        public void setTuesday(Day tuesday) { Tuesday = tuesday; }

        public Day getWednesday() { return Wednesday; }
        public void setWednesday(Day wednesday) { Wednesday = wednesday; }

        public Day getThursday() { return Thursday; }
        public void setThursday(Day thursday) { Thursday = thursday; }

        public Day getFriday() { return Friday; }
        public void setFriday(Day friday) { Friday = friday; }

        public Day getSaturday() { return Saturday; }
        public void setSaturday(Day saturday) { Saturday = saturday; }

        public Day getSunday() { return Sunday; }
        public void setSunday(Day sunday) { Sunday = sunday; }
    }

    public static class Day {
        private String exercise;
        private String distance;
        private String pace;
        private boolean completed;
        private String adjustmentNote;
        private String date;
        private String dayOfWeek;

        public String getExercise() { return exercise; }
        public void setExercise(String exercise) { this.exercise = exercise; }

        public String getDistance() { return distance; }
        public void setDistance(String distance) { this.distance = distance; }

        public String getPace() { return pace; }
        public void setPace(String pace) { this.pace = pace; }

        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }

        public String getAdjustmentNote() { return adjustmentNote; }
        public void setAdjustmentNote(String adjustmentNote) { this.adjustmentNote = adjustmentNote; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    }

    private static float metersToMiles(float m) { return m / 1609.344f; }

    private static int parseMmSsToSec(String s) {
        if (s == null) return -1;
        s = s.trim();
        if (!s.matches("\\d{1,2}:\\d{2}")) return -1;
        String[] p = s.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }
    private static float parsePlanMiles(String distanceText) {
        if (distanceText == null) return 0f;
        String t = distanceText.toLowerCase(Locale.US);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([0-9]+(?:\\.[0-9]+)?)")
                .matcher(t);
        if (!m.find()) return 0f;
        float val;
        try { val = Float.parseFloat(m.group(1)); } catch (Exception e) { return 0f; }
        boolean hasMi = t.contains("mi");
        boolean hasKm = t.contains("km");
        if (hasMi && !hasKm) return val;
        return hasKm ? (val * 0.621371f) : val;
    }

    private static int[] parsePlanPaceBandSecPerMi(String planPace) {
        if (planPace == null) return null;
        planPace = normalizeWeirdPaceString(planPace);
        String[] tokens = planPace.split("-");
        java.util.List<Integer> vals = new java.util.ArrayList<>();
        for (String t : tokens) {
            int v = parseMmSsToSec(t.trim());
            if (v > 0) vals.add(v);
        }
        if (vals.isEmpty()) return null;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int v : vals) { min = Math.min(min, v); max = Math.max(max, v); }
        if (vals.size() == 1) { min -= 15; max += 15; }
        return new int[]{min, max};
    }

    @SuppressWarnings("unused")
    private static String secToMmSs(int sec) {
        if (sec < 0) sec = 0;
        return String.format(Locale.US, "%d:%02d", sec/60, sec%60);
    }

    public static String getTrafficLightStatus(TrainingPlan.Day day, Activity act) {
        if (day == null || act == null) return "N/A";

        float plannedMi = parsePlanMiles(day.getDistance());

        float actualMi  = metersToMiles(act.getDistance());
        int   timeSec   = act.getMoving_time();
        if (actualMi <= 0f || timeSec <= 0) return "N/A";
        float actualSecPerMi = timeSec / actualMi;

        int[] band = parsePlanPaceBandSecPerMi(day.getPace());
        boolean paceOk = true;
        if (band != null) {
            paceOk = (actualSecPerMi >= band[0] && actualSecPerMi <= band[1]);
        }

        if (plannedMi <= 0f) {
            return "N/A";
        }
        boolean distGreen  = (actualMi >= plannedMi * 0.95f && actualMi <= plannedMi * 1.10f);
        boolean distYellow = (!distGreen && actualMi >= plannedMi * 0.85f && actualMi <= plannedMi * 1.20f);

        if (distGreen && paceOk) return "GREEN";
        if ((distGreen && !paceOk) || (distYellow && paceOk)) return "YELLOW";
        return "RED";
    }

    public static float parseDistanceToMeters(String distanceStr) {
        float miles = parsePlanMiles(distanceStr);
        return miles * 1609.344f;
    }



    public static String normalizeWeirdPaceString(String pace) {
        if (pace == null) return null;
        java.util.regex.Pattern P = java.util.regex.Pattern.compile("\\b(\\d{3}):(\\d{2})\\b");
        java.util.regex.Matcher m = P.matcher(pace);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String left  = m.group(1);
            String secs2 = m.group(2);
            int minutes = Integer.parseInt(left.substring(0, left.length()-1));
            int tens    = Integer.parseInt(left.substring(left.length()-1));
            int units   = Integer.parseInt(secs2.substring(1));
            int fixedSec = tens * 10 + units;
            String repl = String.format(java.util.Locale.US, "%d:%02d", minutes, fixedSec);
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }



}
