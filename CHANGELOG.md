# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-02-02

### Added
- Initial release
- Automatic metrics collection for Spring Batch jobs and steps
- Support for Prometheus Pushgateway
- Throughput measurement in items/second
- Detailed error tracking (skips, rollbacks, failures)
- Ready-to-use Grafana dashboard
- Auto-registration on all Jobs and Steps via `@EnableBatchMonitoring`
- JVM metrics integration via Spring Boot Actuator
- Comprehensive documentation and examples

### Supported Metrics
- Job execution duration and count
- Step items (read, written, skipped, filtered)
- Error metrics (retries, failures)
- Throughput per step
- Duration percentiles (p50, p95, p99)

[Unreleased]: https://github.com/sambouch79/spring-batch-observability/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/sambouch79/spring-batch-observability/releases/tag/v1.0.0

    