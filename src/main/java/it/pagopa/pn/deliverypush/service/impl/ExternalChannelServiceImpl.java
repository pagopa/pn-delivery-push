package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowUtils;
import it.pagopa.pn.deliverypush.action.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressFeedback;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.externalchannel.ExternalChannelSendClient;
import it.pagopa.pn.deliverypush.service.ExternalChannelService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
public class ExternalChannelServiceImpl implements ExternalChannelService {
    private final ExternalChannelUtils externalChannelUtils;
    private final ExternalChannelSendClient externalChannel;
    private final NotificationUtils notificationUtils;
    private final DigitalWorkFlowUtils digitalWorkFlowUtils;
    private final NotificationService notificationService;
    
    public ExternalChannelServiceImpl(ExternalChannelUtils externalChannelUtils,
                                      ExternalChannelSendClient externalChannel,
                                      NotificationUtils notificationUtils,
                                      DigitalWorkFlowUtils digitalWorkFlowUtils,
                                      NotificationService notificationService) {
        this.externalChannelUtils = externalChannelUtils;
        this.externalChannel = externalChannel;
        this.notificationUtils = notificationUtils;
        this.digitalWorkFlowUtils = digitalWorkFlowUtils;
        this.notificationService = notificationService;
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
     * @return eventId relativo al SEND_DIGITAL_DOMICILE
     */
    @Override
    public String sendDigitalNotification(NotificationInt notification,
                                          LegalDigitalAddressInt digitalAddress,
                                          DigitalAddressSourceInt addressSource,
                                          Integer recIndex,
                                          int sentAttemptMade,
                                          boolean sendAlreadyInProgress
    ) {

        String aarKey = externalChannelUtils.getAarKey(notification.getIun(), recIndex);
        NotificationRecipientInt recipientFromIndex = notificationUtils.getRecipientFromIndex(notification, recIndex);
        Map<String, String> recipientsQuickAccessLinkTokens = notificationService.getRecipientsQuickAccessLinkToken(notification.getIun());
        recipientFromIndex.setQuickAccessLinkToken(recipientsQuickAccessLinkTokens.get(recipientFromIndex.getInternalId()));

        String eventId;
        if (!sendAlreadyInProgress)
        {
            log.debug("Start sendDigitalNotification - iun={} recipientIndex={} attempt={}", notification.getIun(), recIndex, sentAttemptMade);

            eventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                    EventId.builder()
                            .iun(notification.getIun())
                            .recIndex(recIndex)
                            .source(addressSource)
                            .sentAttemptMade(sentAttemptMade)
                            .build()
            );

            externalChannel.sendLegalNotification(notification, recipientFromIndex, digitalAddress, eventId, aarKey);
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

            externalChannel.sendLegalNotification(notification, recipientFromIndex, digitalAddress, eventId, aarKey);

            DigitalAddressFeedback digitalAddressFeedback = DigitalAddressFeedback.builder()
                    .retryNumber(sentAttemptMade)
                    .eventTimestamp(Instant.now())
                    .digitalAddressSource(addressSource)
                    .digitalAddress(digitalAddress)
                    .build();
            
            digitalWorkFlowUtils.addDigitalDeliveringProgressTimelineElement(
                    notification, 
                    EventCodeInt.DP00,
                    recIndex,
                    false, 
                    null,
                    digitalAddressFeedback);

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
        String aarKey = externalChannelUtils.getAarKey(notification.getIun(), recIndex);
        externalChannel.sendCourtesyNotification(notification, notificationUtils.getRecipientFromIndex(notification,recIndex), courtesyAddress, eventId, aarKey);
    }

}
