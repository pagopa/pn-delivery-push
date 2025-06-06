package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.middleware.queue.consumer.router.SupportedEventType;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PaperChannelResponseHandler;
import org.springframework.stereotype.Component;

@Component
public class PrepareAnalogResponseEventHandler extends AbstractPaperChannelEventHandler {
    public PrepareAnalogResponseEventHandler(PaperChannelResponseHandler paperChannelResponseHandler) {
        super(paperChannelResponseHandler);
    }

    @Override
    public SupportedEventType getSupportedEventType() {
        return SupportedEventType.PREPARE_ANALOG_RESPONSE;
    }
}
