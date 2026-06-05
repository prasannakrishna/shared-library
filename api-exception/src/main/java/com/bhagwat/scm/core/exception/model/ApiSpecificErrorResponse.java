package com.bhagwat.scm.core.exception.model;
import com.bhagwat.scm.core.exception.config.ExceptionProperties;
import org.springframework.http.HttpMethod;

public class ApiSpecificErrorResponse {

    private Severity severity;
    private HttpMethod httpMethod;
    private String apiPathPattern;
    private int httpCode;
    private String code;
    private DisplayResult displayResult;

    // This connects back to the properties structure we discussed earlier
    private ExceptionProperties.HttpErrorConfig errorConfig;

    // Default Constructor
    public ApiSpecificErrorResponse() {}

    // Getters and Setters
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public HttpMethod getHttpMethod() { return httpMethod; }
    public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }

    public String getApiPathPattern() { return apiPathPattern; }
    public void setApiPathPattern(String apiPathPattern) { this.apiPathPattern = apiPathPattern; }

    public int getHttpCode() { return httpCode; }
    public void setHttpCode(int httpCode) { this.httpCode = httpCode; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public DisplayResult getDisplayResult() { return displayResult; }
    public void setDisplayResult(DisplayResult displayResult) { this.displayResult = displayResult; }

    public ExceptionProperties.HttpErrorConfig getErrorConfig() { return errorConfig; }
    public void setErrorConfig(ExceptionProperties.HttpErrorConfig errorConfig) { this.errorConfig = errorConfig; }
}