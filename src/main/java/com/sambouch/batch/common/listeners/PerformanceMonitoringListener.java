package com.sambouch.batch.common.listeners;

import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.scope.context.ChunkContext;


/**
 * Automatic monitoring listener for Spring Batch.
 * Collects metrics for:
 * - Job: duration, execution count, status
 * - Step: duration, items read/written/skipped, errors
 * - Chunk: duration, errors
 */
public class PerformanceMonitoringListener
        implements StepExecutionListener, JobExecutionListener, ChunkListener {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringListener.class);



    private final MeterRegistry meterRegistry;


    // Timers
    // ThreadLocal pour la thread-safety
    private final ThreadLocal<Timer.Sample> jobSampleHolder = new ThreadLocal<>();
    private final ThreadLocal<Timer.Sample> stepSampleHolder = new ThreadLocal<>();
    private final ThreadLocal<Timer.Sample> chunkSampleHolder = new ThreadLocal<>();


    public PerformanceMonitoringListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    //=============================================================
    // STEP LISTENERS
    // ===========================================================
    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepSampleHolder.set(Timer.start(meterRegistry));
        log.debug("Step started : {}", stepExecution.getStepName());
    }


    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();
        String status = stepExecution.getExitStatus().getExitCode();
        Timer.Sample sample = stepSampleHolder.get();
        stepSampleHolder.remove(); // avoid  ThreadLocal leaks

        // Debug logs
        if (log.isDebugEnabled()) {
            log.debug(" Step {} - Read: {}, Write: {}, Skip: {}, Rollback: {}, Filter: {}",
                    stepName,
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount(),
                    stepExecution.getRollbackCount(),
                    stepExecution.getFilterCount());
        }

        // Duration
        if (sample != null) {
            long durationNanos = sample.stop(Timer.builder("batch.step.duration")
                    .tag("job.name", jobName)
                    .tag("step.name", stepName)
                    .tag("status", status)
                    .publishPercentileHistogram(true)
                    .register(meterRegistry));

            long durationMs = durationNanos / 1_000_000;
            if (durationMs > 0) {
                recordThroughput(stepExecution, durationMs);
            }
        }
        // Items read
        Counter.builder("batch.step.items.read")
                .tag("job.name", jobName)
                .tag("step.name", stepName)
                .description("Total number of items read")
                .register(meterRegistry)
                .increment(stepExecution.getReadCount());

        // Items written
        Counter.builder("batch.step.items.written")
                .tag("job.name", jobName)
                .tag("step.name", stepName)
                .description("Total number of items written")
                .register(meterRegistry)
                .increment(stepExecution.getWriteCount());

        // Items skipped
        int totalSkipped = (int) (stepExecution.getReadSkipCount()
                        + stepExecution.getProcessSkipCount()
                        + stepExecution.getWriteSkipCount());

        Counter.builder("batch.step.items.skipped")
                .tag("job.name", jobName)
                .tag("step.name", stepName)
                .description("Total number of items skipped")
                .register(meterRegistry)
                .increment(totalSkipped);

        // Retrie
        Counter.builder("batch.step.retries")
                .tag("job.name", jobName)
                .tag("step.name", stepName)
                .description("Number of rollbacks (retry attempts)")
                .register(meterRegistry)
                .increment(stepExecution.getRollbackCount());

        // Filtered
        Counter.builder("batch.step.items.filtered")
                .tag("job.name", jobName)
                .tag("step.name", stepName)
                .description("Items filtered by processor")
                .register(meterRegistry)
                .increment(stepExecution.getFilterCount());

        // Failures
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            Counter.builder("batch.step.failures")
                    .tag("job.name", jobName)
                    .tag("step.name", stepName)
                    .description("Critical failures (step failed)")
                    .register(meterRegistry)
                    .increment(stepExecution.getFailureExceptions().size());
        }

        return stepExecution.getExitStatus();
    }

    //=============================================================
    // JOB LISTENERS
    // ===========================================================
    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobSampleHolder.set(Timer.start(meterRegistry));
        log.debug(" Job started : {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Timer.Sample sample = jobSampleHolder.get();
        jobSampleHolder.remove();
        String jobName = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().toString();

        // Job duration
        if (sample != null) {
            sample.stop(Timer.builder("batch.job.duration")
                    .tag("job.name", jobName)
                    .tag("status", status)
                    .description("Duration of batch job execution")
                    .publishPercentileHistogram(true)
                    .register(meterRegistry));
        }


            // Total number of batch job executions
            Counter.builder("batch.job.executions")
                    .tag("job.name", jobName)
                    .tag("status", status)
                    .description("Total number of batch job executions")
                    .register(meterRegistry)
                    .increment();


            // Items written
            long totalWritten = jobExecution.getStepExecutions()
                    .stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum();

            Counter.builder("batch.job.items.written")
                    .tag("job.name", jobName)
                    .description("Total items written by job")
                    .register(meterRegistry)
                    .increment(totalWritten);

            log.debug("📊 Job completed : {} - Status : {}", jobName, status);
        // APPEL CRITIQUE ICI :

        log.info("Job terminé. Synchronisation des métriques finales...");

    }


    // ═══════════════════════════════════════════════════════════
    // CHUNK LISTENERS
    // ═══════════════════════════════════════════════════════════

    @Override
    public void beforeChunk(ChunkContext context) {
        chunkSampleHolder.set(Timer.start(meterRegistry));
    }

    @Override
    public void afterChunk(ChunkContext context) {
        Timer.Sample sample = chunkSampleHolder.get();
        chunkSampleHolder.remove();

        if (sample != null) {
            StepExecution stepExecution = context.getStepContext().getStepExecution();
            String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
            String stepName = stepExecution.getStepName();

            sample.stop(Timer.builder("batch.chunk.duration")
                    .tag("job.name", jobName)
                    .tag("step.name", stepName)
                    .description("Duration of chunk processing")
                    .register(meterRegistry));
        }

    }

    @Override
    public void afterChunkError(ChunkContext context) {
        Timer.Sample sample = chunkSampleHolder.get();
        chunkSampleHolder.remove();

        StepExecution stepExecution = context.getStepContext().getStepExecution();
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();

        if (sample != null) {
            sample.stop(Timer.builder("batch.chunk.duration")
                    .tag("job.name", jobName)
                    .tag("step.name", stepName)
                    .tag("status", "ERROR")
                    .register(meterRegistry));
        }

        // Métrique d'erreur exploitable dans Grafana
        Counter.builder("batch.chunk.errors")
                .tag("job.name", jobName)
                .tag("step.name", stepName)
                .description("Number of chunk errors")
                .register(meterRegistry)
                .increment();

        log.error("Error in chunk for step : {}", stepName);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private void recordThroughput(StepExecution stepExecution, long durationMs) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();

        double durationSeconds = durationMs / 1000.0;
        if (durationSeconds < 0.001) {
            log.warn("Duration too short ({} ms), throughput not calculated", durationMs);
            return;
        }

        double throughput = stepExecution.getWriteCount() / durationSeconds;

        DistributionSummary.builder("batch.step.throughput")
                .description("Items processed per second")
                .tags("job.name", jobName, "step.name", stepName)
                .register(meterRegistry)
                .record(throughput);

        log.debug("Throughput: {} items/sec (duration: {}ms, items: {})",
                String.format("%.2f", throughput), durationMs, stepExecution.getWriteCount());
    }

}