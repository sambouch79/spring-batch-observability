package com.sambouch.batch.common.config;

import com.sambouch.batch.common.listeners.PerformanceMonitoringListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
@ConditionalOnClass(name = "org.springframework.batch.core.Job")
public class BatchMonitoringAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(BatchMonitoringAutoConfiguration.class);

    /**
     * Crée le listener de monitoring de performance.
     * Spring Batch 5.x enregistre automatiquement les beans JobExecutionListener
     * sur tous les Jobs.
     */
    @Bean
    @ConditionalOnMissingBean
    public PerformanceMonitoringListener performanceMonitoringListener(MeterRegistry meterRegistry) {
        log.info("Batch Monitoring activé via @EnableBatchMonitoring");
        return new PerformanceMonitoringListener(meterRegistry);
    }

    /**
     * Crée le BeanPostProcessor qui enregistre automatiquement le listener
     * sur tous les Steps créés par Spring.
     */
    @Bean
    public AutomaticStepMonitoringPostProcessor automaticStepMonitoringPostProcessor(PerformanceMonitoringListener listener) {
        log.info("Auto-enregistrement des Steps activé");
        return new AutomaticStepMonitoringPostProcessor(listener);
    }

}
