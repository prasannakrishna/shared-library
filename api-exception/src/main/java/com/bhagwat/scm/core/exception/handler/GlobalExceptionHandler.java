package com.bhagwat.scm.core.exception.handler;

import com.bhagwat.scm.core.exception.config.ExceptionProperties;
import com.bhagwat.scm.core.exception.custom.AbstractApiException;
import com.bhagwat.scm.core.exception.custom.BadRequestException;
import com.bhagwat.scm.core.exception.custom.InternalServerErrorException;
import com.bhagwat.scm.core.exception.model.ApiErrorResponse;
import com.bhagwat.scm.core.exception.model.ApiErrorResponseWrapper;
import com.bhagwat.scm.core.exception.model.Severity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.swing.text.html.Option;
import java.util.Optional;

@RestControllerAdvice
@ConditionalOnProperty(name = "service.core.global-exception-handler.enabled", matchIfMissing  = true)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ExceptionProperties exceptionProperties;
    private final ApiSpecificErrorHandler apiSpecificErrorHandler;

    public GlobalExceptionHandler(ExceptionProperties exceptionProperties, ApiSpecificErrorHandler apiSpecificErrorHandler) {
        this.exceptionProperties = exceptionProperties;
        this.apiSpecificErrorHandler = apiSpecificErrorHandler;
        log.debug("Configuring GlobalExceptionHandler");
    }

    /**
     * Handles all custom exceptions inheriting from AbstractApiException
     */
    @ExceptionHandler(AbstractApiException.class)
    public ResponseEntity<ApiErrorResponseWrapper> handleBaseException(AbstractApiException ex, HttpServletRequest request) {
        ApiErrorResponseWrapper errorResponse = convertToXApiResponse(ex, request);

        // Log based on severity
        logAtLevel(ex.getSeverity(), "{} occurred. Error: {}", ex.getClass().getSimpleName(), ex.getMessage());

        // Assuming AbstractApiException has a method to resolve HTTP Status,
        // otherwise default to 500 or use a map.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles Validation Errors (400)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponseWrapper> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.error("Constraint violation: {}", ex.getMessage());
        // Wrap standard exception into our custom BadRequestException
        BadRequestException brEx = new BadRequestException(ex.getMessage(), "VAL-400", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(convertToXApiResponse(brEx, request));
    }

    /**
     * Overriding standard Spring MVC validation handling
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        BadRequestException brEx = new BadRequestException(ex.getMessage(), "VAL-401", null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(convertToXApiResponse(brEx, servletRequest));
    }

    /**
     * Final Fallback for unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseWrapper> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception: ", ex);
        InternalServerErrorException serverEx = new InternalServerErrorException("Internal Server Error", "ERR-500", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(convertToXApiResponse(serverEx, request));
    }

    private ApiErrorResponseWrapper convertToXApiResponse(AbstractApiException ex, HttpServletRequest request) {
        // 1. Start with the data inside the Exception
        ApiErrorResponse response = new ApiErrorResponse();
        response.setCode(ex.getCode());
        response.setSeverity(ex.getSeverity());
        response.setDisplayResult(ex.getDisplayResult());

        // 2. Check for API-specific overrides in Configuration
        Optional<ApiErrorResponse> apiSpecificErrorResponse = getApiSpecificErrorResponse(request, ex.getHttpStatus().value());

        if (apiSpecificErrorResponse.isPresent()) {
            log.debug("Applying API-specific override for {}", ex.getClass().getSimpleName());
            applyOverride(response, apiSpecificErrorResponse.get());
        }
        // 3. If no specific override, check for Default HTTP overrides from YAML
        else {
            // Logic to fetch from exceptionProperties.getDefaultHttp() map based on status code
            response.setDisplayResult(exceptionProperties.getDefaultDisplayResult(ex.getHttpStatus().value()));
        }

        return new ApiErrorResponseWrapper(response);
    }

    private Optional<ApiErrorResponse> getApiSpecificErrorResponse(HttpServletRequest request, int httpStatusCode){
        return apiSpecificErrorHandler.getApiErrorResponse(exceptionProperties.getApiLevelErrors(), request, httpStatusCode);
    }

    private void logAtLevel(Severity severity, String msg, Object... args) {
        switch (severity) {
            case INFO -> log.info(msg, args);
            case WARNING -> log.warn(msg, args);
            case FATAL, ERROR -> log.error(msg, args);
            default -> log.debug(msg, args);
        }
    }

    private void applyOverride(ApiErrorResponse target, ApiErrorResponse source) {
        if (source.getCode() != null) target.setCode(source.getCode());
        if (source.getDisplayResult() != null) target.setDisplayResult(source.getDisplayResult());
    }
}