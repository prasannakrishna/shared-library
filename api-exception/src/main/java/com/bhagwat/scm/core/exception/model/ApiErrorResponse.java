package com.bhagwat.scm.core.exception.model;

import java.time.OffsetDateTime;
import java.util.List;

public class ApiErrorResponse {
    private Severity severity;
    private String code;
    private DisplayResult displayResult;

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DisplayResult getDisplayResult() {
        return displayResult;
    }

    public void setDisplayResult(DisplayResult displayResult) {
        this.displayResult = displayResult;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    private OffsetDateTime timestamp;
}
