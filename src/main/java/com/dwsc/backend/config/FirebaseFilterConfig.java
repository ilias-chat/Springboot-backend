package com.dwsc.backend.config;

import com.dwsc.backend.auth.FirebaseAuthFilter;
import com.dwsc.backend.auth.FirebaseAuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnProperty(name = "dwsc.security.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseFilterConfig {

    @Bean
    public FilterRegistrationBean<FirebaseAuthFilter> firebaseAuthFilterRegistration(FirebaseAuthService firebaseAuthService) {
        var reg = new FilterRegistrationBean<>(new FirebaseAuthFilter(firebaseAuthService));
        reg.setOrder(Ordered.LOWEST_PRECEDENCE - 50);
        reg.addUrlPatterns("/api/users", "/api/users/*");
        return reg;
    }
}
