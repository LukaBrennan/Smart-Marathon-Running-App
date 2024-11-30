//package com.example.smartmarathonrunningapp;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import java.util.List;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class Register extends AppCompatActivity {
//
//    private static final String CLIENT_ID = "136889";
//    private static final String CLIENT_SECRET = "965e30fa3ac626ee90d757a1d48d147fc80ed035";
//    private static final String REDIRECT_URI = "smartmarathon://register";
//    private static final String AUTHORIZATION_URL = "https://www.strava.com/oauth/authorize";
//    private static final int STRAVA_REQUEST_CODE = 100;
//
//    // UI components
//    private EditText etLastCycleStartDate;
//    private EditText etCycleLength;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.register);
//
//        // Initialize UI components
//        etLastCycleStartDate = findViewById(R.id.etLastCycleStartDate);
//        etCycleLength = findViewById(R.id.etCycleLength);
//        RadioGroup rgGender = findViewById(R.id.Gender);
//        Button btnLinkStrava = findViewById(R.id.LinkStrava);
//
//        // Hide menstrual fields initially
//        setMenstrualFieldsVisibility(false);
//
//        // Set listener to show menstrual cycle fields only for female users
//        rgGender.setOnCheckedChangeListener((group, checkedId) -> {
//            boolean isFemale = (checkedId == R.id.Female);
//            setMenstrualFieldsVisibility(isFemale);
//        });
//
//        // Initialize Strava authorization button
//        btnLinkStrava.setOnClickListener(v -> initiateStravaAuth());
//    }
//
//    // Toggle visibility of menstrual fields based on gender
//    private void setMenstrualFieldsVisibility(boolean isVisible) {
//        int visibility = isVisible ? View.VISIBLE : View.GONE;
//        findViewById(R.id.tvLastCycleStartDate).setVisibility(visibility);
//        etLastCycleStartDate.setVisibility(visibility);
//        findViewById(R.id.tvCycleLength).setVisibility(visibility);
//        etCycleLength.setVisibility(visibility);
//    }
//
//    // Start the Strava OAuth authorization process
//    private void initiateStravaAuth() {
//        Uri uri = Uri.parse(AUTHORIZATION_URL)
//                .buildUpon()
//                .appendQueryParameter("client_id", CLIENT_ID)
//                .appendQueryParameter("redirect_uri", REDIRECT_URI)
//                .appendQueryParameter("response_type", "code")
//                .appendQueryParameter("scope", "read,activity:read")
//                .build();
//
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        startActivityForResult(intent, STRAVA_REQUEST_CODE);
//    }
//
//    // Handle the result from Strava authorization
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == STRAVA_REQUEST_CODE && data != null && data.getData() != null) {
//            Uri uri = data.getData();
//            String authorizationCode = uri.getQueryParameter("code");
//
//            if (authorizationCode != null) {
//                // Exchange the authorization code for an access token
//                exchangeAuthorizationCodeForToken(authorizationCode);
//            }
//        }
//    }
//
//    // Fetch activities with access token
//    private void fetchUserActivities(String accessToken) {
//        StravaApiService stravaService = RetrofitClient.getStravaService();
//        Call<List<Activity>> call = stravaService.getUserActivities(accessToken);
//
//        call.enqueue(new Callback<List<Activity>>() {
//            @Override
//            public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    List<Activity> activities = response.body();
//                    // Handle activities, e.g., display them in the UI
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
//                t.printStackTrace();
//            }
//        });
//    }
//
//    // Exchange authorization code for token (use Retrofit)
//    private void exchangeAuthorizationCodeForToken(String authorizationCode) {
//        StravaApiService stravaService = RetrofitClient.getStravaService();
//        Call<TokenResponse> call = stravaService.getAccessToken(
//                CLIENT_ID,
//                CLIENT_SECRET,
//                authorizationCode,
//                "authorization_code"
//        );
//
//        call.enqueue(new Callback<TokenResponse>() {
//            @Override
//            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    String accessToken = response.body().getAccessToken();
//                    saveAccessToken(accessToken);
//                    fetchUserActivities(accessToken);
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
//                t.printStackTrace();
//            }
//        });
//    }
//
//    // Save access token locally
//    private void saveAccessToken(String token) {
//        SharedPreferences prefs = getSharedPreferences("strava_prefs", MODE_PRIVATE);
//        prefs.edit().putString("access_token", token).apply();
//    }
//
//    // Retrieve saved access token
//    private String getAccessToken() {
//        SharedPreferences prefs = getSharedPreferences("strava_prefs", MODE_PRIVATE);
//        return prefs.getString("access_token", null);
//    }
//}
