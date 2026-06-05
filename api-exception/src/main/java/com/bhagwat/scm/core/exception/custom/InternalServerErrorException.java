package com.bhagwat.scm.core.exception.custom;

import com.bhagwat.scm.core.exception.model.DisplayResult;
import com.bhagwat.scm.core.exception.model.Severity;
import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends AbstractApiException {
    public InternalServerErrorException(String message, String code, DisplayResult displayResult) {
        super(message, Severity.FATAL, code, displayResult);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
