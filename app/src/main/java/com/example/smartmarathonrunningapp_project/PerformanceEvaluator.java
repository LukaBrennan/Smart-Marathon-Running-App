package com.example.smartmarathonrunningapp_project;

public class PerformanceEvaluator {

    public static float predictPaceFromHR(float heartRate) {
        return -9.84f * heartRate + 1828.12f; // seconds per mile
    }

    public static int calculatePaceDelta(float actual, float predicted) {
        float delta = predicted - actual;
        return Math.abs(delta) < 5 ? 0 : Math.round(delta);
    }
    public static String getTrafficLightColor(float plannedPace, float actualPace, float plannedDistance, float actualDistance) {
        float paceDev = Math.abs(actualPace - plannedPace) / plannedPace * 100;
        float distDev = Math.abs(actualDistance - plannedDistance) / plannedDistance * 100;

        String paceColor = (paceDev <= 8) ? "Green" : (paceDev <= 15) ? "Yellow" : "Red";
        String distColor = (distDev <= 10) ? "Green" : (distDev <= 20) ? "Yellow" : "Red";

        if (paceColor.equals("Red") || distColor.equals("Red")) return "Red";
        if (paceColor.equals("Yellow") || distColor.equals("Yellow")) return "Yellow";
        return "Green";
    }

    public static String generateFeedback(float actualPace, float predictedPace, String trafficLight) {
        StringBuilder feedback = new StringBuilder();
        feedback.append("Predicted pace: ").append(formatSecondsToPace(predictedPace));
        feedback.append("\nActual pace: ").append(formatSecondsToPace(actualPace));

        switch (trafficLight) {
            case "Green":
                feedback.append("\n✅ Great job! You're on track.");
                break;
            case "Yellow":
                feedback.append("\n⚠️ Slight deviation. Consider adjusting effort.");
                break;
            case "Red":
                feedback.append("\n❌ Off plan. We’ll slow your next run slightly.");
                break;
        }

        return feedback.toString();
    }

    public static String formatSecondsToPace(float seconds) {
        int mins = (int) (seconds / 60);
        int secs = Math.round(seconds % 60);
        return String.format("%d:%02d /mi", mins, secs);
    }

    public static int convertPaceToSeconds(String paceStr) {
        if (paceStr == null || !paceStr.contains(":")) return 0;
        String[] parts = paceStr.split(":");
        return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
    }
}
