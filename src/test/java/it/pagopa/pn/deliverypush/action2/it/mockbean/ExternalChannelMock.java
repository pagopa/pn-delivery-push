package it.pagopa.pn.deliverypush.action2.it.mockbean;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.*;
import it.pagopa.pn.deliverypush.action2.ExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
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
    public void sendLegalNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, LegalDigitalAddressInt digitalAddress, String timelineEventId) {
        //Invio messaggio legali necessità di risposta da external channel
        sendDigitalNotification(digitalAddress.getAddress(), notificationInt, timelineEventId, true);
    }


    @Override
    public void sendCourtesyNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, CourtesyDigitalAddressInt digitalAddress, String timelineEventId) {
        //Invio messaggio di cortesia non necessità di risposta da external channel

        sendDigitalNotification(digitalAddress.getAddress(), notificationInt, timelineEventId, false);

    }

    private void sendDigitalNotification(String address, NotificationInt notificationInt, String timelineEventId, boolean legal){
        log.info("sendDigitalNotification address:{} requestId:{}", address, timelineEventId);

        ProgressEventCategory status;

        String eventId = timelineEventId;
        String retryNumberPart = eventId.replaceFirst(".*([0-9]+)$", "$1");

        if (address != null) {
            String domainPart = address.replaceFirst(".*@", "");

            if (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_BOTH)
                    || (domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST) && "1".equals(retryNumberPart))) {
                status = ProgressEventCategory.ERROR;
            } else if (domainPart.startsWith(EXT_CHANNEL_WORKS) || domainPart.startsWith(EXT_CHANNEL_SEND_FAIL_FIRST)) {
                status = ProgressEventCategory.OK;
            } else {
                throw new IllegalArgumentException("PecAddress " + address + " do not match test rule for mocks");
            }
        } else {
            throw new IllegalArgumentException("PecAddress is null");
        }

        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        if (legal)
        {
            LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
            extChannelResponse.setStatus(status);
            extChannelResponse.setEventTimestamp(Instant.now());
            extChannelResponse.setRequestId(timelineEventId);

            singleStatusUpdate.setDigitalLegal(extChannelResponse);
        }
        else
        {
            CourtesyMessageProgressEvent extChannelResponse = new CourtesyMessageProgressEvent();
            extChannelResponse.setStatus(status);
            extChannelResponse.setEventTimestamp(Instant.now());
            extChannelResponse.setRequestId(timelineEventId);

            singleStatusUpdate.setDigitalCourtesy(extChannelResponse);
        }


        externalChannelHandler.extChannelResponseReceiver(singleStatusUpdate);
    }


    @Override
    public void sendAnalogNotification(NotificationInt notificationInt, NotificationRecipientInt recipientInt, PhysicalAddress physicalAddress, String timelineEventId, ANALOG_TYPE analogType, String aarKey) {
        log.info("sendAnalogNotification address:{} recipient:{} requestId:{} aarkey:{}", physicalAddress.getAddress(), recipientInt.getDenomination(), timelineEventId, aarKey);
        String status;
        String newAddress;

        PhysicalAddress destinationAddress = physicalAddress;
        String street = destinationAddress.getAddress();

        Matcher matcher = NEW_ADDRESS_INPUT_PATTERN.matcher(street);
        if (matcher.find()) {
            status = "__005__";
            newAddress = matcher.group(1).trim();
        } else if (street.startsWith(EXTCHANNEL_SEND_FAIL)) {
            status = "__005__";
            newAddress = null;
        } else if (street.startsWith(EXTCHANNEL_SEND_SUCCESS)) {
            status = "__004__";
            newAddress = null;
        } else {
            throw new IllegalArgumentException("Address " + street + " do not match test rule for mocks");
        }


        PaperProgressStatusEvent extChannelResponse = new PaperProgressStatusEvent();
        extChannelResponse.setStatusCode(status);
        extChannelResponse.setRequestId(timelineEventId);
        extChannelResponse.setIun(notificationInt.getIun());
        extChannelResponse.setStatusDateTime(Instant.now());
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setUrl("safestorage://urlditest");
        attachmentDetails.setId("123");
        attachmentDetails.setDate(Instant.now());
        attachmentDetails.setDocumentType("ricevuta");
        extChannelResponse.setAttachments(List.of(attachmentDetails));
        if (newAddress != null) {

            DiscoveredAddress newDestinationAddress = new DiscoveredAddress();
            newDestinationAddress.setCountry(destinationAddress.getForeignState());
            newDestinationAddress.setCap(destinationAddress.getZip());
            newDestinationAddress.setNameRow2(destinationAddress.getAt());
            newDestinationAddress.setAddressRow2(destinationAddress.getAddressDetails());
            newDestinationAddress.setCity(destinationAddress.getMunicipality());
            newDestinationAddress.setPr(destinationAddress.getProvince());
            newDestinationAddress.setAddress(newAddress);

            extChannelResponse.setDiscoveredAddress(newDestinationAddress);
        }

        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(extChannelResponse);


        externalChannelHandler.extChannelResponseReceiver(singleStatusUpdate);
    }
}
