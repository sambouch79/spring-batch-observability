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
 * Configuration pour le push des métriques vers Prometheus Pushgateway
 * Compatible avec simpleclient_pushgateway 0.9.0
 */
@Configuration
@ConditionalOnClass(name = "io.prometheus.client.exporter.PushGateway")
@ConditionalOnProperty(name = "monitoring.prometheus.pushgateway.enabled", havingValue = "true", matchIfMissing = true)
public class PrometheusPushGatewayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PrometheusPushGatewayConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "monitoring.prometheus.pushgateway.url")
    public JobExecutionListener prometheusPushGatewayListener(
            MeterRegistry meterRegistry,
            MonitoringProperties properties) {

        log.info("✅ Configuration Pushgateway (simpleclient 0.9.0): {}",
                properties.getPrometheus().getPushgateway().getUrl());

        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.debug("Job démarré: {}", jobExecution.getJobInstance().getJobName());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                log.info("Job terminé: {} avec statut: {}",
                        jobExecution.getJobInstance().getJobName(),
                        jobExecution.getStatus());
                /*try {
                    pushMetrics(meterRegistry, jobExecution, properties);
                } catch (Exception e) {
                    System.err.println(">>> ERREUR PUSH: " + e.getMessage());
                }finally {
                    // Fermer proprement le registre (parfois insuffisant)
                    meterRegistry.close();
                }*/
            }
        };
    }

    private void pushMetrics(MeterRegistry meterRegistry,
                             JobExecution jobExecution,
                             MonitoringProperties properties) {
        try {
            if (!(meterRegistry instanceof PrometheusMeterRegistry)) {
                log.warn("⚠️ MeterRegistry n'est pas de type Prometheus");
                return;
            }

            PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) meterRegistry;

            // ---------------------------------------
            CollectorRegistry collectorRegistry = prometheusRegistry.getPrometheusRegistry();

            String jobName = jobExecution.getJobInstance().getJobName();
            String url = properties.getPrometheus().getPushgateway().getUrl();
            String applicationName = properties.getApplicationName();

            // Créer le PushGateway
            PushGateway pushGateway = new PushGateway(url);

            // Grouping key avec labels additionnels
            Map<String, String> groupingKey = new HashMap<>();
            groupingKey.put("instance", applicationName);
            groupingKey.put("job_execution_id", String.valueOf(jobExecution.getId()));
            groupingKey.put("status", jobExecution.getStatus().toString());

            // Push avec grouping key
            pushGateway.push(collectorRegistry, jobName, groupingKey);

            log.info("✅ Métriques poussées vers Pushgateway: {} (job: {})", url, jobName);

        } catch (IOException e) {
            log.error("❌ Erreur lors du push des métriques vers Pushgateway", e);
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors du push des métriques", e);
        }
    }

}
