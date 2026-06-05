package com.bhagwat.scm.core.logging.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Aspect
@Configuration
@ConditionalOnProperty(
        name = "service.core.log.method-entry-exit-enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class MethodExecutionLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(MethodExecutionLoggingAspect.class);

    public MethodExecutionLoggingAspect() {
        log.info("Auto-configuring method entry-exit loggable aspect");
    }

    // Define which packages/classes are intercepted
    @Pointcut("within(com.bhagwat..*)")
    public void loggableComponentPointcut() {
        // pointcut definition only
    }

    // Wrap around the method execution
    @Around("loggableComponentPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        if (args.length > 0 && args[0] instanceof RuntimeException) {
            RuntimeException exception = (RuntimeException) args[0];
            String exceptionType = exception.getClass().getSimpleName();
            log.error("Handling exception of type {}", exceptionType);
        }

        long startTime = System.currentTimeMillis();
        try {
            log.info("Entering {}; METHOD: {}", className, methodName);
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            log.info("Exiting {}; METHOD: {}; TIME_TAKEN: {}ms",
                    className, methodName, (endTime - startTime));
            return result;
        } catch (Exception e) {
            log.error("EXCEPTION in {}; METHOD: {}; ERROR: {}", className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}