package com.bhagwat.scm.core.rest.api;

import lombok.Data;
import org.springframework.http.HttpMethod;

@Data
public class ApiConfig {
    private String host;
    private String apiPath;
    private HttpMethod httpMethod;
    private boolean overrideDefaultHttpProperties;

    private int responseTimeout;
    private int connectionTimeout;
    private int requestTimeout;
}
