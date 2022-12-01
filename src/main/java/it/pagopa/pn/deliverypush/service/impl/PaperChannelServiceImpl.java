package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.AttachmentUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PaperChannelServiceImpl implements PaperChannelService {
    private final PaperChannelUtils paperChannelUtils;
    private final PaperChannelSendClient paperChannelSendClient;
    private final NotificationUtils notificationUtils;
    private final AarUtils aarUtils;
    private final TimelineUtils timelineUtils;
    private final MVPParameterConsumer mvpParameterConsumer;
    private final AnalogWorkflowUtils analogWorkflowUtils;
    private final AttachmentUtils attachmentUtils;

    public PaperChannelServiceImpl(PaperChannelUtils paperChannelUtils,
                                   PaperChannelSendClient paperChannelSendClient,
                                   NotificationUtils notificationUtils,
                                   AarUtils aarUtils,
                                   TimelineUtils timelineUtils,
                                   MVPParameterConsumer mvpParameterConsumer,
                                   AnalogWorkflowUtils analogWorkflowUtils,
                                   AttachmentUtils attachmentUtils) {
        this.paperChannelUtils = paperChannelUtils;
        this.paperChannelSendClient = paperChannelSendClient;
        this.notificationUtils = notificationUtils;
        this.aarUtils = aarUtils;
        this.timelineUtils = timelineUtils;
        this.mvpParameterConsumer = mvpParameterConsumer;
        this.analogWorkflowUtils = analogWorkflowUtils;
        this.attachmentUtils = attachmentUtils;
    }


    /**
     * Send registered letter to external channel
     * to use when all pec send fails
     * Invio di RACCOMANDATA SEMPLICE quando falliscono tutti i tentativi via PEC
     */
    @Override
    public void prepareAnalogNotificationForSimpleRegisteredLetter(NotificationInt notification,  Integer recIndex) {
        log.debug("Start sendNotificationForRegisteredLetter - iun={} recipientIndex={}", notification.getIun(), recIndex);
                 
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed){

            prepareSimpleRegisteredLetter(notification, recIndex);

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
    @Override
    public void prepareAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        log.debug("Start prepareAnalogNotification - iun {} id {}", notification.getIun(), recIndex);

        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if( !isNotificationAlreadyViewed ){
            String senderTaxId = notification.getSender().getPaTaxId();

            if( Boolean.FALSE.equals( mvpParameterConsumer.isMvp( senderTaxId ) ) ){

                prepareAnalogDomicile(notification, recIndex, sentAttemptMade);
                log.info("Paper notification sent to externalChannel - iun {} id {}", notification.getIun(), recIndex);

            }else {
                log.info("Paper message is not handled, paper notification will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
                paperChannelUtils.addPaperNotificationNotHandledToTimeline(notification, recIndex);
            }
        } else {
            log.info("Notification is already viewed, paper notification will not be sent to externalChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    private void prepareSimpleRegisteredLetter(NotificationInt notification, Integer recIndex) {
        String eventId = paperChannelUtils.buildPrepareSimpleRegisteredLetterEventId(notification, recIndex);

        PhysicalAddressInt paProvidedAddress = retrievePrepareInfoAndInvoke(notification, recIndex, eventId,
                PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER, null, null);


        paperChannelUtils.addPrepareSimpleRegisteredLetterToTimeline(notification, paProvidedAddress, recIndex, eventId, 1);
    }

    private void prepareAnalogDomicile(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        String eventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade);

        // nel caso sia un ritentativo, vado in cerca del precedente feedback dell'eventuale discovered address
        String relatedEventId = null;
        PhysicalAddressInt discoveredAddress = null;
        if (sentAttemptMade > 0)
        {
            // ricostruisco il related corrispondente, tanto ha la forma che gli avevo dato io all'iterazione precedente
            relatedEventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade - 1);

            // ricostruisco il feedback corrispondente, tanto ha la forma che gli avevo dato io all'iterazione precedente.
            // mi serve per recuperare il discoveredAddress all'interno
            String relatedAnalogFeedbackEventId = paperChannelUtils.buildSendAnalogFeedbackEventId(notification, recIndex, sentAttemptMade - 1);

            TimelineElementInternal previousResult = paperChannelUtils.getPaperChannelNotificationTimelineElement(notification.getIun(), relatedAnalogFeedbackEventId);
            discoveredAddress = ((SendAnalogFeedbackDetailsInt)previousResult.getDetails()).getNewAddress();
        }


        // c'è una discrepanza nella nomenclatura tra l'enum serviceleveltype.SIMPLE_REGISTERED_LETTER che si traduce in AR e non SR.
        // Cmq se non è 890, si intende AR.
        PhysicalAddressInt paProvidedAddress = retrievePrepareInfoAndInvoke(notification, recIndex, eventId,
                getAnalogType(notification),
                relatedEventId, discoveredAddress
                );


        paperChannelUtils.addPrepareAnalogNotificationToTimeline(notification, paProvidedAddress, recIndex, relatedEventId, sentAttemptMade, eventId);
    }


    /**
     * Recupera le informazioni necessarie all'invio della prepare
     * @param notification notifica
     * @param recIndex indice destinatario
     * @param eventId timelineid (requestId)
     * @param analogType tipo invio
     * @param relatedRequestId eventuale requestId originario
     * @param discoveredAddress eventuale indirizzo indagine
     * @return indirizzo
     */
    private PhysicalAddressInt retrievePrepareInfoAndInvoke(NotificationInt notification, Integer recIndex,
                                                            String eventId, PhysicalAddressInt.ANALOG_TYPE analogType, String relatedRequestId, PhysicalAddressInt discoveredAddress) {
        AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

        PhysicalAddressInt paProvidedAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);

        // recupero tutti gli allegati da inviare, ai quali aggiungo l'AAR
        List<String> attachments = attachmentUtils.getNotificationAttachments(notification, recIndex);
        attachments.add(0, aarGenerationDetails.getGeneratedAarUrl());



        paperChannelSendClient.prepare(new PaperChannelPrepareRequest(notification,
                notificationUtils.getRecipientFromIndex(notification, recIndex),
                paProvidedAddress, eventId, analogType,
                attachments, relatedRequestId, discoveredAddress));

        return paProvidedAddress;
    }

    public void sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex, String requestId, PhysicalAddressInt receiverAddress){

        Integer sendCost = retrieveSendInfoAndInvoke(notification, recIndex, requestId, getAnalogType(notification), receiverAddress);

        paperChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, receiverAddress, recIndex,   requestId, sendCost);
    }

    public void sendAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade,
                                       String requestId, PhysicalAddressInt receiverAddress){

        String relatedEventId = null;
        if (sentAttemptMade > 0)
        {
            relatedEventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade - 1);
        }

        Integer sendCost = retrieveSendInfoAndInvoke(notification, recIndex, requestId, getAnalogType(notification), receiverAddress);

        paperChannelUtils.addSendAnalogNotificationToTimeline(notification, receiverAddress, recIndex,   requestId, sentAttemptMade, sendCost, relatedEventId);
    }

    /**
     * Recupera le informazioni necessarie all'invio della send
     * @param notification notifica
     * @param recIndex indice destinatario
     * @param eventId timelineid (requestId)
     * @param analogType tipo invio
     * @param receiverAddress indirizzo a cui spedire
     *
     */
    private Integer retrieveSendInfoAndInvoke(NotificationInt notification, Integer recIndex,
                                                         String eventId, PhysicalAddressInt.ANALOG_TYPE analogType,
                                                         PhysicalAddressInt receiverAddress) {

        AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

        // recupero tutti gli allegati da inviare, ai quali aggiungo l'AAR
        List<String> attachments = attachmentUtils.getNotificationAttachments(notification, recIndex);
        attachments.add(0, aarGenerationDetails.getGeneratedAarUrl());

        // FIXME: come recuperare arAddress e senderAddress?
        return paperChannelSendClient.send(
                new PaperChannelSendRequest(notification, notificationUtils.getRecipientFromIndex(notification, recIndex),
                        receiverAddress, eventId, analogType, attachments, null, null));

    }

    @NotNull
    private PhysicalAddressInt.ANALOG_TYPE getAnalogType(NotificationInt notification) {
        return notification.getPhysicalCommunicationType() == ServiceLevelTypeInt.REGISTERED_LETTER_890 ? PhysicalAddressInt.ANALOG_TYPE.REGISTERED_LETTER_890 : PhysicalAddressInt.ANALOG_TYPE.AR_REGISTERED_LETTER;
    }
}
