package com.example.smartmarathonrunningapp_project;
public class TrainingPlan {
    private final Days training_plan;

    public TrainingPlan(Days trainingPlan) {
        training_plan = trainingPlan;
    }

    public Days getTraining_plan() {
        return training_plan;
    }

    public static class Days {
        private Day Monday;
        private Day Tuesday;
        private Day Wednesday;
        private Day Thursday;
        private Day Friday;
        private Day Saturday;
        private Day Sunday;

        // Getters and Setters for each day
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

    public static class Day
    {
        private String exercise;
        private String distance;
        private String pace;

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
    }
}