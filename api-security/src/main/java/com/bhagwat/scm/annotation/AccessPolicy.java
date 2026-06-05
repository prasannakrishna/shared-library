package com.bhagwat.scm.annotation;

import com.bhagwat.scm.common.DomainType;
import com.bhagwat.scm.common.RoleType;
import com.bhagwat.scm.common.SubscriptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessPolicy {

    DomainType[] domains() default {};

    SubscriptionType minSubscription() default SubscriptionType.TRIAL;

    RoleType[] roles() default {};
}

/*@AccessPolicy(
        domains = {DomainType.SELLER, DomainType.WAREHOUSE},
        minSubscription = SubscriptionType.DIAMOND,
        roles = {"ADMIN", "ASSOCIATE"}
)*/