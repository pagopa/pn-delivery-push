package it.pagopa.pn.deliverypush.action2.it.testbean;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.deliverypush.action2.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.external.ExternalChannel;

import java.time.Instant;

public class ExternalChannelTest implements ExternalChannel {
    ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExternalChannelTest(ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }

    @Override
    public void sendNotification(PnExtChnEmailEvent event) {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.OK)
                .digitalUsedAddress(
                        DigitalAddress.builder()
                                .address(event.getPayload().getEmailAddress())
                                .type(DigitalAddressType.PEC)
                                .build()
                ).notificationDate(Instant.now())
                .iun(event.getPayload().getIun())
                .taxId(event.getPayload().getRecipientTaxId())
                .build();
        externalChannelResponseHandler.extChannelResponseReceiver(extChannelResponse);
    }

    @Override
    public void sendNotification(PnExtChnPecEvent event) {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.OK)
                .digitalUsedAddress(
                        DigitalAddress.builder()
                                .address(event.getPayload().getPecAddress())
                                .type(DigitalAddressType.PEC)
                                .build()
                ).notificationDate(Instant.now())
                .iun(event.getPayload().getIun())
                .taxId(event.getPayload().getRecipientTaxId())
                .build();
        externalChannelResponseHandler.extChannelResponseReceiver(extChannelResponse);
    }

    @Override
    public void sendNotification(PnExtChnPaperEvent event) {
        throw new UnsupportedOperationException();
    }
}
