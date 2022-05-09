package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.*;
import it.pagopa.pn.deliverypush.external.ExternalChannel;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddress;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
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
    public void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, DigitalAddressSource addressSource, Integer recIndex,
                                        int sentAttemptMade) {
        PnExtChnPecEvent pnExtChnPecEvent = externalChannelUtils.getExtChannelPecEvent(notification, digitalAddress, addressSource, recIndex, sentAttemptMade);

        externalChannelUtils.addSendDigitalNotificationToTimeline(notification, digitalAddress, addressSource, recIndex, sentAttemptMade, pnExtChnPecEvent.getHeader().getEventId());
        externalChannel.sendNotification(pnExtChnPecEvent);
    }

    /**
     * Send courtesy message to external channel
     */
    public void sendCourtesyNotification(Notification notification, DigitalAddress courtesyAddress, Integer recIndex, String eventId) {
        PnExtChnEmailEvent pnExtChnEmailEvent = externalChannelUtils.getExtChannelEmailRequest(notification, courtesyAddress, recIndex, eventId);

        externalChannelUtils.addSendCourtesyMessageToTimeline(notification, courtesyAddress, recIndex, eventId);
        externalChannel.sendNotification(pnExtChnEmailEvent);
    }

    /**
     * Send registered letter to external channel
     */
    public void sendNotificationForRegisteredLetter(Notification notification, PhysicalAddress physicalAddress, Integer recIndex) {
        PnExtChnPaperEvent pnExtChnPaperEvent = externalChannelUtils.getExtChannelPaperRequest(notification, physicalAddress, recIndex);
        externalChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, physicalAddress, recIndex, pnExtChnPaperEvent.getHeader().getEventId());
        externalChannel.sendNotification(pnExtChnPaperEvent);
    }

    /**
     * Send paper notification to external channel
     */
    public void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
        PnExtChnPaperEvent pnExtChnPaperEvent = externalChannelUtils.getExtChannelPaperRequest(notification, physicalAddress, recIndex, investigation, sentAttemptMade);
        externalChannelUtils.addSendAnalogNotificationToTimeline(notification, physicalAddress, recIndex, investigation, sentAttemptMade, pnExtChnPaperEvent.getHeader().getEventId());
        externalChannel.sendNotification(pnExtChnPaperEvent);
    }
}
