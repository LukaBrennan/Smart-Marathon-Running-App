package com.example.smartmarathonrunningapp_project;

import java.util.Arrays;
import java.util.List;

public class TrainingPlan {
    private List<TrainingWeek> training_weeks;
    private String adjustmentNote;

    public List<TrainingWeek> getTraining_weeks() {
        return training_weeks;
    }

    public void setTraining_weeks(List<TrainingWeek> training_weeks) {
        this.training_weeks = training_weeks;
    }

    public String getAdjustmentNote() {
        return adjustmentNote;
    }

    public void setAdjustmentNote(String adjustmentNote) {
        this.adjustmentNote = adjustmentNote;
    }

    // Traffic Light System Methods
    public static float parseDistanceToMeters(String distanceStr) {
        try {
            if (distanceStr == null || distanceStr.equals("0 mi")) {
                return 0f;
            }
            return Float.parseFloat(distanceStr.replace("mi", "").trim()) * 1609.34f;
        } catch (Exception e) {
            return 0f;
        }
    }

    public static float[] parsePaceToSecPerKm(String paceStr) {
        if (paceStr == null || paceStr.isEmpty()) {
            return new float[]{0, 0};
        }

        try {
            String[] parts = paceStr.split(" - ");
            float minPace = convertPaceToSecKm(parts[0]);
            float maxPace = parts.length > 1 ? convertPaceToSecKm(parts[1]) : minPace;
            return new float[]{minPace, maxPace};
        } catch (Exception e) {
            return new float[]{0, 0};
        }
    }

    private static float convertPaceToSecKm(String pace) {
        String[] timeParts = pace.split(":");
        int minutes = Integer.parseInt(timeParts[0]);
        int seconds = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
        return (minutes * 60 + seconds) * 0.621371f; // Convert min/mile to sec/km
    }

    public static String getTrafficLightStatus(Day plannedDay, Activity completedRun) {
        if (plannedDay == null || completedRun == null ||
                plannedDay.getDistance() == null || plannedDay.getDistance().equals("0 mi")) {
            return "N/A";
        }

        float plannedDistMeters = parseDistanceToMeters(plannedDay.getDistance());
        if (plannedDistMeters == 0) return "N/A";

        float[] plannedPaceRange = parsePaceToSecPerKm(plannedDay.getPace());
        float actualDistMeters = completedRun.getDistance();
        float actualPaceSecKm = completedRun.getMoving_time() / (completedRun.getDistance() / 1000f);

        float distDeviation = Math.abs(actualDistMeters - plannedDistMeters) / plannedDistMeters;
        float paceDeviation = calculatePaceDeviation(actualPaceSecKm, plannedPaceRange);

        if (distDeviation <= 0.10f && paceDeviation <= 0.10f) return "GREEN";
        else if (distDeviation <= 0.20f && paceDeviation <= 0.20f) return "YELLOW";
        else return "RED";
    }

    private static float calculatePaceDeviation(float actualPace, float[] plannedRange) {
        if (plannedRange[0] == 0) return 0f;

        if (actualPace < plannedRange[0]) {
            return (plannedRange[0] - actualPace) / plannedRange[0];
        } else if (actualPace > plannedRange[1]) {
            return (actualPace - plannedRange[1]) / plannedRange[1];
        }
        return 0f;
    }

    public static class TrainingWeek {
        private String week;
        private Days training_plan;

        public String getWeek() { return week; }
        public void setWeek(String week) { this.week = week; }
        public Days getTraining_plan() { return training_plan; }
        public void setTraining_plan(Days training_plan) { this.training_plan = training_plan; }
    }

    public static class Days {
        private Day Monday;
        private Day Tuesday;
        private Day Wednesday;
        private Day Thursday;
        private Day Friday;
        private Day Saturday;
        private Day Sunday;

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

        public String getExercise() { return exercise; }
        public void setExercise(String exercise) { this.exercise = exercise; }
        public String getDistance() { return distance; }
        public void setDistance(String distance) { this.distance = distance; }
        public String getPace() { return pace; }
        public void setPace(String pace) { this.pace = pace; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public String getAdjustmentNote() { return adjustmentNote; }
        public void setAdjustmentNote(String note) { this.adjustmentNote = note; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }
}