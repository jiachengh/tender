package io.jiache.util;

/**
 * Created by jiacheng on 17-8-22.
 */

public final class Assert {
    private Assert(){
    }

    public static <T> void checkNull (T object, String objectName) {
        if(object == null) {
            throw new NullPointerException(objectName + " cannot be null");
        }
    }

    public static <T> void check(boolean expression, String errorMessage) {
        if(!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
