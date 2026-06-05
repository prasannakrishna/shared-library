package com.bhagwat.scm.core.exception.custom;

import java.io.Serializable;
import org.springframework.http.HttpStatus;

public class BaseException extends RuntimeException implements Serializable {

    private final  HttpStatus httpSatus;
    private final String errorCode;
    private final String exceptionType = getClass().getSimpleName();
    private final String severity;

    public BaseException(String severity, HttpStatus httpSatus, String errorCode) {
        this.severity = severity;
        this.httpSatus = httpSatus;
        this.errorCode = errorCode;
    }


    public String getSeverity() {
        return severity;
    }

    public HttpStatus getHttpSatus() {
        return httpSatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getExceptionType() {
        return exceptionType;
    }
}
