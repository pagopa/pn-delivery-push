package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowUtils;
import it.pagopa.pn.deliverypush.action.utils.*;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.ServiceLevelTypeInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.PhysicalAddressRelatedTimelineElement;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelPrepareRequest;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendClient;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public PaperChannelServiceImpl(PaperChannelUtils paperChannelUtils,
                                   PaperChannelSendClient paperChannelSendClient,
                                   NotificationUtils notificationUtils,
                                   AarUtils aarUtils,
                                   TimelineUtils timelineUtils,
                                   MVPParameterConsumer mvpParameterConsumer,
                                   AnalogWorkflowUtils analogWorkflowUtils) {
        this.paperChannelUtils = paperChannelUtils;
        this.paperChannelSendClient = paperChannelSendClient;
        this.notificationUtils = notificationUtils;
        this.aarUtils = aarUtils;
        this.timelineUtils = timelineUtils;
        this.mvpParameterConsumer = mvpParameterConsumer;
        this.analogWorkflowUtils = analogWorkflowUtils;
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

            log.info("Prepare request for registered Letter sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);
        }else {
            log.info("Notification is already viewed, registered Letter will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
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
                log.info("Paper notification sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);

            }else {
                log.info("Paper message is not handled, paper notification will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
                paperChannelUtils.addPaperNotificationNotHandledToTimeline(notification, recIndex);
            }
        } else {
            log.info("Notification is already viewed, paper notification will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    private void prepareSimpleRegisteredLetter(NotificationInt notification, Integer recIndex) {
        String eventId = paperChannelUtils.buildPrepareSimpleRegisteredLetterEventId(notification, recIndex);

        // nel caso della simple registgered, l'indirizzo è sempre quello fornito dalla pa
        PhysicalAddressInt receiverAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);

        retrievePrepareInfoAndInvoke(notification, recIndex, receiverAddress, eventId,
                PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER, null, null);


        paperChannelUtils.addPrepareSimpleRegisteredLetterToTimeline(notification, receiverAddress, recIndex, eventId, 1);
    }

    private void prepareAnalogDomicile(NotificationInt notification, Integer recIndex, int sentAttemptMade) {
        String eventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade);

        // nel caso sia un ritentativo, vado in cerca del precedente feedback dell'eventuale discovered address
        String relatedEventId = null;
        PhysicalAddressInt receiverAddress;
        PhysicalAddressInt discoveredAddress = null;
        if (sentAttemptMade > 0)
        {
            // ricostruisco il related corrispondente, tanto ha la forma che gli avevo dato io all'iterazione precedente
            // il relatedEventId del primo tentativo serve a paperChannel distinguere e correlare la seconda invocazione dalla prima
            relatedEventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade - 1);

            // ricostruisco il feedback corrispondente, tanto ha la forma che gli avevo dato io all'iterazione precedente.
            // mi serve per recuperare il discoveredAddress all'interno
            String relatedAnalogFeedbackEventId = paperChannelUtils.buildSendAnalogFeedbackEventId(notification, recIndex, sentAttemptMade - 1);

            TimelineElementInternal previousResult = paperChannelUtils.getPaperChannelNotificationTimelineElement(notification.getIun(), relatedAnalogFeedbackEventId);
            discoveredAddress = ((SendAnalogFeedbackDetailsInt)previousResult.getDetails()).getNewAddress();

            // se la relatedrequestid non è nulla, il receiver address è quello usato nella prima send
            String eventIdPreviousSend = paperChannelUtils.buildSendAnalogDomicileEventId(notification, recIndex, sentAttemptMade-1);
            TimelineElementInternal previousSendEvent = paperChannelUtils.getPaperChannelNotificationTimelineElement(notification.getIun(), eventIdPreviousSend);
            
            //PaperChannel ha bisogno per il secondo tentativo dell'indirizzo del primo tentativo
            receiverAddress = ((PhysicalAddressRelatedTimelineElement)previousSendEvent.getDetails()).getPhysicalAddress();
        }
        else
        {
            // se sentAttemptMade è 0, il receiver addres è quello fornito dalla PA
            receiverAddress = analogWorkflowUtils.getPhysicalAddress(notification, recIndex);
        }
        
        retrievePrepareInfoAndInvoke(notification, recIndex, receiverAddress, eventId,
                getAnalogType(notification),
                relatedEventId, discoveredAddress
                );


        paperChannelUtils.addPrepareAnalogNotificationToTimeline(notification, receiverAddress, recIndex, relatedEventId, sentAttemptMade, eventId, discoveredAddress);
    }


    /**
     * Recupera le informazioni necessarie all'invio della prepare.
     * Se il relatedRequestId è null, si intende come prima richiesta, e recupera l'indirizzo della PA fornito.
     * Se  il relatedRequestId NON è null, si intende che è una seconda richiesta non recupera l'indirizzo della PA
     *
     * @param notification notifica
     * @param recIndex indice destinatario
     * @param eventId timelineid (requestId)
     * @param analogType tipo invio
     * @param relatedRequestId eventuale requestId originario
     * @param discoveredAddress eventuale indirizzo indagine
     */
    private void retrievePrepareInfoAndInvoke(NotificationInt notification, Integer recIndex, PhysicalAddressInt receiverAddress,
                                                            String eventId, PhysicalAddressInt.ANALOG_TYPE analogType, String relatedRequestId, PhysicalAddressInt discoveredAddress) {
        // recupero gli allegati
        List<String> attachments = retrieveAttachments(notification, recIndex, analogType== PhysicalAddressInt.ANALOG_TYPE.SIMPLE_REGISTERED_LETTER);

        paperChannelSendClient.prepare (new PaperChannelPrepareRequest(notification,
                notificationUtils.getRecipientFromIndex(notification, recIndex),
                receiverAddress, eventId, analogType,
                attachments, relatedRequestId, discoveredAddress));
    }


    /**
     * Recupera gli allegati della notifica, in base al tipo di invio
     *
     * @param notification notifica
     * @param recIndex indice destinatario
     * @param isSimpleRegisteredLetter tipo invio
     * @return lista id allegati
     */
    @NotNull
    private List<String> retrieveAttachments(NotificationInt notification, Integer recIndex, boolean isSimpleRegisteredLetter) {
        AarGenerationDetailsInt aarGenerationDetails = aarUtils.getAarGenerationDetails(notification, recIndex);

        List<String> attachments = new ArrayList<>();
        attachments.add(0, aarGenerationDetails.getGeneratedAarUrl());
        // nel caso in cui NON sia simple registered letter, devo allegare anche gli atti
        // Da valutare eventuale inserimento condizionato degli allegati della notifica, per ora vien commentato
        // l'invocazione a attachments  addAll (attachmentUtils  getNotificationAttachments )
        return attachments;
    }

    @Override
    public void sendSimpleRegisteredLetter(NotificationInt notification, Integer recIndex, String requestId, PhysicalAddressInt receiverAddress, String productType){
        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed) {
            log.info("Registered Letter sending to paperChannel - iun={} id={}", notification.getIun(), recIndex);

            Integer sendCost = retrieveSendInfoAndInvoke(notification, recIndex, true, requestId, productType, receiverAddress);

            paperChannelUtils.addSendSimpleRegisteredLetterToTimeline(notification, receiverAddress, recIndex, sendCost, productType);
            log.info("Registered Letter sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);
        }
        else {
            log.info("Notification is already viewed before send, registered Letter will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    @Override
    public void sendAnalogNotification(NotificationInt notification, Integer recIndex, int sentAttemptMade,
                                       String prepareRequestId, PhysicalAddressInt receiverAddress, String productType){

        boolean isNotificationAlreadyViewed = timelineUtils.checkNotificationIsAlreadyViewed(notification.getIun(), recIndex);

        if(! isNotificationAlreadyViewed) {
            log.info("Analog notification sending to paperChannel - iun={} id={}", notification.getIun(), recIndex);
            String relatedEventId = null;
            if (sentAttemptMade > 0) {
                relatedEventId = paperChannelUtils.buildPrepareAnalogDomicileEventId(notification, recIndex, sentAttemptMade - 1);
            }

            Integer sendCost = retrieveSendInfoAndInvoke(notification, recIndex, false, prepareRequestId, productType, receiverAddress);

            paperChannelUtils.addSendAnalogNotificationToTimeline(notification, receiverAddress, recIndex, sentAttemptMade, sendCost, relatedEventId, productType);

            log.info("Analog notification sent to paperChannel - iun={} id={}", notification.getIun(), recIndex);
        }
        else {
            log.info("Notification is already viewed before send, paper notification will not be sent to paperChannel - iun={} recipientIndex={}", notification.getIun(), recIndex);
        }
    }

    /**
     * Recupera le informazioni necessarie all'invio della send
     * @param notification notifica
     * @param recIndex indice destinatario
     * @param productType tipo invio
     * @param receiverAddress indirizzo a cui spedire
     *
     */
    private Integer retrieveSendInfoAndInvoke(NotificationInt notification, Integer recIndex, boolean isSimpleRegisteredLetter,
                                                         String prepareRequestId, String productType,
                                                         PhysicalAddressInt receiverAddress) {

        // recupero gli allegati
        List<String> attachments = retrieveAttachments(notification, recIndex, isSimpleRegisteredLetter);


        // IL sender/ar address son impostati a pagopa
        return paperChannelSendClient.send(
                new PaperChannelSendRequest(notification, notificationUtils.getRecipientFromIndex(notification, recIndex),
                        receiverAddress, prepareRequestId, productType, attachments, paperChannelUtils.getSenderAddress(), paperChannelUtils.getSenderAddress()));

    }

    @NotNull
    private PhysicalAddressInt.ANALOG_TYPE getAnalogType(NotificationInt notification) {
        return notification.getPhysicalCommunicationType() == ServiceLevelTypeInt.REGISTERED_LETTER_890 ? PhysicalAddressInt.ANALOG_TYPE.REGISTERED_LETTER_890 : PhysicalAddressInt.ANALOG_TYPE.AR_REGISTERED_LETTER;
    }


}
