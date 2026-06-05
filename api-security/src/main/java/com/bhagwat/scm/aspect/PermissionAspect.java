package com.bhagwat.scm.aspect;

import com.bhagwat.scm.annotation.PermissionRequired;
import com.bhagwat.scm.config.AuthContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Evaluates @PermissionRequired by checking the user's permissions claim in JWT.
 * Permissions are stored as a JSON object in the JWT: {"inventory.write": true, "order.read": true}
 */
@Aspect
@Component
public class PermissionAspect {

    private final AuthContext authContext;

    public PermissionAspect(AuthContext authContext) {
        this.authContext = authContext;
    }

    @Before("@annotation(perm)")
    public void checkPermission(PermissionRequired perm) {
        // ADMIN bypasses all permission checks
        if (authContext.isAdmin()) return;

        if (!authContext.hasPermission(perm.value())) {
            throw new SecurityException(perm.failMessage());
        }
    }
}
