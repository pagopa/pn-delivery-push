package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.ext;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PaperChannelResponseHandler;
import org.springframework.stereotype.Component;

@Component
public class SendAnalogResponseEventHandler extends AbstractPaperChannelEventHandler {
    public SendAnalogResponseEventHandler(PaperChannelResponseHandler paperChannelResponseHandler) {
        super(paperChannelResponseHandler);
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.SEND_ANALOG_RESPONSE;
    }
}
