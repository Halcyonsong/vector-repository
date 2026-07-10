package io.github.halcyonsong.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WebLogAspect {

    private static final int MAX_LOG_LENGTH = 1000;

    private final ObjectMapper objectMapper;

    @Pointcut("within(io.github.halcyonsong.chat.controller..*) || " +
              "within(io.github.halcyonsong.knowledge.controller..*)"
    )
    public void controllerPointcut() {
    }

    @Around("controllerPointcut() && !within(io.github.halcyonsong.chat.stream.controller.ChatController)")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();

        log.info("REQUEST uri={} method={} handler={}.{} ip={}",
                request.getRequestURI(),
                request.getMethod(),
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                request.getRemoteAddr());

        logRequestArgs(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();

            logResponseResult(result);
            log.info("RESPONSE uri={} cost={}ms",
                    request.getRequestURI(),
                    System.currentTimeMillis() - startTime);

            return result;
        } catch (Throwable exception) {
            log.warn("REQUEST_FAILED uri={} cost={}ms error={}",
                    request.getRequestURI(),
                    System.currentTimeMillis() - startTime,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private void logRequestArgs(Object[] args) {
        List<Object> logArgs = new ArrayList<>();

        for (Object arg : args) {
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse) {
                continue;
            }

            if (arg instanceof MultipartFile file) {
                logArgs.add("MultipartFile(name=" + file.getOriginalFilename()
                        + ", size=" + file.getSize() + ")");
                continue;
            }

            if (arg instanceof String stringArg) {
                logArgs.add(truncate(stringArg));
                continue;
            }

            logArgs.add(arg);
        }

        try {
            log.info("REQUEST_ARGS {}", truncate(objectMapper.writeValueAsString(logArgs)));
        } catch (Exception exception) {
            log.warn("REQUEST_ARGS [参数无法序列化为JSON]");
        }
    }

    private void logResponseResult(Object result) {
        try {
            String resultJson = result == null ? "null" : objectMapper.writeValueAsString(result);
            log.info("RESPONSE_BODY {}", truncate(resultJson));
        } catch (Exception exception) {
            log.warn("RESPONSE_BODY [结果无法序列化为JSON]");
        }
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= MAX_LOG_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_LOG_LENGTH) + "... [Truncated]";
    }
}