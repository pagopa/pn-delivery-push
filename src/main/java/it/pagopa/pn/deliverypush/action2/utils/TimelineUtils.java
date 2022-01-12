package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.actions.PecFailSendPaperActionHandler;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
public class TimelineUtils {

    public TimelineElement buildAcceptedRequestTimelineElement(Notification notification, String taxId) {
        log.debug("buildAcceptedRequestTimelineElement - iun {} and id {}", notification.getIun(), taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .timestamp(Instant.now())
                .iun(notification.getIun())
                .elementId(TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                        EventId.builder()
                                .iun(notification.getIun())
                                .recipientId(taxId)
                                .build()))
                .details(ReceivedDetails.builder()
                        .recipients(notification.getRecipients())
                        .documentsDigests(notification.getDocuments()
                                .stream()
                                .map(NotificationAttachment::getDigests)
                                .collect(Collectors.toList())
                        )
                        .build()
                )
                .build();
    }

    public TimelineElement buildAvailabilitySourceTimelineElement(String taxId, String iun, DigitalAddressSource source, boolean isAvailable, int sentAttemptMade) {
        log.debug("buildAvailabilitySourceTimelineElement - IUN {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.GET_ADDRESS)
                .elementId(
                        TimelineEventId.GET_ADDRESS.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recipientId(taxId)
                                        .source(source)
                                        .index(sentAttemptMade)
                                        .build()
                        )
                )
                .iun(iun)
                .timestamp(Instant.now())
                .details(
                        GetAddressInfo.builder()
                                .taxId(taxId)
                                .source(source)
                                .isAvailable(isAvailable)
                                .attemptDate(Instant.now())
                                .build())
                .build();
    }


    public TimelineElement buildDigitalFailureAttemptTimelineElement(ExtChannelResponse response) {
        log.debug("buildDigitalFailureAttemptTimelineElement - IUN {} and id {}", response.getIun(), response.getTaxId());

        return TimelineElement.builder()
                .iun(response.getIun())
                .category(TimelineElementCategory.SEND_DIGITAL_FEEDBACK_FAILURE)
                .details(SendDigitalFeedbackFailure.builder()
                        .errors(response.getErrorList())
                        .address(response.getDigitalUsedAddress())
                        .taxId(response.getTaxId())
                        .build())
                .build();
    }


    public TimelineElement buildSendCourtesyMessageTimelineElement(String taxId, String iun, DigitalAddress address, Instant sendDate, String eventId) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN {} and id {}", iun, taxId);

