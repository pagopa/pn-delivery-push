package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.userattributes.generated.openapi.clients.userattributes.model.CourtesyDigitalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ExternalChannelUtils {
    private final TimelineService timelineService;
    private final PnDeliveryPushConfigs cfg;
    private final TimelineUtils timelineUtils;
    private final InstantNowSupplier instantNowSupplier;
    private final NotificationUtils notificationUtils;
    
    public ExternalChannelUtils(TimelineService timelineService, PnDeliveryPushConfigs cfg,
                                TimelineUtils timelineUtils, InstantNowSupplier instantNowSupplier, NotificationUtils notificationUtils) {
        this.timelineService = timelineService;
        this.cfg = cfg;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
        this.notificationUtils = notificationUtils;
    }

    /**
     * Generate and send pec notification request to external channel
     */
    public PnExtChnPecEvent getExtChannelPecEvent(NotificationInt notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, Integer recIndex,
                                                  int sentAttemptMade) {
        String eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .index(sentAttemptMade)
                        .build()
        );
        log.info("SendDigitalNotification to external channel - iun {} id {} eventId{}", notification.getIun(), recIndex, eventId);
        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);
        
        return buildSendPecRequest(eventId, notification, recipient, digitalAddress);
    }

    public void addSendDigitalNotificationToTimeline(NotificationInt notification, LegalDigitalAddressInt digitalAddress, DigitalAddressSource addressSource, Integer recIndex, int sentAttemptMade, String eventId) {
        addTimelineElement(timelineUtils.buildSendDigitalNotificationTimelineElement(digitalAddress, addressSource, recIndex, notification, sentAttemptMade, eventId));
    }

    /**
     * Generate and send email notification request to external channel
     */
    public PnExtChnEmailEvent getExtChannelEmailRequest(NotificationInt notification, DigitalAddress courtesyAddress, Integer recIndex, String eventId) {
        log.info("SendCourtesyMessage to external channel eventId{} - iun {} id {}", eventId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        return buildSendEmailRequest(eventId,
                notification,
                recipient,
                courtesyAddress);
    }

    public void addSendCourtesyMessageToTimeline(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId) {
        addTimelineElement(timelineUtils.buildSendCourtesyMessageTimelineElement(recIndex, notification.getIun(), courtesyAddress, instantNowSupplier.get(), eventId));
    }

    /**
     * Generate and send simple registered letter notification request to external channel
     */
    public PnExtChnPaperEvent getExtChannelPaperRequest(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex) {
        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        log.info("SendNotificationForRegisteredLetter to external channel for iun {} id {} eventId{} ", notification.getIun(), recIndex, eventId);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        return buildSendPaperRequest(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,
                ServiceLevelTypeInt.SIMPLE_REGISTERED_LETTER,
                false,
                physicalAddress
        );
    }

    public void addSendSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex, String eventId) {
        addTimelineElement(timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recIndex, notification.getIun(), physicalAddress, eventId));
    }

    /**
     * Generate and send analog notification request to external channel
     */
    public PnExtChnPaperEvent getExtChannelPaperRequest(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
        String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .index(sentAttemptMade)
                        .build()
        );
        log.info("SendAnalogNotification to external channel eventId{} - iun {} id {}", eventId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        return buildSendPaperRequest(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,
                notification.getPhysicalCommunicationType(),
                investigation,
                physicalAddress);
    }

    public void addSendAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade, String eventId) {
        addTimelineElement(timelineUtils.buildSendAnalogNotificationTimelineElement(physicalAddress, recIndex, notification, investigation, sentAttemptMade, eventId));
    }

    public PnExtChnPecEvent buildSendPecRequest(String eventId, NotificationInt notification,
                                                NotificationRecipientInt recipient, DigitalAddress address) {
        final String accessUrl = getAccessUrl(notification.getIun());
        return PnExtChnPecEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun())
                        .eventId(eventId)
                        .eventType(EventType.SEND_PEC_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(instantNowSupplier.get())
                        .build()
                )
                .payload(PnExtChnPecEventPayload.builder()
                        .iun(notification.getIun())
                        .requestCorrelationId(eventId)
                        .recipientTaxId(recipient.getTaxId())
                        .recipientDenomination(recipient.getDenomination())
                        .senderId(notification.getSender().getPaId())
                        .senderDenomination(notification.getSender().getPaId())
                        .senderPecAddress("Not required")
                        .pecAddress(address.getAddress())
                        .shipmentDate(notification.getSentAt())
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
    }

    public PnExtChnPaperEvent buildSendPaperRequest(
            String eventId,
            NotificationRecipientInt recipient,
            NotificationInt notification,
            CommunicationType communicationType,
            ServiceLevelTypeInt serviceLevelType,
            boolean investigation,
            PhysicalAddress address
    ) {
        final String accessUrl = getAccessUrl(notification.getIun());

        return PnExtChnPaperEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun()) 
                        .eventId(eventId)
                        .eventType(EventType.SEND_PAPER_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(instantNowSupplier.get())
                        .build()
                )
                .payload(PnExtChnPaperEventPayload.builder()
                        .iun(notification.getIun())
                        .requestCorrelationId(eventId)
                        .destinationAddress(it.pagopa.pn.api.dto.notification.address.PhysicalAddress.builder()
                                .address(address.getAddress())
                                .addressDetails(address.getAddressDetails())
                                .at(address.getAt())
                                .foreignState(address.getForeignState())
                                .municipality(address.getMunicipality())
                                .province(address.getProvince())
                                .zip(address.getZip())
                                .build()
                        )
                        .recipientDenomination(recipient.getDenomination())
                        .communicationType(communicationType)
                        .serviceLevel(serviceLevelType != null ? it.pagopa.pn.api.dto.events.ServiceLevelType.valueOf(serviceLevelType.name()) : null)
                        .senderDenomination(notification.getSender().getPaId())
                        .investigation(investigation)
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
    }

    public PnExtChnEmailEvent buildSendEmailRequest(
            String eventId,
            NotificationInt notification,
            NotificationRecipientInt recipient,
            DigitalAddress emailAddress
    ) {
        final String accessUrl = getAccessUrl(notification.getIun());
        return PnExtChnEmailEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun())
                        .eventId(eventId)
                        .eventType(EventType.SEND_COURTESY_EMAIL.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(instantNowSupplier.get())
                        .build()
                )
                .payload(PnExtChnEmailEventPayload.builder()
                        .iun(notification.getIun())
                        .senderId(notification.getSender().getPaId())
                        .senderDenomination(notification.getSender().getPaId())
                        .senderEmailAddress("Not required")
                        .recipientDenomination(recipient.getDenomination())
                        .recipientTaxId(recipient.getTaxId())
                        .emailAddress(emailAddress.getAddress())
                        .shipmentDate(notification.getSentAt())
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
    }

    private String getAccessUrl(String iun) {
        return String.format(cfg.getWebapp().getDirectAccessUrlTemplate(), iun);
    }

    private void addTimelineElement(TimelineElementInternal element) {
        timelineService.addTimelineElement(element);
    }

    public TimelineElementInternal getExternalChannelNotificationTimelineElement(String iun, String eventId) {
        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        Optional<TimelineElementInternal> timelineElement = timelineService.getTimelineElement(iun, eventId);

        if (timelineElement.isPresent()) {
            return timelineElement.get();
        } else {
            log.error("There isn't timelineElement - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " eventId " + eventId);
        }
    }
}
