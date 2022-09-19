package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.deliverypush.action.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class NewNotificationEventHandler {
    private final StartWorkflowHandler startWorkflowHandler;

    public NewNotificationEventHandler(StartWorkflowHandler startWorkflowHandler) {
        this.startWorkflowHandler = startWorkflowHandler;
    }

    @Bean
    public Consumer<Message<PnDeliveryNewNotificationEvent.Payload>> pnDeliveryNewNotificationEventConsumer() {
        return message -> {
            try{
                log.debug("New notification event received, message {}", message);

                PnDeliveryNewNotificationEvent pnDeliveryNewNotificationEvent = PnDeliveryNewNotificationEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();

                String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();

                String messageCount = message.getHeaders().getOrDefault("ApproximateReceiveCount", "1").toString();
                boolean firstDelivery = (StringUtils.hasText(messageCount) && !messageCount.equals("1"));


                startWorkflowHandler.startWorkflow(iun, firstDelivery);

            }catch (Exception ex){
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
