package com.example.smartmarathonrunningapp_project;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import java.util.*;
import java.util.stream.Collectors;

/*
    Light-weight analytics helpers used to interpret the runner’s recent performances, including:
    hrPaceDeltaFromRecentRuns(): naive regression between HR and pace (per-mile) to detect if the latest run looks better/worse than expected.
    Utility helpers to predict pace, compute deltas, and convert formats.
*/
public final class PerformanceEvaluator {

    private PerformanceEvaluator() {}

    private static final float FOUR_MILES_M = 4f * 1609.344f;

    public static int hrPaceDeltaFromRecentRuns(List<Activity> runs) {
        if (runs == null) return 0;

        List<Activity> valid = runs.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getDistance() >= FOUR_MILES_M)
                .collect(Collectors.toList());
        if (valid.size() < 2) return 0;

        valid.sort(Comparator.comparing(a -> DateUtils.parseDate(a.getStart_date())));
        List<Activity> history = valid.subList(0, valid.size() - 1);
        Activity latest = valid.get(valid.size() - 1);

        List<Pair> train = new ArrayList<>();
        for (Activity a : history) train.addAll(extractMilePairs(a));
        List<Pair> test  = extractMilePairs(latest);

        if (train.size() < 3 || test.size() < 2) return 0;

        LinearModel model = fit(train);
        if (model == null) return 0;

        int better = 0, worse = 0;
        for (Pair p : test) {
            double predicted = model.predict(p.hr);
            double diff = predicted - p.paceSec;
            if (diff >= 5.0)       better++;
            else if (diff <= -5.0) worse++;
        }

        if (better > worse && better >= test.size() / 2) return -5;
        if (worse > better && worse >= test.size() / 2) return +5;
        return 0;
    }

    private static final class Pair {
        final double hr;
        final double paceSec;
        Pair(double hr, double paceSec) { this.hr = hr; this.paceSec = paceSec; }
    }

    private static final class LinearModel {
        final double a, b;
        LinearModel(double a, double b) { this.a = a; this.b = b; }
        double predict(double hr) { return a * hr + b; }
    }

    private static List<Pair> extractMilePairs(Activity a) {
        List<Pair> out = new ArrayList<>();
        List<Activity.Split> splits = a.getSplits_standard();
        if (splits == null || splits.size() < 2) return out;

        for (int i = 1; i < splits.size(); i++) {
            Activity.Split s = splits.get(i);
            if (s == null) continue;
            if (s.distance < 1400 || s.distance > 1800) continue;
            if (s.moving_time <= 0 || s.average_heartrate <= 0) continue;

            double paceSecPerMile = (s.moving_time / (s.distance / 1609.344));
            out.add(new Pair(s.average_heartrate, paceSecPerMile));
        }
        return out;
    }

    private static LinearModel fit(List<Pair> pts) {
        int n = pts.size();
        if (n == 0) return null;
        double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0;
        for (Pair p : pts) {
            sumX += p.hr; sumY += p.paceSec;
            sumXX += p.hr * p.hr; sumXY += p.hr * p.paceSec;
        }
        double denom = n * sumXX - sumX * sumX;
        if (Math.abs(denom) < 1e-6) return null;
        double a = (n * sumXY - sumX * sumY) / denom;
        double b = (sumY - a * sumX) / n;
        return new LinearModel(a, b);
    }


    public static float predictPaceFromHR(float heartRate) {
        return (float)(-9.84f * heartRate + 1828.12f);
    }

    public static int calculatePaceDelta(float actual, float predicted) {
        float delta = predicted - actual;
        return Math.abs(delta) < 5 ? 0 : Math.round(delta);
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

