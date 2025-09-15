package com.tinysteps.reportservice.client;

import com.tinysteps.reportservice.model.AppointmentDto;
import com.tinysteps.reportservice.model.ScheduleServiceResponse;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

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

                    // Always deserialize into wrapper DTO
                    String rawResponse = webClient.get()
                        .uri(uri)
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .block();
                    log.info("Raw schedule service response: {}", rawResponse);

                    ScheduleServiceResponse response = null;
                    if (rawResponse != null && !rawResponse.isEmpty()) {
                        response = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                            .readValue(rawResponse, ScheduleServiceResponse.class);
                    }

                    if (response != null && response.getData() != null && response.getData().getContent() != null) {
                        // Convert ScheduleAppointmentDto to AppointmentDto
                        return response.getData().getContent().stream()
                            .map(AppointmentDto::fromScheduleAppointment)
                            .toList();
                    } else {
                        log.warn("No appointments found or response is null");
                        return Collections.emptyList();
                    }

                } catch (WebClientResponseException e) {
                    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("No appointments found for the given criteria");
                        return Collections.emptyList();
                    }
                    log.error("HTTP error calling schedule service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new RuntimeException("Failed to retrieve appointments: HTTP " + e.getStatusCode(), e);
                } catch (Exception e) {
                    log.error("Unexpected error calling schedule service: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to retrieve appointments from schedule service", e);
                }
            })
        );
    }
}
