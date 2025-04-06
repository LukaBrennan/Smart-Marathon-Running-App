package com.example.smartmarathonrunningapp_project.managers;
import android.content.Context;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.TrainingPlan;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

    // Manages loading and accessing the training plan from JSON assets
public class TrainingPlanManager
{
    // TAG for errors
    private static final String TAG = "TrainingPlanManager";
    private final Context context; // Needed to access app assets

    public TrainingPlanManager(Context context)
    {
        this.context = context;
    }
    // Loads training plan from JSON file in app assets
    public TrainingPlan loadTrainingPlanFromAssets()
    {
        try
        {
            InputStream inputStream = context.getAssets().open("TrainingPlan.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            Gson gson = new Gson();
            return gson.fromJson(reader, TrainingPlan.class);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed to load training plan", e);
            return null;
        }
    }
    // Finds a training day by its name
    public static TrainingPlan.Day getDayByName(TrainingPlan.TrainingWeek week, String dayName)
    {
        // Map day names to their corresponding Day objects
        Map<String, TrainingPlan.Day> dayMap = Map.of(
                "Monday", week.getTraining_plan().getMonday(),
                "Tuesday", week.getTraining_plan().getTuesday(),
                "Wednesday", week.getTraining_plan().getWednesday(),
                "Thursday", week.getTraining_plan().getThursday(),
                "Friday", week.getTraining_plan().getFriday(),
                "Saturday", week.getTraining_plan().getSaturday(),
                "Sunday", week.getTraining_plan().getSunday()
        );
        return dayMap.getOrDefault(dayName, null);
    }
}