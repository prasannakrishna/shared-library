package com.bhagwat.scm.aspect;

import com.bhagwat.scm.config.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Intercepts @AccessPolicy and @PermissionRequired denials and logs them
 * to userService's audit endpoint. Also logs successful access to sensitive operations.
 */
@Aspect
@Component
@Slf4j
public class AuditInterceptor {

    @Autowired private AuthContext authContext;
    @Autowired private HttpServletRequest request;
    @Autowired(required = false) private RestTemplate restTemplate;

    @Value("${audit.service.url:http://localhost:8087/api/audit}")
    private String auditUrl;

    @Value("${audit.enabled:true}")
    private boolean auditEnabled;

    @Around("@annotation(com.bhagwat.scm.annotation.AccessPolicy) || @annotation(com.bhagwat.scm.annotation.PermissionRequired)")
    public Object auditSecuredAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!auditEnabled) return joinPoint.proceed();

        String resource = request.getRequestURI();
        String action = request.getMethod();
        String ip = request.getRemoteAddr();

        try {
            Object result = joinPoint.proceed();
            // Only log write operations (POST/PUT/PATCH/DELETE) to avoid noise
            if (!"GET".equalsIgnoreCase(action)) {
                sendAuditAsync("ACCESS", resource, action, ip, "SUCCESS", null);
            }
            return result;
        } catch (SecurityException e) {
            sendAuditAsync("PERMISSION_DENIED", resource, action, ip, "DENIED", e.getMessage());
            throw e;
        }
    }

    @Async
    void sendAuditAsync(String eventType, String resource, String action, String ip, String status, String details) {
        if (restTemplate == null) return;
        try {
            String userId = authContext.getUserId();
            String username = authContext.getUsername();
            String orgId = authContext.getOrgId();

            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "userId", userId != null ? userId : "",
                    "username", username != null ? username : "",
                    "orgId", orgId != null ? orgId : "",
                    "resource", resource,
                    "action", action,
                    "ipAddress", ip,
                    "status", status,
                    "details", details != null ? details : ""
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(auditUrl, new HttpEntity<>(event, headers), Void.class);
        } catch (Exception e) {
            log.debug("Audit log send failed (non-fatal): {}", e.getMessage());
        }
    }
}
