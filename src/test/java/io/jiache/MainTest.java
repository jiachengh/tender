package io.jiache;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Created by jiacheng on 17-7-26.
 */

public class MainTest {
    public static void main(String[] args) {
        int i = 0;
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(
                ()->{
                    for(int j=0; j<100000; ++j) {
                        System.out.println(j);
                    }
                    return 99;
                }
        );
        future.thenAccept((k)->System.out.println("hello" + k));
        for(int k=0; k<100000; ++k)
            System.out.println("hahaha");
        i = future.join();

    }

}

