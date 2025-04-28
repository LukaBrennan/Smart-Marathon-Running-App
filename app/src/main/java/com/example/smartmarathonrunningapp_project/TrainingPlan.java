package com.example.smartmarathonrunningapp_project;
import java.util.List;
    //  This class represents a complete training plan structure containing, multiple training weeks, daily workout details.
public class TrainingPlan
{
    //  This is a list of training weeks that make up the complete plan
    private List<TrainingWeek> training_weeks;
    //  Note explaining any automatic adjustments made to the plan
    private String adjustmentNote;
    public List<TrainingWeek> getTraining_weeks()
    {
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
    //  Represents a single week within the training plan
    public static class TrainingWeek
    {
        private String week;
        private Days training_plan;

        public String getWeek() {
            return week;
        }

        public void setWeek(String week) {
            this.week = week;
        }

        public Days getTraining_plan() {
            return training_plan;
        }

        public void setTraining_plan(Days training_plan) {
            this.training_plan = training_plan;
        }
    }
    //  Container for all training days in a week
    public static class Days
    {
        private Day Monday;
        private Day Tuesday;
        private Day Wednesday;
        private Day Thursday;
        private Day Friday;
        private Day Saturday;
        private Day Sunday;

        public Day getMonday() {
            return Monday;
        }

        public void setMonday(Day monday) {
            Monday = monday;
        }

        public Day getTuesday() {
            return Tuesday;
        }

        public void setTuesday(Day tuesday) {
            Tuesday = tuesday;
        }

        public Day getWednesday() {
            return Wednesday;
        }

        public void setWednesday(Day wednesday) {
            Wednesday = wednesday;
        }

        public Day getThursday() {
            return Thursday;
        }

        public void setThursday(Day thursday) {
            Thursday = thursday;
        }

        public Day getFriday() {
            return Friday;
        }

        public void setFriday(Day friday) {
            Friday = friday;
        }

        public Day getSaturday() {
            return Saturday;
        }

        public void setSaturday(Day saturday) {
            Saturday = saturday;
        }

        public Day getSunday() {
            return Sunday;
        }

        public void setSunday(Day sunday) {
            Sunday = sunday;
        }
    }
    //  Represents a single day's workout details
    public static class Day
    {
        private String exercise;
        private String distance;
        private String pace;
        private boolean completed;
        private String adjustmentNote;


        public String getExercise() {
            return exercise;
        }

        public void setExercise(String exercise) {
            this.exercise = exercise;
        }

        public String getDistance() {
            return distance;
        }

        public void setDistance(String distance) {
            this.distance = distance;
        }

        public String getPace() {
            return pace;
        }

        public void setPace(String pace) {
            this.pace = pace;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public String getAdjustmentNote() { return adjustmentNote; }
        public void setAdjustmentNote(String note) { this.adjustmentNote = note; }
    }
}