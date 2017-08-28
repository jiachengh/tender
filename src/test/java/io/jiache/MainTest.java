package io.jiache;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by jiacheng on 17-7-26.
 */

public class MainTest {
    public static void main(String[] args) {
        Random random =new Random();
        for(int i=0; i<100; ++i) {
            System.out.println(random.nextInt(10));
        }
    }

}

