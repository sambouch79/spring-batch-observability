package com.sambouch.batch.common.listeners;

import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener de monitoring automatique pour Spring Batch.
 * Collecte les métriques de :
 * - Job : durée, nombre d'exécutions, statut
 * - Step : durée, items lus/écrits/skippés, erreurs
 * - Chunk : durée, erreurs
 */
public class PerformanceMonitoringListener
        implements StepExecutionListener, JobExecutionListener, ChunkListener {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringListener.class);


    private final MeterRegistry meterRegistry;

    // Timers pour mesurer les durées
    private Timer.Sample jobSample;
    private Timer.Sample stepSample;
    private Timer.Sample chunkSample;
    private final Map<String, Counter> itemsReadCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> itemsWrittenCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> itemsSkippedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> retriesCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> filteredCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> failuresCounters = new ConcurrentHashMap<>();
    private final Map<String, DistributionSummary> throughputSummaries = new ConcurrentHashMap<>();
    private final Map<String, Counter> jobExecutionsCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> jobItemsWrittenCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> jobDurationTimers = new ConcurrentHashMap<>();

    public PerformanceMonitoringListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    //=============================================================
    // STEP LISTENERS
    // ===========================================================
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepSample = Timer.start(meterRegistry);
        log.debug("Step démarré : {}", stepExecution.getStepName());
    }


    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();
        String status = stepExecution.getExitStatus().getExitCode();
        String key = buildKey(jobName, stepName);

        // Logs de debug
        if (log.isDebugEnabled()) {
            log.debug(" Step {} - Read: {}, Write: {}, Skip: {}, Rollback: {}, Filter: {}",
                    stepName,
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount(),
                    stepExecution.getRollbackCount(),
                    stepExecution.getFilterCount());
        }

        // Durée
        long durationNanos = this.stepSample.stop(Timer.builder("batch.step.duration")
                .tag("job.name", jobName)
                .tag("step.name", stepName)
                .tag("status", status)
                .publishPercentileHistogram(true)
                .register(meterRegistry));

        long durationMs = durationNanos / 1_000_000;

        // Throughput
        if (durationMs > 0) {
            recordThroughput(stepExecution, durationMs, key);
        }

        // Items read
        itemsReadCounters.computeIfAbsent(key, k ->
                Counter.builder("batch.step.items.read")
                        .tag("job.name", jobName)
                        .tag("step.name", stepName)
                        .description("Total number of items read")
                        .register(meterRegistry)
        ).increment(stepExecution.getReadCount());

        // Items written
        itemsWrittenCounters.computeIfAbsent(key, k ->
                Counter.builder("batch.step.items.written")
                        .tag("job.name", jobName)
                        .tag("step.name", stepName)
                        .description("Total number of items written")
                        .register(meterRegistry)
        ).increment(stepExecution.getWriteCount());

        // Items skipped
        int totalSkipped = (int) (stepExecution.getReadSkipCount()
                        + stepExecution.getProcessSkipCount()
                        + stepExecution.getWriteSkipCount());

        itemsSkippedCounters.computeIfAbsent(key, k ->
                Counter.builder("batch.step.items.skipped")
                        .tag("job.name", jobName)
                        .tag("step.name", stepName)
                        .description("Total number of items skipped")
                        .register(meterRegistry)
        ).increment(totalSkipped);

        // Retries
        retriesCounters.computeIfAbsent(key, k ->
                Counter.builder("batch.step.retries")
                        .tag("job.name", jobName)
                        .tag("step.name", stepName)
                        .description("Number of rollbacks (retry attempts)")
                        .register(meterRegistry)
        ).increment(stepExecution.getRollbackCount());

        // Filtered
        filteredCounters.computeIfAbsent(key, k ->
                Counter.builder("batch.step.items.filtered")
                        .tag("job.name", jobName)
                        .tag("step.name", stepName)
                        .description("Items filtered by processor")
                        .register(meterRegistry)
        ).increment(stepExecution.getFilterCount());

        // Failures
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            failuresCounters.computeIfAbsent(key, k ->
                    Counter.builder("batch.step.failures")
                            .tag("job.name", jobName)
                            .tag("step.name", stepName)
                            .description("Critical failures (step failed)")
                            .register(meterRegistry)
            ).increment(stepExecution.getFailureExceptions().size());
        }

        return stepExecution.getExitStatus();
    }

    //=============================================================
    // JOB LISTENERS
    // ===========================================================
    @Override
    public void beforeJob(JobExecution jobExecution) {
        this.jobSample = Timer.start(meterRegistry);
        log.debug(" Job démarré : {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
            String jobName = jobExecution.getJobInstance().getJobName();
            String status = jobExecution.getStatus().toString();
            String key = jobName + ":" + status;

            // Durée du job
            Timer jobTimer = jobDurationTimers.computeIfAbsent(key, k ->
                    Timer.builder("batch.job.duration")
                            .tag("job.name", jobName)
                            .tag("status", status)
                            .description("Duration of batch job execution")
                            .publishPercentileHistogram(true)
                            .register(meterRegistry)
            );

            this.jobSample.stop(jobTimer);

            // Nombre d'exécutions (avec cache)
            jobExecutionsCounters.computeIfAbsent(key, k ->
                    Counter.builder("batch.job.executions")
                            .tag("job.name", jobName)
                            .tag("status", status)
                            .description("Total number of batch job executions")
                            .register(meterRegistry)
            ).increment();

            // Items written (avec cache)
            long totalWritten = jobExecution.getStepExecutions()
                    .stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum();

            jobItemsWrittenCounters.computeIfAbsent(jobName, k ->
                    Counter.builder("batch.job.items.written")
                            .tag("job.name", jobName)
                            .description("Total items written by job")
                            .register(meterRegistry)
            ).increment(totalWritten);

            log.debug("📊 Job terminé : {} - Statut : {}", jobName, status);

    }


    // ═══════════════════════════════════════════════════════════
    // CHUNK LISTENERS
    // ═══════════════════════════════════════════════════════════

    @Override
    public void beforeChunk(ChunkContext context) {
        this.chunkSample = Timer.start(meterRegistry);
    }

    @Override
    public void afterChunk(ChunkContext context) {
        if (this.chunkSample != null) {
            StepExecution stepExecution = context.getStepContext().getStepExecution();
            String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
            String stepName = stepExecution.getStepName();

                // Métrique : Durée du chunk
                this.chunkSample.stop(Timer.builder("batch.chunk.duration")
                        .tag("job.name", jobName)
                        .tag("step.name", stepName)
                        .description("Duration of chunk processing")
                        .register(meterRegistry));

                this.chunkSample = null; // Reset pour le prochain chunk
        }


    }

    @Override
    public void afterChunkError(ChunkContext context) {
        StepExecution stepExecution = context.getStepContext().getStepExecution();
        String stepName = stepExecution.getStepName();

        log.error("Erreur dans le chunk du step : {}", stepName);
    }

    private void recordThroughput(StepExecution stepExecution, long durationMs, String key) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();

        // Validation
        double durationSeconds = durationMs / 1000.0;
        if (durationSeconds < 0.001) {
            log.warn(" Durée trop courte ({} ms), throughput non calculé", durationMs);
            return;
        }

        // Calcul
        double throughput = stepExecution.getWriteCount() / durationSeconds;

        // Enregistrement
        throughputSummaries.computeIfAbsent(key, k ->
                DistributionSummary.builder("batch.step.throughput")
                        .description("Items processed per second")
                        .tags("job.name", jobName, "step.name", stepName)
                        .register(meterRegistry)
        ).record(throughput);

        log.info("Throughput: {} items/sec (durée: {}ms, items: {})",
                String.format("%.2f", throughput), durationMs, stepExecution.getWriteCount());
    }

    private String buildKey(String jobName, String stepName) {
        return jobName + ":" + stepName;
    }
}