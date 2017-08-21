package io.jiache;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * Created by jiacheng on 17-7-26.
 */

public class MainTest {
    @Test
    public void run() {
        List<Integer> list = Arrays.asList(4,3,5,2,1,8,6,3,5,7);
        List<Integer> res = list.parallelStream().sorted().collect(Collectors.toList());
        System.out.println(res);
    }

}

