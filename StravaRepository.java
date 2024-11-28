package com.example.smartmarathonrunningapp;
import java.util.List;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
// Handles API interactions with the Strava API
public class StravaRepository {
    private final StravaApiService apiService; // Retrofit interface for Strava API

    // Constructor initializes Retrofit and shared preferences
    public StravaRepository(MainActivity mainActivity) {
        // Stores user preferences or settings

        // Create Retrofit instance for making network requests
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.strava.com/") // Base URL for Strava API
                .addConverterFactory(GsonConverterFactory.create()) // Converts JSON to Java objects
                .build();

        apiService = retrofit.create(StravaApiService.class); // Create API service instance
    }

    // Fetches activities from the Strava API using a hardcoded access token
    public void fetchActivities(int page, int perPage, Callback<List<Activity>> callback) {
        // Hardcoded access token for API authentication
        String accessToken = "cee39cc9d13b2afa4c36ad12772d0876723149a9";

        // Make an asynchronous API request to fetch user activities
        apiService.getUserActivities("Bearer " + accessToken, page, perPage).enqueue(callback);
    }
}
