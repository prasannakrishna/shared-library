package com.bhagwat.scm.config;

import com.bhagwat.scm.common.DomainType;
import com.bhagwat.scm.common.RoleType;
import com.bhagwat.scm.common.SubscriptionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Reads JWT claims from the incoming request's Authorization header.
 * Token validation is handled upstream by API gateway.
 *
 * JWT claims:
 *   "domainType"       — maps to DomainType
 *   "subscriptionType" — maps to SubscriptionType
 *   "roleType"         — maps to RoleType
 *   "tenantId"         — tenant identifier
 *   "permissions"      — List of granted permissions (e.g. ["inventory.write", "order.read"])
 *   "userId"           — user identifier
 *   "orgId"            — organization identifier
 */
@Slf4j
@Component
public class AuthContext {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private HttpServletRequest request;

    @SuppressWarnings("unchecked")
    private Map<String, Object> getClaims() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("No Bearer token in request Authorization header");
        }
        String token = authHeader.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) throw new IllegalStateException("Invalid JWT format");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            return MAPPER.readValue(payloadBytes, Map.class);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JWT payload: " + e.getMessage(), e);
        }
    }

    public DomainType getDomain() {
        Object val = getClaims().get("domainType");
        if (val == null) throw new IllegalStateException("JWT missing 'domainType' claim");
        return DomainType.valueOf(unwrap(val));
    }

    public SubscriptionType getSubscription() {
        Object val = getClaims().get("subscriptionType");
        if (val == null) throw new IllegalStateException("JWT missing 'subscriptionType' claim");
        return SubscriptionType.valueOf(unwrap(val));
    }

    public RoleType getRole() {
        Object val = getClaims().get("roleType");
        if (val == null) throw new IllegalStateException("JWT missing 'roleType' claim");
        return RoleType.valueOf(unwrap(val));
    }

    public boolean isAdmin() {
        return getRole() == RoleType.ADMIN;
    }

    public boolean hasRole(RoleType role) {
        return getRole() == role;
    }

    public boolean hasRoleAtLeast(RoleType minRole) {
        return getRole().isAtLeast(minRole);
    }

    public String getTenantId() {
        Object val = getClaims().get("tenantId");
        return val != null ? unwrap(val) : null;
    }

    public String getUserId() {
        Object val = getClaims().get("userId");
        return val != null ? unwrap(val) : null;
    }

    public String getOrgId() {
        Object val = getClaims().get("orgId");
        return val != null ? unwrap(val) : null;
    }

    public String getUsername() {
        Map<String, Object> claims = getClaims();
        Object val = claims.get("preferred_username");
        if (val == null) val = claims.get("sub");
        return val != null ? val.toString() : null;
    }

    /**
     * Checks if the user has a specific permission.
     * Permissions can be stored in JWT as:
     *   - List: ["inventory.write", "order.read"]
     *   - Map: {"inventory.write": true}
     */
    @SuppressWarnings("unchecked")
    public boolean hasPermission(String permission) {
        Object perms = getClaims().get("permissions");
        if (perms == null) return false;

        if (perms instanceof List) {
            return ((List<String>) perms).contains(permission);
        }
        if (perms instanceof Map) {
            Object val = ((Map<String, Object>) perms).get(permission);
            return Boolean.TRUE.equals(val);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissions() {
        Object perms = getClaims().get("permissions");
        if (perms instanceof List) return (List<String>) perms;
        if (perms instanceof Map) {
            return ((Map<String, Boolean>) perms).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .toList();
        }
        return Collections.emptyList();
    }

    /** Keycloak stores user attributes as List — unwrap single-element lists */
    private String unwrap(Object val) {
        if (val instanceof List<?> list && !list.isEmpty()) {
            return list.get(0).toString();
        }
        return val.toString();
    }
}
