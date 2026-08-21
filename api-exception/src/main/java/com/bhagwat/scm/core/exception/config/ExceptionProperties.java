package com.bhagwat.scm.core.exception.config;

import com.bhagwat.scm.core.exception.model.ApiSpecificErrorResponse;
import com.bhagwat.scm.core.exception.model.DisplayResult;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "exception")
public class ExceptionProperties {
    private Map<Integer, HttpErrorConfig> defaultHttp = new HashMap<>();
    private List<ApiSpecificErrorResponse> apiLevelErrors = new ArrayList<>();

    // Getters and Setters
    public Map<Integer, HttpErrorConfig> getDefaultHttp() { return defaultHttp; }
    public void setDefaultHttp(Map<Integer, HttpErrorConfig> defaultHttp) { this.defaultHttp = defaultHttp; }

    public List<ApiSpecificErrorResponse> getApiLevelErrors() { return apiLevelErrors; }
    public void setApiLevelErrors(List<ApiSpecificErrorResponse> apiLevelErrors) { this.apiLevelErrors = apiLevelErrors; }

    public DisplayResult getDefaultDisplayResult(int httpStatus){
        if (defaultHttp == null) return null;
        HttpErrorConfig config = defaultHttp.get(httpStatus);
        return config != null ? new DisplayResult(config.getTemplateId(), config.getTitle(), config.getDetails(), config.getParagraph()) : null;
    }
    /**
     * Inner class for specific HTTP error formatting
     */
    public static class HttpErrorConfig {
        private String errorCode;
        private String templateId;
        private String title;
        private String details;
        private List<String> paragraph;

        // Getters and Setters
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public List<String> getParagraph() { return paragraph; }
        public void setParagraph(List<String> paragraph) { this.paragraph = paragraph; }
    }
}
