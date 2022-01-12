package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatus;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.action2.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action2.it.TestUtils;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalChannelMock implements ExternalChannel {
    ExternalChannelResponseHandler externalChannelResponseHandler;

    public ExternalChannelMock(@Lazy ExternalChannelResponseHandler externalChannelResponseHandler) {
        this.externalChannelResponseHandler = externalChannelResponseHandler;
    }

    @Override
    public void sendNotification(PnExtChnEmailEvent event) {
        //Invio messaggio di cortesia non necessità di risposta da external channel
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


    private static final Pattern NEW_ADDRESS_INPUT_PATTERN = Pattern.compile("^NEW_ADDR:(.*)$");

    @Override
    public void sendNotification(PnExtChnPaperEvent event) {
        ExtChannelResponseStatus status;
        String newAddress;

        PhysicalAddress destinationAddress = event.getPayload().getDestinationAddress();
        String street = destinationAddress.getAddress();

        Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher( street );
        if(matcher.find() ) {
            status = ExtChannelResponseStatus.KO;
            newAddress = matcher.group(1).trim();
        }
        else if( street.startsWith("FAIL") ) {
            status = ExtChannelResponseStatus.KO;
            newAddress = null;
        }
        else if( street.startsWith("OK") ) {
            status = ExtChannelResponseStatus.OK;
            newAddress = null;
        }
        else {
            throw new IllegalArgumentException("Address " + street + " do not match test rule for mocks");
        }


        ExtChannelResponse.ExtChannelResponseBuilder responseBuilder = ExtChannelResponse.builder()
                .iun(event.getPayload().getIun())
                .notificationDate(Instant.now())
                .analogUsedAddress(event.getPayload().getDestinationAddress())
                .responseStatus( status )
                .eventId(event.getHeader().getEventId());

        if( newAddress != null ) {
            PhysicalAddress newDestinationAddress = destinationAddress.toBuilder()
                    .address( newAddress )
                    .build();
            responseBuilder.analogNewAddressFromInvestigation( newDestinationAddress );
        }


        externalChannelResponseHandler.extChannelResponseReceiver( responseBuilder.build() );

    }
}
