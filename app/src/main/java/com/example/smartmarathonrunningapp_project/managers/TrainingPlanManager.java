package com.example.smartmarathonrunningapp_project.managers;
import static android.content.Context.MODE_PRIVATE;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.TrainingPlan;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
    //  TrainingPlanManager class manges the different training plans that can be shown, it has been changed to only show the adjusted training plan for now
public class TrainingPlanManager
{
    private static final String TAG = "TrainingPlanManager";    //  TAG for logging
    private static final String TRAINING_PREFS = "TrainingPrefs";   //
    private static final String ADJUSTED_PLAN_KEY = "adjusted_plan";    //
    private final Context context;  //
    //
    public TrainingPlanManager(Context context)
    {
        this.context = context;
    }
    //
    public TrainingPlan loadTrainingPlanFromAssets()
    {
        try
        {
            InputStream inputStream = context.getAssets().open("TrainingPlan.json");    //  Loads the static TrainingPlan from the assets folder
            InputStreamReader reader = new InputStreamReader(inputStream);
            Gson gson = new Gson(); //
            return gson.fromJson(reader, TrainingPlan.class);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed to load training plan", e);
            return null;
        }
    }
    //
    public void saveAdjustedPlan(TrainingPlan plan)
    {
        try
        {
            SharedPreferences prefs = context.getSharedPreferences(TRAINING_PREFS, MODE_PRIVATE);   //
            Gson gson = new Gson();
            // Always save as adjusted plan
            prefs.edit().putString(ADJUSTED_PLAN_KEY, gson.toJson(plan)).apply();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to save plan", e);
        }
    }
    //
    public TrainingPlan loadAdjustedPlan()
    {
        try
        {
            SharedPreferences prefs = context.getSharedPreferences(TRAINING_PREFS, MODE_PRIVATE);
            String json = prefs.getString(ADJUSTED_PLAN_KEY, null);
            if (json != null)
            {
                return new Gson().fromJson(json, TrainingPlan.class);
            }
            // If no adjusted plan exists, create one from the assets
            return loadTrainingPlanFromAssets();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to load plan", e);
            return loadTrainingPlanFromAssets();
        }
    }
}