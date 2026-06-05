package com.bhagwat.scm.filter;

import com.bhagwat.scm.config.MultiTenancyProperties;
import com.bhagwat.scm.config.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final MultiTenancyProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled()) {
            log.debug("Multi-tenancy is disabled");
            return true;
        }

        String tenantId = extractTenantIdFromRequest(request);

        if (tenantId == null) {
            handleMissingTenant();
        } else {
            TenantContext.setTenantId(tenantId);
            log.debug("Tenant context set: {}", tenantId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    private String extractTenantIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token in request");
            return null;
        }

        String token = authHeader.substring(7);
        return extractClaimFromJwt(token, properties.getTenantIdClaimName());
    }

    private String extractClaimFromJwt(String token, String claimName) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                log.warn("Invalid JWT format");
                return null;
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);

            Object claim = claims.get(claimName);
            return claim != null ? claim.toString() : null;

        } catch (Exception e) {
            log.warn("Failed to decode JWT payload: {}", e.getMessage());
            return null;
        }
    }

    private void handleMissingTenant() {
        if (properties.getDefaultTenantId() != null) {
            TenantContext.setTenantId(properties.getDefaultTenantId());
            log.debug("Using default tenant: {}", properties.getDefaultTenantId());
        } else if (properties.isFailOnMissingTenant()) {
            throw new RuntimeException("Tenant ID is required but not found in request");
        }
    }
}
