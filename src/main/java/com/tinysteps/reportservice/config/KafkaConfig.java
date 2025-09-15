package com.tinysteps.reportservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.report-events}")
    private String reportEventsTopic;

    @Bean
    public NewTopic reportEventsTopic() {
        return TopicBuilder.name(reportEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}