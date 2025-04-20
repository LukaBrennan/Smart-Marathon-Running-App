package com.example.smartmarathonrunningapp_project;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.example.smartmarathonrunningapp_project.processors.ActivityProcessor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class StravaRepository
{
    private final StravaApiService apiService;
    private static final String TAG = "StravaRepository";
    private Context context;
    private List<Activity> cachedActivities = new ArrayList<>();
    public StravaRepository(Context context)
    {
        this.context = context;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.strava.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(StravaApiService.class);
    }
    public void fetchActivities(String accessToken, int page, int perPage, Callback<List<Activity>> callback)
    {
        // Logging
        apiService.getUserActivities("Bearer " + accessToken, page, perPage)
                .enqueue(new Callback<>()
                {
                    @Override
                    public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
                    {
                        if (response.isSuccessful() && response.body() != null) {
                            cachedActivities = response.body();
                            callback.onResponse(call, response);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
                        Log.e(TAG, "API Call Failed", t);
                        callback.onFailure(call, t);
                    }
                });
    }

    public List<Activity> getCachedActivities()
    {
        Log.d("StravaRepository", "Returning " + (cachedActivities != null ? cachedActivities.size() : "null") + " cached activities.");
        return cachedActivities;
    }

    // Helper method to convert date string to Unix timestamp
    private long convertDateToUnixTimestamp(String dateStr) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            Date date = dateFormat.parse(dateStr);
            return (date != null) ? date.getTime() / 1000 : 0;
        }
        catch (ParseException e)
        {
            Log.e(TAG, "Failed to parse date: " + dateStr, e);
            return 0;
        }
    }
    public void refreshAccessToken(Callback<TokenResponse> callback)
    {
        String clientId = "136889";
        String clientSecret = "965e30fa3ac626ee90d757a1d48d147fc80ed035";
        String refreshToken = "e4f2b4dc8956373147a167e5afaa1e4be5e91dfb"; //Unique
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        apiService.getAccessToken(params).enqueue(callback);
    }

    public void loadLocalActivities(Context context) {
        try {
            InputStream is = context.getAssets().open("OldRuns.json");
            Type listType = new TypeToken<List<Activity>>(){}.getType();
            List<Activity> allActivities = new Gson().fromJson(new InputStreamReader(is), listType);

            // Filter valid runs but keep all activities (remove daily run filtering)
            cachedActivities = new ActivityProcessor().filterValidRuns(allActivities);

        } catch (IOException e) {
            Log.e(TAG, "Error loading local activities", e);
            cachedActivities = new ArrayList<>();
        }
    }
}
