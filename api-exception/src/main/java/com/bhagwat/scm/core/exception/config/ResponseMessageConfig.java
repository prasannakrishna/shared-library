package com.bhagwat.scm.core.exception.config;

import com.bhagwat.scm.core.exception.model.ApiErrorResponse;
import org.modelmapper.ModelMapper;

import java.util.Map;

public class ResponseMessageConfig {
    private final ModelMapper modelMapper;
    private Map<String, ApiErrorResponse> responseMessages;

    public ResponseMessageConfig() {
        this.modelMapper = new ModelMapper();
    }

    // Standard Getter for the whole map
    public Map<String, ApiErrorResponse> getResponseMessages() {
        return responseMessages;
    }

    // Standard Setter for the map (useful for @ConfigurationProperties binding)
    public void setResponseMessages(Map<String, ApiErrorResponse> responseMessages) {
        this.responseMessages = responseMessages;
    }

    /**
     * Retrieves the raw ApiErrorResponse by key.
     */
    public ApiErrorResponse getResponseMessage(String key) {
        if (responseMessages == null) return null;
        return responseMessages.get(key);
    }

    /**
     * Retrieves the response and maps it to a specific Class type.
     * Use this to transform the internal error model into a DTO.
     */
    public <T> T getResponseMessage(String key, Class<T> msgClass) {
        ApiErrorResponse responseMessage = getResponseMessage(key);
        if (responseMessage == null) {
            return null;
        }
        return modelMapper.map(responseMessage, msgClass);
    }
}
