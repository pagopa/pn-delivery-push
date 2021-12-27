package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.SendPaperDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.deliverypush.actions.ExtChnEventUtils;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExternalChannelServiceImpl implements ExternalChannelService {
    private TimelineService timelineService;
    private ExtChnEventUtils extChnEventUtils;
    private ExternalChannel externalChannel;

    /**
     * Generate and send pec notification request to external channel
     */
    @Override
    public void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, NotificationRecipient recipient) {
        log.debug("Start sendDigitalNotification to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
        String eventId = null; //TODO Generare un event id, capire come generarlo
        PnExtChnPecEvent pnExtChnPecEvent = extChnEventUtils.buildSendPecRequest2(eventId, notification, recipient, digitalAddress);
        externalChannel.sendNotification(pnExtChnPecEvent);
        addSendDigitalNotificationToTimeline(digitalAddress, recipient);
    }

    /**
     * Generate and send email notification request to external channel
     */
    @Override
    public void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient) {
        log.debug("Start SendCourtesyMessage to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());
        String eventId = null; //TODO Generare eventId e capire come generarlo
        PnExtChnEmailEvent pnExtChnEmailEvent = extChnEventUtils.buildSendEmailRequest2(eventId,
                notification,
                recipient,
                courtesyAddress);

        externalChannel.sendNotification(pnExtChnEmailEvent);
    }

    /**
     * Generate and send simple registered letter notification request to external channel
     */
    @Override
    public void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient) {

        String eventId = null; //TODO Generare un event id, capire come generarlo

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
    }

    /**
     * Generate and send analog notification request to external channel
     */
    @Override
    public void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation) {
        log.debug("Start sendAnalogNotification to external channel for iun {} id {}", notification.getIun(), recipient.getTaxId());

        String eventId = null; //TODO Generare un event id, capire come generarlo
        final PnExtChnPaperEvent pnExtChnPaperEvent = extChnEventUtils.buildSendPaperRequest2(
                eventId,
                recipient,
                notification,
                CommunicationType.RECIEVED_DELIVERY_NOTICE, //TODO Da capire cosa si intende e se si può eliminare
                notification.getPhysicalCommunicationType(),
                investigation,
                physicalAddress);

        externalChannel.sendNotification(pnExtChnPaperEvent);
        addSendAnalogNotificationToTimeline(notification.getIun(), physicalAddress, recipient, notification, investigation);
    }

    private void addSendDigitalNotificationToTimeline(DigitalAddress digitalAddress, NotificationRecipient recipient) {
        TimelineElement.builder()
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .details(SendDigitalDetails.sendBuilder()
                        .taxId(recipient.getTaxId())
                        .address(digitalAddress)
                        .build()
                )
                .build();
    }

    private void addSendAnalogNotificationToTimeline(String iun, PhysicalAddress address, NotificationRecipient recipient, Notification notification, boolean investigation) {
        TimelineElement.builder()
                .category(TimelineElementCategory.SEND_ANALOG_DOMICILE)
                .iun(iun)
                .details(SendPaperDetails.builder()
                        .taxId(recipient.getTaxId())
                        .address(address)
                        .serviceLevel(notification.getPhysicalCommunicationType())
                        .investigation(investigation)
                        .build()
                ).build();
    }
}
