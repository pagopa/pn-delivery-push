package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.AarUtils;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.AarGenerationDetails;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.DigitalAddressSource;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.PhysicalAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExternalChannelSendHandler {
    private final ExternalChannelUtils externalChannelUtils;
    private final ExternalChannelSendClient externalChannel;
    private final NotificationUtils notificationUtils;
    private final AarUtils aarUtils;
    private final TimelineUtils timelineUtils;

    public ExternalChannelSendHandler(ExternalChannelUtils externalChannelUtils, ExternalChannelSendClient externalChannel, NotificationUtils notificationUtils, AarUtils aarUtils, TimelineUtils timelineUtils) {
        this.externalChannelUtils = externalChannelUtils;
        this.externalChannel = externalChannel;
        this.notificationUtils = notificationUtils;
        this.aarUtils = aarUtils;
        this.timelineUtils = timelineUtils;
    }

    /**
     * Send pec notification to external channel
     * Messaggio con valore legale (PEC)
     */
    public void sendDigitalNotification(NotificationInt notification, LegalDigitalAddressInt digitalAddress, DigitalAddressSource addressSource, Integer recIndex,
                                        int sentAttemptMade) {
        log.debug("Start sendDigitalNotification - iun={} recipientIndex={}", notification.getIun(), recIndex);

        String eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(addressSource)
                        .index(sentAttemptMade)
                        .build()
        );
        
        externalChannel.sendLegalNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), digitalAddress, eventId);
        externalChannelUtils.addSendDigitalNotificationToTimeline(notification, digitalAddress, addressSource, recIndex, sentAttemptMade, eventId);
    }

    /**
     * Send courtesy message to external channel
     * Messaggio senza valore legale (EMAIL, SMS)
     *
     */
    public void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId) {
        log.debug("Start sendCourtesyNotification - iun {} id {}", notification.getIun(), recIndex);
        
        externalChannel.sendCourtesyNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), courtesyAddress, eventId);
        externalChannelUtils.addSendCourtesyMessageToTimeline(notification, courtesyAddress, recIndex, eventId);
    }

    /**
     * Send registered letter to external channel
     * to use when all pec send fails
     * Invio di RACCOMANDATA SEMPLICE quando falliscono tutti i tentativi via PEC
     */
    public void sendNotificationForRegisteredLetter(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex) {
        log.debug("Start sendNotificationForRegisteredLetter - iun={} recipientIndex={}", notification.getIun(), recIndex);
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed){
            String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .build()
            );

            AarGenerationDetails aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);
            
            // la tipologia qui è sempre raccomandata semplice SR
            externalChannel.sendAnalogNotification(
                    notification, 
                    notificationUtils.getRecipientFromIndex(notification,recIndex),
                    physicalAddress, 
                    eventId, 
                    ExternalChannelSendClient.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER,
                    aarGenerationDetails.getGeneratedAarUrl()
            );

            externalChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, physicalAddress, recIndex, eventId, aarGenerationDetails.getNumberOfPages());

            log.info("Registered Letter sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
        }else {
            log.info("Notification is already viewed, registered Letter will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    /**
     * Send paper notification to external channel
     * AR o 890
     *
     */
    public void sendAnalogNotification(NotificationInt notification, PhysicalAddress physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
        log.debug("Start sendAnalogNotification - iun {} id {}", notification.getIun(), recIndex);

        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if( !isNotificationAlreadyViewed ){
            
            String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .index(sentAttemptMade)
                            .build()
            );

            AarGenerationDetails aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

            // c'è una discrepanza nella nomenclatura tra l'enum serviceleveltype.SIMPLE_REGISTERED_LETTER che si traduce in AR e non SR.
            // Cmq se non è 890, si intende AR.
            externalChannel.sendAnalogNotification(
                    notification,
                    notificationUtils.getRecipientFromIndex(notification,recIndex),
                    physicalAddress,
                    eventId,
                    notification.getPhysicalCommunicationType()== ServiceLevelTypeInt.REGISTERED_LETTER_890 ? ExternalChannelSendClient.ANALOG_TYPE.REGISTERED_LETTER_890 : ExternalChannelSendClient.ANALOG_TYPE.AR_REGISTERED_LETTER,
                    aarGenerationDetails.getGeneratedAarUrl()
            );

            externalChannelUtils.addSendAnalogNotificationToTimeline(notification, physicalAddress, recIndex, investigation, sentAttemptMade, eventId, aarGenerationDetails.getNumberOfPages());

            log.info("Registered Letter sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
        } else {
            log.info("Notification is already viewed, paper notification will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

}
