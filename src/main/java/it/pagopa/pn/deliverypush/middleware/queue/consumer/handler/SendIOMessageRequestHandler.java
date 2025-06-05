package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnExtRegistryIOSentMessageEvent;
import it.pagopa.pn.deliverypush.action.iosentmessage.IOSentMessageHandler;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@CustomLog
@AllArgsConstructor
public class SendIOMessageRequestHandler implements EventHandler<PnExtRegistryIOSentMessageEvent.Payload> {
    private final IOSentMessageHandler ioSentMessageHandler;

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.SEND_IO_MESSAGE_REQUEST;
    }

    @Override
    public Class<PnExtRegistryIOSentMessageEvent.Payload> getPayloadType() {
        return PnExtRegistryIOSentMessageEvent.Payload.class;
    }

    @Override
    public void handle(PnExtRegistryIOSentMessageEvent.Payload payload, MessageHeaders headers) {
        final String processName = "IO SENT MESSAGE EVENT";

        try {
            log.debug("Handle message from {} with payload {} and headers {}", PnExternalRegistryClient.CLIENT_NAME, payload, headers);

            PnExtRegistryIOSentMessageEvent ioSentMessageEvent = PnExtRegistryIOSentMessageEvent.builder()
                    .payload(payload)
                    .header(HandleEventUtils.mapStandardEventHeader(headers))
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
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
