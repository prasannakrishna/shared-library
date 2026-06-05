package com.bhagwat.scm.core.exception.custom;

import com.bhagwat.scm.core.exception.model.DisplayResult;
import com.bhagwat.scm.core.exception.model.Severity;
import org.springframework.http.HttpStatus;

// 429 Too Many Requests
public class TooManyRequestsException extends AbstractApiException {
    public TooManyRequestsException(String message, String code, DisplayResult displayResult) {
        super(message, Severity.WARNING, code, displayResult);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}