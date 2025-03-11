package com.example.smartmarathonrunningapp_project;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StravaRepository {
    private final StravaApiService apiService;

    public StravaRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.strava.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(StravaApiService.class);
    }

    // Fetch activities with date filters
    public void fetchActivities(String accessToken, int page, int perPage, Callback<List<Activity>> callback) {
        // Convert START_DATE and END_DATE to Unix timestamps
        long after = convertDateToUnixTimestamp(MainActivity.START_DATE);
        long before = convertDateToUnixTimestamp(MainActivity.END_DATE);

        // Fetch activities with date filters
        apiService.getUserActivities("Bearer " + accessToken, page, perPage, after, before).enqueue(callback);
    }

    // Helper method to convert date string to Unix timestamp
    private long convertDateToUnixTimestamp(String dateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = dateFormat.parse(dateStr);
            return date.getTime() / 1000; // Convert to seconds
        } catch (ParseException e) {
            Log.e("StravaRepository", "Failed to parse date: " + dateStr, e);
            return 0;
        }
    }
    public void refreshAccessToken(Callback<TokenResponse> callback) {
        String clientId = "136889";
        String clientSecret = "965e30fa3ac626ee90d757a1d48d147fc80ed035";
        String refreshToken = "e4f2b4dc8956373147a167e5afaa1e4be5e91dfb"; //Unique TODO: During final release to ensure that this is not hardcoded, so that all users refresh_tokens are able to work!

        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");

        apiService.getAccessToken(params).enqueue(callback);
    }


}
