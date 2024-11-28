package com.example.smartmarathonrunningapp;

public class TokenResponse {
    private String access_token;
    private String refresh_token;
    private long expires_at;

    public String getAccessToken() {
        return access_token;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public long getExpiresAt() {
        return expires_at;
    }
}
