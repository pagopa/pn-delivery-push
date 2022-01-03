package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook2;
import it.pagopa.pn.deliverypush.service.CourtesyMessageService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class CourtesyMessageServiceImpl implements CourtesyMessageService {
    private final AddressBook2 addressBook;
    private final ExternalChannelService externalChannelService;
    private final TimelineService timelineService;

    public CourtesyMessageServiceImpl(AddressBook2 addressBook, ExternalChannelService externalChannelService, TimelineService timelineService) {
        this.addressBook = addressBook;
        this.externalChannelService = externalChannelService;
        this.timelineService = timelineService;
    }

    /**
     * Get recipient addresses and send courtesy messages.
     */
    public void sendCourtesyMessage(Notification notification, NotificationRecipient recipient) {
        log.info("Start sendCourtesyMessage IUN {} id {} ", notification.getIun(), recipient.getTaxId());

        //Vengono ottenuti tutti gli indirizzi di cortesia per il recipient ...
        addressBook.getAddresses(recipient.getTaxId(), notification.getSender())
                .ifPresent(addressBookItem -> {
                    int index = 0;
                    for (DigitalAddress courtesyAddress : addressBookItem.getCourtesyAddresses()) {
                        log.info("send courtesy message for address index {}", index);

                        //... Per ogni indirizzo di cortesia ottenuto viene inviata la notifica del messaggio di cortesia tramite external channel
                        String eventId = getTimelineElementId(recipient.getTaxId(), notification.getIun(), index);
                        externalChannelService.sendCourtesyNotification(notification, courtesyAddress, recipient, eventId);
                        index++;
                    }

                });

        log.debug("End sendCourtesyMessage IUN {} id {}", notification.getIun(), recipient.getTaxId());
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
     * @return
     */
    @Override
    public Optional<SendCourtesyMessageDetails> getFirstSentCourtesyMessage(String iun, String taxId) {
        String timeLineCourtesyId = getTimelineElementId(taxId, iun, 0);
        log.debug("Get courtesy message for timelineCourtesyId {}", timeLineCourtesyId);

        return timelineService.getTimelineElement(iun, timeLineCourtesyId, SendCourtesyMessageDetails.class);
    }
}
