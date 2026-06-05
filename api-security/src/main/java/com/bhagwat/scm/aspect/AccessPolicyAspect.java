package com.bhagwat.scm.aspect;

import com.bhagwat.scm.config.AuthContext;
import com.bhagwat.scm.common.DomainType;
import com.bhagwat.scm.common.RoleType;
import com.bhagwat.scm.common.SubscriptionType;
import com.bhagwat.scm.annotation.AccessPolicy;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class AccessPolicyAspect {

    private final AuthContext authContext;

    public AccessPolicyAspect(AuthContext authContext) {
        this.authContext = authContext;
    }

    @Before("@annotation(policy)")
    public void validateAccess(AccessPolicy policy) {
        // ADMIN bypasses all checks
        if (authContext.isAdmin()) return;

        DomainType domain = authContext.getDomain();
        SubscriptionType subscription = authContext.getSubscription();
        RoleType role = authContext.getRole();

        boolean domainAllowed =
                policy.domains().length == 0 ||
                        Arrays.asList(policy.domains()).contains(domain);

        boolean subscriptionAllowed =
                subscription.isAtLeast(policy.minSubscription());

        boolean roleAllowed =
                policy.roles().length == 0 ||
                        Arrays.asList(policy.roles()).contains(role);

        if (!domainAllowed || !subscriptionAllowed || !roleAllowed) {
            throw new SecurityException("Access denied: insufficient domain, subscription, or role");
        }
    }
}
