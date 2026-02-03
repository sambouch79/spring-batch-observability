package com.sambouch.batch.common.config;

import com.sambouch.batch.common.listeners.PerformanceMonitoringListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.partition.support.PartitionStep;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Post-processor that automatically registers the PerformanceMonitoringListener
 * on all Spring Batch Jobs and Steps detected in the context.
 */
public class AutomaticStepMonitoringPostProcessor implements BeanPostProcessor {

    private final PerformanceMonitoringListener listener;
    private static final Logger log = LoggerFactory.getLogger(AutomaticStepMonitoringPostProcessor.class);

    public AutomaticStepMonitoringPostProcessor(PerformanceMonitoringListener listener) {
        this.listener = listener;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        if (!(bean instanceof Step)) {
            return bean;
        }

        try {
            if (bean instanceof TaskletStep taskletStep) {
                taskletStep.registerStepExecutionListener(listener);
                taskletStep.registerChunkListener(listener);
                log.debug(" Monitoring enabled for Step: {}", beanName);
            } else if (bean instanceof PartitionStep partitionStep) {
                partitionStep.registerStepExecutionListener(listener);
                log.debug(" Monitoring enabled for PartitionStep: {}", beanName);
            } else {
                log.debug("⚠️ Unsupported Step type: {} (type: {})",
                        beanName, bean.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.warn("❌ Failed to register listener on {}: {}",
                    beanName, e.getMessage());
        }

        return bean;
    }
}

