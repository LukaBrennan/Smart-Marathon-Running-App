package com.example.smartmarathonrunningapp_project.managers;
import static android.content.Context.MODE_PRIVATE;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.TrainingPlan;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
/*
    Manages loading/saving training plans from assets and local storage.
Provides two types of plans:
    Base plan: static JSON from assets.
    Adjusted plan: persisted JSON with athlete-specific adjustments.
 */
public class TrainingPlanManager {
    private static final String TAG = "TrainingPlanManager";
    private static final String PREFS = "TrainingPrefs";
    private static final String KEY_ADJUSTED_PLAN = "adjusted_plan";
    private static final String ASSET_PLAN = "TrainingPlan.json";
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    public TrainingPlanManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, MODE_PRIVATE);
        gson = new Gson();
    }
    //  Loads the pristine training plan from the assets folder
    public TrainingPlan loadBasePlan() {
        try (InputStream in = context.getAssets().open(ASSET_PLAN);
             InputStreamReader reader = new InputStreamReader(in)) {
            return gson.fromJson(reader, TrainingPlan.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load base plan from assets/" + ASSET_PLAN, e);
            return null;
        }
    }
    //  Loads the adjusted TrainingPlan
    public TrainingPlan loadAdjustedPlan() {
        try {
            String json = prefs.getString(KEY_ADJUSTED_PLAN, null);
            if (json == null) return null;
            return gson.fromJson(json, TrainingPlan.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load adjusted plan", e);
            return null;
        }
    }
    //  Adjusted TrainingPlan is saved back to SharedPreferences
    public void saveAdjustedPlan(TrainingPlan plan) {
        try {
            prefs.edit().putString(KEY_ADJUSTED_PLAN, gson.toJson(plan)).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save adjusted plan", e);
        }
    }

}
