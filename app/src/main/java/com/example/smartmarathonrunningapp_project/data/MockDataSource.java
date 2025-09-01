package com.example.smartmarathonrunningapp_project.data;
import android.content.Context;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.Activity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

/*
    Loads a static list of Activity objects from an assets JSON file.
    Useful for development, demos, or when Strava isn’t reachable.
 */
public final class MockDataSource implements DataSource {
    private static final String TAG = "MockDataSource";
    private final Context ctx;
    private final String assetFile;
    public MockDataSource(Context ctx, String assetFile) {
        this.ctx = ctx.getApplicationContext();
        this.assetFile = assetFile;
    }
    @Override public void fetchActivities(ActivityListCallback cb) {
        try (InputStreamReader r = new InputStreamReader(ctx.getAssets().open(assetFile))) {
            List<Activity> acts = new Gson().fromJson(r, new TypeToken<List<Activity>>(){}.getType());
            Log.d(TAG, "Loaded " + (acts == null ? 0 : acts.size()) + " from assets/" + assetFile);
            cb.onResult(acts == null ? Collections.emptyList() : acts);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load assets/" + assetFile, e);
            cb.onError(e);
        }
    }
    @Override public String getName() { return "Mock"; }
}
