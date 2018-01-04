package io.jiache.util;

/**
 * Created by jiacheng on 17-8-23.
 */
public class Random {
    private static ThreadLocal<java.util.Random> random = ThreadLocal.withInitial(java.util.Random::new);

    private Random(){}

    public static int nextInt(int i) {
        return random.get().nextInt(i);
    }
}
