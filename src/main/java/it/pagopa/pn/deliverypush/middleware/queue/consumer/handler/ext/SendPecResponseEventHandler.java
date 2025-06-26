package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ext;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@AllArgsConstructor
public class SendPecResponseEventHandler implements EventHandler<SingleStatusUpdate> {
    private final ExternalChannelResponseHandler externalChannelResponseHandler;

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.SEND_PEC_RESPONSE;
    }

    @Override
    public Class<SingleStatusUpdate> getPayloadType() {
        return SingleStatusUpdate.class;
    }

    @Override
    public void handle(SingleStatusUpdate singleStatusUpdate, MessageHeaders headers) {
        final String processName = "SEND PEC RESPONSE";

            try {
                log.debug("Handle message from {} with payload {}", ExternalChannelSendClient.CLIENT_NAME, singleStatusUpdate);

                log.logStartingProcess(processName);
                externalChannelResponseHandler.extChannelResponseReceiver(singleStatusUpdate);
                log.logEndingProcess(processName);
            } catch (Exception ex) {
                log.logEndingProcess(processName, false, ex.getMessage());
                HandleEventUtils.handleException(headers, ex);
                throw ex;
            }
    }
}
