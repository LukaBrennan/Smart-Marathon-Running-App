package com.example.smartmarathonrunningapp_project;
import com.google.gson.annotations.SerializedName;
public class OpenWeatherResponse{
    @SerializedName("main")
    private Main main;

    @SerializedName("wind")
    private Wind wind;

    @SerializedName("weather")
    private Weather[] weather;

    public Main getMain() { return main; }
    public Wind getWind() { return wind; }
    public Weather[] getWeather() { return weather; }

    public static class Main {
        @SerializedName("temp")
        private float temp;

        @SerializedName("humidity")
        private int humidity;

        public float getTemp() { return temp; }
        public int getHumidity() { return humidity; }
    }

    public static class Wind {
        @SerializedName("speed")
        private float speed;

        public float getSpeed() { return speed; }
    }

    public static class Weather {
        @SerializedName("description")
        private String description;

        public String getDescription() { return description; }
    }
}