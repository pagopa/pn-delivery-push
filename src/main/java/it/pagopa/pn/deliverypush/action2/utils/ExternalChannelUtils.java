package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.service.TimelineService;
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

    public ExternalChannelUtils(TimelineService timelineService, PnDeliveryPushConfigs cfg,
                                TimelineUtils timelineUtils, InstantNowSupplier instantNowSupplier) {
        this.timelineService = timelineService;
        this.cfg = cfg;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
    }

    /**
     * Generate and send pec notification request to external channel
     */
    public PnExtChnPecEvent getExtChannelPecEvent(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient,
                                                  int sentAttemptMade) {
        String eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .source(addressSource)
                        .index(sentAttemptMade)
                        .build()
        );
        log.info("SendDigitalNotification to external channel - iun {} id {} eventId{}", notification.getIun(), recipient.getTaxId(), eventId);

        return buildSendPecRequest(eventId, notification, recipient, digitalAddress);
    }

    public void addSendDigitalNotificationToTimeline(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient, int sentAttemptMade, String eventId) {
        addTimelineElement(timelineUtils.buildSendDigitalNotificationTimelineElement(digitalAddress, addressSource, recipient, notification, sentAttemptMade, eventId));
    }

    /**
     * Generate and send email notification request to external channel
     */
    public PnExtChnEmailEvent getExtChannelEmailRequest(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient, String eventId) {
        log.info("SendCourtesyMessage to external channel eventId{} - iun {} id {}", eventId, notification.getIun(), recipient.getTaxId());

        return buildSendEmailRequest(eventId,
                notification,
                recipient,
                courtesyAddress);
    }

    public void addSendCourtesyMessageToTimeline(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient, String eventId) {
        addTimelineElement(timelineUtils.buildSendCourtesyMessageTimelineElement(recipient.getTaxId(), notification.getIun(), courtesyAddress, instantNowSupplier.get(), eventId));
    }

    /**
     * Generate and send simple registered letter notification request to external channel
     */
    public PnExtChnPaperEvent getExtChannelPaperRequest(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient) {
        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .build()
        );

        log.info("SendNotificationForRegisteredLetter to external channel for iun {} id {} eventId{} ", notification.getIun(), recipient.getTaxId(), eventId);

        return buildSendPaperRequest(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,
                ServiceLevelType.SIMPLE_REGISTERED_LETTER,
                false,
                physicalAddress
        );
    }

    public void addSendSimpleRegisteredLetterToTimeline(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, String eventId) {
        addTimelineElement(timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recipient.getTaxId(), notification.getIun(), physicalAddress, eventId));
    }

    /**
     * Generate and send analog notification request to external channel
     */
    public PnExtChnPaperEvent getExtChannelPaperRequest(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation, int sentAttemptMade) {
        String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .index(sentAttemptMade)
                        .build()
        );
        log.info("SendAnalogNotification to external channel eventId{} - iun {} id {}", eventId, notification.getIun(), recipient.getTaxId());

        return buildSendPaperRequest(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,
                notification.getPhysicalCommunicationType(),
                investigation,
                physicalAddress);
    }

    public void addSendAnalogNotificationToTimeline(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation, int sentAttemptMade, String eventId) {
        addTimelineElement(timelineUtils.buildSendAnalogNotificationTimelineElement(physicalAddress, recipient, notification, investigation, sentAttemptMade, eventId));
    }

    public PnExtChnPecEvent buildSendPecRequest(String eventId, Notification notification,
                                                NotificationRecipient recipient, DigitalAddress address) {
        final String accessUrl = getAccessUrl(recipient);
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
            NotificationRecipient recipient,
            Notification notification,
            CommunicationType communicationType,
            ServiceLevelType serviceLevelType,
            boolean investigation,
            PhysicalAddress address
    ) {
        final String accessUrl = getAccessUrl(recipient);

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
                        .destinationAddress(address)
                        .recipientDenomination(recipient.getDenomination())
                        .communicationType(communicationType)
                        .serviceLevel(serviceLevelType)
                        .senderDenomination(notification.getSender().getPaId())
                        .investigation(investigation)
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
    }

    public PnExtChnEmailEvent buildSendEmailRequest(
            String eventId,
            Notification notification,
            NotificationRecipient recipient,
            DigitalAddress emailAddress
    ) {
        final String accessUrl = getAccessUrl(recipient);
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

    private String getAccessUrl(NotificationRecipient recipient) {
        return String.format(cfg.getWebapp().getDirectAccessUrlTemplate(), recipient.getToken());
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

    public TimelineElement getExternalChannelNotificationTimelineElement(String iun, String eventId) {
        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        Optional<TimelineElement> timelineElement = timelineService.getTimelineElement(iun, eventId);

        if (timelineElement.isPresent()) {
            return timelineElement.get();
        } else {
            log.error("There isn't timelineElement - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " eventId " + eventId);
        }
    }
}
