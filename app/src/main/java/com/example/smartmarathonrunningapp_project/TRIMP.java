package com.example.smartmarathonrunningapp_project;

public class TRIMP {
    private static final float MALE_FACTOR = 0.64f;
    private static final float FEMALE_FACTOR = 0.86f;

    public static float calculate(float durationMinutes, float avgHR,
                                  float restingHR, float maxHR, boolean isMale) {
        if (restingHR <= 0 || maxHR <= 0 || avgHR <= restingHR) return 0;

        float hrReserve = (avgHR - restingHR) / (maxHR - restingHR);
        float genderFactor = isMale ? MALE_FACTOR : FEMALE_FACTOR;

        return (float)(durationMinutes * hrReserve * 0.64 * Math.exp(genderFactor * hrReserve));
    }
}