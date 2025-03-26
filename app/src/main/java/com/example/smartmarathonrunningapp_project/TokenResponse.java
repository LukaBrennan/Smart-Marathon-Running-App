// Class for getting the accessToken from STRAVA users accounts
package com.example.smartmarathonrunningapp_project;
public class TokenResponse
{
    private final String access_token;
    public TokenResponse(String accessToken)
    {
        access_token = accessToken;
    }

    public String getAccessToken() {
        return access_token;
    }

}
