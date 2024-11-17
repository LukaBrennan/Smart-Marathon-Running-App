package com.example.smartmarathonrunningapp;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface StravaApiService {

    @POST("oauth/token")
    Call<TokenResponse> getAccessToken(
            @Query("client_id") String clientId,
            @Query("client_secret") String clientSecret,
            @Query("code") String code,
            @Query("grant_type") String grantType
    );

    @GET("api/v3/athlete/activities")
    Call<List<Activity>> getUserActivities(
            @Header("Authorization") String accessToken
    );
}
