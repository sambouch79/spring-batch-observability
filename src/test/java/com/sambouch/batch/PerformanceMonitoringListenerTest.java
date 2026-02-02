package com.sambouch.batch;


import com.sambouch.batch.common.listeners.PerformanceMonitoringListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceMonitoringListenerTest {

    private MeterRegistry registry;
    private PerformanceMonitoringListener listener;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        listener = new PerformanceMonitoringListener(registry);
    }

    @Test
    void shouldIncrementJobExecutionCounterOnAfterJob() {
        // GIVEN
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "myJob"), null);
        jobExecution.setStatus(BatchStatus.COMPLETED);
        // On simule un timer démarré dans beforeJob
        listener.beforeJob(jobExecution);

        // WHEN
        listener.afterJob(jobExecution);

        // THEN
        double count = registry.get("batch.job.executions")
                .tag("job.name", "myJob")
                .tag("status", "COMPLETED")
                .counter().count();

        assertThat(count).isEqualTo(1.0);

        // Vérifie aussi que la durée a été enregistrée
        assertThat(registry.find("batch.job.duration").timer()).isNotNull();
    }

    @Test
    void shouldCalculateThroughputCorrectyOnAfterStep() {
        // GIVEN
        JobExecution jobExecution = new JobExecution(new JobInstance(1L, "myJob"), null);
        StepExecution stepExecution = new StepExecution("myStep", jobExecution);
        stepExecution.setWriteCount(100);
        stepExecution.setExitStatus(ExitStatus.COMPLETED);

        listener.beforeStep(stepExecution);
        // On attend artificiellement 10ms pour simuler une durée (ou on mocke le Timer)

        // WHEN
        listener.afterStep(stepExecution);

        // THEN
        var summary = registry.find("batch.step.throughput").summary();
        assertThat(summary).isNotNull();
        // On vérifie que les items lus/écrits sont incrémentés
        assertThat(registry.get("batch.step.items.written").counter().count()).isEqualTo(100.0);
    }
}