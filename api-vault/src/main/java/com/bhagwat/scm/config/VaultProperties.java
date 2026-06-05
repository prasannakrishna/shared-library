package com.bhagwat.scm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vault")
public class VaultProperties {

    private String uri;
    private String token;
    private String transitKeyPath;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTransitKeyPath() {
        return transitKeyPath;
    }

    public void setTransitKeyPath(String transitKeyPath) {
        this.transitKeyPath = transitKeyPath;
    }
}
