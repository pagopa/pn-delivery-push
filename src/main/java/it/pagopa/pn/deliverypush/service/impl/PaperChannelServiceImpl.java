package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaperChannelServiceImpl implements PaperChannelService {
    private final PaperChannelUtils paperChannelUtils;
    private final ExternalChannelSendClient externalChannel;
    private final NotificationUtils notificationUtils;
    private final AarUtils aarUtils;
    private final TimelineUtils timelineUtils;
    private final MVPParameterConsumer mvpParameterConsumer;
    private final ExternalChannelUtils externalChannelUtils;

    public PaperChannelServiceImpl(PaperChannelUtils paperChannelUtils,
                                   ExternalChannelSendClient externalChannel,
                                   NotificationUtils notificationUtils,
                                   AarUtils aarUtils,
                                   TimelineUtils timelineUtils,
                                   MVPParameterConsumer mvpParameterConsumer,
                                   ExternalChannelUtils externalChannelUtils) {
        this.paperChannelUtils = paperChannelUtils;
        this.externalChannel = externalChannel;
        this.notificationUtils = notificationUtils;
        this.aarUtils = aarUtils;
        this.timelineUtils = timelineUtils;
        this.mvpParameterConsumer = mvpParameterConsumer;
        this.externalChannelUtils = externalChannelUtils;
    }


    /**
     * Send registered letter to external channel
     * to use when all pec send fails
     * Invio di RACCOMANDATA SEMPLICE quando falliscono tutti i tentativi via PEC
     */
    @Override
    public void sendNotificationForRegisteredLetter(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex) {
        log.debug("Start sendNotificationForRegisteredLetter - iun={} recipientIndex={}", notification.getIun(), recIndex);
                 
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed){

            sendRegisteredLetterToExternalChannel(notification, physicalAddress, recIndex);

            log.info("Registered Letter sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
        }else {
            log.info("Notification is already viewed, registered Letter will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    private void sendRegisteredLetterToExternalChannel(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex) {
        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

        // la tipologia qui è sempre raccomandata semplice SR
        externalChannel.sendAnalogNotification(
                notification,
                notificationUtils.getRecipientFromIndex(notification, recIndex),
                physicalAddress,
                eventId,
                PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER,
                aarGenerationDetails.getGeneratedAarUrl()
        );

        paperChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, physicalAddress, recIndex, eventId, aarGenerationDetails.getNumberOfPages());
    }

    /**
     * Send paper notification to external channel
     * AR o 890
     *
     */
    @Override
    public void sendAnalogNotification(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
        log.debug("Start sendAnalogNotification - iun {} id {}", notification.getIun(), recIndex);
        
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if( !isNotificationAlreadyViewed ){
            String senderTaxId = notification.getSender().getPaTaxId();
            
            if( Boolean.FALSE.equals( mvpParameterConsumer.isMvp( senderTaxId ) ) ){
                
                sendAnalogToExternalChannel(notification, physicalAddress, recIndex, investigation, sentAttemptMade);
                log.info("Paper notification sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);
                
            }else {
                log.info("Paper message is not handled, paper notification will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
                externalChannelUtils.addPaperNotificationNotHandledToTimeline(notification, recIndex);
            }
        } else {
            log.info("Notification is already viewed, paper notification will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    private void sendAnalogToExternalChannel(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, boolean investigation, int sentAttemptMade) {
        String eventId = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );

        AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

        // c'è una discrepanza nella nomenclatura tra l'enum serviceleveltype.SIMPLE_REGISTERED_LETTER che si traduce in AR e non SR.
        // Cmq se non è 890, si intende AR.
        externalChannel.sendAnalogNotification(
                notification,
                notificationUtils.getRecipientFromIndex(notification, recIndex),
                physicalAddress,
                eventId,
                notification.getPhysicalCommunicationType()== ServiceLevelTypeInt.REGISTERED_LETTER_890 ? PhysicalAddressInt.ANALOG_TYPE.REGISTERED_LETTER_890 : PhysicalAddressInt.ANALOG_TYPE.AR_REGISTERED_LETTER,
                aarGenerationDetails.getGeneratedAarUrl()
        );

        paperChannelUtils.addSendAnalogNotificationToTimeline(notification, physicalAddress, recIndex, investigation, sentAttemptMade, eventId, aarGenerationDetails.getNumberOfPages());
    }
}
