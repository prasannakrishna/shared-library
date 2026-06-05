package com.bhagwat.scm.core.rest.annotation;

import com.bhagwat.scm.core.rest.config.RestClientConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({
        RestClientConfig.class
})
public @interface EnableRestClient {
}
