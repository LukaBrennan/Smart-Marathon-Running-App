package com.example.smartmarathonrunningapp_project.processors;
public class WeeklyReport {
    public final int weekNumber;
    public final String content;

    public WeeklyReport(int weekNumber, String content) {
        this.weekNumber = weekNumber;
        this.content = content;
    }
}