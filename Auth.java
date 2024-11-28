package com.example.smartmarathonrunningapp;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
// Handles OAuth authentication flow for the Strava API
public class Auth extends AppCompatActivity {
    private final String redirectUri = "http://localhost/callback"; // Redirect URI for OAuth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (data != null && data.toString().startsWith(redirectUri)) {
            // Handle OAuth redirect
            handleRedirect(data);
        } else {
            // Start OAuth process
            initiateOAuth();
        }
    }

    private void initiateOAuth() {
        // Build OAuth URL and open it in the browser
        // Strava client ID
        String clientId = "136889";
        String authUrl = "https://www.strava.com/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + Uri.encode(redirectUri)
                + "&response_type=code"
                + "&scope=activity:read";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(intent);
    }

    private void handleRedirect(Uri uri) {
        // Extract the authorization code from the redirect URI
        String code = uri.getQueryParameter("code");
        if (code != null) {
            exchangeAuthorizationCodeForTokens(code);
        } else {
            String error = uri.getQueryParameter("error");
            Toast.makeText(this, "Authorization failed: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    private void exchangeAuthorizationCodeForTokens(String code) {
        // Placeholder for token exchange logic (not implemented here)
        Toast.makeText(this, "Authorization code received: " + code, Toast.LENGTH_SHORT).show();
    }
}
