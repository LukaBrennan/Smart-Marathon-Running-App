package com.example.smartmarathonrunningapp_project.data;
import com.example.smartmarathonrunningapp_project.Activity;
import java.util.List;
//  Abstraction for “where activities come from”.
//  Implementations can be live (Strava) or offline (Mock assets).
public interface DataSource {
    interface ActivityListCallback {
        void onResult(List<Activity> data);
        void onError(Throwable t);
    }
    void fetchActivities(ActivityListCallback cb);
    String getName();
}
