package com.bhagwat.scm.annotation;

import com.bhagwat.scm.common.DomainType;
import com.bhagwat.scm.common.SubscriptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainAccess {

    DomainType[] allowedDomains();

    SubscriptionType minimumSubscription() default SubscriptionType.TRIAL;
}
