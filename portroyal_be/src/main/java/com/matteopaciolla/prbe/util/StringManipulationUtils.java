package com.matteopaciolla.prbe.util;

import java.util.Map;

public class StringManipulationUtils {

    public static String insertVariable(String string, String variable) {
        return string.replace("{}", variable);
    }

    public static String insertVariables(String string, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            string = string.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return string;
    }
}
