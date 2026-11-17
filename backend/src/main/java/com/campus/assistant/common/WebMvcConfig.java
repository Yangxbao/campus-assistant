package com.campus.assistant.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.List;

/**
 * Web 配置：拦截路径、本地文件静态映射（/api/files/** → upload-dir）。
 * 拦截器仅作用于需认证的 /api/mp/** 与 /api/admin/**，其余路径放行。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${campus.upload.dir}")
    private String uploadDir;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addFormatters(org.springframework.format.FormatterRegistry registry) {
        // 兼容前端传 true/false 或 1/0 的布尔条件参数（hasAc、hasWifi 等 Integer 字段）
        registry.addConverter(String.class, Integer.class, source -> {
            if (source == null || source.isBlank()) return null;
            String s = source.trim().toLowerCase();
            if ("true".equals(s)) return 1;
            if ("false".equals(s)) return 0;
            return Integer.valueOf(s);
        });
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/mp/**", "/api/admin/**")
                .excludePathPatterns(List.of(
                        "/api/mp/auth/login",
                        "/api/admin/auth/login",
                        "/error"
                ));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传文件预览：GET /api/files/{ossKey} → upload-dir/{ossKey}
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        registry.addResourceHandler("/api/files/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + File.separator);
    }

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        // 管理后台 dev 服务 5173
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
