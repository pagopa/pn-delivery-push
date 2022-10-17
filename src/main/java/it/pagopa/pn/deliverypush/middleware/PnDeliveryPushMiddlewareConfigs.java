package it.pagopa.pn.deliverypush.middleware;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.action.sqs.SqsActionProducer;
import it.pagopa.pn.deliverypush.middleware.queue.producer.webhook.sqs.SqsWebhookProducer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnDeliveryPushMiddlewareConfigs {

    private final PnDeliveryPushConfigs cfg;

    public PnDeliveryPushMiddlewareConfigs(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @Bean @Primary
    public SqsActionProducer actionsEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsActionProducer( sqs, cfg.getTopics().getScheduledActions(), objMapper);
    }

    @Bean @Qualifier("action-done")
    public SqsActionProducer actionsDoneEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsActionProducer( sqs, cfg.getTopics().getExecutedActions(), objMapper);
    }

    @Bean
    public SqsWebhookProducer webhookActionsEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsWebhookProducer( sqs, cfg.getTopics().getScheduledActions(), objMapper);
    }
}

