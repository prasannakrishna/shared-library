package com.bhagwat.scm.core.logging.filters;

import com.bhagwat.scm.core.logging.provider.DynamicLogProvider;
import com.bhagwat.scm.core.logging.util.LogUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bhagwat.scm.core.logging.config.DynamicLoggingConstants.LOG_KEY;

public class DynamicLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(DynamicLoggingFilter.class);
    private final List<DynamicLogProvider> dynamicLogProviderList;

    public DynamicLoggingFilter(List<DynamicLogProvider> dynamicLogProviderList) {
        this.dynamicLogProviderList = dynamicLogProviderList.stream()
                .sorted(Comparator.comparingInt(DynamicLogProvider::order))
                .collect(Collectors.toList());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        List<Optional<String>> logLevelList = dynamicLogProviderList.stream()
                .map(provider -> {
                    Optional<String> level = provider.getLogLevel(request);
                    logger.debug("dynamic log provider {} returned {}", provider.name(), level);
                    return level;
                }).collect(Collectors.toList());
        Optional<String> logLevel = LogUtil.getHighestLogLevel(logLevelList);
        try {
            logLevel.ifPresent(level -> {
                MDC.put(LOG_KEY, level);
                logger.info("log level dynamically set to {}", level);
            });
        } catch (Exception e) {
            logger.error("Error setting log {}", e.getMessage());
        } finally {
            filterChain.doFilter(request, response);
            logger.debug("removing log key from MDC {}", LOG_KEY);
            MDC.remove(LOG_KEY);
        }
    }
}
