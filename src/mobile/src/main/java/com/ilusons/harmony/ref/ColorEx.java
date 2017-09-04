package com.ilusons.harmony.ref;

import android.graphics.Color;
import android.support.annotation.ColorInt;

import java.util.Random;

public class ColorEx {

    /**
     * Convert color into OpenGL color format.
     *
     * @param color some color
     * @return array of floats: [red, green, blue, alpha]
     */
    public static float[] normalize(@ColorInt int color) {
        return new float[]{
                Color.red(color) / 255f,
                Color.green(color) / 255f,
                Color.blue(color) / 255f,
                Color.alpha(color) / 255f
        };
    }

    public static int blend(int c1, int c2, float ratio) {
        if (ratio > 1f) ratio = 1f;
        else if (ratio < 0f) ratio = 0f;
        float ir = 1.0f - ratio;

        int a1 = (c1 >> 24 & 0xff);
        int r1 = ((c1 & 0xff0000) >> 16);
        int g1 = ((c1 & 0xff00) >> 8);
        int b1 = (c1 & 0xff);

        int a2 = (c2 >> 24 & 0xff);
        int r2 = ((c2 & 0xff0000) >> 16);
        int g2 = ((c2 & 0xff00) >> 8);
        int b2 = (c2 & 0xff);

        int a = (int) ((a1 * ir) + (a2 * ratio));
        int r = (int) ((r1 * ir) + (r2 * ratio));
        int g = (int) ((g1 * ir) + (g2 * ratio));
        int b = (int) ((b1 * ir) + (b2 * ratio));

        return (a << 24 | r << 16 | g << 8 | b);
    }

    public static synchronized int randomColor(Random random) {
        return 0xff000000 + 256 * 256 * random.nextInt(256) + 256 * random.nextInt(256) + random.nextInt(256);
    }

}
