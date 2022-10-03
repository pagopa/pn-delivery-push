package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@ConditionalOnProperty( name = "pn.delivery-push.featureflags.externalchannel", havingValue = "new")
public class ExternalChannelServiceImpl implements ExternalChannelService {
    private final ExternalChannelUtils externalChannelUtils;
    private final ExternalChannelSendClient externalChannel;
    private final NotificationUtils notificationUtils;
    private final AarUtils aarUtils;
    private final TimelineUtils timelineUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    
    public ExternalChannelServiceImpl(ExternalChannelUtils externalChannelUtils,
                                      ExternalChannelSendClient externalChannel,
                                      NotificationUtils notificationUtils,
                                      AarUtils aarUtils,
                                      TimelineUtils timelineUtils,
                                      PnDeliveryPushConfigs pnDeliveryPushConfigs, DigitalWorkFlowUtils digitalWorkFlowUtils) {
        this.externalChannelUtils = externalChannelUtils;
        this.externalChannel = externalChannel;
        this.notificationUtils = notificationUtils;
        this.aarUtils = aarUtils;
        this.timelineUtils = timelineUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
    }

    /**
     * Send pec notification to external channel
     * Messaggio con valore legale (PEC)
     * Tramite il sendAlreadyInProgress indica se è il primo tentativo, o se invece è un ritentativo breve
     *  @param notification notitica
     * @param digitalAddress indirizzo
     * @param addressSource sorgente
     * @param recIndex indice destinatario
     * @param sentAttemptMade tentativo
     * @param sendAlreadyInProgress indica se l'invio è già stato eseguito e si sta eseguendo un ritentativo
     * @return
     */
    @Override
    public String sendDigitalNotification(NotificationInt notification,
                                          LegalDigitalAddressInt digitalAddress,
                                          DigitalAddressSourceInt addressSource,
                                          Integer recIndex,
                                          int sentAttemptMade,
                                          boolean sendAlreadyInProgress
    ) {
        String eventId;
        if (!sendAlreadyInProgress)
        {
            log.debug("Start sendDigitalNotification - iun={} recipientIndex={} attempt={}", notification.getIun(), recIndex, sentAttemptMade);

            eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
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
        else
        {
            int progressIndex = digitalWorkFlowUtils.getPreviousTimelineProgress(notification, recIndex, sentAttemptMade, addressSource).size() + 1;

            log.debug("Start sendDigitalNotification for retry - iun={} recipientIndex={} attempt={} progressIndex={}", notification.getIun(), recIndex, sentAttemptMade, progressIndex);

            eventId = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .source(addressSource)
                            .sentAttemptMade(sentAttemptMade)
                            .progressIndex(progressIndex)
                            .build()
            );

            externalChannel.sendLegalNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), digitalAddress, eventId);
            digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(
                    notification, 
                    EventCodeInt.DP00,
                    recIndex,
                    sentAttemptMade, 
                    digitalAddress,
                    addressSource, 
                    false, 
                    null, 
                    Instant.now());

        }

        return eventId;
    }

    /**
     * Send courtesy message to external channel
     * Messaggio senza valore legale (EMAIL, SMS)
     *
     */
    @Override
    public void sendCourtesyNotification(NotificationInt notification, CourtesyDigitalAddressInt courtesyAddress, Integer recIndex, String eventId) {
        log.debug("Start sendCourtesyNotification - iun {} id {}", notification.getIun(), recIndex);
        externalChannel.sendCourtesyNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), courtesyAddress, eventId);
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

        externalChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, physicalAddress, recIndex, eventId, aarGenerationDetails.getNumberOfPages());
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
            
            if( Boolean.FALSE.equals( pnDeliveryPushConfigs.getPaperMessageNotHandled()) ){
                
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
                        .index(sentAttemptMade)
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

        externalChannelUtils.addSendAnalogNotificationToTimeline(notification, physicalAddress, recIndex, investigation, sentAttemptMade, eventId, aarGenerationDetails.getNumberOfPages());
    }
}
