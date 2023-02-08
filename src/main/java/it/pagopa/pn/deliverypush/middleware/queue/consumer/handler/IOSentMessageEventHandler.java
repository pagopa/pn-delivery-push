package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnExtRegistryIOSentMessageEvent;
import it.pagopa.pn.deliverypush.action.iosentmessage.IOSentMessageHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class IOSentMessageEventHandler {
    private final IOSentMessageHandler ioSentMessageHandler;

    public IOSentMessageEventHandler(IOSentMessageHandler ioSentMessageHandler) {
        this.ioSentMessageHandler = ioSentMessageHandler;
    }

    @Bean
    public Consumer<Message<PnExtRegistryIOSentMessageEvent.Payload>> pnExtRegistryIOSentMessageConsumer() {
        return message -> {
            try {
                log.debug("IOSentMessage event received, message {}", message);

                PnExtRegistryIOSentMessageEvent notificationPaidEvent = PnExtRegistryIOSentMessageEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();
                
                Instant eventDate = notificationPaidEvent.getPayload().getSendDate();
                int recIndex = notificationPaidEvent.getPayload().getRecIndex();
                String internalId =notificationPaidEvent.getPayload().getInternalId();
                String iun =notificationPaidEvent.getPayload().getIun();
                
                log.info("pnExtRegistryIOSentMessageConsumer - eventDate={} iun={} recIndex={} internalId={}", eventDate, iun, recIndex, internalId);

                ioSentMessageHandler.handleIOSentMessage(iun, recIndex, eventDate);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
