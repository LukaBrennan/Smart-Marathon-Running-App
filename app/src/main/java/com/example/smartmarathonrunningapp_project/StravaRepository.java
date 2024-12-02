package com.example.smartmarathonrunningapp_project;

import java.util.List;
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
}
