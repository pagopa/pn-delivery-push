package it.pagopa.pn.deliverypush.actions2;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.api.dto.extchannel.ExtChannelDigitalResponse;
import it.pagopa.pn.api.dto.extchannel.ExternalChannelAnalogResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.actions.PecFailSendPaperActionHandler;

import java.time.Instant;

public class ExternalChannelImpl implements ExternalChannel {
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    private AnalogWorkflowHandler analogWorkflowHandler;
    private TimelineService timelineService;
    private CompletionWorkFlow completionWorkflow;

    @Override
    public void sendDigitalNotification(Notification notification, DigitalAddress digitalAddress, String iun, NotificationRecipient recipient) {
        PnExtChnPecEvent pnExtChnPecEvent = buildSendPecRequest(iun, notification, recipient, digitalAddress, null);
        //TODO Implementare invio verso ExternalChannel
        addSendDigitalNotificationToTimeline(digitalAddress, recipient);
    }

    public PnExtChnPecEvent buildSendPecRequest(String iun, Notification notification,
                                                NotificationRecipient recipient, DigitalAddress digitalAddress, PnDeliveryPushConfigs cfg) {
        final String accessUrl = null; //getAccessUrl(recipient, cfg); //TODO CAPIRE Cosa è l'access URL e a cosa serve
        return PnExtChnPecEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(iun)
                        .eventId(null) //TODO Capire a cosa serve, veniva inserito l'actionId, quindi capire cosa inserire se non si ha una action 
                        .eventType(EventType.SEND_PEC_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
                        .build()
                )
                .payload(PnExtChnPecEventPayload.builder()
                        .iun(notification.getIun())
                        .requestCorrelationId(null) //TODO Capire cosa inserire, veniva inserito actionId
                        .recipientTaxId(recipient.getTaxId())
                        .recipientDenomination(recipient.getDenomination())
                        .senderId(notification.getSender().getPaId())
                        .senderDenomination(notification.getSender().getPaId())
                        .senderPecAddress("Not required")
                        .pecAddress(digitalAddress.getAddress())
                        .shipmentDate(notification.getSentAt())
                        .accessUrl(accessUrl)
                        .build()
                )
                .build();
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

    @Override
    public void sendNotificationForRegisteredLetter(Notification notification, String address) {
        //TODO DA Implementare
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendAnalogNotification(Notification notification, PhysicalAddress physicalAddress, String iun, NotificationRecipient recipient, boolean investigation) {
        buildSendPaperRequest(iun, notification, recipient, CommunicationType.RECIEVED_DELIVERY_NOTICE, notification.getPhysicalCommunicationType(), investigation, physicalAddress, null);
        //TODO Effettuare invio verso external channel
        addSendAnalogNotificationToTimeline(iun, physicalAddress, recipient, notification, investigation);
    }

    public PnExtChnPaperEvent buildSendPaperRequest(
            String iun,
            Notification notification,
            NotificationRecipient recipient,
            CommunicationType communicationType,
            ServiceLevelType serviceLevelType,
            boolean investigation,
            PhysicalAddress address,
            PnDeliveryPushConfigs cfg
    ) {
        final String accessUrl = null; //TODO Capire come valorizzarlo nella maniera corretta

        return PnExtChnPaperEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(iun)
                        .eventId(null) //TODO Capire come valorizzare nella maniera corretta
                        .eventType(EventType.SEND_PAPER_REQUEST.name())
                        .publisher(EventPublisher.DELIVERY_PUSH.name())
                        .createdAt(Instant.now())
                        .build()
                )
                .payload(PnExtChnPaperEventPayload.builder()
                        .iun(iun)
                        .requestCorrelationId(null) //TODO Capire come valorizzare nella maniera corretta
                        .destinationAddress(address)
                        .recipientDenomination(recipient.getDenomination())
                        .communicationType(communicationType) //TODO Capire come valorizzarlo, al momento con CommunicationType.RECIEVED_DELIVERY_NOTICE, cosa indica
                        .serviceLevel(serviceLevelType)
                        .senderDenomination(notification.getSender().getPaId())
                        .investigation(investigation)
                        .accessUrl(accessUrl)
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


    /**
     * Handle Digital notification response from external channel. Positive response means notification is delivered correctly, so the workflow can be completed successfully,
     * negative response means notification could not be delivered to the indicated address so need to start next workflow action.
     *
     * @param response Notification response
     */
    public void extChannelResponseReceiverForDigital(ExtChannelDigitalResponse response) {
        //Conservare ricevuta PEC //TODO capire cosa si intende

        switch (response.getResponseStatus()) {
            case OK:
                addSuccessWorkflowToTimeline(response.getTaxId(), response.getIun());
                completionWorkflow.completionDigitalWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                addDigitalFailureAttemptToTimeline(response);
                digitalWorkFlowHandler.nextWorkFlowAction(response.getIun(), response.getTaxId());
                break;
        }
    }

    public void extChannelResponseReceiverForAnalog(ExternalChannelAnalogResponse response) {

        switch (response.getResponseStatus()) {
            case OK:
                addSuccessWorkflowToTimeline(response.getTaxId(), response.getIun());
                completionWorkflow.completionAnalogWorkflow(response.getTaxId(), response.getIun(), response.getNotificationDate(), EndWorkflowStatus.SUCCESS);
                break;
            case KO:
                addAnalogFailureAttemptToTimeline(response);
                analogWorkflowHandler.analogWorkflowHandler(response.getIun(), response.getTaxId());
                break;
        }
    }

    private void addDigitalFailureAttemptToTimeline(ExtChannelDigitalResponse response) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .iun(response.getIun())
                .category(TimelineElementCategory.SEND_DIGITAL_FEEDBACK_FAILURE)
                .details(SendDigitalFeedbackFailure.builder()
                        .downstreamId(response.getDownstreamId())
                        .errors(response.getErrorList())
                        .address(response.getAddress())
                        .taxId(response.getTaxId())
                        .build())
                .build());
    }

    private void addAnalogFailureAttemptToTimeline(ExternalChannelAnalogResponse response) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SEND_PAPER_FEEDBACK)
                .details(new SendPaperFeedbackDetails(
                        SendPaperDetails.builder()
                                .taxId(response.getTaxId())
                                .address(response.getUsedAddress())
                                .serviceLevel(PecFailSendPaperActionHandler.DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL) //TODO Capirne il senso
                                .build(),
                        response.getNewAddressFromInvestigation(),
                        response.getAttachmentKeys(),
                        response.getErrorList()

                ))
                .build());
    }

    private void addSuccessWorkflowToTimeline(String taxId, String iun) {
        timelineService.addTimelineElement(TimelineElement.builder()
                .iun(iun)
                .category(TimelineElementCategory.SUCCESS_WORKFLOW)
                .details(GetAddressInfo.builder()
                        .taxId(taxId)
                        .build())
                .build());
    }
}
