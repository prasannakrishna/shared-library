package com.bhagwat.scm.core.exception.constants;

public enum ApiExceptionConstants {
    DEFAULT_BASE_EXCEPTION_ERROR_CODE("EX01", "System error"),
    DEFAULT_GENERAL_ERROR_CODE("GE01", "Unexpected error occured");

    private final String code;
    private final String defaultMessage;

    ApiExceptionConstants(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
