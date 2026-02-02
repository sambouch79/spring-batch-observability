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
 * Auto-configuration pour le monitoring automatique des batchs Spring Batch.
 *
 * <p>Active automatiquement le monitoring des Jobs et Steps avec export vers
 * Prometheus Pushgateway.</p>
 *
 * <p>Peut être désactivé via la property :
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
     * Crée le listener de monitoring de performance.
     *
     * <p>Spring Batch 5.x enregistre automatiquement les beans {@link org.springframework.batch.core.JobExecutionListener}
     * sur tous les Jobs du contexte.</p>
     *
     * @param meterRegistry le registry Micrometer pour l'enregistrement des métriques
     * @return le listener configuré
     */
    @Bean
    @ConditionalOnMissingBean
    public PerformanceMonitoringListener performanceMonitoringListener(MeterRegistry meterRegistry) {
        log.info("Spring Batch Observability activé");
        return new PerformanceMonitoringListener(meterRegistry);
    }

    /**
     * Crée le BeanPostProcessor qui enregistre automatiquement le listener
     * sur tous les Steps créés par Spring.
     *
     * @param listener le listener à enregistrer sur les Steps
     * @return le post-processor configuré
     */
    @Bean
    public AutomaticStepMonitoringPostProcessor automaticStepMonitoringPostProcessor(PerformanceMonitoringListener listener) {
        log.info("Auto-enregistrement des listeners sur Jobs et Steps");
        return new AutomaticStepMonitoringPostProcessor(listener);
    }

}
