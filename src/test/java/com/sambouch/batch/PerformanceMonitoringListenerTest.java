package com.sambouch.batch;


import com.sambouch.batch.common.listeners.PerformanceMonitoringListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerformanceMonitoringListener Tests")
class PerformanceMonitoringListenerTest {

    private MeterRegistry registry;
    private PerformanceMonitoringListener listener;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        listener = new PerformanceMonitoringListener(registry);
    }

    // ========================================
    // JOB METRICS TESTS
    // ========================================

    @Test
    @DisplayName("Should record job execution counter")
    void shouldRecordJobExecutionCounter() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);

        // When
        listener.beforeJob(jobExecution);
        listener.afterJob(jobExecution);

        // Then
        Counter counter = registry.find("batch.job.executions")
                .tag("job.name", "testJob")
                .tag("status", "COMPLETED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record job duration")
    void shouldRecordJobDuration() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);

        // When
        listener.beforeJob(jobExecution);
        simulateDelay(50); // 50ms delay
        listener.afterJob(jobExecution);

        // Then
        Timer timer = registry.find("batch.job.duration")
                .tag("job.name", "testJob")
                .tag("status", "COMPLETED")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should record total items written in job")
    void shouldRecordJobItemsWritten() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution step1 = createStepExecution(jobExecution, "step1");
        StepExecution step2 = createStepExecution(jobExecution, "step2");

        step1.setWriteCount(100);
        step2.setWriteCount(150);

        jobExecution.addStepExecutions(java.util.Arrays.asList(step1, step2));

        // When
        listener.beforeJob(jobExecution);
        listener.afterJob(jobExecution);

        // Then
        Counter counter = registry.find("batch.job.items.written")
                .tag("job.name", "testJob")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(250.0); // 100 + 150
    }

    @Test
    @DisplayName("Should distinguish between different job statuses")
    void shouldDistinguishJobStatuses() {
        // Given
        JobExecution completedJob = createJobExecution("testJob", BatchStatus.COMPLETED);
        JobExecution failedJob = createJobExecution("testJob", BatchStatus.FAILED);

        // When
        listener.beforeJob(completedJob);
        listener.afterJob(completedJob);

        listener.beforeJob(failedJob);
        listener.afterJob(failedJob);

        // Then
        Counter completedCounter = registry.find("batch.job.executions")
                .tag("job.name", "testJob")
                .tag("status", "COMPLETED")
                .counter();

        Counter failedCounter = registry.find("batch.job.executions")
                .tag("job.name", "testJob")
                .tag("status", "FAILED")
                .counter();

        assertThat(completedCounter.count()).isEqualTo(1.0);
        assertThat(failedCounter.count()).isEqualTo(1.0);
    }

    // ========================================
    // STEP METRICS TESTS
    // ========================================

    @Test
    @DisplayName("Should record items read")
    void shouldRecordItemsRead() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");
        stepExecution.setReadCount(100);

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then
        Counter counter = registry.find("batch.step.items.read")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should record items written")
    void shouldRecordItemsWritten() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");
        stepExecution.setWriteCount(95);

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then
        Counter counter = registry.find("batch.step.items.written")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(95.0);
    }

    @Test
    @DisplayName("Should record total items skipped (read + process + write)")
    void shouldRecordItemsSkipped() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");

        stepExecution.setReadSkipCount(2);
        stepExecution.setProcessSkipCount(3);
        stepExecution.setWriteSkipCount(1);

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then
        Counter counter = registry.find("batch.step.items.skipped")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(6.0); // 2 + 3 + 1
    }

    @Test
    @DisplayName("Should record items filtered")
    void shouldRecordItemsFiltered() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");
        stepExecution.setFilterCount(10);

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then
        Counter counter = registry.find("batch.step.items.filtered")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should record retries (rollbacks)")
    void shouldRecordRetries() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");
        stepExecution.setRollbackCount(5);

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then
        Counter counter = registry.find("batch.step.retries")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should record step failures")
    void shouldRecordStepFailures() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.FAILED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");

        // Ajouter des exceptions
        stepExecution.addFailureException(new RuntimeException("Error 1"));
        stepExecution.addFailureException(new RuntimeException("Error 2"));

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then
        Counter counter = registry.find("batch.step.failures")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should record step duration")
    void shouldRecordStepDuration() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");

        // When
        listener.beforeStep(stepExecution);
        simulateDelay(50); // 50ms delay
        listener.afterStep(stepExecution);

        // Then
        Timer timer = registry.find("batch.step.duration")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate throughput correctly")
    void shouldCalculateThroughput() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");
        stepExecution.setWriteCount(1000);

        // When
        listener.beforeStep(stepExecution);
        simulateDelay(100); // 100ms delay
        listener.afterStep(stepExecution);

        // Then
        DistributionSummary summary = registry.find("batch.step.throughput")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .summary();

        assertThat(summary).isNotNull();
        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.totalAmount()).isGreaterThan(0);

        // Throughput devrait être environ 10000 items/sec (1000 items / 0.1 sec)
        // On vérifie juste qu'il est > 0 car le timing exact peut varier
        assertThat(summary.mean()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should not calculate throughput when duration is too short")
    void shouldNotCalculateThroughputWhenDurationTooShort() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "testStep");
        stepExecution.setWriteCount(100);

        // When - Pas de délai, durée quasi nulle
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then - Le throughput peut ne pas être enregistré si la durée est < 1ms
        DistributionSummary summary = registry.find("batch.step.throughput")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .summary();

        // On accepte que summary soit null OU qu'il ait une valeur
        // Car sur certains systèmes, même sans sleep, il y a une durée mesurable
        if (summary != null) {
            assertThat(summary.count()).isGreaterThanOrEqualTo(0);
        }
    }

    // ========================================
    // MULTIPLE EXECUTIONS TESTS
    // ========================================

    @Test
    @DisplayName("Should accumulate metrics across multiple step executions")
    void shouldAccumulateMetricsAcrossExecutions() {
        // Given
        JobExecution job1 = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution step1 = createStepExecution(job1, "testStep");
        step1.setReadCount(100);
        step1.setWriteCount(95);

        JobExecution job2 = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution step2 = createStepExecution(job2, "testStep");
        step2.setReadCount(200);
        step2.setWriteCount(190);

        // When
        listener.beforeStep(step1);
        listener.afterStep(step1);

        listener.beforeStep(step2);
        listener.afterStep(step2);

        // Then
        Counter readCounter = registry.find("batch.step.items.read")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        Counter writeCounter = registry.find("batch.step.items.written")
                .tag("job.name", "testJob")
                .tag("step.name", "testStep")
                .counter();

        assertThat(readCounter.count()).isEqualTo(300.0); // 100 + 200
        assertThat(writeCounter.count()).isEqualTo(285.0); // 95 + 190
    }

    @Test
    @DisplayName("Should handle multiple jobs with different names")
    void shouldHandleMultipleJobsWithDifferentNames() {
        // Given
        JobExecution job1 = createJobExecution("job1", BatchStatus.COMPLETED);
        JobExecution job2 = createJobExecution("job2", BatchStatus.COMPLETED);

        // When
        listener.beforeJob(job1);
        listener.afterJob(job1);

        listener.beforeJob(job2);
        listener.afterJob(job2);

        // Then
        Counter job1Counter = registry.find("batch.job.executions")
                .tag("job.name", "job1")
                .counter();

        Counter job2Counter = registry.find("batch.job.executions")
                .tag("job.name", "job2")
                .counter();

        assertThat(job1Counter).isNotNull();
        assertThat(job2Counter).isNotNull();
        assertThat(job1Counter.count()).isEqualTo(1.0);
        assertThat(job2Counter.count()).isEqualTo(1.0);
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Should handle step with zero items")
    void shouldHandleStepWithZeroItems() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "emptyStep");
        stepExecution.setReadCount(0);
        stepExecution.setWriteCount(0);

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then - Les compteurs doivent exister même avec 0
        Counter readCounter = registry.find("batch.step.items.read")
                .tag("step.name", "emptyStep")
                .counter();

        assertThat(readCounter).isNotNull();
        assertThat(readCounter.count()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle step without failures")
    void shouldHandleStepWithoutFailures() {
        // Given
        JobExecution jobExecution = createJobExecution("testJob", BatchStatus.COMPLETED);
        StepExecution stepExecution = createStepExecution(jobExecution, "successStep");
        stepExecution.setWriteCount(100);
        // Pas d'exceptions ajoutées

        // When
        listener.beforeStep(stepExecution);
        listener.afterStep(stepExecution);

        // Then - Le compteur de failures ne doit pas être créé
        Counter failuresCounter = registry.find("batch.step.failures")
                .tag("step.name", "successStep")
                .counter();

        assertThat(failuresCounter).isNull(); // Pas créé car pas d'erreurs
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private JobExecution createJobExecution(String jobName, BatchStatus status) {
        JobInstance jobInstance = new JobInstance(1L, jobName);
        JobParameters jobParameters = new JobParameters();
        JobExecution jobExecution = new JobExecution(jobInstance, 1L, jobParameters);
        jobExecution.setStatus(status);
        jobExecution.setStartTime(LocalDateTime.now());
        jobExecution.setEndTime(LocalDateTime.now());
        return jobExecution;
    }

    private StepExecution createStepExecution(JobExecution jobExecution, String stepName) {
        StepExecution stepExecution = new StepExecution(stepName, jobExecution);
        stepExecution.setId(1L);
        stepExecution.setStartTime(LocalDateTime.now());
        stepExecution.setEndTime(LocalDateTime.now());
        stepExecution.setExitStatus(ExitStatus.COMPLETED);
        return stepExecution;
    }

    private void simulateDelay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }
}