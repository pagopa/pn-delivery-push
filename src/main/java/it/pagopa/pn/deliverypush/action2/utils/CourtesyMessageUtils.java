package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.deliverypush.action2.ExternalChannelSendHandler;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.SendCourtesyMessageDetails;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class CourtesyMessageUtils {
    private final AddressBookService addressBookService;
    private final ExternalChannelSendHandler externalChannelSendHandler;
    private final TimelineService timelineService;
    private final NotificationUtils notificationUtils;
    
    public CourtesyMessageUtils(AddressBookService addressBookService, ExternalChannelSendHandler externalChannelSendHandler, TimelineService timelineService, NotificationUtils notificationUtils) {
        this.addressBookService = addressBookService;
        this.externalChannelSendHandler = externalChannelSendHandler;
        this.timelineService = timelineService;
        this.notificationUtils = notificationUtils;
    }

    /**
     * Get recipient addresses and send courtesy messages.
     */
    public void checkAddressesForSendCourtesyMessage(NotificationInt notification, Integer recIndex) {
        log.info("CheckAddressesForSendCourtesyMessage - iun {} id {} ", notification.getIun(), recIndex);
        
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        
        //Vengono ottenuti tutti gli indirizzi di cortesia per il recipient ...
        addressBookService.getCourtesyAddress(recipient.getTaxId(), notification.getSender().getPaId())
                .ifPresent(listCourtesyAddresses -> {
                    int courtesyAddrIndex = 0;
                    for (DigitalAddress courtesyAddress : listCourtesyAddresses) {
                        sendCourtesyMessage(notification, recIndex, courtesyAddrIndex, courtesyAddress);
                        courtesyAddrIndex++;
                    }
                });

        log.debug("End sendCourtesyMessage - IUN {} id {}", notification.getIun(),recIndex);
    }

    private void sendCourtesyMessage(NotificationInt notification, Integer recIndex, int courtesyAddrIndex, DigitalAddress courtesyAddress) {
        log.debug("Send courtesy message address index {} - iun {} id {} ", courtesyAddrIndex, notification.getIun(), recIndex);

        //... Per ogni indirizzo di cortesia ottenuto viene inviata la notifica del messaggio di cortesia tramite external channel
        String eventId = getTimelineElementId(recIndex, notification.getIun(), courtesyAddrIndex);
        externalChannelSendHandler.sendCourtesyNotification(notification, courtesyAddress, recIndex, eventId);
    }

    private String getTimelineElementId(Integer recIndex, String iun, int index) {
        return TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .index(index)
                .build()
        );
    }
    
    public Optional<SendCourtesyMessageDetails> getFirstSentCourtesyMessage(String iun, Integer recIndex) {
        String timeLineCourtesyId = getTimelineElementId(recIndex, iun, 0);
        log.debug("Get courtesy message for timelineCourtesyId {} - IUN {} id {}", timeLineCourtesyId, iun, recIndex);
        return timelineService.getTimelineElementDetails(iun, timeLineCourtesyId, SendCourtesyMessageDetails.class);
    }

}
