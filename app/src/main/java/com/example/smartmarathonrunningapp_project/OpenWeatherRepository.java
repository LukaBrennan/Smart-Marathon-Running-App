package com.example.smartmarathonrunningapp_project;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.OpenWeatherResponse;
import com.example.smartmarathonrunningapp_project.OpenWeatherApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class OpenWeatherRepository {
    private final OpenWeatherApiService apiService;
    private static final String API_KEY = "53010b4c7d877089516291d5b7ab4e94";

    public OpenWeatherRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(OpenWeatherApiService.class);
    }
    public void fetchWeather(double lat, double lon, Callback<OpenWeatherResponse> callback) {
        apiService.getWeather(lat, lon, API_KEY, "metric").enqueue(callback);
    }
}