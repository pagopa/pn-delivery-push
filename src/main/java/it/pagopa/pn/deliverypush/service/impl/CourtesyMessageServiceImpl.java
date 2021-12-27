package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendCourtesyMessageDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.commons.pnclients.addressbook.AddressBook2;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.service.CourtesyMessageService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class CourtesyMessageServiceImpl implements CourtesyMessageService {
    private AddressBook2 addressBook;
    private ExtChnEventUtils extChnEventUtils;
    private ExternalChannelService externalChannelService;
    private TimelineService timelineService;

    /**
     * Get recipient addresses and send courtesy messages.
     */
    public void sendCourtesyMessage(Notification notification, NotificationRecipient recipient) {
        log.debug("Start sendCourtesyMessage IUN {} id {} ", notification.getIun(), recipient.getTaxId());

        //Vengono ottenuti tutti gli indirizzi di cortesia per il recipient ...
        addressBook.getAddresses(recipient.getTaxId(), notification.getSender())
                .ifPresent(addressBookItem -> {
                    int index = 0;
                    for (DigitalAddress courtesyAddress : addressBookItem.getCourtesyAddresses()) {
                        //... Per ogni indirizzo di cortesia ottenuto viene inviata la notifica del messaggio di cortesia tramite external channel

                        externalChannelService.sendCourtesyNotification(notification, courtesyAddress, recipient);
                        addSendCourtesyMessageToTimeline(recipient.getTaxId(), notification.getIun(), courtesyAddress, Instant.now(), index);

                        index++;
                    }

                });

        log.debug("End sendCourtesyMessage IUN {} id {}", notification.getIun(), recipient.getTaxId());
    }

    private void addSendCourtesyMessageToTimeline(String taxId, String iun, DigitalAddress address, Instant sendDate, int index) {
        log.debug("Add send courtesy message to timeline");

        //Viene aggiunto l'invio alla timeline con un particolare elementId utile ad ottenere tali elementi successivamente nel workflow (Start analog workflow)
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SEND_COURTESY_MESSAGE)
                .elementId(getTimeLineCourtesyId(taxId, index))
                .iun(iun)
                .details(SendCourtesyMessageDetails.builder()
                        .taxId(taxId)
                        .address(address)
                        .sendDate(sendDate)
                        .build())
                .build());
    }

    private String getTimeLineCourtesyId(String taxId, int index) {
        return taxId + "_" + index + "courtesy";
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
        String timeLineCourtesyId = getTimeLineCourtesyId(taxId, 0);
        log.debug("Get courtesy message for timelineCourtesyId {}", timeLineCourtesyId);

        return timelineService.getTimelineElement(iun, timeLineCourtesyId, SendCourtesyMessageDetails.class);
    }
}
