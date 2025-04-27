package com.example.smartmarathonrunningapp_project;
import java.util.List;
// All of the api calls to get data from STRAVA
public class Activity {
    private String name;
    private float distance;
    private int moving_time;
    private int elapsed_time;
    private float total_elevation_gain;
    private String type;
    private String start_date;
    private float average_speed;
    private float average_heartrate;
    private float max_heartrate;
    private float max_speed;
    private List<Float> start_latlng;
    private float resting_heartrate = 60;
    private boolean isMale = true;
    public float getMax_speed() {
        return max_speed;
    }

    public void setMax_speed(float max_speed) {
        this.max_speed = max_speed;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public int getMoving_time() {
        return moving_time;
    }

    public void setMoving_time(int moving_time) {
        this.moving_time = moving_time;
    }

    public int getElapsed_time() {
        return elapsed_time;
    }

    public void setElapsed_time(int elapsed_time) {
        this.elapsed_time = elapsed_time;
    }

    public float getTotal_elevation_gain() {
        return total_elevation_gain;
    }

    public void setTotal_elevation_gain(float total_elevation_gain) {
        this.total_elevation_gain = total_elevation_gain;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getName() {
        return name;
    }

    public float getAverage_speed() {
        return average_speed;
    }

    public void setAverage_speed(float average_speed) {
        this.average_speed = average_speed;
    }

    public float getMax_heartrate() {
        return max_heartrate;
    }

    public void setMax_heartrate(float max_heartrate) {
        this.max_heartrate = max_heartrate;
    }

    public float getAverage_heartrate() {
        return average_heartrate;
    }

    public void setAverage_heartrate(float average_heartrate) {
        this.average_heartrate = average_heartrate;
    }

    public List<Float> getStart_latlng() {
        return start_latlng;
    }

    public void setStart_latlng(List<Float> start_latlng) {
        this.start_latlng = start_latlng;
    }

    public double getLatitude() {
        return (start_latlng != null && !start_latlng.isEmpty()) ? start_latlng.get(0) : 0.0;
    }

    public double getLongitude() {
        return (start_latlng != null && start_latlng.size() > 1) ? start_latlng.get(1) : 0.0;
    }

    public float getResting_heartrate() { return resting_heartrate; }
    public void setResting_heartrate(float resting_heartrate) {
        this.resting_heartrate = resting_heartrate;
    }

    public boolean isMale() { return isMale; }
    public void setMale(boolean isMale) {
        this.isMale = isMale;
    }

    // Converts average speed (m/s) to pace (min/mile)
    public float getPaceInSeconds() {
        float metersPerMile = 1609.34f;
        return distance > 0 ? (moving_time / (distance / metersPerMile)) : 0;
    }

    public int getHeartRate() {
        return (int) average_heartrate;
    }


}
