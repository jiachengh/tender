package io.jiache.util;

import java.util.HashMap;
import java.util.Map;

public class ParaParser {
    private ParaParser() {
    }

    /**
     * args pattern --param=value
     */
    public static Map<String, String> parse(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            String[] ss = arg.split("=");
            if (ss.length == 2 && ss[0].matches("^--.+")) {
                map.put(ss[0].substring(2, ss[0].length()), ss[1]);
            }
        }
        return map;
    }
}
