package com.ilusons.harmony.ref;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class ArrayEx {

    /**
     * Check if all elements are null
     *
     * @param array some array
     * @return true if all elements are null, false otherwise
     */
    public static <T> boolean allElementsAreNull(T[] array) {
        for (T element : array) {
            if (element != null)
                return false;
        }
        return true;
    }

    /**
     * Get index of object in array.
     *
     * @param array  some array
     * @param object some object
     * @return index of object in array or -1
     */
    public static <T> int indexOf(@NonNull T[] array, @Nullable T object) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == object)
                return i;
        }
        return -1;
    }

    /**
     * Check if all array elements are false
     *
     * @param array some array
     * @return true if all elements are equals to false
     */
    public static boolean allElementsAreFalse(@NonNull boolean[] array) {
        for (boolean wavesWorkingState : array) {
            if (wavesWorkingState)
                return false;
        }
        return true;
    }

    /**
     * Check if all array elements equal to zero
     *
     * @param array some array
     * @return true if all elements equal to zero
     */
    public static boolean allElementsAreZero(byte[] array) {
        for (byte b : array) {
            if (b != 0)
                return false;
        }
        return true;
    }

    public static <T> void move(int sourceIndex, int targetIndex, List<T> list) {
        if (sourceIndex <= targetIndex) {
            Collections.rotate(list.subList(sourceIndex, targetIndex + 1), -1);
        } else {
            Collections.rotate(list.subList(targetIndex, sourceIndex + 1), 1);
        }
    }

}
