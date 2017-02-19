package com.ilusons.harmony.ref;

import java.util.Random;

public class MathEx {

    public static float clamp(float val, float min, float max) {
        return Math.max(Math.min(val, max), min);
    }

    public static int clamp(int val, int min, int max) {
        return Math.max(Math.min(val, max), min);
    }

    public static float normalize(float val, float from, float to) {
        if (val < from)
            return 0;
        if (val > to)
            return 1;
        return val / (to - from);
    }

    /**
     * Convert square of magnitude to decibels
     *
     * @param squareMag square of magnitude
     * @return decibels
     */
    public static float magnitudeToDb(float squareMag) {
        if (squareMag == 0)
            return 0;
        return (float) (20 * Math.log10(squareMag));
    }

    /**
     * Exponential smoothing (Holt - Winters).
     *
     * @param prevValue previous values in series <code>X[i-1]</code>
     * @param newValue  new value in series <code>X[i]</code>
     * @param a         smooth coefficient
     * @return smoothed value
     */
    public static float smooth(float prevValue, float newValue, float a) {
        return a * newValue + (1 - a) * prevValue;
    }

    /**
     * Quadratic Bezier curve.
     *
     * @param t  time
     * @param p0 start point
     * @param p1 control point
     * @param p2 end point
     * @return point on Bezier curve at some time <code>t</code>
     */
    public static float quad(float t, float p0, float p1, float p2) {
        return (float) (p0 * Math.pow(1 - t, 2) + p1 * 2 * t * (1 - t) + p2 * t * t);
    }

    public static float randomize(float value, Random random) {
        float perc = MathEx.clamp((random.nextInt(100) + 70) / 100, 0.7f, 1.3f);
        return perc * value;
    }

}
