package com.sambouch.batch.common.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for pushing metrics to Prometheus Pushgateway.
 * Compatible with simpleclient_pushgateway 0.16.0
 */
@Configuration
@ConditionalOnClass(name = "io.prometheus.client.exporter.PushGateway")
@ConditionalOnProperty(name = "monitoring.prometheus.pushgateway.enabled", havingValue = "true", matchIfMissing = false)
public class PrometheusPushGatewayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PrometheusPushGatewayConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "monitoring.prometheus.pushgateway.url")
    public JobExecutionListener prometheusPushGatewayListener(
            MeterRegistry meterRegistry,
            MonitoringProperties properties) {

        log.info("✅  Pushgateway Configuration (simpleclient 0.16.0): {}",
                properties.getPrometheus().getPushgateway().getUrl());

        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.debug("Job started: {}", jobExecution.getJobInstance().getJobName());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                log.debug("Job completed: {} with status: {}",
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getStatus());
                log.debug("Job completed: {} with status: {}",
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getStatus());

                // Appeler pushMetrics après la fin du job
                pushMetrics(meterRegistry, jobExecution, properties);
            }
        };
    }

    private void pushMetrics(MeterRegistry meterRegistry,
                             JobExecution jobExecution,
                             MonitoringProperties properties) {
        try {
            if (!(meterRegistry instanceof PrometheusMeterRegistry)) {
                log.warn("⚠️  MeterRegistry is not of type Prometheus");
                return;
            }

            PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) meterRegistry;

            // ---------------------------------------
            CollectorRegistry collectorRegistry = prometheusRegistry.getPrometheusRegistry();

            String jobName = jobExecution.getJobInstance().getJobName();
            String url = properties.getPrometheus().getPushgateway().getUrl();
            String applicationName = properties.getApplicationName();

            // create PushGateway
            PushGateway pushGateway = new PushGateway(url);

            // Grouping key by labels additionnels
            Map<String, String> groupingKey = new HashMap<>();
            groupingKey.put("instance", applicationName);
            groupingKey.put("job_execution_id", String.valueOf(jobExecution.getId()));
            groupingKey.put("status", jobExecution.getStatus().toString());

            // Push and grouping key
            pushGateway.push(collectorRegistry, jobName, groupingKey);

            log.debug("✅ Metrics pushed to Pushgateway: {} (job: {})", url, jobName);

        } catch (IOException e) {
            log.error("❌ Error pushing metrics to Pushgateway", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error pushing metrics", e);
        }
    }

}
