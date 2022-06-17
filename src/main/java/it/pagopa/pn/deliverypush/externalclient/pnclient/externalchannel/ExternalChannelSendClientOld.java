package it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;
import org.springframework.stereotype.Component;

/**
 * @deprecated
 * Deprecata in attesa di un mock di externalChannel con le nuove api
 */
@Deprecated(since = "PN-612", forRemoval = true)
@Component
public class ExternalChannelSendClientOld {

    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private final MomProducer<PnExtChnPecEvent> pecRequestProducer;
    private final MomProducer<PnExtChnEmailEvent> emailRequestProducer;

    public ExternalChannelSendClientOld(MomProducer<PnExtChnPaperEvent> paperRequestProducer,
                                        MomProducer<PnExtChnPecEvent> pecRequestProducer,
                                        MomProducer<PnExtChnEmailEvent> emailRequestProducer) {
        this.paperRequestProducer = paperRequestProducer;
        this.pecRequestProducer = pecRequestProducer;
        this.emailRequestProducer = emailRequestProducer;
    }

    public void sendNotification(PnExtChnEmailEvent pnExtChnEmailEvent) {
        emailRequestProducer.push(pnExtChnEmailEvent);
    }

    public void sendNotification(PnExtChnPecEvent pnExtChnPecEvent) {
        pecRequestProducer.push(pnExtChnPecEvent);
    }

    public void sendNotification(PnExtChnPaperEvent pnExtChnPaperEvent) {
        paperRequestProducer.push(pnExtChnPaperEvent);
    }
    
}
