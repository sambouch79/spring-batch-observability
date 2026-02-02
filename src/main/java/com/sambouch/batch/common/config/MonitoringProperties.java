package com.sambouch.batch.common.config;

import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties pour le monitoring
 */
@Component
@ConfigurationProperties(prefix = "monitoring")
public class MonitoringProperties {

    private String applicationName = "batch-application";
    private MonitoringProperties.Prometheus prometheus = new MonitoringProperties.Prometheus();

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public MonitoringProperties.Prometheus getPrometheus() {
        return prometheus;
    }

    public void setPrometheus(MonitoringProperties.Prometheus prometheus) {
        this.prometheus = prometheus;
    }

    public static class Prometheus {
        private MonitoringProperties.Prometheus.Pushgateway pushgateway = new MonitoringProperties.Prometheus.Pushgateway();

        public MonitoringProperties.Prometheus.Pushgateway getPushgateway() {
            return pushgateway;
        }

        public void setPushgateway(MonitoringProperties.Prometheus.Pushgateway pushgateway) {
            this.pushgateway = pushgateway;
        }

        public static class Pushgateway {
            private boolean enabled = true;
            private String url = "localhost:9091";

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }
    }
}

