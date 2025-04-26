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
public class TrainingPlanManager
    // This code manages the loading of Training plans from the JSON file and SharedPreferences
{
    private static final String TAG = "TrainingPlanManager";    //  This is for logging error messages
    private static final String PLAN_KEY = "OriginalPlan";
    private static final String TRAINING_PREFS = "TrainingPrefs";

    private final Context context;  //  Application context which is needed for file operations

    public TrainingPlanManager(Context context)
    {
        this.context = context;
    }
    //  Loads the default training plan from TrainingPlan.json in the assets folder, and returns the Training plan.
    public TrainingPlan loadTrainingPlanFromAssets()
    {
        try
        {
            //  Opens the JSON file
            InputStream inputStream = context.getAssets().open("TrainingPlan.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            //  Parse the JSON into the TrainingPlan object using GSON
            Gson gson = new Gson();
            return gson.fromJson(reader, TrainingPlan.class);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed to load training plan", e);
            return null;    //  If the TrainingPlan file cant be located, return null. Future release will allow for multiple training plans
        }
    }
    //  Saves both the original training plan and the runner adjusted training plan
    public void saveAdjustedPlan(TrainingPlan plan)
    {
        try
        {
            //  Accessing the apps shared preferences
            SharedPreferences prefs = context.getSharedPreferences(TRAINING_PREFS, MODE_PRIVATE);
            Gson gson = new Gson();
            //  Save the original if this is the first time saving
            if (!prefs.contains(PLAN_KEY))
            {
                TrainingPlan original = loadTrainingPlanFromAssets();
                prefs.edit().putString("original_plan", gson.toJson(original)).apply();
            }
            //  Save the adjusted plan
            prefs.edit().putString("adjusted_plan", gson.toJson(plan)).apply();
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to save plan", e);
        }
    }
    //  Loads the user adjusted training plan from shared preferences, and will fall back to the original plan if no user plan exists
    public TrainingPlan loadAdjustedPlan()
    {
        try
        {
            SharedPreferences prefs = context.getSharedPreferences(TRAINING_PREFS, MODE_PRIVATE);
            String json = prefs.getString("adjusted_plan", null);
            if (json != null)
            {
                return new Gson().fromJson(json, TrainingPlan.class);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to load plan", e);
        }
        //  Fallback to the original training plan
        return loadTrainingPlanFromAssets();
    }

    //  Loads the original training plan
    public TrainingPlan loadOriginalPlan()
    {
        try
        {
            SharedPreferences prefs = context.getSharedPreferences(TRAINING_PREFS, MODE_PRIVATE);
            String json = prefs.getString(PLAN_KEY, null);
            if (json != null)
            {
                return new Gson().fromJson(json, TrainingPlan.class);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Failed to load original plan", e);
        }
        return loadTrainingPlanFromAssets();
    }
}