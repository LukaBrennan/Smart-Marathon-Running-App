package com.example.smartmarathonrunningapp_project.data;
import android.util.Log;
import com.example.smartmarathonrunningapp_project.Activity;
import com.example.smartmarathonrunningapp_project.StravaRepository;
import com.example.smartmarathonrunningapp_project.TokenResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
 /*
    Fetches activities from Strava using Retrofit through StravaRepository.
    Handles the two-step flow:
    1) Refresh OAuth access token.
    2) Use the fresh token to request recent activities.
 */
public final class StravaDataSource implements DataSource {
    private static final String TAG = "StravaDataSource";
    private final StravaRepository repo;
    public StravaDataSource(StravaRepository repo) { this.repo = repo; }
    @Override public void fetchActivities(ActivityListCallback cb) {
        repo.refreshAccessToken(new Callback<>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    Log.e(TAG, "Token refresh failed code=" + resp.code());
                    cb.onError(new RuntimeException("Token refresh failed"));
                    return;
                }
                String token = resp.body().getAccessToken();
                repo.fetchActivities(token, 1, 60, new Callback<>() {
                    @Override
                    public void onResponse(Call<List<Activity>> call, Response<List<Activity>> r) {
                        if (!r.isSuccessful() || r.body() == null) {
                            Log.e(TAG, "Fetch activities failed code=" + r.code());
                            cb.onError(new RuntimeException("Fetch failed"));
                            return;
                        }
                        cb.onResult(r.body());
                    }
                    @Override
                    public void onFailure(Call<List<Activity>> call, Throwable t) {
                        Log.e(TAG, "Fetch activities error", t);
                        cb.onError(t);
                    }
                });
            }
            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Log.e(TAG, "Token refresh error", t);
                cb.onError(t);
            }
        });
    }
    @Override public String getName() { return "Strava"; }
}
