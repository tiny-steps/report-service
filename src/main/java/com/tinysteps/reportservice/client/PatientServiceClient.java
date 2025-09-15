package com.tinysteps.reportservice.client;

import com.tinysteps.reportservice.model.PatientDto;
import com.tinysteps.reportservice.model.PatientServiceResponse;
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
 * Client for communicating with patient-service
 */
@Slf4j
@Component
public class PatientServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public PatientServiceClient(@Qualifier("secureWebClient") WebClient webClient,
                               CircuitBreaker patientServiceCircuitBreaker,
                               Retry patientServiceRetry,
                               TimeLimiter patientServiceTimeLimiter) {
        this.webClient = webClient;
        this.circuitBreaker = patientServiceCircuitBreaker;
        this.retry = patientServiceRetry;
        this.timeLimiter = patientServiceTimeLimiter;
    }

    @Value("${integration.patient-service.base-url:http://ts-patient-service/api/v1/patients}")
    private String patientServiceUrl;
    
    @Value("${integration.patient-service.timeout-seconds:10}")
    private int timeoutSeconds;

    public Optional<PatientDto> getPatientById(String patientId) {
        return circuitBreaker.executeSupplier(() -> 
            retry.executeSupplier(() -> {
                try {
                    String uri = patientServiceUrl + "/" + patientId;
                    log.info("Calling patient service: {}", uri);
                    
                    PatientServiceResponse response = webClient.get()
                            .uri(uri)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse -> {
                                log.warn("Patient not found with id: {}", patientId);
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Patient not found", ex));
                            })
                            .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                                log.error("Client error calling patient service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Client error: " + clientResponse.statusCode(), ex));
                            })
                            .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                                log.error("Server error from patient service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Patient service unavailable: " + clientResponse.statusCode(), ex));
                            })
                            .bodyToMono(PatientServiceResponse.class)
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .block();

                    if (response != null && response.getData() != null) {
                        log.info("Successfully retrieved patient: {}", patientId);
                        return Optional.of(response.getData());
                    } else {
                        log.warn("Patient response is null or has no data for ID: {}", patientId);
                        return Optional.empty();
                    }

                } catch (WebClientResponseException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("Patient not found with id: {}", patientId);
                        return Optional.empty();
                    }
                    log.error("HTTP error calling patient service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Failed to retrieve patient: HTTP " + e.getStatusCode(), e);
                } catch (Exception e) {
                    log.error("Unexpected error calling patient service: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to retrieve patient from patient service", e);
                }
            })
        );
    }
}