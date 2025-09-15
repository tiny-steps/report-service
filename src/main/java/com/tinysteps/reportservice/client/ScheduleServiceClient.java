package com.tinysteps.reportservice.client;

import com.tinysteps.reportservice.model.AppointmentDto;
import com.tinysteps.reportservice.model.ScheduleServiceResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Client for communicating with schedule-service
 */
@Slf4j
@Component
public class ScheduleServiceClient {

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public ScheduleServiceClient(@Qualifier("secureWebClient") WebClient webClient,
                                CircuitBreaker scheduleServiceCircuitBreaker,
                                Retry scheduleServiceRetry,
                                TimeLimiter scheduleServiceTimeLimiter) {
        this.webClient = webClient;
        this.circuitBreaker = scheduleServiceCircuitBreaker;
        this.retry = scheduleServiceRetry;
        this.timeLimiter = scheduleServiceTimeLimiter;
    }

    @Value("${integration.schedule-service.base-url:http://ts-schedule-service/api/v1/appointments}")
    private String scheduleServiceUrl;

    @Value("${integration.schedule-service.timeout-seconds:10}")
    private int timeoutSeconds;

    public List<AppointmentDto> getAppointments(String doctorId, String patientId, String branchId, LocalDate startDate, LocalDate endDate) {
        return circuitBreaker.executeSupplier(() ->
            retry.executeSupplier(() -> {
                try {
                    UriComponentsBuilder uriBuilder = UriComponentsBuilder
                            .fromUriString(scheduleServiceUrl);

                    if (doctorId != null) {
                        uriBuilder.queryParam("doctorId", doctorId);
                    }
                    if (patientId != null) {
                        uriBuilder.queryParam("patientId", patientId);
                    }
                    if (branchId != null) {
                        uriBuilder.queryParam("branchId", branchId);
                    }
                    if (startDate != null) {
                        uriBuilder.queryParam("startDate", startDate);
                    }
                    if (endDate != null) {
                        uriBuilder.queryParam("endDate", endDate);
                    }

                    String uri = uriBuilder.toUriString();
                    log.info("Calling schedule service: {}", uri);

                    ScheduleServiceResponse response = webClient.get()
                            .uri(uri)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .onStatus(status -> status.equals(HttpStatus.NOT_FOUND), clientResponse -> {
                                log.warn("No appointments found for the given criteria");
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("No appointments found", ex));
                            })
                            .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                                log.error("Client error calling schedule service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Client error: " + clientResponse.statusCode(), ex));
                            })
                            .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                                log.error("Server error from schedule service: {}", clientResponse.statusCode());
                                return clientResponse.createException().map(ex ->
                                    new RuntimeException("Schedule service unavailable: " + clientResponse.statusCode(), ex));
                            })
                            .bodyToMono(ScheduleServiceResponse.class)
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .block();

                    if (response != null && response.getData() != null && response.getData().getContent() != null) {
                        List<AppointmentDto> appointments = response.getData().getContent().stream()
                                .map(AppointmentDto::fromScheduleAppointment)
                                .toList();

                        log.info("Successfully retrieved {} appointments from schedule service", appointments.size());
                        return appointments;
                    } else {
                        log.warn("No appointments found in response or response structure is unexpected");
                        return Collections.emptyList();
                    }

                } catch (WebClientResponseException e) {
                    log.error("HTTP error calling schedule service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.info("No appointments found, returning empty list");
                        return Collections.emptyList();
                    }
                    throw new RuntimeException("Failed to retrieve appointments: HTTP " + e.getStatusCode(), e);
                } catch (Exception e) {
                    log.error("Unexpected error calling schedule service: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to retrieve appointments from schedule service", e);
                }
            })
        );
    }
}
