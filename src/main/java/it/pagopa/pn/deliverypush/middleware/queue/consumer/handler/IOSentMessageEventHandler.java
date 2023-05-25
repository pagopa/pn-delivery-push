package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnExtRegistryIOSentMessageEvent;
import it.pagopa.pn.deliverypush.action.iosentmessage.IOSentMessageHandler;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import lombok.CustomLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.util.function.Consumer;

@Configuration
@CustomLog
public class IOSentMessageEventHandler {
    private final IOSentMessageHandler ioSentMessageHandler;

    public IOSentMessageEventHandler(IOSentMessageHandler ioSentMessageHandler) {
        this.ioSentMessageHandler = ioSentMessageHandler;
    }

    @Bean
    public Consumer<Message<PnExtRegistryIOSentMessageEvent.Payload>> pnExtRegistryIOSentMessageConsumer() {
        final String processName = "IO SENT MESSAGE EVENT";

        return message -> {
            try {
                log.debug("Handle message from {} with content {}", PnExternalRegistryClient.CLIENT_NAME, message);
                
                PnExtRegistryIOSentMessageEvent ioSentMessageEvent = PnExtRegistryIOSentMessageEvent.builder()
                        .payload(message.getPayload())
                        .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                        .build();
                
                Instant eventDate = ioSentMessageEvent.getPayload().getSendDate();
                int recIndex = ioSentMessageEvent.getPayload().getRecIndex();
                String iun =ioSentMessageEvent.getPayload().getIun();
                HandleEventUtils.addIunAndRecIndexToMdc(iun, recIndex);

                log.logStartingProcess(processName);
                ioSentMessageHandler.handleIOSentMessage(iun, recIndex, eventDate);
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }
}
