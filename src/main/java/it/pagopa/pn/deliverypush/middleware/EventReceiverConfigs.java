package it.pagopa.pn.deliverypush.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.eventhandler.EventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.List;

@Configuration
public class EventReceiverConfigs {

    private final PnDeliveryPushConfigs cfg;

    public EventReceiverConfigs(PnDeliveryPushConfigs cfg) {
        this.cfg = cfg;
    }

    @Bean
    public EventReceiver newNotificationEventReceiver(SqsClient sqs, ObjectMapper objMapper, List<EventHandler> handlers) {
        return new SqsEventReceiver( sqs, objMapper, handlers, cfg.getTopics().getNewNotifications() );
    }

    @Bean
    public SqsPecRequestProducer pecRequestSender( SqsClient sqs, ObjectMapper objMapper) {
        return new SqsPecRequestProducer( sqs, cfg.getTopics().getToExternalChannel(), objMapper);
    }
}
