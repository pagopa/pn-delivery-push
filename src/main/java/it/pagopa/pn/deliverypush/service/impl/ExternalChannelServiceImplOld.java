package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.AarUtils;
import it.pagopa.pn.deliverypush.action.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClientOld;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @deprecated
 * Deprecata in attesa di un mock di externalChannel con le nuove api
 */
@Deprecated(since = "PN-612", forRemoval = true)
@Slf4j
@Service
@ConditionalOnProperty( name = "pn.delivery-push.featureflags.externalchannel", havingValue = "old")
public class ExternalChannelServiceImplOld implements ExternalChannelService {

    private final ExternalChannelUtils externalChannelUtils;
    private final ExternalChannelSendClientOld externalChannel;
    private final TimelineUtils timelineUtils;
    private final NotificationUtils notificationUtils;
    private final PnDeliveryPushConfigs cfg;
    private final AarUtils aarUtils;

    public ExternalChannelServiceImplOld(ExternalChannelUtils externalChannelUtils, ExternalChannelSendClientOld externalChannel,
                                         TimelineUtils timelineUtils, NotificationUtils notificationUtils, PnDeliveryPushConfigs cfg, 
                                         AarUtils aarUtils) {
        this.externalChannelUtils = externalChannelUtils;
        this.externalChannel = externalChannel;
        this.timelineUtils = timelineUtils;
        this.notificationUtils = notificationUtils;
        this.cfg = cfg;
        this.aarUtils = aarUtils;
    }
    
/**
 * Send pec notification to external channel
 * @return
 */

    @Override
    public String sendDigitalNotification(NotificationInt notification,
                                          LegalDigitalAddressInt digitalAddress,
                                          DigitalAddressSourceInt addressSource,
                                          Integer recIndex,
                                          int sentAttemptMade,
                                          boolean sendAlreadyInProgress) {
        log.debug("Start sendDigitalNotification - iun {} id {}", notification.getIun(), recIndex);

        PnExtChnPecEvent pnExtChnPecEvent = getExtChannelPecEvent(notification, digitalAddress, addressSource, recIndex, sentAttemptMade);

        externalChannel.sendNotification(pnExtChnPecEvent);

        String eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .index(sentAttemptMade)
                        .build()
        );
        externalChannelUtils.addSendDigitalNotificationToTimeline(notification, digitalAddress, addressSource, recIndex, sentAttemptMade, eventId);

        return eventId;
    }
    
    
/**
 * Send courtesy message to external channel
 */

    @Override
    public void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId) {
        log.debug("Start sendCourtesyNotification - iun {} id {}", notification.getIun(), recIndex);

        PnExtChnEmailEvent pnExtChnEmailEvent = getExtChannelEmailRequest(notification, courtesyAddress, recIndex, eventId);
        externalChannel.sendNotification(pnExtChnEmailEvent);
    }
    
/**
 * Send registered letter to external channel
 */

    @Override
    public void sendNotificationForRegisteredLetter(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex) {
        log.debug("Start sendNotificationForRegisteredLetter - iun {} id {}", notification.getIun(), recIndex);
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed){

            PnExtChnPaperEvent pnExtChnPaperEvent = getExtChannelPaperRequest(notification, physicalAddress, recIndex);
            externalChannel.sendNotification(pnExtChnPaperEvent);

            AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

            String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .build()
            );
            externalChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, physicalAddress, recIndex, eventId, aarGenerationDetails.getNumberOfPages());

            log.info("Registered Letter sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
        }else {
            log.info("Notification is already viewed, registered Letter will not be sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
        }
    }

    
/**
 * Send paper notification to external channel
 */

    @Override
    public void sendAnalogNotification(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
        log.debug("Start sendAnalogNotification - iun {} id {}", notification.getIun(), recIndex);

        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed){

            PnExtChnPaperEvent pnExtChnPaperEvent = getExtChannelPaperRequest(notification, physicalAddress, recIndex, investigation, sentAttemptMade);
            externalChannel.sendNotification(pnExtChnPaperEvent);

            String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .index(sentAttemptMade)
                            .build()
            );

            AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

            externalChannelUtils.addSendAnalogNotificationToTimeline(notification, physicalAddress, recIndex, investigation, sentAttemptMade, eventId, aarGenerationDetails.getNumberOfPages());

            log.info("Analog notification sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
        }else {
            log.info("Notification is already viewed, paper notification will not be sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
        }
    }
    

/**
 * Generate and send pec notification request to external channel
 */

    public PnExtChnPecEvent getExtChannelPecEvent(NotificationInt notification,
                                                  LegalDigitalAddressInt digitalAddress,
                                                  DigitalAddressSourceInt addressSource,
                                                  Integer recIndex,
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
    
    
/**
 * Generate and send email notification request to external channel
 */

    public PnExtChnEmailEvent getExtChannelEmailRequest(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId) {
        log.info("SendCourtesyMessage to external channel eventId{} - iun {} id {}", eventId, notification.getIun(), recIndex);

        NotificationRecipientInt recipient = notificationUtils.getRecipientFromIndex(notification,recIndex);

        return buildSendEmailRequest(eventId,
                notification,
                recipient,
                courtesyAddress);
    }

    
/**
 * Generate and send simple registered letter notification request to external channel
 */

    public PnExtChnPaperEvent getExtChannelPaperRequest(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex) {
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

    
/**
 * Generate and send analog notification request to external channel
 */

    public PnExtChnPaperEvent getExtChannelPaperRequest(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
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

    public PnExtChnPecEvent buildSendPecRequest(String eventId, NotificationInt notification,
                                                NotificationRecipientInt recipient, LegalDigitalAddressInt address) {
        final String accessUrl = getAccessUrl(notification.getIun());
        return PnExtChnPecEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun())
                        .eventId(eventId)
                        .eventType(EventType.SEND_PEC_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
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
            PhysicalAddressInt address
    ) {
        final String accessUrl = getAccessUrl(notification.getIun());

        return PnExtChnPaperEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun())
                        .eventId(eventId)
                        .eventType(EventType.SEND_PAPER_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
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
            CourtesyDigitalAddressInt emailAddress
    ) {
        final String accessUrl = getAccessUrl(notification.getIun());
        return PnExtChnEmailEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(notification.getIun())
                        .eventId(eventId)
                        .eventType(EventType.SEND_COURTESY_EMAIL.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt( Instant.now() )
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



}
