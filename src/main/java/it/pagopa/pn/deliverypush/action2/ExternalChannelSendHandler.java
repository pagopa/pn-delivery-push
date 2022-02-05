package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPaperEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalChannelSendHandler {
    private final ExternalChannelUtils externalChannelUtils;
    private final ExternalChannel externalChannel;

    public ExternalChannelSendHandler(ExternalChannelUtils externalChannelUtils, ExternalChannel externalChannel) {
        this.externalChannelUtils = externalChannelUtils;
        this.externalChannel = externalChannel;
    }

    /**
     * Send pec notification to external channel
     */
    public void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient,
                                        int sentAttemptMade) {
        PnExtChnPecEvent pnExtChnPecEvent = externalChannelUtils.getExtChannelPecEvent(notification, digitalAddress, addressSource, recipient, sentAttemptMade);

        externalChannelUtils.addSendDigitalNotificationToTimeline(notification, digitalAddress, addressSource, recipient, sentAttemptMade, pnExtChnPecEvent.getHeader().getEventId());
        externalChannel.sendNotification(pnExtChnPecEvent);
    }

    /**
     * Send courtesy message to external channel
     */
    public void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, NotificationRecipient recipient, String eventId) {
        PnExtChnEmailEvent pnExtChnEmailEvent = externalChannelUtils.getExtChannelEmailRequest(notification, courtesyAddress, recipient, eventId);

        externalChannelUtils.addSendCourtesyMessageToTimeline(notification, courtesyAddress, recipient, eventId);
        externalChannel.sendNotification(pnExtChnEmailEvent);
    }

    /**
     * Send registered letter to external channel
     */
    public void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient) {
        PnExtChnPaperEvent pnExtChnPaperEvent = externalChannelUtils.getExtChannelPaperRequest(notification, physicalAddress, recipient);
        externalChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, physicalAddress, recipient, pnExtChnPaperEvent.getHeader().getEventId());
        externalChannel.sendNotification(pnExtChnPaperEvent);
    }

    /**
     * Send paper notification to external channel
     */
    public void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, NotificationRecipient recipient, boolean investigation, int sentAttemptMade) {
        PnExtChnPaperEvent pnExtChnPaperEvent = externalChannelUtils.getExtChannelPaperRequest(notification, physicalAddress, recipient, investigation, sentAttemptMade);
        externalChannelUtils.addSendAnalogNotificationToTimeline(notification, physicalAddress, recipient, investigation, sentAttemptMade, pnExtChnPaperEvent.getHeader().getEventId());
        externalChannel.sendNotification(pnExtChnPaperEvent);
    }
}
