package com.ilusons.harmony.ref;

import android.graphics.Color;
import android.support.annotation.ColorInt;

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

}
