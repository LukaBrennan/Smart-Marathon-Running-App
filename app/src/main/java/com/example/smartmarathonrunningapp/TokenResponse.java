package com.example.smartmarathonrunningapp;

public class TokenResponse {

    private String access_token;
    private String token_type;
    private String refresh_token;
    private int expires_at;

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String token_type) {
        this.token_type = token_type;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void setRefreshToken(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public int getExpiresAt() {
        return expires_at;
    }

    public void setExpiresAt(int expires_at) {
        this.expires_at = expires_at;
    }
}
