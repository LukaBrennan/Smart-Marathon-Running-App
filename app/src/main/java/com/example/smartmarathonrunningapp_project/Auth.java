package com.example.smartmarathonrunningapp_project;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
public class Auth extends AppCompatActivity
{
    private final String redirectUri = "http://localhost/callback"; // Redirect URI for OAuth

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
        String clientId = "136889"; // Taken form the apps strava info. allowing for runners to connect to it.
        String authUrl = "https://www.strava.com/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + Uri.encode(redirectUri)
                + "&response_type=code"
                + "&scope=activity:read";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
    }

    private void handleRedirect(Uri uri)
    {
        String code = uri.getQueryParameter("code");
        if (code != null)
        {
            // Not implemented yet!!
            exchangeAuthorizationCodeForTokens();
        }
        else
        {
            String error = uri.getQueryParameter("error");
            Toast.makeText(this, "Authorization failed: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    // Not implemented yet!!
    private void exchangeAuthorizationCodeForTokens()
    {
    }

}
