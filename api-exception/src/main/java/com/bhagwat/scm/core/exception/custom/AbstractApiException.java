package com.bhagwat.scm.core.exception.custom;

import com.bhagwat.scm.core.exception.model.DisplayResult;
import com.bhagwat.scm.core.exception.model.Severity;

public abstract class AbstractApiException extends RuntimeException implements ApiException {
    private final Severity severity;
    private final String code;
    private final DisplayResult displayResult;

    public AbstractApiException(String message, Severity severity, String code, DisplayResult displayResult) {
        super(message);
        this.severity = severity;
        this.code = code;
        this.displayResult = displayResult;
    }

    // Getters
    public Severity getSeverity() { return severity; }
    public String getCode() { return code; }
    public DisplayResult getDisplayResult() { return displayResult; }
}