package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ext;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.EventHandler;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PaperChannelResponseHandler;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.springframework.messaging.MessageHeaders;

@CustomLog
@AllArgsConstructor
public abstract class AbstractPaperChannelEventHandler implements EventHandler<PaperChannelUpdate> {
    protected final PaperChannelResponseHandler paperChannelResponseHandler;

    @Override
    public Class<PaperChannelUpdate> getPayloadType() {
        return PaperChannelUpdate.class;
    }

    @Override
    public void handle(PaperChannelUpdate singleStatusUpdate, MessageHeaders headers) {
        final String processName = "PAPER_CHANNEL_EVENT_HANDLER";

        try {
            log.debug("Handle message from {} with payload {}", PaperChannelSendClient.CLIENT_NAME, singleStatusUpdate);
            log.logStartingProcess(processName);
            paperChannelResponseHandler.paperChannelResponseReceiver(singleStatusUpdate);
            log.logEndingProcess(processName);
        } catch (Exception ex) {
            log.logEndingProcess(processName, false, ex.getMessage());
            HandleEventUtils.handleException(headers, ex);
            throw ex;
        }
    }
}
