package com.bhagwat.scm.core.logging.util;

import java.util.*;

public class LogUtil {
    private static final List<String> validLogTypes = Arrays.asList("INFO", "DEBUG", "WARN", "ERROR", "TRACE", "ALL");
    private static final Map<String, Integer> logLevelPriority = Map.of(
            "TRACE", 1,
            "DEBUG", 2,
            "INFO", 3,
            "WARN", 4,
            "ERROR", 5
    );

    public static Optional<String> getHighestLogLevel(List<Optional<String>> logLevelList) {
        return logLevelList.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(String::toUpperCase)
                .filter(logLevelPriority::containsKey)
                .min(Comparator.comparingInt(logLevelPriority::get));
    }

    public static boolean isValidType(String header) {
        return validLogTypes.stream().anyMatch(logtype -> logtype.equalsIgnoreCase(header));
    }
}
