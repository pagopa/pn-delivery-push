package it.pagopa.pn.deliverypush.middleware;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.momproducer.action.sqs.SqsActionProducer;
import it.pagopa.pn.deliverypush.middleware.momproducer.emailrequest.sqs.SqsEmailRequestProducer;
import it.pagopa.pn.deliverypush.middleware.momproducer.paperrequest.sqs.SqsPaperRequestProducer;
import it.pagopa.pn.deliverypush.middleware.momproducer.pecrequest.sqs.SqsPecRequestProducer;
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
/*
    @Bean
    public EventReceiver newNotificationEventReceiver(SqsClient sqs, ObjectMapper objMapper, NewNotificationEventHandler handler1, NotificationViewedEventHandler handler2) {
        return new SqsEventReceiver(
                sqs,
                cfg.getTopics().getNewNotifications(),
                objMapper,
                Arrays.asList( handler1, handler2 ),
                Arrays.asList( EventType.NEW_NOTIFICATION, EventType.NOTIFICATION_VIEWED  )
            );
    }

    @Bean
    public EventReceiver externalChannelEventReceiver(SqsClient sqs, ObjectMapper objMapper, ExtChannelResponseEventHandler handler) {
        return new SqsEventReceiver(
                sqs,
                cfg.getTopics().getFromExternalChannel(),
                objMapper,
                Arrays.asList( handler, handler ),
                Arrays.asList( EventType.SEND_PEC_RESPONSE, EventType.SEND_PAPER_RESPONSE )
            );
    }

    @Bean
    public EventReceiver webhookActionDoneEventReceiver(SqsClient sqs, ObjectMapper objMapper, WebhookBufferWriterService handler) {
        return new SqsEventReceiver(
                sqs,
                cfg.getTopics().getExecutedActions(),
                objMapper,
                Collections.singletonList( handler ),
                Collections.singletonList( ActionEventType.ACTION_GENERIC )
        );
    }
*/
    @Bean
    public SqsPecRequestProducer pecRequestSender(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsPecRequestProducer( sqs, cfg.getTopics().getToExternalChannelPec(), objMapper);
    }
    
    @Bean
    public SqsEmailRequestProducer emailRequestSender(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsEmailRequestProducer( sqs, cfg.getTopics().getToExternalChannelEmail(), objMapper);
    }
    
    @Bean
    public SqsPaperRequestProducer paperRequestSender(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsPaperRequestProducer( sqs, cfg.getTopics().getToExternalChannelPaper(), objMapper);
    }

    @Bean @Primary
    public SqsActionProducer actionsEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsActionProducer( sqs, cfg.getTopics().getScheduledActions(), objMapper);
    }

    @Bean @Qualifier("action-done")
    public SqsActionProducer actionsDoneEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsActionProducer( sqs, cfg.getTopics().getExecutedActions(), objMapper);
    }
}

