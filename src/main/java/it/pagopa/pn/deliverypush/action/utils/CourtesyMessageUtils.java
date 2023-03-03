package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendCourtesyMessageDetailsInt;
import it.pagopa.pn.deliverypush.service.AddressBookService;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.IoService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ERRORCOURTESY;
import static it.pagopa.pn.externalregistry.generated.openapi.clients.externalregistry.model.SendMessageResponse.ResultEnum.*;

@Component
@Slf4j
public class CourtesyMessageUtils {
    public static final int FIRST_COURTESY_ELEMENT = 0;

    private final AddressBookService addressBookService;
    private final ExternalChannelService externalChannelService;
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    private final IoService iOservice;

    public CourtesyMessageUtils(AddressBookService addressBookService,
                                ExternalChannelService externalChannelService,
                                TimelineService timelineService,
                                TimelineUtils timelineUtils,
                                NotificationUtils notificationUtils,
                                IoService iOservice) {
        this.addressBookService = addressBookService;
        this.externalChannelService = externalChannelService;
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
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
        log.debug("Send courtesy message address index {} - iun={} id={} type={}", courtesyAddrIndex, notification.getIun(), recIndex, courtesyAddress.getType());

        try {
            //... Per ogni indirizzo di cortesia ottenuto viene inviata la notifica del messaggio di cortesia
            String eventId = getTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType());
            boolean timelineShouldBeSaved = true;
            IoSendMessageResultInt ioSendMessageResult = null;
            
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
                    SendMessageResponse.ResultEnum result =  iOservice.sendIOMessage(notification, recIndex);
                    
                    if(SENT_COURTESY.equals(result) || SENT_OPTIN.equals(result) || NOT_SENT_OPTIN_ALREADY_SENT.equals(result)){
                        ioSendMessageResult = IoSendMessageResultInt.valueOf(result.getValue());
                    }else {
                        timelineShouldBeSaved = false;
                    }
                    
                }
                default -> handleCourtesyTypeError(notification, recIndex, courtesyAddress);
            }

            if (timelineShouldBeSaved)
            {
                addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, Instant.now(), eventId, ioSendMessageResult);
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

    public void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant sentDate) {
        this.addSendCourtesyMessageToTimeline(notification, recIndex, courtesyAddress, sentDate, getTimelineElementId(recIndex, notification.getIun(), courtesyAddress.getType()),
                IoSendMessageResultInt.SENT_COURTESY);
    }

    private void addSendCourtesyMessageToTimeline(NotificationInt notification, Integer recIndex, CourtesyDigitalAddressInt courtesyAddress, Instant sentDate, String eventId,
                                                  IoSendMessageResultInt ioSendMessageResult) {
        addTimelineElement(
                timelineUtils.buildSendCourtesyMessageTimelineElement(recIndex, notification, courtesyAddress, sentDate, eventId, ioSendMessageResult),
                notification
        );
    }

    private String getTimelineElementId(Integer recIndex, String iun, CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT courtesyAddressType) {
        return TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(EventId.builder()
                .iun(iun)
                .recIndex(recIndex)
                .courtesyAddressType(courtesyAddressType)
                .build()
        );
    }


    public List<SendCourtesyMessageDetailsInt> getSentCourtesyMessagesDetails(String iun, int recIndex){
        // cerco dal DB tutte le timeline relative a iun/recindex che sono di tipo courtesy
        String elementIdForSearch = TimelineEventId.SEND_COURTESY_MESSAGE
                .buildSearchEventIdByIunAndRecipientIndex(iun, recIndex);

        return this.timelineService.getTimelineByIunTimelineId(iun, elementIdForSearch, false)
                .stream().map(x -> (SendCourtesyMessageDetailsInt)x.getDetails()).toList();
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }
}
