package com.example.smartmarathonrunningapp_project;
import com.example.smartmarathonrunningapp_project.processors.ProgressiveFitnessAnalyzer.WeekMetrics;
import com.example.smartmarathonrunningapp_project.utils.PaceUtils;

import java.util.List;

public class FeedbackGenerator {

    public static String generateFeedback(String name, WeekMetrics current, WeekMetrics previous) {
        StringBuilder feedback = new StringBuilder("Hey " + name + ",\n\n");

        float paceChange = previous.pace - current.pace;
        if (paceChange > 0) {
            feedback.append("🏃‍♂️ You’ve gotten faster! Your pace improved by ")
                    .append(PaceUtils.formatPaceDifference(paceChange))
                    .append(" this week.\n");
        } else if (paceChange < 0) {
            feedback.append("⚠️ Slight slowdown in pace this week (")
                    .append(PaceUtils.formatPaceDifference(-paceChange))
                    .append(").\n");
        }

        float distChange = (current.distance - previous.distance) / 1000;
        if (distChange > 0) {
            feedback.append("📏 You ran ")
                    .append(String.format("%.1f km", distChange))
                    .append(" more than last week.\n");
        } else if (distChange < 0) {
            feedback.append("🔻 You ran a bit less than usual (")
                    .append(String.format("%.1f km", -distChange))
                    .append(").\n");
        }

        float hrChange = previous.heartRate - current.heartRate;
        if (current.heartRate > 0 && previous.heartRate > 0) {
            if (hrChange > 0) {
                feedback.append("❤️ Your average heart rate dropped by ")
                        .append(String.format("%.1f bpm", hrChange))
                        .append(" — great cardio progress!\n");
            } else if (hrChange < 0) {
                feedback.append("⚠️ Your heart rate increased a bit — might be fatigue or effort.\n");
            }
        }

        if (paceChange == 0 && distChange == 0 && hrChange == 0) {
            feedback.append("✅ You’re staying consistent — well done!\n");
        }

        return feedback.toString();
    }

    public static String generateWeeklyFeedback(int weekNumber, WeekMetrics current, WeekMetrics previous, WeekMetrics baseline) {
        StringBuilder content = new StringBuilder();

        content.append(String.format("WEEK %d (%s)\n\n", weekNumber, current.weekLabel));
        content.append("📊 Weekly Averages:\n");
        content.append(String.format("- Runs: %d\n", current.numberOfRuns));
        content.append(String.format("- Distance: %.1f km\n", current.distance / 1000));
        content.append(String.format("- Pace: %s/mile\n", PaceUtils.convertSecondsToPace((int) current.pace)));

        if (current.heartRate > 0) {
            content.append(String.format("- Avg HR: %.1f bpm\n", current.heartRate));
        }

        content.append("\n📈 Trend Analysis:\n");
        content.append(generateTrendFeedback(current, previous, baseline));

        return content.toString();
    }

    public static String generateTrendFeedback(WeekMetrics current, WeekMetrics previous, WeekMetrics baseline) {
        StringBuilder analysis = new StringBuilder();

        float paceChange = previous.pace - current.pace;
        String paceTrend = getTrend(paceChange, "Improving", "Declining", "Stable");
        analysis.append(String.format("- Pace: %s (%s)\n",
                paceTrend,
                PaceUtils.formatPaceDifference(Math.abs(paceChange))));

        float distChange = current.distance - previous.distance;
        String distTrend = getTrend(distChange, "Increasing", "Decreasing", "Stable");
        analysis.append(String.format("- Distance: %s (%.1f km)\n", distTrend, Math.abs(distChange) / 1000));

        if (current.heartRate > 0 && previous.heartRate > 0) {
            float hrChange = previous.heartRate - current.heartRate;
            String hrTrend = getTrend(hrChange, "Improving (-", "Declining (+", "Stable (");
            analysis.append(String.format("- Heart Rate: %s%.1f bpm)\n", hrTrend, Math.abs(hrChange)));
        }

        analysis.append("\n🕰️ Since Starting Week:\n");
        analysis.append(String.format("- Pace: %s/mile %s\n",
                PaceUtils.formatPaceDifference(baseline.pace - current.pace),
                baseline.pace > current.pace ? "faster" : "slower"));

        analysis.append(String.format("- Distance: %.1f km %s\n",
                Math.abs(current.distance - baseline.distance) / 1000,
                current.distance > baseline.distance ? "longer" : "shorter"));

        return analysis.toString();
    }

    public static String generateFinalSummary(WeekMetrics start, WeekMetrics end, List<WeekMetrics> allMetrics) {
        StringBuilder content = new StringBuilder("🏁 TRAINING PERIOD SUMMARY\n\n");
        content.append(String.format("📆 Duration: %d weeks\n", allMetrics.size()));

        float totalDistance = allMetrics.stream()
                .map(w -> w.distance * w.numberOfRuns)
                .reduce(0f, Float::sum);

        float avgWeeklyDistance = totalDistance / allMetrics.size();

        float paceImprovement = (start.pace - end.pace) / start.pace * 100;
        float distImprovement = (end.distance - start.distance) / start.distance * 100;
        float hrImprovement = (start.heartRate > 0 && end.heartRate > 0)
                ? (start.heartRate - end.heartRate) / start.heartRate * 100 : 0;

        content.append("\n📉 Performance Changes:\n");
        content.append(String.format("- Pace: %.1f%% %s\n", Math.abs(paceImprovement),
                paceImprovement > 0 ? "faster" : "slower"));

        content.append(String.format("- Distance: %.1f%% %s\n", Math.abs(distImprovement),
                distImprovement > 0 ? "longer" : "shorter"));

        if (hrImprovement != 0) {
            content.append(String.format("- Heart Rate: %.1f%% %s\n", Math.abs(hrImprovement),
                    hrImprovement > 0 ? "lower" : "higher"));
        }

        content.append("\n💪 Training Load:\n");
        content.append(String.format("- Total Distance: %.1f km\n", totalDistance / 1000));
        content.append(String.format("- Avg Weekly Distance: %.1f km\n", avgWeeklyDistance / 1000));

        content.append("\n🔁 Recommendations:\n");
        if (paceImprovement > 5 && distImprovement > 10) {
            content.append("- Excellent progress! Consider adding speed work.\n");
        } else if (hrImprovement > 3) {
            content.append("- Good cardiovascular improvement detected.\n");
        } else if (distImprovement > 5) {
            content.append("- Maintain current training load.\n");
        } else {
            content.append("- Consider increasing volume or intensity.\n");
        }

        return content.toString();
    }

    private static String getTrend(float change, String positive, String negative, String neutral) {
        if (change > 0) return positive;
        else if (change < 0) return negative;
        return neutral;
    }
}

