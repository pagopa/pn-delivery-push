package it.pagopa.pn.deliverypush.external;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.commons.abstractions.MomProducer;
import org.springframework.stereotype.Component;

@Component
public class ExternalChannelImpl implements ExternalChannel {
    
    private final MomProducer<PnExtChnPaperEvent> paperRequestProducer;
    private final MomProducer<PnExtChnPecEvent> pecRequestProducer;
    private final MomProducer<PnExtChnEmailEvent> emailRequestProducer;

    public ExternalChannelImpl(MomProducer<PnExtChnPaperEvent> paperRequestProducer, MomProducer<PnExtChnPecEvent> pecRequestProducer, MomProducer<PnExtChnEmailEvent> emailRequestProducer) {
        this.paperRequestProducer = paperRequestProducer;
        this.pecRequestProducer = pecRequestProducer;
        this.emailRequestProducer = emailRequestProducer;
    }

    @Override
    public void sendNotification(PnExtChnEmailEvent pnExtChnEmailEvent) {
        emailRequestProducer.push(pnExtChnEmailEvent);
    }

    @Override
    public void sendNotification(PnExtChnPecEvent pnExtChnPecEvent) {
        pecRequestProducer.push(pnExtChnPecEvent);
    } 

    @Override
    public void sendNotification(PnExtChnPaperEvent pnExtChnPaperEvent) {
        paperRequestProducer.push(pnExtChnPaperEvent);
    }
}
