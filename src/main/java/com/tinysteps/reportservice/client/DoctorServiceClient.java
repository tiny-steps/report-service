package com.tinysteps.reportservice.client;

import com.tinysteps.reportservice.model.DoctorDto;
import com.tinysteps.reportservice.model.DoctorServiceResponse;
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
 * Client for communicating with doctor-service
 */
@Slf4j
@Component
public class DoctorServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public DoctorServiceClient(@Qualifier("secureWebClient") WebClient webClient,
                              CircuitBreaker doctorServiceCircuitBreaker,
                              Retry doctorServiceRetry,
                              TimeLimiter doctorServiceTimeLimiter) {
        this.webClient = webClient;
        this.circuitBreaker = doctorServiceCircuitBreaker;
        this.retry = doctorServiceRetry;
        this.timeLimiter = doctorServiceTimeLimiter;
    }

    @Value("${integration.doctor-service.base-url:http://ts-doctor-service/api/v1/doctors}")
    private String doctorServiceUrl;
    
    @Value("${integration.doctor-service.timeout-seconds:10}")
    private int timeoutSeconds;

    public Optional<DoctorDto> getDoctorById(String doctorId) {
        return circuitBreaker.executeSupplier(() -> 
            retry.executeSupplier(() -> {
                try {
                    String uri = doctorServiceUrl + "/" + doctorId;
                    log.info("Calling doctor service: {}", uri);
                    
                    DoctorServiceResponse response = webClient.get()
                            .uri(uri)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("X-Internal-Secret", "internal-secret-key-2024")
                            .retrieve()
                            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse -> {
                                log.warn("Doctor not found with id: {}", doctorId);
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Doctor not found", ex));
                            })
                            .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                                log.error("Client error calling doctor service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Client error: " + clientResponse.statusCode(), ex));
                            })
                            .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                                log.error("Server error from doctor service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Doctor service unavailable: " + clientResponse.statusCode(), ex));
                            })
                            .bodyToMono(DoctorServiceResponse.class)
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .block();

                    if (response != null && response.getData() != null) {
                        log.info("Successfully retrieved doctor: {}", doctorId);
                        return Optional.of(response.getData());
                    } else {
                        log.warn("Doctor response is null or has no data for ID: {}", doctorId);
                        return Optional.empty();
                    }

                } catch (WebClientResponseException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("Doctor not found with id: {}", doctorId);
                        return Optional.empty();
                    }
                    log.error("HTTP error calling doctor service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Failed to retrieve doctor: HTTP " + e.getStatusCode(), e);
                } catch (Exception e) {
                    log.error("Unexpected error calling doctor service: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to retrieve doctor from doctor service", e);
                }
            })
        );
    }
}