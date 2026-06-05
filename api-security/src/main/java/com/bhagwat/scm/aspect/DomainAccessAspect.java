package com.bhagwat.scm.aspect;

import com.bhagwat.scm.config.AuthContext;
import com.bhagwat.scm.common.DomainType;
import com.bhagwat.scm.common.SubscriptionType;
import com.bhagwat.scm.annotation.DomainAccess;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;
import java.util.Arrays;

@Aspect
@Component
public class DomainAccessAspect {

    private final AuthContext authContext;

    public DomainAccessAspect(AuthContext authContext) {
        this.authContext = authContext;
    }

    @Before("@annotation(domainAccess)")
    public void checkAccess(DomainAccess domainAccess) throws AccessDeniedException {

        DomainType currentDomain = authContext.getDomain();
        SubscriptionType currentSub = authContext.getSubscription();

        boolean domainAllowed = Arrays.asList(domainAccess.allowedDomains())
                .contains(currentDomain);

        boolean subscriptionAllowed =
                currentSub.isAtLeast(domainAccess.minimumSubscription());

        if (!domainAllowed || !subscriptionAllowed) {
            throw new AccessDeniedException("Not authorized");
        }
    }
}
