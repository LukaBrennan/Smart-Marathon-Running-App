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

    public void saveAdjustedPlan(TrainingPlan plan) {
        try {
            String json = new Gson().toJson(plan);
            // Save to SharedPreferences or file
            SharedPreferences prefs = context.getSharedPreferences("TrainingPrefs", MODE_PRIVATE);
            prefs.edit().putString("adjusted_plan", json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save plan", e);
        }
    }

    public TrainingPlan loadAdjustedPlan() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("TrainingPrefs", MODE_PRIVATE);
            String json = prefs.getString("adjusted_plan", null);
            if (json != null) {
                return new Gson().fromJson(json, TrainingPlan.class);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load plan", e);
        }
        return loadTrainingPlanFromAssets(); // Fallback
    }

}