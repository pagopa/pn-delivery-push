package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.IoService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY;

@Component
@Slf4j
public class CourtesyMessageUtils {
    public static final int FIRST_COURTESY_ELEMENT = 0;

    private final AddressBookService addressBookService;
    private final ExternalChannelService externalChannelService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final InstantNowSupplier instantNowSupplier;
    private final NotificationUtils notificationUtils;
    private final IoService iOservice;

    public CourtesyMessageUtils(AddressBookService addressBookService,
                                ExternalChannelService externalChannelService,
                                TimelineService timelineService,
                                TimelineUtils timelineUtils,
                                InstantNowSupplier instantNowSupplier,
                                NotificationUtils notificationUtils,
                                IoService iOservice) {
        this.addressBookService = addressBookService;
        this.externalChannelService = externalChannelService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
        this.notificationUtils = notificationUtils;
        this.iOservice = iOservice;
    }

    /**
     * Get recipient addresses and send courtesy messages.
     */
    public void checkAddressesAndSendCourtesyMessage(NotificationInt notification, Integer recIndex) {
        log.debug("Start checkAddressesForSendCourtesyMessage - iun={} id={} ", notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification, recIndex);

        //Vengono ottenuti tutti gli indirizzi di cortesia per il recipient ...
        addressBookService.getCourtesyAddress(recipient.getInternalId(), notification.getSender().getPaId())
                .ifPresent(listCourtesyAddresses -> {
                    int courtesyAddrIndex = FIRST_COURTESY_ELEMENT;

                    for (CourtesyDigitalAddressInt courtesyAddress : listCourtesyAddresses) {
                        courtesyAddrIndex = sendCourtesyMessage(notification, recIndex, courtesyAddrIndex, courtesyAddress);
                    }
                });

        log.debug("End sendCourtesyMessage - IUN={} id={}", notification.getIun(), recIndex);
    }

    private int sendCourtesyMessage(NotificationInt notification,
                                    Integer recIndex,
                                    int courtesyAddrIndex,
                                    CourtesyDigitalAddressInt courtesyAddress) {
        log.debug("Send courtesy message address index {} - iun={} id={} ", courtesyAddrIndex, notification.getIun(), recIndex);

        try {
            //... Per ogni indirizzo di cortesia ottenuto viene inviata la notifica del messaggio di cortesia
            String eventId = getTimelineElementId(recIndex, notification.getIun(), courtesyAddrIndex);
            boolean timelineShouldBeSaved = true;

            switch (courtesyAddress.getType()) {
                case EMAIL, SMS -> {
                    log.info("Send courtesy message to externalChannel courtesyType={} - iun={} id={} ", courtesyAddress.getType(), notification.getIun(), recIndex);
                    externalChannelService.sendCourtesyNotification(notification, courtesyAddress, recIndex, eventId);
                }
                case APPIO -> {
                    // nel caso di IO, il messaggio potrebbe NON essere inviato. Al netto del fatto di eccezioni, che vengono catchate sotto
                    // ci sono casi in cui non viene inviato perchè l'utente non ha abilitato IO. Quindi in questi casi non viene salvato l'evento di timeline
                    // NB: anche nel caso di invio di Opt-in, non salvo l'evento in timeline.
                    log.info("Send courtesy message to App IO - iun={} id={} ", notification.getIun(), recIndex);
                    timelineShouldBeSaved = iOservice.sendIOMessage(notification, recIndex);
                }
                default -> handleCourtesyTypeError(notification, recIndex, courtesyAddress);
            }

            if (timelineShouldBeSaved)
            {
                addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, eventId);
            }
            else
            {
                log.info("skipping saving courtesy timeline iun={} id={}", notification.getIun(), recIndex);
            }

            courtesyAddrIndex++;
        } catch (Exception ex) {
            //Se l'invio del messaggio di cortesia fallisce per un qualsiasi motivo il processo non si blocca. Viene fatto catch exception e loggata
            log.error("Exception in send courtesy message, courtesyType={} ex={} - iun={} id={}", courtesyAddress.getType(), ex, notification.getIun(), recIndex);
        }

        return courtesyAddrIndex;
    }

    private void handleCourtesyTypeError(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress) {
        log.error("Is not possibile to send courtesy message, courtesyAddressType={} is not defined - iun={} id={}",
                courtesyAddress.getType(), notification.getIun(), recIndex);
        throw new PnInternalException("Is not possibile to send courtesy message, courtesyAddressType="+ courtesyAddress.getType()+
                " is not defined - iun="+ notification.getIun()+" id="+ recIndex, ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY);
    }

    private void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, String eventId) {
        addTimelineElement(
                timelineUtils.buildSendCourtesyMessageTimelineElement(recIndex, notification, courtesyAddress, instantNowSupplier.get(), eventId),
                notification
        );
    }

    private String getTimelineElementId(Integer recIndex, String iun, int index) {
        return TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .index(index)
                .build()
        );
    }

    public Optional<SendCourtesyMessageDetailsInt> getFirstSentCourtesyMessage(String iun, Integer recIndex) {
        String timeLineCourtesyId = getTimelineElementId(recIndex, iun, FIRST_COURTESY_ELEMENT);
        log.debug("Get courtesy message for timelineCourtesyId={} - IUN={} id={}", timeLineCourtesyId, iun, recIndex);
        return timelineService.getTimelineElementDetails(iun, timeLineCourtesyId, SendCourtesyMessageDetailsInt.class);
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
