package com.example.smartmarathonrunningapp_project;

import java.util.Locale;
import java.util.Map;

public class DailyFeedbackGenerator {
    private static final float SIGNIFICANT_PACE_IMPROVEMENT = 30f; // seconds
    private static final float SIGNIFICANT_HR_DROP = 5f; // bpm
    private static final float DISTANCE_INCREASE_THRESHOLD = 1.2f;
    private static final float DISTANCE_DECREASE_THRESHOLD = 0.8f;
    private static final float ELEVATION_THRESHOLD = 50f; // meter1s
    private static final float MIN_PACE = 120f; // 2 min/mile
    private static final float MAX_PACE = 1200f; // 20 min/mile
    private static final float MIN_HR = 50f;
    private static final float MAX_HR = 220f;

    public static String generate(Map<String, Float> currentRun,
                                  Map<String, Float> previousRun) {
        if (!isValidRun(currentRun) || !isValidRun(previousRun)) {
            return "Not enough data for comparison\n\n";
        }

        StringBuilder feedback = new StringBuilder("🏃 Run Analysis:\n");
        feedback.append(generatePaceFeedback(currentRun, previousRun));
        feedback.append(generateHeartRateFeedback(currentRun, previousRun));
        feedback.append(generateDistanceFeedback(currentRun, previousRun));

        return feedback.toString();
    }

    private static boolean isValidRun(Map<String, Float> run) {
        if (run == null) return false;
        Float pace = run.get("pace");
        Float hr = run.get("heart_rate");
        Float dist = run.get("distance");

        return pace != null && hr != null && dist != null
                && pace >= MIN_PACE && pace <= MAX_PACE
                && hr >= MIN_HR && hr <= MAX_HR
                && dist > 1000; // At least 1km
    }

    private static String generatePaceFeedback(Map<String, Float> current,
                                               Map<String, Float> previous) {
        float paceDiff = previous.get("pace") - current.get("pace");
        String formattedDiff = formatPace(Math.abs(paceDiff));

        if (paceDiff > SIGNIFICANT_PACE_IMPROVEMENT) {
            return String.format(Locale.getDefault(),
                    "  🚀 Pace: %s faster than last time!\n", formattedDiff);
        } else if (paceDiff > 0) {
            return String.format(Locale.getDefault(),
                    "  ↗ Pace: Improved by %s\n", formattedDiff);
        } else if (paceDiff < -SIGNIFICANT_PACE_IMPROVEMENT) {
            return String.format(Locale.getDefault(),
                    "  ⚠️ Pace: %s slower. Consider more recovery\n", formattedDiff);
        }
        return "  ↔ Pace: Consistent with previous run\n";
    }

    private static String generateHeartRateFeedback(Map<String, Float> current,
                                                    Map<String, Float> previous) {
        Float currentHR = current.get("heart_rate");
        Float previousHR = previous.get("heart_rate");

        if (currentHR == null || previousHR == null) {
            return "";
        }

        float hrDiff = previousHR - currentHR;
        float absHrDiff = Math.abs(hrDiff);

        if (absHrDiff < 1) {
            return "";
        }

        if (hrDiff > SIGNIFICANT_HR_DROP) {
            return String.format(Locale.getDefault(),
                    "  ❤️ HR: Dropped by %.1f bpm (great efficiency!)\n",
                    hrDiff);
        } else if (hrDiff > 0) {
            return String.format(Locale.getDefault(),
                    "  ❤️ HR: %.1f bpm lower\n",
                    hrDiff);
        } else {
            return String.format(Locale.getDefault(),
                    "  ⚠️ HR: %.1f bpm higher\n",
                    absHrDiff);
        }
    }

    private static String generateDistanceFeedback(Map<String, Float> current,
                                                   Map<String, Float> previous) {
        Float currentDist = current.get("distance");
        Float previousDist = previous.get("distance");

        if (currentDist == null || previousDist == null || previousDist == 0) {
            return "";
        }

        float distRatio = currentDist / previousDist;

        if (distRatio > DISTANCE_INCREASE_THRESHOLD) {
            return String.format(Locale.getDefault(),
                    "  📏 Distance: +%.1f%% (good progression!)\n",
                    (distRatio - 1) * 100);
        } else if (distRatio < DISTANCE_DECREASE_THRESHOLD) {
            return "  📏 Distance: Shorter (good for recovery)\n";
        }
        return "";
    }

    private static String generateElevationFeedback(Map<String, Float> current,
                                                    Map<String, Float> previous) {
        Float currentElev = current.get("elevation");
        Float previousElev = previous.get("elevation");

        if (currentElev == null || previousElev == null) {
            return "";
        }

        float elevDiff = currentElev - previousElev;
        if (elevDiff > ELEVATION_THRESHOLD) {
            return String.format(Locale.getDefault(),
                    "  ⛰ Elevation: +%.0fm (strong hill work!)\n",
                    elevDiff);
        } else if (elevDiff < -ELEVATION_THRESHOLD) {
            return "  ⛰ Elevation: Less climbing today\n";
        }
        return "";
    }

    public static String formatPace(float seconds) {
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, secs);
    }
}