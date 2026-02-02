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
 * Post-processor qui enregistre automatiquement le PerformanceMonitoringListener
 * sur tous les Jobs et Steps Spring Batch détectés dans le contexte.
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
                log.info(" Monitoring activé pour Step: {}", beanName);
            } else if (bean instanceof PartitionStep partitionStep) {
                partitionStep.registerStepExecutionListener(listener);
                log.info(" Monitoring activé pour PartitionStep: {}", beanName);
            } else {
                log.debug(" Type de Step non supporté: {} (type: {})",
                        beanName, bean.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.warn(" Erreur lors de l'enregistrement du listener sur {}: {}",
                    beanName, e.getMessage());
        }

        return bean;
    }
}

