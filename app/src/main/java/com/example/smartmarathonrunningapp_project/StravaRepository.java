package com.example.smartmarathonrunningapp_project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Handles API interactions with the Strava API
public class StravaRepository {
    private final StravaApiService apiService; // Retrofit interface for Strava API

    // Constructor initializes Retrofit
    public StravaRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.strava.com/") // Base URL for Strava API
                .addConverterFactory(GsonConverterFactory.create()) // Converts JSON to Java objects
                .build();

        apiService = retrofit.create(StravaApiService.class); // Create API service instance
    }

    // Fetches activities from the Strava API using a dynamic access token
    public void fetchActivities(String accessToken, int page, int perPage, Callback<List<Activity>> callback) {
        apiService.getUserActivities("Bearer " + accessToken, page, perPage).enqueue(callback);
    }
    public void refreshAccessToken(Callback<TokenResponse> callback) {
        String clientId = "136889";
        String clientSecret = "965e30fa3ac626ee90d757a1d48d147fc80ed035";
        String refreshToken = "e4f2b4dc8956373147a167e5afaa1e4be5e91dfb";

        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("refresh_token", refreshToken);
        params.put("grant_type", "refresh_token");

        apiService.getAccessToken(params).enqueue(callback);
    }


}
