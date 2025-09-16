package com.tinysteps.reportservice.client;

import com.tinysteps.reportservice.model.SessionOfferingDto;
import com.tinysteps.reportservice.model.SessionServiceResponse;
import com.tinysteps.reportservice.model.SessionTypeDto;
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
 * Client for communicating with session-service
 */
@Slf4j
@Component
public class SessionServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public SessionServiceClient(@Qualifier("secureWebClient") WebClient webClient,
                               CircuitBreaker sessionServiceCircuitBreaker,
                               Retry sessionServiceRetry,
                               TimeLimiter sessionServiceTimeLimiter) {
        this.webClient = webClient;
        this.circuitBreaker = sessionServiceCircuitBreaker;
        this.retry = sessionServiceRetry;
        this.timeLimiter = sessionServiceTimeLimiter;
    }

    @Value("${integration.session-service.session-types-url:http://ts-session-service/api/v1/session-types}")
    private String sessionTypesUrl;

    @Value("${integration.session-service.session-offerings-url:http://ts-session-service/api/v1/session-offerings}")
    private String sessionOfferingsUrl;

    @Value("${integration.session-service.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${service.internal-secret}")
    private String internalApiSecret;

    /**
     * Get session type by ID
     */
    public Optional<SessionTypeDto> getSessionTypeById(String sessionTypeId) {
        return circuitBreaker.executeSupplier(() ->
            retry.executeSupplier(() -> {
                try {
                    String uri = sessionTypesUrl + "/" + sessionTypeId;
                    log.debug("Calling session service for session type: {}", uri);

                    // Log raw response before deserialization with timeout handling
                    String rawResponse = webClient.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Secret", internalApiSecret)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .onErrorReturn(ex -> {
                            if (ex instanceof java.util.concurrent.TimeoutException) {
                                log.warn("Timeout occurred while calling session service for session type: {}", uri);
                                return true;
                            }
                            return false;
                        }, "")
                        .doOnError(ex -> log.error("Error calling session service for session type: {}", ex.getMessage(), ex))
                        .block();
                    log.info("Raw session type response for ID {}: {}", sessionTypeId, rawResponse);

                    if (rawResponse == null || rawResponse.isEmpty()) {
                        log.warn("Empty or null response from session service for session type ID: {}", sessionTypeId);
                        return Optional.empty();
                    }

                    // Deserialize directly into SessionTypeDto since the response is not wrapped
                    SessionTypeDto sessionType = null;
                    try {
                        sessionType = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                            .readValue(rawResponse, SessionTypeDto.class);
                    } catch (Exception e) {
                        log.error("Failed to parse session type response: {}", e.getMessage(), e);
                        return Optional.empty();
                    }

                    if (sessionType != null) {
                        log.debug("Successfully retrieved session type: {}", sessionTypeId);
                        return Optional.of(sessionType);
                    } else {
                        log.warn("Session type response is null for ID: {}", sessionTypeId);
                        return Optional.empty();
                    }

                } catch (WebClientResponseException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("Session type not found with id: {}", sessionTypeId);
                        return Optional.empty();
                    }
                    log.error("HTTP error calling session service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Failed to retrieve session type: HTTP " + e.getStatusCode(), e);
                } catch (Exception e) {
                    log.error("Unexpected error calling session service: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to retrieve session type from session service", e);
                }
            })
        );
    }

    /**
     * Get session offering by ID
     */
    public Optional<SessionOfferingDto> getSessionOfferingById(String sessionOfferingId) {
        return circuitBreaker.executeSupplier(() ->
            retry.executeSupplier(() -> {
                try {
                    String uri = sessionOfferingsUrl + "/" + sessionOfferingId;
                    log.debug("Calling session service for session offering: {}", uri);

                    // Log raw response before deserialization with timeout handling
                    String rawResponse = webClient.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Secret", internalApiSecret)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .onErrorReturn(ex -> {
                            if (ex instanceof java.util.concurrent.TimeoutException) {
                                log.warn("Timeout occurred while calling session service for session offering: {}", uri);
                                return true;
                            }
                            return false;
                        }, "")
                        .doOnError(ex -> log.error("Error calling session service for session offering: {}", ex.getMessage(), ex))
                        .block();
                    log.info("Raw session offering response for ID {}: {}", sessionOfferingId, rawResponse);

                    if (rawResponse == null || rawResponse.isEmpty()) {
                        log.warn("Empty or null response from session service for session offering ID: {}", sessionOfferingId);
                        return Optional.empty();
                    }

                    // Deserialize directly into SessionOfferingDto since the response is not wrapped
                    SessionOfferingDto sessionOffering = null;
                    try {
                        sessionOffering = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                            .readValue(rawResponse, SessionOfferingDto.class);
                    } catch (Exception e) {
                        log.error("Failed to parse session offering response: {}", e.getMessage(), e);
                        return Optional.empty();
                    }

                    if (sessionOffering != null) {
                        log.debug("Successfully retrieved session offering: {}", sessionOfferingId);
                        return Optional.of(sessionOffering);
                    } else {
                        log.warn("Session offering response is null for ID: {}", sessionOfferingId);
                        return Optional.empty();
                    }

                } catch (WebClientResponseException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("Session offering not found with id: {}", sessionOfferingId);
                        return Optional.empty();
                    }
                    log.error("HTTP error calling session service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Failed to retrieve session offering: HTTP " + e.getStatusCode(), e);
                } catch (Exception e) {
                    log.error("Unexpected error calling session service: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to retrieve session offering from session service", e);
                }
            })
        );
    }
}
