package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.deliverypush.action2.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelResponse;
import it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.ResponseStatus;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalChannelMock implements ExternalChannelSendClient {
    //DIGITAL
    public static final String EXT_CHANNEL_SEND_FAIL_BOTH = "fail-both";
    public static final String EXT_CHANNEL_SEND_FAIL_FIRST = "fail-first";
    public static final String EXT_CHANNEL_WORKS = "works";

    //ANALOG
    public static final String EXTCHANNEL_SEND_SUCCESS = "OK"; //Invio notifica ok
    public static final String EXTCHANNEL_SEND_FAIL = "FAIL"; //Invio notifica fallita
    public static final String EXT_CHANNEL_SEND_NEW_ADDR = "NEW_ADDR:"; //Invio notifica fallita con nuovo indirizzo da investigazione
    //Esempio: La combinazione di EXT_CHANNEL_SEND_NEW_ADDR + EXTCHANNEL_SEND_OK ad esempio significa -> Invio notifica fallito ma con nuovo indirizzo trovato e l'invio a tale indirzzo avrà successo

    private static final Pattern NEW_ADDRESS_INPUT_PATTERN = Pattern.compile("^" + EXT_CHANNEL_SEND_NEW_ADDR + "(.*)$");

    ExternalChannelResponseHandler externalChannelHandler;

    public ExternalChannelMock(@Lazy ExternalChannelResponseHandler externalChannelHandler) {
        this.externalChannelHandler = externalChannelHandler;
    }

    @Override
    public void sendDigitalNotification(NotificationInt notificationInt, DigitalAddress digitalAddress, String timelineEventId) {
        //Invio messaggio di cortesia non necessità di risposta da external channel
        ResponseStatus status;

        String pecAddress = digitalAddress.getAddress();

        String eventId = timelineEventId;
        String retryNumberPart = eventId.replaceFirst(".*([0-9]+)$", "$1");

        if (pecAddress != null) {
            String domainPart = pecAddress.replaceFirst(".*@", "");

            if (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_BOTH)
                    || (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST) && "1".equals(retryNumberPart))) {
                status = ResponseStatus.KO;
            } else if (domainPart.startsWith(EXT_CHANNEL_WORKS) || domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST)) {
                status = ResponseStatus.OK;
            } else {
                throw new IllegalArgumentException("PecAddress " + pecAddress + " do not match test rule for mocks");
            }
        } else {
            throw new IllegalArgumentException("PecAddress is null");
        }

        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .eventId(timelineEventId)
                .responseStatus(status)
                .notificationDate(Instant.now())
                .iun(notificationInt.getIun())
                .build();
        externalChannelHandler.extChannelResponseReceiver(extChannelResponse);
    }

    @Override
    public void sendAnalogNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, String timelineEventId, ANALOG_TYPE analogType, String aarKey) {
        ResponseStatus status;
        String newAddress;

        PhysicalAddress destinationAddress = recipientInt.getPhysicalAddress();
        String street = destinationAddress.getAddress();

        Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher(street);
        if (matcher.find()) {
            status = ResponseStatus.KO;
            newAddress = matcher.group(1).trim();
        } else if (street.startsWith(EXTCHANNEL_SEND_FAIL)) {
            status = ResponseStatus.KO;
            newAddress = null;
        } else if (street.startsWith(EXTCHANNEL_SEND_SUCCESS)) {
            status = ResponseStatus.OK;
            newAddress = null;
        } else {
            throw new IllegalArgumentException("Address " + street + " do not match test rule for mocks");
        }


        ExtChannelResponse.ExtChannelResponseBuilder responseBuilder = ExtChannelResponse.builder()
                .iun(notificationInt.getIun())
                .notificationDate(Instant.now())
                .responseStatus(status)
                .eventId(timelineEventId);

        if (newAddress != null) {

            it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress newDestinationAddress = it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress.builder()
                    .foreignState(destinationAddress.getForeignState())
                    .zip(destinationAddress.getZip())
                    .at(destinationAddress.getAt())
                    .addressDetails(destinationAddress.getAddressDetails())
                    .municipality(destinationAddress.getMunicipality())
                    .province(destinationAddress.getProvince())
                    .address(newAddress)
                    .build();
            responseBuilder.analogNewAddressFromInvestigation(newDestinationAddress);
        }

        externalChannelHandler.extChannelResponseReceiver(responseBuilder.build());
    }
}
