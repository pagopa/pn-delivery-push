package it.pagopa.pn.deliverypush.middleware;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.queue.producer.webhook.sqs.SqsWebhookProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnDeliveryPushMiddlewareConfigs {

    private final PnDeliveryPushConfigs cfg;

    public PnDeliveryPushMiddlewareConfigs(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @Bean
    public SqsWebhookProducer webhookActionsEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsWebhookProducer( sqs, cfg.getTopics().getScheduledActions(), objMapper);
    }
}

