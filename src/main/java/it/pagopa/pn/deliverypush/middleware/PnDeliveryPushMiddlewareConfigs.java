package it.pagopa.pn.deliverypush.middleware;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.middleware.eventhandlers.ExtChannelResponseEventHandler;
import it.pagopa.pn.deliverypush.middleware.eventhandlers.NewNotificationEventHandler;
import it.pagopa.pn.deliverypush.middleware.momproducer.action.sqs.SqsActionProducer;
import it.pagopa.pn.deliverypush.middleware.momproducer.emailrequest.sqs.SqsEmailRequestProducer;
import it.pagopa.pn.deliverypush.middleware.momproducer.pecrequest.sqs.SqsPecRequestProducer;
import it.pagopa.pn.deliverypush.temp.mom.consumer.EventReceiver;
import it.pagopa.pn.deliverypush.temp.mom.consumer.SqsEventReceiver;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class PnDeliveryPushMiddlewareConfigs {

    private final PnDeliveryPushConfigs cfg;

    public PnDeliveryPushMiddlewareConfigs(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @Bean
    public EventReceiver newNotificationEventReceiver(SqsClient sqs, ObjectMapper objMapper, NewNotificationEventHandler handler) {
        return new SqsEventReceiver(
                sqs,
                cfg.getTopics().getNewNotifications(),
                objMapper,
                Collections.singletonList( handler),
                Collections.singletonList( EventType.NEW_NOTIFICATION )
            );
    }


    @Bean
    public EventReceiver externalChannelEventReceiver(SqsClient sqs, ObjectMapper objMapper, ExtChannelResponseEventHandler handler) {
        return new SqsEventReceiver(
                sqs,
                cfg.getTopics().getFromExternalChannel(),
                objMapper,
                Collections.singletonList( handler ),
                Collections.singletonList( EventType.SEND_PEC_RESPONSE )
            );
    }

    @Bean
    public SqsPecRequestProducer pecRequestSender(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsPecRequestProducer( sqs, cfg.getTopics().getToExternalChannelPec(), objMapper);
    }
    
    @Bean
    public SqsEmailRequestProducer emailRequestSender(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsEmailRequestProducer( sqs, cfg.getTopics().getToExternalChannelEmail(), objMapper);
    }

    @Bean
    public SqsActionProducer actionsEventProducer(SqsClient sqs, ObjectMapper objMapper) {
        return new SqsActionProducer( sqs, cfg.getTopics().getScheduledActions(), objMapper);
    }
}
