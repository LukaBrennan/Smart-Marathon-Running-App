package com.example.smartmarathonrunningapp_project;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
public class Auth extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initiateOAuth();
    }
    private void initiateOAuth()
            // TODO - Only 1 runner is able to use, last iteration make this dynamic with a DataBase
    {
        String clientId = "136889"; // Taken form the apps strava info. allowing for runners to connect to it.
        // Redirect URI for OAuth
        String redirectUri = "http://localhost/callback";
        String authUrl = "https://www.strava.com/oauth/authorize?client_id=" + clientId
                + "&redirect_uri=" + Uri.encode(redirectUri)
                + "&response_type=code"
                + "&scope=activity:read";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
    }
}
