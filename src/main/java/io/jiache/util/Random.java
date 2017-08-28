package io.jiache.util;

/**
 * Created by jiacheng on 17-8-23.
 */
public class Random {
    private static java.util.Random random = new java.util.Random();

    private Random(){}

    public static int nextInt(int i) {
        return random.nextInt(i);
    }
}
