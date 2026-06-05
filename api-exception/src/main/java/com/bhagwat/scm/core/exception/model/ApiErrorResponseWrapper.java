package com.bhagwat.scm.core.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponseWrapper {
    private ApiErrorResponse message;

    public ApiErrorResponseWrapper(ApiErrorResponse message) {
        this.message = message;
    }

    public ApiErrorResponse getMessage() {
        return message;
    }

    public void setMessage(ApiErrorResponse message) {
        this.message = message;
    }
}
