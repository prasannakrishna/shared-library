package com.bhagwat.scm.core.exception.model;

import java.util.List;

public class DisplayResult {
    private String templateID;
    private String title;
    private String details;
    private List<String> paragraph;

    // Standard Getters/Setters and a convenience Constructor
    public DisplayResult(String templateID, String title, String details, List<String> paragraph) {
        this.templateID = templateID;
        this.title = title;
        this.details = details;
        this.paragraph = paragraph;
    }
}
