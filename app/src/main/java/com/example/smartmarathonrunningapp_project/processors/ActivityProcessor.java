package com.example.smartmarathonrunningapp_project.processors;
import com.example.smartmarathonrunningapp_project.Activity;
import com.example.smartmarathonrunningapp_project.TrainingPlan;
import com.example.smartmarathonrunningapp_project.utils.DateUtils;
import com.example.smartmarathonrunningapp_project.utils.PaceUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Processes Strava activity data to filter and analyze running activities
public class ActivityProcessor
{
    // Filters activities to only those within the specified date range, For testing using OldRuns.JSON
    public List<Activity> filterActivitiesByDate(List<Activity> activities, Date startDate, Date endDate)
    {
        List<Activity> filteredActivities = new ArrayList<>();
        for (Activity activity : activities)
        {
            Date activityDate = DateUtils.parseDate(activity.getStart_date());
            if (activityDate != null && !activityDate.before(startDate) && !activityDate.after(endDate))
            {
                filteredActivities.add(activity);
            }
        }
        return filteredActivities;
    }
    // Selects only one run per day (the first one recorded), going to change later
    public List<Activity> extractDailyRuns(List<Activity> activities)
    {
        Map<String, Activity> runsByDate = new HashMap<>();
        for (Activity activity : activities)
        {
            // Extract just the date portion (before 'T' in ISO format)
            String activityDate = activity.getStart_date().split("T")[0];
            // Only keep the first activity for each date
            runsByDate.putIfAbsent(activityDate, activity);
        }
        return new ArrayList<>(runsByDate.values());
    }
    // Checks if an activity matches the planned training plan, Going to change as TrainingPlan is not fully set up
    public boolean activityMatchesPlan(Activity activity, TrainingPlan.Day day)
    {
        // If pace is specified in plan, verify the activity meets it
        if (!"Run".equals(activity.getType()))
        {
            return false;
        }
        if (day.getPace() != null)
        {
            float activityPace = activity.getMoving_time() / activity.getDistance();
            float requiredPace = PaceUtils.convertPaceToSeconds(day.getPace());
            return activityPace <= requiredPace;
        }
        return true;
    }
}