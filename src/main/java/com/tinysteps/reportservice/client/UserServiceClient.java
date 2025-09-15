package com.tinysteps.reportservice.client;

import com.tinysteps.reportservice.model.UserDto;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

/**
 * Client for communicating with user-service
 */
@Slf4j
@Component
public class UserServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public UserServiceClient(@Qualifier("secureWebClient") WebClient webClient,
                            CircuitBreaker userServiceCircuitBreaker,
                            Retry userServiceRetry,
                            TimeLimiter userServiceTimeLimiter) {
        this.webClient = webClient;
        this.circuitBreaker = userServiceCircuitBreaker;
        this.retry = userServiceRetry;
        this.timeLimiter = userServiceTimeLimiter;
    }

    @Value("${integration.user-service.base-url:http://ts-user-service/api/v1/users}")
    private String userServiceUrl;
    
    @Value("${integration.user-service.timeout-seconds:10}")
    private int timeoutSeconds;

    public Optional<UserDto> getUserById(String userId) {
        return circuitBreaker.executeSupplier(() -> 
            retry.executeSupplier(() -> {
                try {
                    String uri = userServiceUrl + "/" + userId;
                    log.info("Calling user service: {}", uri);
                    
                    UserDto user = webClient.get()
                            .uri(uri)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse -> {
                                log.warn("User not found with id: {}", userId);
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("User not found", ex));
                            })
                            .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                                log.error("Client error calling user service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Client error: " + clientResponse.statusCode(), ex));
                            })
                            .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                                log.error("Server error from user service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("User service unavailable: " + clientResponse.statusCode(), ex));
                            })
                            .bodyToMono(UserDto.class)
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .block();

                    log.info("Successfully retrieved user: {}", userId);
                    return Optional.ofNullable(user);

                } catch (WebClientResponseException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("User not found with id: {}", userId);
                        return Optional.empty();
                    }
                    log.error("HTTP error calling user service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Failed to retrieve user: HTTP " + e.getStatusCode(), e);
                } catch (Exception e) {
                    log.error("Unexpected error calling user service: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to retrieve user from user service", e);
                }
            })
        );
    }
}