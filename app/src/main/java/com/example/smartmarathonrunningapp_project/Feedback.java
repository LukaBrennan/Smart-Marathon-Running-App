package com.example.smartmarathonrunningapp_project;
import android.util.Log;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class Feedback {
    private double paceDifference;
    private int heartRateDifference;
    private double todayPace;
    private int todayHeartRate;

    public Feedback(double paceDifference, int heartRateDifference, double todayPace, int todayHeartRate) {
        this.paceDifference = paceDifference;
        this.heartRateDifference = heartRateDifference;
        this.todayPace = todayPace;
        this.todayHeartRate = todayHeartRate;
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Great job today, Luka! You've made some awesome progress!\n\n");

        if (paceDifference > 0) {
            sb.append("🏃 Your pace improvement is impressive! ");
            sb.append("Last run was slower, but today you ran at ").append(String.format("%.2f", todayPace));
            sb.append(" min/mile. That’s a ").append(String.format("%.2f", paceDifference)).append(" improvement—awesome work!\n\n");
        } else {
            sb.append("🏃 Pace was a bit slower today. Try to rest well and bounce back!\n\n");
        }

        if (heartRateDifference > 0) {
            sb.append("❤️ Your heart rate is staying strong! ");
            sb.append("Today it was ").append(todayHeartRate).append(" bpm, a drop of ").append(heartRateDifference);
            sb.append(" bpm. You're becoming more efficient!\n");
        } else {
            sb.append("❤️ Heart rate was slightly higher today—stay hydrated and recover well!\n");
        }

        return sb.toString();
    }
}
