package com.bhagwat.scm.core.exception.custom;

import org.springframework.http.HttpStatus;

public interface ApiException {
    String getCode();
    HttpStatus getHttpStatus();
}
