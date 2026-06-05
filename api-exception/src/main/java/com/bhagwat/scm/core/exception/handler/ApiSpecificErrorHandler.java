package com.bhagwat.scm.core.exception.handler;
import com.bhagwat.scm.core.exception.model.ApiErrorResponse;
import com.bhagwat.scm.core.exception.model.ApiSpecificErrorResponse;
import com.bhagwat.scm.core.exception.model.DisplayResult;
import com.bhagwat.scm.core.exception.model.Severity;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class ApiSpecificErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiSpecificErrorHandler.class);
    private final AntPathMatcher pathMatcher;

    public ApiSpecificErrorHandler() {
        this.pathMatcher = new AntPathMatcher();
    }

    /**
     * Searches through the configured API-level errors to find a match based on
     * Request Method, Path Pattern, and HTTP Status Code.
     */
    public Optional<ApiErrorResponse> getApiErrorResponse(
            List<ApiSpecificErrorResponse> apiSpecificErrorResponseList,
            HttpServletRequest request,
            int httpStatusCode) {

        if (apiSpecificErrorResponseList == null || apiSpecificErrorResponseList.isEmpty()) {
            return Optional.empty();
        }

        return apiSpecificErrorResponseList.stream()
                .filter(entry -> matchesPattern(entry, request, httpStatusCode))
                .findFirst()
                .map(this::convertToApiErrorResponse);
    }

    /**
     * Validates if the current request context matches the configuration entry.
     */
    private boolean matchesPattern(ApiSpecificErrorResponse config, HttpServletRequest request, int httpStatusCode) {
        String servletHttpMethod = request.getMethod();
        String servletRequestPath = request.getRequestURI();

        // Extract patterns from config
        String methodPattern = config.getHttpMethod() != null ? config.getHttpMethod().name() : null;
        String pathPattern = config.getApiPathPattern();
        int expectedStatusCode = config.getHttpCode();

        boolean methodMatches = methodPattern == null || methodPattern.equalsIgnoreCase(servletHttpMethod);
        boolean pathMatches = pathPattern == null || pathMatcher.match(pathPattern, servletRequestPath);
        boolean statusMatches = expectedStatusCode == httpStatusCode;

        boolean result = methodMatches && pathMatches && statusMatches;

        if (result) {
            log.debug("Match Found! HTTP {} - Method: {}, Path: {} matches config pattern",
                    httpStatusCode, servletHttpMethod, servletRequestPath);
        }

        return result;
    }

    /**
     * Transforms the Configuration object (ApiSpecificErrorResponse)
     * into the final Response object (ApiErrorResponse).
     */
    private ApiErrorResponse convertToApiErrorResponse(ApiSpecificErrorResponse config) {
        ApiErrorResponse response = new ApiErrorResponse();

        // Use configured severity or default to ERROR
        response.setSeverity(config.getSeverity() != null ? config.getSeverity() : Severity.ERROR);

        // Map the internal ErrorConfig fields to the DisplayResult
        if (config.getErrorConfig() != null) {
            var errorConfig = config.getErrorConfig();
            response.setCode(errorConfig.getErrorCode());
            response.setTimestamp(OffsetDateTime.now());

            DisplayResult display = new DisplayResult(
                    errorConfig.getTemplateId(),
                    errorConfig.getTitle(),
                    errorConfig.getDetails(),
                    errorConfig.getParagraph()
            );
            response.setDisplayResult(display);
        }

        return response;
    }
}