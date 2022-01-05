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
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class ExternalChannelUtils {
    private final TimelineService timelineService;
    private final PnDeliveryPushConfigs cfg;
    private final ExternalChannel externalChannel;
    private final TimelineUtils timelineUtils;

    public ExternalChannelUtils(TimelineService timelineService, PnDeliveryPushConfigs cfg,
                                ExternalChannel externalChannel, TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.cfg = cfg;
        this.externalChannel = externalChannel;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Generate and send pec notification request to external channel
     */
    public void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient,
                                        int sentAttemptMade) {
        log.info("Start sendDigitalNotification to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
        String eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .source(addressSource)
                        .index(sentAttemptMade)
                        .build()
        );

        PnExtChnPecEvent pnExtChnPecEvent = buildSendPecRequest2(eventId, notification, recipient, digitalAddress);
        externalChannel.sendNotification(pnExtChnPecEvent);
        addTimelineElement(timelineUtils.buildSendDigitalNotificationTimelineElement(digitalAddress, recipient, notification, sentAttemptMade, eventId));
    }

    /**
     * Generate and send email notification request to external channel
     */
    public void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient, String eventId) {
        log.info("Start SendCourtesyMessage to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
        PnExtChnEmailEvent pnExtChnEmailEvent = buildSendEmailRequest2(eventId,
                notification,
                recipient,
                courtesyAddress);

        externalChannel.sendNotification(pnExtChnEmailEvent);
        addTimelineElement(timelineUtils.buildSendCourtesyMessageTimelineElement(recipient.getTaxId(), notification.getIun(), courtesyAddress, Instant.now(), eventId));
    }

    /**
     * Generate and send simple registered letter notification request to external channel
     */
    public void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient) {
        log.info("Start sendNotificationForRegisteredLetter to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());

        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .build()
        );

        PnExtChnPaperEvent pnExtChnPaperEvent = buildSendPaperRequest2(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,  //TODO Da capire cosa si intende e se si può eliminare
                ServiceLevelType.SIMPLE_REGISTERED_LETTER,
                false,
                physicalAddress
        );

        externalChannel.sendNotification(pnExtChnPaperEvent);
        addTimelineElement(timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recipient.getTaxId(), notification.getIun(), physicalAddress, eventId));
    }

    /**
     * Generate and send analog notification request to external channel
     */
    public void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation, int sentAttemptMade) {
        log.info("Start sendAnalogNotification to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
        String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .index(sentAttemptMade)
                        .build()
        );

        final PnExtChnPaperEvent pnExtChnPaperEvent = buildSendPaperRequest2(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE, //TODO Da capire cosa si intende e se si può eliminare
                notification.getPhysicalCommunicationType(),
                investigation,
                physicalAddress);

        externalChannel.sendNotification(pnExtChnPaperEvent);
        addTimelineElement(timelineUtils.buildSendAnalogNotificationTimelineElement(physicalAddress, recipient, notification, investigation, sentAttemptMade, eventId));
    }

    public PnExtChnPecEvent buildSendPecRequest2(String eventId, Notification notification,
                                                 NotificationRecipient recipient, DigitalAddress address) {
        final String accessUrl = getAccessUrl(recipient);
        return PnExtChnPecEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun()) //TODO Lo iun viene replicato anche nel payload ha probabilmente senso eliminarne uno
                        .eventId(eventId) //TODO Da capire cosa inserire
                        .eventType(EventType.SEND_PEC_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
                        .build()
                )
                .payload(PnExtChnPecEventPayload.builder()
                        .iun(notification.getIun())
                        .requestCorrelationId(eventId) //TODO Duplicato ha senso averne uno solo
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

    public PnExtChnPaperEvent buildSendPaperRequest2(
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
                        .iun(notification.getIun()) //TODO Lo iun viene replicato anche nel payload ha probabilmente senso eliminarne uno
                        .eventId(eventId) //TODO Da capire cosa inserire
                        .eventType(EventType.SEND_PAPER_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
                        .build()
                )
                .payload(PnExtChnPaperEventPayload.builder()
                        .iun(notification.getIun())
                        .requestCorrelationId(eventId) //TODO Duplicato ha senso averne uno solo
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

    public PnExtChnEmailEvent buildSendEmailRequest2(
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
                        .createdAt(Instant.now())
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
        //TODO Capire a cosa serve
        return "test";
        //return String.format(cfg.getWebapp().getDirectAccessUrlTemplate(), recipient.getToken());
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