        //Viene aggiunto l'invio alla timeline con un particolare elementId utile a ottenere tali elementi successivamente nel workflow (Start analog workflow)
        return TimelineElement.builder()
                .category(TimelineElementCategory.SEND_COURTESY_MESSAGE)
                .elementId(eventId)
                .iun(iun)
                .timestamp(Instant.now())
                .details(SendCourtesyMessageDetails.builder()
                        .taxId(taxId)
                        .address(address)
                        .sendDate(sendDate)
                        .build())
                .build();
    }


    public TimelineElement buildSendSimpleRegisteredLetterTimelineElement(String taxId, String iun, PhysicalAddress address, String eventId) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN {} and id {}", iun, taxId);

        //Viene aggiunto l'invio alla timeline con un particolare elementId utile a ottenere tali elementi successivamente nel workflow (Start analog workflow)
        return TimelineElement.builder()
                .category(TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER)
                .elementId(eventId)
                .iun(iun)
                .timestamp(Instant.now())
                .details(SimpleRegisteredLetterDetails.builder()
                        .taxId(taxId)
                        .address(address)
                        .build())
                .build();
    }


    public TimelineElement buildSendDigitalNotificationTimelineElement(DigitalAddress digitalAddress, NotificationRecipient recipient, Notification notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recipient.getTaxId());

        return TimelineElement.builder()
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .iun(notification.getIun())
                .timestamp(Instant.now())
                .elementId(eventId)
                .details(SendDigitalDetails.sendBuilder()
                        .taxId(recipient.getTaxId())
                        .retryNumber(sentAttemptMade)
                        .address(digitalAddress)
                        .build()
                )
                .build();
    }


    public TimelineElement buildSendAnalogNotificationTimelineElement(PhysicalAddress address, NotificationRecipient recipient, Notification notification, boolean investigation,
                                                                      int sentAttemptMade, String eventId) {
        log.debug("buildSendAnalogNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recipient.getTaxId());

        return TimelineElement.builder()
                .category(TimelineElementCategory.SEND_ANALOG_DOMICILE)
                .iun(notification.getIun())
                .timestamp(Instant.now())
                .elementId(eventId)
                .details(SendPaperDetails.builder()
                        .taxId(recipient.getTaxId())
                        .address(address)
                        .serviceLevel(notification.getPhysicalCommunicationType())
                        .sentAttemptMade(sentAttemptMade)
                        .investigation(investigation)
                        .build()
                ).build();
    }


    public TimelineElement buildSuccessDigitalWorkflowTimelineElement(String taxId, String iun, DigitalAddress address) {
        log.debug("buildSuccessDigitalWorkflowTimelineElement - IUN {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW)
                .iun(iun)
                .elementId(TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())
                )
                .timestamp(Instant.now())
                .details(DigitalSuccessWorkflow.builder()
                        .taxId(taxId)
                        .address(address)
                        .build())
                .build();
    }


    public TimelineElement buildFailureDigitalWorkflowTimelineElement(String taxId, String iun) {
        log.debug("buildFailureDigitalWorkflowTimelineElement - IUN {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW)
                .iun(iun)
                .elementId(TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())
                )
                .timestamp(Instant.now())
                .details(DigitalFailureWorkflow.builder()
                        .taxId(taxId)
                        .build())
                .build();
    }


    public TimelineElement buildSuccessAnalogWorkflowTimelineElement(String taxId, String iun, PhysicalAddress address) {
        log.debug("buildSuccessAnalogWorkflowTimelineElement - iun {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW)
                .iun(iun)
                .elementId(TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())
                )
                .timestamp(Instant.now())
                .details(AnalogSuccessWorkflow.builder()
                        .taxId(taxId)
                        .address(address)
                        .build())
                .build();
    }


    public TimelineElement buildFailureAnalogWorkflowTimelineElement(String taxId, String iun) {
        log.debug("buildFailureAnalogWorkflowTimelineElement - iun {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.ANALOG_FAILURE_WORKFLOW)
                .iun(iun)
                .elementId(TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build())
                )
                .timestamp(Instant.now())
                .details(AnalogFailureWorkflow.builder()
                        .taxId(taxId)
                        .build())
                .build();
    }


    public TimelineElement buildPublicRegistryResponseCallTimelineElement(String iun, String taxId, PublicRegistryResponse response) {
        log.debug("buildPublicRegistryResponseCallTimelineElement - iun {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .iun(iun)
                .elementId(response.getCorrelationId())
                .category(TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE)
                .timestamp(Instant.now())
                .details(PublicRegistryResponseDetails.builder()
                        .taxId(taxId)
                        .digitalAddress(response.getDigitalAddress())
                        .physicalAddress(response.getPhysicalAddress())
                        .build())
                .build();
    }


    public TimelineElement buildPublicRegistryCallTimelineElement(String iun, String taxId, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        log.debug("buildPublicRegistryCallTimelineElement - iun {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .iun(iun)
                .elementId(eventId)
                .category(TimelineElementCategory.PUBLIC_REGISTRY_CALL)
                .details(PublicRegistryCallDetails.builder()
                        .taxId(taxId)
                        .contactPhase(contactPhase)
                        .sentAttemptMade(sentAttemptMade)
                        .deliveryMode(deliveryMode)
                        .build())
                .build();
    }


    public TimelineElement buildAnalogFailureAttemptTimelineElement(ExtChannelResponse response, String taxId, int sentAttemptMade) {
        log.debug("buildAnalogFailureAttemptTimelineElement - iun {} and id {}", response.getIun(), response.getTaxId());

        String iun = response.getIun();

        return TimelineElement.builder()
                .category(TimelineElementCategory.SEND_PAPER_FEEDBACK)
                .elementId(TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .index(sentAttemptMade)
                                .build()
                ))
                .iun(iun)
                .timestamp(Instant.now())
                .details(new SendPaperFeedbackDetails(
                        SendPaperDetails.builder()
                                .taxId(taxId)
                                .address(response.getAnalogUsedAddress())
                                .sentAttemptMade(sentAttemptMade)
                                .serviceLevel(PecFailSendPaperActionHandler.DIGITAL_FAILURE_PAPER_FALLBACK_SERVICE_LEVEL) //TODO Capirne il senso
                                .build(),
                        response.getAnalogNewAddressFromInvestigation(),
                        response.getAttachmentKeys(),
                        response.getErrorList()
                ))
                .build();
    }

    public TimelineElement buildNotificationViewedTimelineElement(String iun, String taxId) {
        log.debug("buildNotificationViewedTimelineElement - iun {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.NOTIFICATION_VIEWED)
                .timestamp(Instant.now())
                .iun(iun)
                .elementId(TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build()))
                .details(NotificationViewedDetails.builder()
                        .taxId(taxId)
                        .build()
                )
                .build();
    }


    public TimelineElement buildCompletelyUnreachableTimelineElement(String iun, String taxId) {
        log.debug("buildCompletelyUnreachableTimelineElement - iun {} and id {}", iun, taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.COMPLETELY_UNREACHABLE)
                .timestamp(Instant.now())
                .iun(iun)
                .elementId(TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build()))
                .details(CompletlyUnreachableDetails.builder()
                        .taxId(taxId)
                        .build()
                )
                .build();
    }

    public TimelineElement buildRefinementTimelineElement(String iun, String taxId) {
        log.debug("buildRefinementTimelineElement - iun {} and id {}", iun, taxId);
        return TimelineElement.builder()
                .category(TimelineElementCategory.REFINEMENT)
                .timestamp(Instant.now())
                .iun(iun)
                .elementId(TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .build()))
                .details(RefinementDetails.builder()
                        .taxId(taxId)
                        .build()
                )
                .build();
    }

}
