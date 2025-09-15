package com.tinysteps.reportservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Resilience4j components used in external service integrations
 */
@Configuration
public class ResilienceConfig {

    // Schedule Service Resilience Components
    @Bean
    public CircuitBreaker scheduleServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-schedule-service");
    }

    @Bean
    public Retry scheduleServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-schedule-service");
    }

    @Bean
    public TimeLimiter scheduleServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-schedule-service");
    }

    // Doctor Service Resilience Components (for future use)
    @Bean
    public CircuitBreaker doctorServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-doctor-service");
    }

    @Bean
    public Retry doctorServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-doctor-service");
    }

    @Bean
    public TimeLimiter doctorServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-doctor-service");
    }

    // Patient Service Resilience Components
    @Bean
    public CircuitBreaker patientServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-patient-service");
    }

    @Bean
    public Retry patientServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-patient-service");
    }

    @Bean
    public TimeLimiter patientServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-patient-service");
    }
    
    // User Service Resilience Components
    @Bean
    public CircuitBreaker userServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("ts-user-service");
    }

    @Bean
    public Retry userServiceRetry(RetryRegistry registry) {
        return registry.retry("ts-user-service");
    }

    @Bean
    public TimeLimiter userServiceTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("ts-user-service");
    }
}