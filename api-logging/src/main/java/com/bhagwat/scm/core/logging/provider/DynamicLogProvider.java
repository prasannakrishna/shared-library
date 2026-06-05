package com.bhagwat.scm.core.logging.provider;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface DynamicLogProvider {
    String name();

    int order();

    Optional<String> getLogLevel(HttpServletRequest request);
}
