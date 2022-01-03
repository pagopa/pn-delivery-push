package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.EventId;
import it.pagopa.pn.api.dto.notification.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class ExternalChannelServiceImpl implements ExternalChannelService {
    private TimelineService timelineService;
    private final ExtChnEventUtils extChnEventUtils;
    private final ExternalChannel externalChannel;

    public ExternalChannelServiceImpl(TimelineService timelineService, ExtChnEventUtils extChnEventUtils, ExternalChannel externalChannel) {
        this.timelineService = timelineService;
        this.extChnEventUtils = extChnEventUtils;
        this.externalChannel = externalChannel;
    }

    /**
     * Generate and send pec notification request to external channel
     */
    @Override
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

        PnExtChnPecEvent pnExtChnPecEvent = extChnEventUtils.buildSendPecRequest2(eventId, notification, recipient, digitalAddress);
        externalChannel.sendNotification(pnExtChnPecEvent);
        timelineService.addSendDigitalNotificationToTimeline(digitalAddress, recipient, notification, sentAttemptMade, eventId);
    }

    /**
     * Generate and send email notification request to external channel
     */
    @Override
    public void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient, String eventId) {
        log.info("Start SendCourtesyMessage to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
        PnExtChnEmailEvent pnExtChnEmailEvent = extChnEventUtils.buildSendEmailRequest2(eventId,
                notification,
                recipient,
                courtesyAddress);

        externalChannel.sendNotification(pnExtChnEmailEvent);
        timelineService.addSendCourtesyMessageToTimeline(recipient.getTaxId(), notification.getIun(), courtesyAddress, Instant.now(), eventId);
    }

    /**
     * Generate and send simple registered letter notification request to external channel
     */
    @Override
    public void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient) {
        log.info("Start sendNotificationForRegisteredLetter to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());

        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .build()
        );

        PnExtChnPaperEvent pnExtChnPaperEvent = extChnEventUtils.buildSendPaperRequest2(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE,  //TODO Da capire cosa si intende e se si può eliminare
                ServiceLevelType.SIMPLE_REGISTERED_LETTER,
                false,
                physicalAddress
        );

        externalChannel.sendNotification(pnExtChnPaperEvent);
        timelineService.addSendSimpleRegisteredLetterToTimeline(recipient.getTaxId(), notification.getIun(), physicalAddress, eventId);
    }

    /**
     * Generate and send analog notification request to external channel
     */
    @Override
    public void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation, int sentAttemptMade) {
        log.info("Start sendAnalogNotification to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
        String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(recipient.getTaxId())
                        .index(sentAttemptMade)
                        .build()
        );

        final PnExtChnPaperEvent pnExtChnPaperEvent = extChnEventUtils.buildSendPaperRequest2(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE, //TODO Da capire cosa si intende e se si può eliminare
                notification.getPhysicalCommunicationType(),
                investigation,
                physicalAddress);

        externalChannel.sendNotification(pnExtChnPaperEvent);
        timelineService.addSendAnalogNotificationToTimeline(physicalAddress, recipient, notification, investigation, sentAttemptMade, eventId);
    }

}
