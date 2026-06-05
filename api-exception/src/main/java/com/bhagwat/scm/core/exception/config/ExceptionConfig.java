package com.bhagwat.scm.core.exception.config;

import com.bhagwat.scm.core.exception.handler.ApiSpecificErrorHandler;
import com.bhagwat.scm.core.exception.handler.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties(ExceptionProperties.class)
@PropertySource("classpath:core-exceptions.properties")
@Import({GlobalExceptionHandler.class, ApiSpecificErrorHandler.class, ResponseMessageConfig.class})
public class ExceptionConfig {
}
