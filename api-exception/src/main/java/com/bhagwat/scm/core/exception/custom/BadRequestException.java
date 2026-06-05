package com.bhagwat.scm.core.exception.custom;

import com.bhagwat.scm.core.exception.model.DisplayResult;
import com.bhagwat.scm.core.exception.model.Severity;
import org.springframework.http.HttpStatus;

public class BadRequestException extends AbstractApiException {
    public BadRequestException(String message, String code, DisplayResult displayResult) {
        super(message, Severity.WARNING, code, displayResult);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}