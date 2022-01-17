package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.actions.PecFailSendPaperActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TimelineUtils {

    private final InstantNowSupplier instantNowSupplier;

    public TimelineUtils(InstantNowSupplier instantNowSupplier) {
        this.instantNowSupplier = instantNowSupplier;
    }

    public TimelineElement buildAcceptedRequestTimelineElement(Notification notification, String taxId) {
        log.debug("buildAcceptedRequestTimelineElement - iun {} and id {}", notification.getIun(), taxId);

        return TimelineElement.builder()
                .category(TimelineElementCategory.REQUEST_ACCEPTED)
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
                .details(
                        GetAddressInfo.builder()
                                .taxId(taxId)
                                .source(source)
                                .isAvailable(isAvailable)
                                .attemptDate(instantNowSupplier.get())
                                .build())
                .build();
    }


    public TimelineElement buildDigitaFeedbackTimelineElement(ExtChannelResponse response) {
        log.debug("buildDigitaFeedbackTimelineElement - IUN {} and id {}", response.getIun(), response.getTaxId());

        return TimelineElement.builder()
                .iun(response.getIun())
                .category(TimelineElementCategory.SEND_DIGITAL_FEEDBACK)
                .details(SendDigitalFeedback.builder()
                        .errors(response.getErrorList())
                        .address(response.getDigitalUsedAddress())
                        .responseStatus(response.getResponseStatus())
                        .taxId(response.getTaxId())
                        .notificationDate(response.getNotificationDate())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
                .details(SimpleRegisteredLetterDetails.builder()
                        .taxId(taxId)
                        .address(address)
                        .build())
                .build();
    }


    public TimelineElement buildSendDigitalNotificationTimelineElement(DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient, Notification notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recipient.getTaxId());

        return TimelineElement.builder()
                .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                .iun(notification.getIun())
                .timestamp(instantNowSupplier.get())
                .elementId(eventId)
                .details(SendDigitalDetails.sendBuilder()
                        .taxId(recipient.getTaxId())
                        .retryNumber(sentAttemptMade)
                        .address(digitalAddress)
                        .addressSource(addressSource)
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
                .category(TimelineElementCategory.PUBLIC_REGISTRY_CALL)
                .details(PublicRegistryCallDetails.builder()
                        .taxId(taxId)
                        .contactPhase(contactPhase)
                        .sentAttemptMade(sentAttemptMade)
                        .deliveryMode(deliveryMode)
                        .sendDate(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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
                .timestamp(instantNowSupplier.get())
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

    public TimelineElement buildScheduleDigitalWorkflowTimeline(String iun, String taxId, DigitalAddressInfo lastAttemptInfo) {
        log.debug("buildScheduledActionTimeline - iun {} and id {}", iun, taxId);
        return TimelineElement.builder()
                .category(TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW)
                .timestamp(instantNowSupplier.get())
                .iun(iun)
                .elementId(
                        TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recipientId(taxId)
                                        .build())
                )
                .details(ScheduleDigitalWorkflow.builder()
                        .taxId(taxId)
                        .lastAttemptInfo(lastAttemptInfo)
                        .build()
                )
                .build();
    }

    public TimelineElement buildScheduleAnalogWorkflowTimeline(String iun, String taxId) {
        log.debug("buildScheduleAnalogWorkflowTimeline - iun {} and id {}", iun, taxId);
        return TimelineElement.builder()
                .category(TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW)
                .timestamp(instantNowSupplier.get())
                .iun(iun)
                .elementId(
                        TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recipientId(taxId)
                                        .build())
                )
                .details(ScheduleAnalogWorkflow.builder()
                        .taxId(taxId)
                        .build()
                )
                .build();
    }

    public TimelineElement buildScheduleRefinement(String iun, String taxId) {
        log.debug("buildScheduleRefinement - iun {} and id {}", iun, taxId);
        return TimelineElement.builder()
                .category(TimelineElementCategory.SCHEDULE_REFINEMENT)
                .timestamp(instantNowSupplier.get())
                .iun(iun)
                .elementId(
                        TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                                EventId.builder()
                                        .iun(iun)
                                        .recipientId(taxId)
                                        .build())
                )
                .details(ScheduleRefinement.builder()
                        .taxId(taxId)
                        .build()
                )
                .build();
    }
}
