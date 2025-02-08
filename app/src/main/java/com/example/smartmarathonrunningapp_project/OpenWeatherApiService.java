package com.example.smartmarathonrunningapp_project;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface OpenWeatherApiService {
    @GET("data/2.5/weather")
    Call<OpenWeatherResponse> getWeather(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units
    );
}