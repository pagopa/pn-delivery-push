package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ExternalChannelUtils {
    private final TimelineService timelineService;
    private final PnDeliveryPushConfigs cfg;
    private final ExternalChannel externalChannel;
    private final TimelineUtils timelineUtils;
    private final InstantNowSupplier instantNowSupplier;

    public ExternalChannelUtils(TimelineService timelineService, PnDeliveryPushConfigs cfg,
                                ExternalChannel externalChannel, TimelineUtils timelineUtils, InstantNowSupplier instantNowSupplier) {
        this.timelineService = timelineService;
        this.cfg = cfg;
        this.externalChannel = externalChannel;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
    }

    /**
     * Generate and send pec notification request to external channel
     */
    public void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient,
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

        PnExtChnPecEvent pnExtChnPecEvent = buildSendPecRequest(eventId, notification, recipient, digitalAddress);

        addTimelineElement(timelineUtils.buildSendDigitalNotificationTimelineElement(digitalAddress, recipient, notification, sentAttemptMade, eventId));
        externalChannel.sendNotification(pnExtChnPecEvent);
    }

    /**
     * Generate and send email notification request to external channel
     */
    public void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient, String eventId) {
        PnExtChnEmailEvent pnExtChnEmailEvent = buildSendEmailRequest(eventId,
                notification,
                recipient,
                courtesyAddress);
        log.info("SendCourtesyMessage to external channel eventId{} - iun {} id {}", eventId, notification.getIun(), recipient.getTaxId());

        addTimelineElement(timelineUtils.buildSendCourtesyMessageTimelineElement(recipient.getTaxId(), notification.getIun(), courtesyAddress, instantNowSupplier.get(), eventId));
        externalChannel.sendNotification(pnExtChnEmailEvent);
    }

    /**
     * Generate and send simple registered letter notification request to external channel
     */
    public void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient) {
        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .build()
        );

        log.info("SendNotificationForRegisteredLetter to external channel for iun {} id {} eventId{} ", notification.getIun(), recipient.getTaxId(), eventId);

        PnExtChnPaperEvent pnExtChnPaperEvent = buildSendPaperRequest(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,  //TODO Da capire cosa si intende e se si può eliminare
                ServiceLevelType.SIMPLE_REGISTERED_LETTER,
                false,
                physicalAddress
        );

        addTimelineElement(timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recipient.getTaxId(), notification.getIun(), physicalAddress, eventId));
        externalChannel.sendNotification(pnExtChnPaperEvent);
    }

    /**
     * Generate and send analog notification request to external channel
     */
    public void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation, int sentAttemptMade) {
        String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .index(sentAttemptMade)
                        .build()
        );
        log.info("SendAnalogNotification to external channel eventId{} - iun {} id {}", eventId, notification.getIun(), recipient.getTaxId());

        final PnExtChnPaperEvent pnExtChnPaperEvent = buildSendPaperRequest(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE, //TODO Da capire cosa si intende e se si può eliminare
                notification.getPhysicalCommunicationType(),
                investigation,
                physicalAddress);

        addTimelineElement(timelineUtils.buildSendAnalogNotificationTimelineElement(physicalAddress, recipient, notification, investigation, sentAttemptMade, eventId));
        externalChannel.sendNotification(pnExtChnPaperEvent);
    }

    public PnExtChnPecEvent buildSendPecRequest(String eventId, Notification notification,
                                                NotificationRecipient recipient, DigitalAddress address) {
        final String accessUrl = getAccessUrl(recipient);
        return PnExtChnPecEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun()) //TODO Lo iun viene replicato anche nel payload ha probabilmente senso eliminarne uno
                        .eventId(eventId) //TODO Da capire cosa inserire
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
                        .iun(notification.getIun()) //TODO Lo iun viene replicato anche nel payload ha probabilmente senso eliminarne uno
                        .eventId(eventId)
                        .eventType(EventType.SEND_PAPER_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(instantNowSupplier.get())
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

    public SendPaperDetails getSendAnalogDomicileTimelineElement(String iun, String eventId) {
        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica ad external Channel
        Optional<SendPaperDetails> optTimeLineSendAnalogDomicile = timelineService.getTimelineElement(iun, eventId, SendPaperDetails.class);

        if (optTimeLineSendAnalogDomicile.isPresent()) {
            return optTimeLineSendAnalogDomicile.get();
        } else {
            log.error("There isn't timelineElement for iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't timelineElement for iun " + iun + " eventId " + eventId);
        }

    }

    private String getAccessUrl(NotificationRecipient recipient) {
        //TODO In fase di test fallisce capire a cosa serve
        return "test";
        //return String.format(cfg.getWebapp().getDirectAccessUrlTemplate(), recipient.getToken());
    }

    private void addTimelineElement(TimelineElement element) {
        timelineService.addTimelineElement(element);
    }

}
