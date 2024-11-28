package com.example.smartmarathonrunningapp;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
public interface StravaApiService {

    // To fetch the access token using authorization code
    @POST("oauth/token")
    Call<TokenResponse> getAccessToken(@Body Map<String, String> params);

    // To fetch user activities
    @GET("api/v3/athlete/activities")
    Call<List<Activity>> getUserActivities(
            @Header("Authorization") String accessToken,
            @Query("page") int page,
            @Query("per_page") int perPage
    );
}