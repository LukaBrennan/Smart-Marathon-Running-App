package com.example.smartmarathonrunningapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class TrainingPlan extends AppCompatActivity
{
    private final String clientId = "136889";
    private final String redirectUri = "http://localhost/callback";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (data != null && data.toString().startsWith(redirectUri))
        {
            handleRedirect(data);
        }
        else
        {
            initiateOAuth();
        }
    }

    private void initiateOAuth()
    {
        String authUrl = "https://www.strava.com/oauth/authorize";
        String url = authUrl + "?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&response_type=code&scope=activity:read";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void handleRedirect(Uri uri)
    {
        String code = uri.getQueryParameter("code");
        if (code != null)
        {
            exchangeAuthorizationCodeForTokens(code);
        }
        else
        {
            String error = uri.getQueryParameter("error");
            Toast.makeText(this, "Authorization failed: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    private void exchangeAuthorizationCodeForTokens(String code)
    {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.strava.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        StravaApiService apiService = retrofit.create(StravaApiService.class);

        String clientSecret = "965e30fa3ac626ee90d757a1d48d147fc80ed035";
        Call<TokenResponse> call = apiService.getAccessToken(clientId, clientSecret, code, "authorization_code");
        call.enqueue(new Callback<TokenResponse>()
        {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    TokenResponse tokenResponse = response.body();
                    saveTokens(tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresAt());
                    fetchUserActivities(tokenResponse.getAccessToken());
                }
                else
                {
                    Toast.makeText(TrainingPlan.this, "Token exchange failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t)
            {
                Toast.makeText(TrainingPlan.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserActivities(String accessToken)
    {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.strava.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        StravaApiService apiService = retrofit.create(StravaApiService.class);

        Call<List<Activity>> call = apiService.getUserActivities("Bearer " + accessToken);
        call.enqueue(new Callback<List<Activity>>()
        {
            @Override
            public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response)
            {
                if (response.isSuccessful() && response.body() != null)
                {
                    List<Activity> activities = response.body();
                    Toast.makeText(TrainingPlan.this, "Fetched " + activities.size() + " activities", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(TrainingPlan.this, "Failed to fetch activities", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t)
            {
                Toast.makeText(TrainingPlan.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTokens(String accessToken, String refreshToken, int expiresAt)
    {
        getSharedPreferences("StravaPrefs", MODE_PRIVATE).edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putInt("expires_at", expiresAt)
                .apply();
    }
}
