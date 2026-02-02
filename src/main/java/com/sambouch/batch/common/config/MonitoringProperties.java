package com.sambouch.batch.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties de configuration pour le monitoring des batchs Spring Batch.
 *
 * <p>Préfixe : {@code monitoring}</p>
 *
 * <p>Exemple de configuration :
 * <pre>
 * monitoring:
 *   application-name: my-batch-app
 *   prometheus:
 *     pushgateway:
 *       enabled: true
 *       url: http://localhost:9091
 *       job: spring-batch
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {
    /**
     * Active ou désactive le monitoring Spring Batch.
     * Par défaut : true
     */
    private boolean enabled = true;

    /**
     * Nom de l'application
     */
    private String applicationName = "batch-application";

    /**
     * Configuration Prometheus
     */
    private Prometheus prometheus = new Prometheus();

    @Data
    public static class Prometheus {
        private Pushgateway pushgateway = new Pushgateway();
    }

    @Data
    public static class Pushgateway {
        /**
         * URL du Pushgateway
         */
        private String url = "http://localhost:9091";

        /**
         * Nom du job dans Prometheus
         */
        private String job = "spring-batch";
    }
}

