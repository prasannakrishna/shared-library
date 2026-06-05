package com.bhagwat.scm.core.exception.custom;

import com.bhagwat.scm.core.exception.model.DisplayResult;
import com.bhagwat.scm.core.exception.model.Severity;
import org.springframework.http.HttpStatus;

public class AuthenticationException extends AbstractApiException {
    public AuthenticationException(String message, String code, DisplayResult displayResult) {
        super(message, Severity.ERROR, code, displayResult);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}