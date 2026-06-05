package com.bhagwat.scm.core.exception.model;

import lombok.Getter;

@Getter
public enum Severity {
    ERROR("ERROR"),
    WARNING("WARNING"),
    INFO("INFO"),
    SUCCESS("SUCCESS"),
    FATAL("FATAL");
    private final String value;

    Severity(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "SEVERITY{" +
                "value='" + value + '\'' +
                '}';
    }
}
