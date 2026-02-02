# 📊 Spring Batch Observability

> Complete observability solution for Spring Batch with Prometheus and Grafana

[![Maven Central](https://img.shields.io/badge/Maven%20Central-1.0.0-blue.svg)](https://search.maven.org/artifact/io.github.sambouch/spring-batch-observability)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-green.svg)](https://spring.io/projects/spring-batch)

## ✨ Features

- ✅ **Automatic metrics collection** for all Spring Batch jobs and steps
- ✅ **Throughput measurement** in items/second
- ✅ **Detailed error tracking** (skips, rollbacks, failures)
- ✅ **Push to Prometheus Pushgateway** for short-lived batch jobs
- ✅ **Ready-to-use Grafana dashboard** with professional visualizations
- ✅ **Zero configuration required** - just add the annotation
- ✅ **JVM metrics included** (memory, CPU, GC)
- ✅ **Production-ready** and battle-tested

## 📊 Metrics Collected

### Job Metrics
- `batch_job_seconds_max` - Job execution duration
- `batch_job_executions_total` - Total job executions by status
- `batch_job_items_written_total` - Total items written across all steps

### Step Metrics
- `batch_step_items_read_total` - Items read
- `batch_step_items_written_total` - Items written
- `batch_step_items_skipped_total` - Items skipped (read + process + write)
- `batch_step_items_filtered_total` - Items filtered by processor
- `batch_step_retries_total` - Rollback attempts (retries)
- `batch_step_failures_total` - Critical step failures
- `batch_step_throughput` - Processing throughput (items/sec)
- `batch_step_duration_seconds` - Step duration with percentiles (p50, p95, p99)

### JVM Metrics (via Spring Boot Actuator)
- `jvm_memory_used_bytes` - JVM memory usage
- `process_cpu_usage` - CPU usage
- `jvm_gc_*` - Garbage collection metrics
- And many more...

## 📋 Prerequisites

- **Java 17+**
- **Spring Boot 3.x**
- **Spring Batch 5.x**
- **Spring Boot Actuator** (for JVM metrics)
- **Micrometer Prometheus Registry**
- **Prometheus Pushgateway** (running instance)

## 📦 Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.sambouch</groupId>
    <artifactId>spring-batch-observability</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Required dependencies (if not already present) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>io.prometheus</groupId>
    <artifactId>simpleclient_pushgateway</artifactId>
    <version>0.16.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.sambouch:spring-batch-observability:1.0.0'

// Required dependencies (if not already present)
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
implementation 'io.prometheus:simpleclient_pushgateway:0.16.0'
```

## 🚀 Quick Start

### 1. Enable Batch Observability

Add the `@EnableBatchMonitoring` annotation to your Spring Boot application:

```java
import com.perso.batch.common.annotation.EnableBatchMonitoring;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchMonitoring  // 👈 That's it!
public class MyBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBatchApplication.class, args);
    }
}
```

### 2. Configure Pushgateway

Add the following configuration to your `application.yml`:

```yaml
monitoring:
  application-name: my-batch-app
  prometheus:
    pushgateway:
      url: http://localhost:9091
      enabled: true
```

Or in `application.properties`:

```properties
monitoring.application-name=my-batch-app
monitoring.prometheus.pushgateway.url=http://localhost:9091
monitoring.prometheus.pushgateway.enabled=true
```

### 3. Create Your Batch Job

```java
@Configuration
public class BatchConfiguration {

    @Bean
    public Job myJob(JobRepository jobRepository, Step myStep) {
        return new JobBuilder("myJob", jobRepository)
            .start(myStep)
            .build();
        // ✅ Monitoring is automatically enabled!
    }

    @Bean
    public Step myStep(JobRepository jobRepository, 
                       PlatformTransactionManager transactionManager) {
        return new StepBuilder("myStep", jobRepository)
            .<String, String>chunk(100, transactionManager)
            .reader(myReader())
            .processor(myProcessor())
            .writer(myWriter())
            .build();
        // ✅ Monitoring is automatically enabled!
    }
}
```

### 4. Run Your Batch

```bash
mvn spring-boot:run
```

**That's it!** Metrics are now being pushed to Prometheus Pushgateway automatically after each job execution.

## 📈 Grafana Dashboard

A production-ready Grafana dashboard is included in the `dashboards/` directory.
Below are some previews of the monitoring interface:

![Dashboard Screenshot 1](./docs/grafanaDashboard1.png)
![Dashboard Screenshot 2](./docs/grafanadashboard2.png)

### Features
- ⏱️ Job and Step duration gauges
- 🚀 Throughput metrics (items/sec)
- 📊 Items processed (read, written, filtered, skipped)
- ⚠️ Error tracking (skips, rollbacks, failures)
- 📉 Duration percentiles (p50, p95, p99)
- 💾 JVM metrics (memory, CPU)
- 📋 Detailed table view per step

### Import the Dashboard

1. Open Grafana
2. Go to **Dashboards** → **Import**
3. Upload `dashboards/spring-batch-dashboard-v1.json`
4. Select your Prometheus datasource
5. Click **Import**

## 🔧 Configuration Options

All configuration options with default values:

```yaml
monitoring:
  application-name: batch-app              # Application identifier
  prometheus:
    pushgateway:
      url: http://localhost:9091           # Pushgateway URL
      enabled: true                        # Enable/disable push
      job: spring-batch                    # Job label in Prometheus
      push-rate: 1s                        # Push interval (not used with Pushgateway)
```

## 🏗️ Architecture

```
┌─────────────────┐
│  Spring Batch   │
│   Application   │
└────────┬────────┘
         │
         │ @EnableBatchMonitoring
         │
         ▼
┌─────────────────────────────┐
│ PerformanceMonitoring       │
│      Listener               │
│  (Auto-registered on all    │
│   Jobs & Steps)             │
└────────┬────────────────────┘
         │
         │ Collect Metrics
         │
         ▼
┌─────────────────┐
│   Micrometer    │
│  MeterRegistry  │
└────────┬────────┘
         │
         │ Push after Job
         │
         ▼
┌─────────────────┐      ┌─────────────┐      ┌─────────────┐
│   Prometheus    │─────▶│ Prometheus  │─────▶│   Grafana   │
│  Pushgateway    │      │   Server    │      │  Dashboard  │
└─────────────────┘      └─────────────┘      └─────────────┘
```

## 🎯 Use Cases

### Use Case 1: Monitor Production Batches

Track the health and performance of your production batch jobs:
- Detect slow steps
- Identify error patterns
- Monitor resource usage
- Set up alerts on failures

### Use Case 2: Performance Optimization

Use throughput and duration metrics to:
- Optimize chunk sizes
- Identify bottlenecks
- Compare before/after performance
- A/B test different configurations

### Use Case 3: Error Analysis

Track and analyze errors:
- Distinguish between skips, rollbacks, and failures
- Identify problematic data patterns
- Monitor error rates over time
- Alert on error thresholds

## 📝 Changelog

See [CHANGELOG.md](CHANGELOG.md) for a list of changes.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Batch team for the excellent framework
- Micrometer team for the metrics abstraction
- Prometheus and Grafana communities

## 📧 Support

- **Issues**: [GitHub Issues](https://github.com/sambouch79/spring-batch-observability/issues)
- **Discussions**: [GitHub Discussions](https://github.com/sambouch79/spring-batch-observability/discussions)

## ⭐ Show Your Support

Give a ⭐️ if this project helped you!

---

**Made with ❤️ for the Spring Batch community**
