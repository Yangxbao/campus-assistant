package com.campus.assistant.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * 操作日志切面：记录到 t_op_log（actor_type=ADMIN）。
 * 防SQL注入与XSS之外的安全审计手段；任何异常都不阻断主流程。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private final ObjectMapper objectMapper;
    private final OpLogService opLogService;

    @Around("execution(* com.campus.assistant.controller.admin..*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Object result = pjp.proceed();
            saveLog(pjp, "ok");
            return result;
        } catch (Throwable e) {
            saveLog(pjp, "fail:" + e.getMessage());
            throw e;
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, String status) {
        try {
            String module = pjp.getSignature().getDeclaringType().getSimpleName();
            String action = pjp.getSignature().getName();
            String detail = objectMapper.writeValueAsString(Arrays.stream(pjp.getArgs())
                    .map(a -> {
                        if (a instanceof org.springframework.web.multipart.MultipartFile f) {
                            return "file:" + f.getOriginalFilename();
                        }
                        return a == null ? null : String.valueOf(a);
                    })
                    .toList());
            String ip = currentIp();
            Long adminId = UserContext.get() != null ? UserContext.get().getAdminId() : null;
            opLogService.save("ADMIN", adminId, module, action + "[" + status + "]",
                    detail.length() > 2000 ? detail.substring(0, 2000) : detail, ip);
        } catch (Exception e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest().getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    /** 延迟注入避免循环依赖，使用独立接口封装落库 */
    @lombok.RequiredArgsConstructor
    @org.springframework.stereotype.Service
    public static class OpLogService {
        private final com.campus.assistant.mapper.OpLogMapper opLogMapper;

        public void save(String actorType, Long actorId, String module, String action,
                         String detail, String ip) {
            com.campus.assistant.entity.CommunityDomain.OpLog log1 = new com.campus.assistant.entity.CommunityDomain.OpLog();
            log1.setActorType(actorType);
            log1.setActorId(actorId);
            log1.setModule(module);
            log1.setAction(action);
            log1.setDetail(detail);
            log1.setIp(ip);
            opLogMapper.insert(log1);
        }
    }
}
