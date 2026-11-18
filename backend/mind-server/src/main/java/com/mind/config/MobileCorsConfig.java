package com.mind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 移动端 CORS 放行：允许 APP 内嵌 WebView / H5 预览跨域调用 /api/mobile/**
 * （APP 打包后走原生请求无跨域问题，此配置主要为浏览器端调试与 H5 部署服务）
 */
@Configuration
public class MobileCorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/mobile/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
