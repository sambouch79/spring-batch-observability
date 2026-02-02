package com.sambouch.batch.common.annotations;


import com.sambouch.batch.common.config.BatchMonitoringAutoConfiguration;
import com.sambouch.batch.common.config.PrometheusPushGatewayConfiguration;
import org.springframework.context.annotation.Import;
import java.lang.annotation.*;


/**
 * Active le monitoring automatique des batchs Spring Batch.
 *
 * <p><strong>Cette annotation est OBLIGATOIRE pour activer le monitoring.</strong>
 *
 * <h3>Exemple d'utilisation :</h3>
 * <pre>
 * {@code
 * @Configuration
 * @EnableBatchMonitoring
 * public class BatchConfig {
 *     @Bean
 *     public Job monJob(JobRepository jobRepository, Step monStep) {
 *         return new JobBuilder("monJob", jobRepository)
 *                 .start(monStep)
 *                 .build();
 *     }
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        BatchMonitoringAutoConfiguration.class,      // Mesure le Batch
        PrometheusPushGatewayConfiguration.class     // Envoie le tout
})
public @interface EnableBatchMonitoring {
}
