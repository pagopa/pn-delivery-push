package it.pagopa.pn.deliverypush.external;


import it.pagopa.pn.deliverypush.dto.ext.externalchannel.PnExtChnEmailEvent;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.PnExtChnPaperEvent;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.PnExtChnPecEvent;

public interface ExternalChannel {
    void sendNotification(PnExtChnEmailEvent pnExtChnEmailEvent);

    void sendNotification(PnExtChnPecEvent pnExtChnPecEvent);

    void sendNotification(PnExtChnPaperEvent pnExtChnPecEvent);

}
