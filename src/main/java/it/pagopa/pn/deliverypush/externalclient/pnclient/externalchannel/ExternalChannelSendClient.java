package it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel;


import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;

public interface ExternalChannelSendClient {
    void sendNotification(PnExtChnEmailEvent pnExtChnEmailEvent);

    void sendNotification(PnExtChnPecEvent pnExtChnPecEvent);

    void sendNotification(PnExtChnPaperEvent pnExtChnPecEvent);
}
