package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.action2.ExternalChannelSendHandler;
import it.pagopa.pn.deliverypush.external.AddressBook;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CourtesyMessageUtils {
    private final AddressBook addressBook;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final TimelineService timelineService;

    public CourtesyMessageUtils(AddressBook addressBook, ExternalChannelSendHandler externalChannelSendHandler, TimelineService timelineService) {
        this.addressBook = addressBook;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.timelineService = timelineService;
    }

    /**
     * Get recipient addresses and send courtesy messages.
     */
    public void checkAddressesForSendCourtesyMessage(Notification notification, NotificationRecipient recipient) {
        log.info("CheckAddressesForSendCourtesyMessage - iun {} id {} ", notification.getIun(), recipient.getTaxId());

        //Vengono ottenuti tutti gli indirizzi di cortesia per il recipient ...
        addressBook.getAddresses(recipient.getTaxId(), notification.getSender())
                .ifPresent(addressBookItem -> {
                    int index = 0;
                    if (addressBookItem.getCourtesyAddresses() != null) {
                        for (DigitalAddress courtesyAddress : addressBookItem.getCourtesyAddresses()) {
                            sendCourtesyMessage(notification, recipient, index, courtesyAddress);
                            index++;
                        }
                    }
                });

        log.debug("End sendCourtesyMessage - IUN {} id {}", notification.getIun(), recipient.getTaxId());
    }

    private void sendCourtesyMessage(Notification notification, NotificationRecipient recipient, int index, DigitalAddress courtesyAddress) {
        log.debug("Send courtesy message address index {} - iun {} id {} ", index, notification.getIun(), recipient.getTaxId());

        //... Per ogni indirizzo di cortesia ottenuto viene inviata la notifica del messaggio di cortesia tramite external channel
        String eventId = getTimelineElementId(recipient.getTaxId(), notification.getIun(), index);
        externalChannelSendHandler.sendCourtesyNotification(notification, courtesyAddress, recipient, eventId);
    }

    private String getTimelineElementId(String taxId, String iun, int index) {
        return TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recipientId(taxId)
                .index(index)
                .build()
        );
    }

    /**
     * Get user courtesy messages from timeline
     *
     * @param iun   Notification unique identifier
     * @param taxId User identifier
     */
    public Optional<SendCourtesyMessageDetails> getFirstSentCourtesyMessage(String iun, String taxId) {
        String timeLineCourtesyId = getTimelineElementId(taxId, iun, 0);
        log.debug("Get courtesy message for timelineCourtesyId {} - IUN {} id {}", timeLineCourtesyId, iun, taxId);

        return timelineService.getTimelineElement(iun, timeLineCourtesyId, SendCourtesyMessageDetails.class);
    }

}
