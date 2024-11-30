package com.example.smartmarathonrunningapp;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
// Main activity that serves as the entry point of the app
public class MainActivity extends AppCompatActivity {
    private MyAdapter adapter; // Adapter for the RecyclerView
    private StravaRepository stravaRepository; // Repository for Strava API interactions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Set the activity's layout

        // Initialize RecyclerView and set its layout manager
        // UI component to display list of activities
        RecyclerView recyclerView = findViewById(R.id.recyclerView); // Replace with the actual RecyclerView ID
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the RecyclerView adapter
        adapter = new MyAdapter(); // Adapter starts with an empty list
        recyclerView.setAdapter(adapter); // Attach adapter to RecyclerView

        // Initialize StravaRepository for API interactions
        stravaRepository = new StravaRepository(this);

        // Fetch and display activities from the API
        fetchActivities();
    }

    // Fetches activities using the repository and updates the UI
    private void fetchActivities() {
        stravaRepository.fetchActivities(1, 30, new Callback<List<Activity>>() {
            @Override
            public void onResponse(@NonNull Call<List<Activity>> call, @NonNull Response<List<Activity>> response) {
                // If the API response is successful, update the RecyclerView with new data
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateData(response.body());
                } else {
                    Log.e("StravaAPI", "Failed to fetch activities: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Activity>> call, @NonNull Throwable t) {
                Log.e("StravaAPI", "API call failed: ", t);
            }
        });
    }
}
