package it.pagopa.pn.deliverypush.external;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import org.springframework.stereotype.Component;

@Component
public class ExternalChannelImpl implements ExternalChannel {
    @Override
    public void sendNotification(PnExtChnEmailEvent pnExtChnEmailEvent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendNotification(PnExtChnPecEvent pnExtChnPecEvent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendNotification(PnExtChnPaperEvent pnExtChnPecEvent) {
        throw new UnsupportedOperationException();
    }
}
