package dev.woori.wooriBank.config.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter filter) {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(1); // 먼저 실행
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration(LoggingFilter filter) {
        FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(2); // 그 다음 실행
        return registration;
    }
}
