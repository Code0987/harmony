package com.ilusons.harmony.ref;

public class GLEx {

    public static float normalizeGl(float val, float newFromVal, float newToVal) {
        return normalizeGl(val, -1, 1, newFromVal, newToVal);
    }

    public static float normalizeGl(float val, float fromVal, float toVal, float newFromVal, float newToVal) {
        float perc = (val - fromVal) / (toVal - fromVal);
        return newFromVal + perc * (newToVal - newFromVal);
    }

}
