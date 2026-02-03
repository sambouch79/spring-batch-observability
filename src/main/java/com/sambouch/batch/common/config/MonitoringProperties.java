package com.sambouch.batch.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Batch monitoring.
 *
 * <p>Prefix: {@code monitoring}</p>
 *
 * <p>Configuration example:
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
     * Enables or disables Spring Batch monitoring.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Application name
     */
    private String applicationName = "batch-application";

    /**
     * Prometheus configuration
     */
    private Prometheus prometheus = new Prometheus();

    @Data
    public static class Prometheus {
        private Pushgateway pushgateway = new Pushgateway();
    }

    @Data
    public static class Pushgateway {
        /**
         * Pushgateway  URL
         */
        private String url = "http://localhost:9091";

        /**
         * Job name in Prometheus
         */
        private String job = "spring-batch";
    }
}

