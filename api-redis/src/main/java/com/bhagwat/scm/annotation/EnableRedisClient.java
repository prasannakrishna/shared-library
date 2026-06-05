package com.bhagwat.scm.annotation;

import com.bhagwat.scm.config.RedisClientConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RedisClientConfiguration.class)
public @interface EnableRedisClient {
}
