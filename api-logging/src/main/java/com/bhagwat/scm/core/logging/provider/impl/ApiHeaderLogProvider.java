package com.bhagwat.scm.core.logging.provider.impl;

import com.bhagwat.scm.core.logging.provider.DynamicLogProvider;
import com.bhagwat.scm.core.logging.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ApiHeaderLogProvider implements DynamicLogProvider {
    private static final Logger logger = LoggerFactory.getLogger(ApiHeaderLogProvider.class);
    private final String dynamicHeaderLoggingKey;

    public ApiHeaderLogProvider(String dynamicHeaderLoggingKey) {
        this.dynamicHeaderLoggingKey = dynamicHeaderLoggingKey;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Optional<String> getLogLevel(HttpServletRequest request) {
        String header = request.getHeader(dynamicHeaderLoggingKey);
        if(header != null && !LogUtil.isValidType(header)){
            logger.warn("invalid dynamic log header value");
            return Optional.empty();
        }
        return Optional.ofNullable(header);
    }
}
