package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnDeliveryNewNotificationEvent;
import it.pagopa.pn.deliverypush.action.startworkflow.StartWorkflowHandler;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.delivery.PnDeliveryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@CustomLog
public class NewNotificationEventHandler {
    private final StartWorkflowHandler startWorkflowHandler;

    public NewNotificationEventHandler(StartWorkflowHandler startWorkflowHandler) {
        this.startWorkflowHandler = startWorkflowHandler;
    }

    @Bean
    public Consumer<Message<PnDeliveryNewNotificationEvent.Payload>> pnDeliveryNewNotificationEventConsumer() {
        final String processName = "NEW NOTIFICATION";

        return message -> {
            try{
                log.debug("Handle message from {} with content {}", PnDeliveryClient.CLIENT_NAME, message);

                PnDeliveryNewNotificationEvent pnDeliveryNewNotificationEvent = PnDeliveryNewNotificationEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();
                String iun = pnDeliveryNewNotificationEvent.getHeader().getIun();

                HandleEventUtils.addIunToMdc(iun);

                log.logStartingProcess(processName);
                startWorkflowHandler.startWorkflow(iun);
                log.logEndingProcess(processName);
            }catch (Exception ex){
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
