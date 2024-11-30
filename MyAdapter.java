package com.example.smartmarathonrunningapp;
import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

// Adapter for the RecyclerView that binds activity data to the views
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private final List<Activity> activities; // List of activities to display

    // Default constructor for creating an adapter with an empty list
    public MyAdapter() {
        this.activities = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout for each list item
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_main, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Bind activity data to the view components
        Activity activity = activities.get(position);
        holder.activityNameTextView.setText(activity.getName());
    }

    @Override
    public int getItemCount() {
        return activities.size(); // Return the total number of activities
    }

    // Updates the adapter's data and refreshes the RecyclerView
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Activity> newActivities) {
        activities.clear();
        activities.addAll(newActivities);
        notifyDataSetChanged(); // Notify the adapter to refresh the UI
    }

    // ViewHolder class that holds references to the views for each list item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView activityNameTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            activityNameTextView = itemView.findViewById(R.id.activityTextView);
        }
    }
}
