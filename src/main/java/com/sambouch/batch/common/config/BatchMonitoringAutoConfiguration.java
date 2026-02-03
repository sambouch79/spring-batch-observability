package com.sambouch.batch.common.config;

import com.sambouch.batch.common.listeners.PerformanceMonitoringListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.Job;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Auto-configuration for automatic Spring Batch monitoring.
 *
 * <p>Automatically enables monitoring of Jobs and Steps with export to
 * Prometheus Pushgateway.</p>
 *
 * <p>Can be disabled via the property:
 * {@code monitoring.enabled=false}</p>
 */
@Configuration
@AutoConfiguration
@ConditionalOnClass(Job.class)
@ConditionalOnProperty(
        prefix = "monitoring",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(MonitoringProperties.class)
public class BatchMonitoringAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BatchMonitoringAutoConfiguration.class);

    /**
     * Creates the performance monitoring listener.
     *
     * <p>Spring Batch 5.x automatically registers {@link org.springframework.batch.core.JobExecutionListener}
     * beans on all Jobs in the context.</p>
     *
     * @param meterRegistry the Micrometer registry for metrics registration
     * @return the configured listener
     */
    @Bean
    @ConditionalOnMissingBean
    public PerformanceMonitoringListener performanceMonitoringListener(MeterRegistry meterRegistry) {
        log.info("Spring Batch Observability enabled");
        return new PerformanceMonitoringListener(meterRegistry);
    }

    /**
     * Creates the BeanPostProcessor that automatically registers the listener
     * on all Steps created by Spring.
     *
     * @param listener the listener to register on Steps
     * @return the configured post-processor
     */
    @Bean
    public AutomaticStepMonitoringPostProcessor automaticStepMonitoringPostProcessor(PerformanceMonitoringListener listener) {
        log.info("Auto-registration of listeners on Jobs and Steps");
        return new AutomaticStepMonitoringPostProcessor(listener);
    }

}
