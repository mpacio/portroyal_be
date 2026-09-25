package com.matteopaciolla.prbe.aspects;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
//@Aspect
//@Component
public class LoggerAspect {

    @Around("execution(* com.matteopaciolla.prbe.controller..*.*(..))")
    public Object logAroundControllers(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // Log request
        log.debug("Req - Method: {} URI: {} Payload: {}", request.getMethod(), request.getRequestURI(), getRequestPayload(joinPoint));
        // Proceed with the method execution
        Object result = joinPoint.proceed();
        // Log response
        log.debug("Res - Payload: {}", result);
        return result;
    }

    private String getRequestPayload(ProceedingJoinPoint joinPoint) {
        // This is a simple implementation. You might want to enhance this
        // to handle different types of payloads (e.g., multipart/form-data)
        return joinPoint.getArgs() != null
                && joinPoint.getArgs().length > 0
                && joinPoint.getArgs()[0] != null
                ? joinPoint.getArgs()[0].toString() : "No payload";
    }
}
