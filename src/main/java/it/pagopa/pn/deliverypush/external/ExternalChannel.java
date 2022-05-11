package it.pagopa.pn.deliverypush.external;


import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;

public interface ExternalChannel {
    void sendNotification(PnExtChnEmailEvent pnExtChnEmailEvent);

    void sendNotification(PnExtChnPecEvent pnExtChnPecEvent);

    void sendNotification(PnExtChnPaperEvent pnExtChnPecEvent);

}
