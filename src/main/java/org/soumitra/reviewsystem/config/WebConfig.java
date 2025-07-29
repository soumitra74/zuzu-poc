package org.soumitra.reviewsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private ApiKeyAuthenticationInterceptor apiKeyAuthenticationInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyAuthenticationInterceptor)
                .addPathPatterns("/api/jobs/**")
                .excludePathPatterns("/api/jobs/health"); // Health check is public
    }
}