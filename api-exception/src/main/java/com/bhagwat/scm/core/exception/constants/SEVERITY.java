package com.bhagwat.scm.core.exception.constants;

public enum SEVERITY {
    ERROR("ERROR"),
    WARNING("WARNING"),
    INFO("INFO"),
    SUCCESS("SUCCESS"),
    FATAL("FATAL");
    private final String value;

    SEVERITY(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SEVERITY{" +
                "value='" + value + '\'' +
                '}';
    }
}
