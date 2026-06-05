package com.bhagwat.scm.core.exception.custom;

import com.bhagwat.scm.core.exception.model.DisplayResult;
import com.bhagwat.scm.core.exception.model.Severity;
import org.springframework.http.HttpStatus;

// 425 Too Early
public class TooEarlyException extends AbstractApiException {
    public TooEarlyException(String message, String code, DisplayResult displayResult) {
        super(message, Severity.INFO, code, displayResult);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.TOO_EARLY;
    }
}