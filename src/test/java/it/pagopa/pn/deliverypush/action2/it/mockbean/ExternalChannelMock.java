package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.action2.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalChannelMock implements ExternalChannel {
    //DIGITAL
    public static final String EXT_CHANNEL_SEND_FAIL_BOTH = "fail-both";
    public static final String EXT_CHANNEL_SEND_FAIL_FIRST = "fail-first";
    public static final String EXT_CHANNEL_WORKS = "works";

    //ANALOG
    public static final String EXTCHANNEL_SEND_SUCCESS = "OK"; //Invio notifica ok
    public static final String EXTCHANNEL_SEND_FAIL = "FAIL"; //Invio notifica fallita
    public static final String EXT_CHANNEL_SEND_NEW_ADDR = "NEW_ADDR:"; //Invio notifica fallita con nuovo indirizzo da investigazione
    //Esempio: La combinazione di EXT_CHANNEL_SEND_NEW_ADDR + EXTCHANNEL_SEND_OK ad esempio significa -> Invio notifica fallito ma con nuovo indirizzo trovato e l'invio a tale indirzzo avrà successo

    ExternalChannelResponseHandler externalChannelHandler;

    public ExternalChannelMock(@Lazy ExternalChannelResponseHandler externalChannelHandler) {
        this.externalChannelHandler = externalChannelHandler;
    }

    @Override
    public void sendNotification(PnExtChnEmailEvent event) {
        //Invio messaggio di cortesia non necessità di risposta da external channel
    }

    @Override
    public void sendNotification(PnExtChnPecEvent event) {
        ExtChannelResponseStatus status;

        String pecAddress = event.getPayload().getPecAddress();

        String eventId = event.getHeader().getEventId();
        String retryNumberPart = eventId.replaceFirst(".*([0-9]+)$", "$1");

        if (pecAddress != null) {
            String domainPart = pecAddress.replaceFirst(".*@", "");

            if (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_BOTH)
                    || (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST) && "1".equals(retryNumberPart))) {
                status = ExtChannelResponseStatus.KO;
            } else if (domainPart.startsWith(EXT_CHANNEL_WORKS) || domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST)) {
                status = ExtChannelResponseStatus.OK;
            } else {
                throw new IllegalArgumentException("PecAddress " + pecAddress + " do not match test rule for mocks");
            }
        } else {
            throw new IllegalArgumentException("PecAddress is null");
        }

        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .eventId(event.getHeader().getEventId())
                .responseStatus(status)
                .notificationDate(Instant.now())
                .iun(event.getPayload().getIun())
                .build();
        externalChannelHandler.extChannelResponseReceiver(extChannelResponse);
    }

    private static final Pattern NEW_ADDRESS_INPUT_PATTERN = Pattern.compile("^" + EXT_CHANNEL_SEND_NEW_ADDR + "(.*)$");

    @Override
    public void sendNotification(PnExtChnPaperEvent event) {
        ExtChannelResponseStatus status;
        String newAddress;

        PhysicalAddress destinationAddress = event.getPayload().getDestinationAddress();
        String street = destinationAddress.getAddress();

        Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher(street);
        if (matcher.find()) {
            status = ExtChannelResponseStatus.KO;
            newAddress = matcher.group(1).trim();
        } else if (street.startsWith(EXTCHANNEL_SEND_FAIL)) {
            status = ExtChannelResponseStatus.KO;
            newAddress = null;
        } else if (street.startsWith(EXTCHANNEL_SEND_SUCCESS)) {
            status = ExtChannelResponseStatus.OK;
            newAddress = null;
        } else {
            throw new IllegalArgumentException("Address " + street + " do not match test rule for mocks");
        }


        ExtChannelResponse.ExtChannelResponseBuilder responseBuilder = ExtChannelResponse.builder()
                .iun(event.getPayload().getIun())
                .notificationDate(Instant.now())
                .responseStatus(status)
                .eventId(event.getHeader().getEventId());

        if (newAddress != null) {
            PhysicalAddress newDestinationAddress = destinationAddress.toBuilder()
                    .address(newAddress)
                    .build();
            responseBuilder.analogNewAddressFromInvestigation(newDestinationAddress);
        }

        externalChannelHandler.extChannelResponseReceiver(responseBuilder.build());
    }
}
