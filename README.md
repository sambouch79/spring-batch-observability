# 📊 Spring Batch Observability

> Production-ready observability solution for Spring Batch with Prometheus and Grafana

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2%2B-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-blue.svg)](https://spring.io/projects/spring-batch)
[![Maven Central](https://img.shields.io/maven-central/v/com.sambouch.batch/spring-batch-observability.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.sambouch.batch/spring-batch-observability)
## ✨ Features

- ✅ **Zero code configuration** - no listener to register manually
- ✅ **Automatic metrics collection** for all Spring Batch jobs and steps
- ✅ **Comprehensive batch metrics** - Items read/written/skipped, errors, retries, throughput
- ✅ **Performance monitoring** - Job, step, and chunk execution duration with percentiles
- ✅ **Throughput measurement** - Automatic calculation in items/second
- ✅ **Prometheus integration** - Works seamlessly with Micrometer and Pushgateway
- ✅ **Ready-to-use Grafana dashboard** - with professional visualizations
- ✅ **JVM metrics included** (memory, CPU, GC)
- ✅ **Designed for production workloads** - Built using Spring Batch & Micrometer best practices

## 📊 Metrics Collected

### Job Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `batch_job_duration_seconds` | Timer | Job execution duration with percentiles |
| `batch_job_executions_total` | Counter | Total job executions by status |
| `batch_job_items_written_total` | Counter | Total items written across all steps |

### Step Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `batch_step_items_read_total` | Counter | Items read from source |
| `batch_step_items_written_total` | Counter | Items successfully written |
| `batch_step_items_skipped_total` | Counter | Items skipped (read + process + write) |
| `batch_step_items_filtered_total` | Counter | Items filtered by processor |
| `batch_step_retries_total` | Counter | Number of rollback attempts |
| `batch_step_failures_total` | Counter | Critical step failures |
| `batch_step_throughput` | DistributionSummary | Processing throughput (items/sec) |
| `batch_step_duration_seconds` | Timer | Step duration with percentiles (p50, p95, p99) |

### Chunk Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `batch_chunk_duration_seconds` | Timer | Chunk processing duration |

### JVM Metrics

Spring Boot Actuator automatically provides:
- `jvm_memory_used_bytes` - JVM memory usage
- `process_cpu_usage` - CPU usage
- `jvm_gc_*` - Garbage collection metrics
- And many more...

## 📋 Prerequisites

- **Java 17+**
- **Spring Boot 3.2+**
- **Spring Batch 5.x**
- **Prometheus Pushgateway** (running instance)

## 📦 Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.sambouch.batch</groupId>
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
```

### Gradle

```gradle
implementation 'com.sambouch.batch:spring-batch-observability:1.0.0'

// Required dependencies (if not already present)
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

## 🚀 Quick Start

### 1. Configure Prometheus Pushgateway

Add the following to your `application.properties`:

```properties
# Enable monitoring (optional - enabled by default)
monitoring.enabled=true
monitoring.application-name=my-batch-app

# Prometheus Pushgateway (Micrometer native)
management.prometheus.metrics.export.pushgateway.enabled=true
management.prometheus.metrics.export.pushgateway.base-url=http://localhost:9091
management.prometheus.metrics.export.pushgateway.job=my-batch-job
management.prometheus.metrics.export.pushgateway.shutdown-operation=PUT
```

Or in `application.yml`:

```yaml
monitoring:
  enabled: true
  application-name: my-batch-app

management:
  prometheus:
    metrics:
      export:
        pushgateway:
          enabled: true
          base-url: http://localhost:9091
          job: my-batch-job
          shutdown-operation: PUT
```

### 2. Create Your Batch Job

**That's it!** No code changes needed. Just create your batch jobs normally:

```java
@Configuration
public class BatchConfiguration {

    @Bean
    public Job myJob(JobRepository jobRepository, Step myStep) {
        return new JobBuilder("myJob", jobRepository)
            .start(myStep)
            .build();
        // ✅ Monitoring is automatically active!
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
        // ✅ Monitoring is automatically active!
    }
}
```

### 3. Run Your Batch

```bash
mvn spring-boot:run
```

**Metrics are now being pushed to Prometheus Pushgateway automatically!**

## 📈 Grafana Dashboard

A production-ready Grafana dashboard is included in the `dashboards/` directory.

### Features

- ⏱️ Job and Step duration gauges with color-coded status
- 🚀 Real-time throughput metrics (items/sec)
- 📊 Items processed breakdown (read, written, filtered, skipped)
- ⚠️ Error tracking (skips, rollbacks, failures)
- 📉 Duration percentiles (p50, p95, p99)
- 💾 JVM metrics monitoring (memory, CPU)
- 📋 Detailed table view per step
### Preview

![Dashboard Overview](/dashboard/screenshots/grafanaDashboard1.png)
*Complete monitoring dashboard with job duration, throughput, and items processed*

![System Metrics](/dashboard/screenshots/grafanaDashboard2.png)
*JVM memory and CPU monitoring*

### Import the Dashboard

1. Open Grafana
2. Go to **Dashboards** → **Import**
3. Upload `dashboards/spring-batch-dashboard-v1.json`
4. Select your Prometheus datasource
5. Click **Import**

## ⚙️ Configuration

### Available Properties

| Property | Default | Description |
|----------|---------|-------------|
| `monitoring.enabled` | `true` | Enable/disable batch monitoring |
| `monitoring.application-name` | `batch-application` | Application identifier |
| `management.prometheus.metrics.export.pushgateway.enabled` | `true` | Enable Pushgateway push |
| `management.prometheus.metrics.export.pushgateway.base-url` | `http://localhost:9091` | Pushgateway URL |
| `management.prometheus.metrics.export.pushgateway.job` | Job name | Prometheus job label |
| `management.prometheus.metrics.export.pushgateway.shutdown-operation` | `PUT` | Operation at shutdown (PUT/POST/DELETE/NONE) |

### Disable Monitoring

To disable monitoring entirely:

```properties
monitoring.enabled=false
```

Or disable just the Pushgateway push:

```properties
management.prometheus.metrics.export.pushgateway.enabled=false
```

## 🏗️ Architecture

```
┌─────────────────────────────────────┐
│  Spring Batch Application           │
│  (Your Jobs & Steps)                │
└────────────┬────────────────────────┘
             │
             │ Auto-registered
             │
             ▼
┌─────────────────────────────────────┐
│  PerformanceMonitoringListener      │
│  (Collects Batch-specific metrics) │
└────────────┬────────────────────────┘
             │
             │ Records metrics
             │
             ▼
┌─────────────────────────────────────┐
│  Micrometer MeterRegistry           │
│  (Stores all metrics)               │
└────────────┬────────────────────────┘
             │
             │ Automatic push
             │
             ▼
┌─────────────────┐    ┌─────────────┐    ┌─────────────┐
│  Pushgateway    │───▶│ Prometheus  │───▶│   Grafana   │
└─────────────────┘    └─────────────┘    └─────────────┘
```

## 🎯 How It Works

### 1. Automatic Listener Registration

The library uses a `BeanPostProcessor` to automatically register the `PerformanceMonitoringListener` on:
- All `JobExecutionListener` beans (auto-registered by Spring Batch 5.x)
- All `Step` beans (via `AutomaticStepMonitoringPostProcessor`)
- All chunk-based steps

**You don't need to register the listener manually!**

### 2. Metrics Collection

The `PerformanceMonitoringListener` implements:
- `JobExecutionListener` - Tracks job-level metrics
- `StepExecutionListener` - Tracks step-level metrics
- `ChunkListener` - Tracks chunk-level metrics

### 3. Metrics Export

Micrometer's native Pushgateway integration handles:
- Periodic push during job execution
- Final push at application shutdown
- Automatic retry on failures

## 🎓 Use Cases

### Production Monitoring

Track the health and performance of your production batch jobs:
- Detect slow steps and bottlenecks
- Identify error patterns and trends
- Monitor resource usage (CPU, memory)
- Set up alerts on failures or degraded performance

### Performance Optimization

Use metrics to optimize your batch configuration:
- Analyze throughput to find optimal chunk sizes
- Compare before/after performance improvements
- Identify the most time-consuming steps
- A/B test different configurations

### Error Analysis

Track and analyze batch errors:
- Distinguish between skips, rollbacks, and failures
- Identify problematic data patterns
- Monitor error rates over time
- Alert on error thresholds

## 🔧 Advanced Configuration

### Custom Grouping Keys

The library uses standard Micrometer grouping with tags:
- `job.name` - Job name
- `step.name` - Step name
- `status` - Execution status (COMPLETED, FAILED, etc.)

### Environment-Specific Configuration

```yaml
# Development
spring:
  config:
    activate:
      on-profile: dev
      
monitoring:
  enabled: true
  
management:
  prometheus:
    metrics:
      export:
        pushgateway:
          base-url: http://localhost:9091

---
# Production
spring:
  config:
    activate:
      on-profile: prod
      
monitoring:
  enabled: true
  
management:
  prometheus:
    metrics:
      export:
        pushgateway:
          base-url: http://pushgateway.prod.company.com:9091
```

## 📚 Example Project

Check out the `examples/` directory for a complete working example including:
- Sample Spring Batch job
- Docker Compose setup (Pushgateway, Prometheus, Grafana)
- Pre-configured Grafana dashboard
- Sample data generation

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

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
