package com.example.smartmarathonrunningapp_project;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
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
    public StravaRepository()
    {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.strava.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(StravaApiService.class);
    }
        //  This method used the access token and how many pages of data to collect
    public void fetchActivities(String accessToken, int page, int perPage, Callback<List<Activity>> callback)
    {
        apiService.getUserActivities("Bearer " + accessToken, page, perPage)
                .enqueue(new Callback<>()
                {
                    @Override
                    public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
                    {
                        if (response.isSuccessful() && response.body() != null) {
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
        //  Dynamically gets the refresh token and uses it when needed
    public void refreshAccessToken(Callback<TokenResponse> callback)
    {
        String clientId = "136889";
        String clientSecret = "965e30fa3ac626ee90d757a1d48d147fc80ed035";
        String refreshToken = "e4f2b4dc8956373147a167e5afaa1e4be5e91dfb";   //  This is the unique refresh token for the runner
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");
        apiService.getAccessToken(params).enqueue(callback);
    }

    public List<Activity> generateTestActivities() {
        List<Activity> testActivities = new ArrayList<>();

        // Create activities with varying performance levels
        testActivities.add(createTestActivity("2025-06-10", 8000, 2400, 150, 180)); // GREEN
        testActivities.add(createTestActivity("2025-06-11", 5000, 1800, 160, 185)); // YELLOW
        testActivities.add(createTestActivity("2025-06-12", 10000, 4200, 170, 190)); // RED
        return testActivities;
    }

    private Activity createTestActivity(String date, float distanceMeters, int movingTimeSec,
                                        float avgHR, float maxHR) {
        Activity activity = new Activity();
        activity.setStart_date(date);
        activity.setDistance(distanceMeters);
        activity.setMoving_time(movingTimeSec);
        activity.setAverage_heartrate(avgHR);
        activity.setMax_heartrate(maxHR);
        activity.setType("Run");
        return activity;
    }
}
