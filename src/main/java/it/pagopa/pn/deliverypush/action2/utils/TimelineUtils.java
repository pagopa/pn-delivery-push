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

    public TimelineElement buildTimeline(String iun, TimelineElementCategory category, String elementId, TimelineElementDetails details) {
        return TimelineElement.builder()
                .category(category)
                .timestamp(instantNowSupplier.get())
                .iun(iun)
                .elementId(elementId)
                .details(details)
                .build();
    }

    public TimelineElement buildAcceptedRequestTimelineElement(Notification notification, String taxId) {
        log.debug("buildAcceptedRequestTimelineElement - iun {} and id {}", notification.getIun(), taxId);

        String elementId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recipientId(taxId)
                        .build());

        ReceivedDetails details = ReceivedDetails.builder()
                .recipients(notification.getRecipients())
                .documentsDigests(notification.getDocuments()
                        .stream()
                        .map(NotificationAttachment::getDigests)
                        .collect(Collectors.toList())
                )
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.REQUEST_ACCEPTED, elementId, details);
    }

    public TimelineElement buildAvailabilitySourceTimelineElement(String taxId, String iun, DigitalAddressSource source, boolean isAvailable, int sentAttemptMade) {
        log.debug("buildAvailabilitySourceTimelineElement - IUN {} and id {}", iun, taxId);

        String elementId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .source(source)
                        .index(sentAttemptMade)
                        .build()
        );

        GetAddressInfo details = GetAddressInfo.builder()
                .taxId(taxId)
                .source(source)
                .isAvailable(isAvailable)
                .attemptDate(instantNowSupplier.get())
                .build();

        return buildTimeline(iun, TimelineElementCategory.GET_ADDRESS, elementId, details);
    }


    public TimelineElement buildDigitaFeedbackTimelineElement(ExtChannelResponse response, SendDigitalDetails sendDigitalDetails) {
        log.debug("buildDigitaFeedbackTimelineElement - IUN {} and id {}", response.getIun(), sendDigitalDetails.getTaxId());

        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(response.getIun())
                        .recipientId(sendDigitalDetails.getTaxId())
                        .index(sendDigitalDetails.getRetryNumber())
                        .build()
        );

        SendDigitalFeedback details = SendDigitalFeedback.builder()
                .errors(response.getErrorList())
                .address(sendDigitalDetails.getAddress())
                .responseStatus(response.getResponseStatus())
                .taxId(sendDigitalDetails.getTaxId())
                .notificationDate(response.getNotificationDate())
                .build();

        return buildTimeline(response.getIun(), TimelineElementCategory.SEND_DIGITAL_FEEDBACK, elementId, details);
    }

    public TimelineElement buildSendCourtesyMessageTimelineElement(String taxId, String iun, DigitalAddress address, Instant sendDate, String eventId) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN {} and id {}", iun, taxId);

        SendCourtesyMessageDetails details = SendCourtesyMessageDetails.builder()
                .taxId(taxId)
                .address(address)
                .sendDate(sendDate)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_COURTESY_MESSAGE, eventId, details);
    }


    public TimelineElement buildSendSimpleRegisteredLetterTimelineElement(String taxId, String iun, PhysicalAddress address, String eventId) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN {} and id {}", iun, taxId);

        SimpleRegisteredLetterDetails details = SimpleRegisteredLetterDetails.builder()
                .taxId(taxId)
                .address(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, eventId, details);
    }


    public TimelineElement buildSendDigitalNotificationTimelineElement(DigitalAddress digitalAddress, DigitalAddressSource addressSource, NotificationRecipient recipient, Notification notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recipient.getTaxId());

        SendDigitalDetails details = SendDigitalDetails.sendBuilder()
                .taxId(recipient.getTaxId())
                .retryNumber(sentAttemptMade)
                .address(digitalAddress)
                .addressSource(addressSource)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_DIGITAL_DOMICILE, eventId, details);
    }


    public TimelineElement buildSendAnalogNotificationTimelineElement(PhysicalAddress address, NotificationRecipient recipient, Notification notification, boolean investigation,
                                                                      int sentAttemptMade, String eventId) {
        log.debug("buildSendAnalogNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recipient.getTaxId());

        SendPaperDetails details = SendPaperDetails.builder()
                .taxId(recipient.getTaxId())
                .address(address)
                .serviceLevel(notification.getPhysicalCommunicationType())
                .sentAttemptMade(sentAttemptMade)
                .investigation(investigation)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_ANALOG_DOMICILE, eventId, details);
    }


    public TimelineElement buildSuccessDigitalWorkflowTimelineElement(String taxId, String iun, DigitalAddress address) {
        log.debug("buildSuccessDigitalWorkflowTimelineElement - IUN {} and id {}", iun, taxId);

        String elementId = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        DigitalSuccessWorkflow details = DigitalSuccessWorkflow.builder()
                .taxId(taxId)
                .address(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, elementId, details);
    }


    public TimelineElement buildFailureDigitalWorkflowTimelineElement(String taxId, String iun) {
        log.debug("buildFailureDigitalWorkflowTimelineElement - IUN {} and id {}", iun, taxId);

        String elementId = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        DigitalFailureWorkflow details = DigitalFailureWorkflow.builder()
                .taxId(taxId)
                .build();

        return buildTimeline(iun, TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, elementId, details);

    }


    public TimelineElement buildSuccessAnalogWorkflowTimelineElement(String taxId, String iun, PhysicalAddress address) {
        log.debug("buildSuccessAnalogWorkflowTimelineElement - iun {} and id {}", iun, taxId);

        String elementId = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        AnalogSuccessWorkflow details = AnalogSuccessWorkflow.builder()
                .taxId(taxId)
                .address(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW, elementId, details);
    }


    public TimelineElement buildFailureAnalogWorkflowTimelineElement(String taxId, String iun) {
        log.debug("buildFailureAnalogWorkflowTimelineElement - iun {} and id {}", iun, taxId);

        String elementId = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        AnalogFailureWorkflow details = AnalogFailureWorkflow.builder()
                .taxId(taxId)
                .build();

        return buildTimeline(iun, TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, elementId, details);
    }


    public TimelineElement buildPublicRegistryResponseCallTimelineElement(String iun, String taxId, PublicRegistryResponse response) {
        log.debug("buildPublicRegistryResponseCallTimelineElement - iun {} and id {}", iun, taxId);
        String correlationId = String.format(
                "response_%s",
                response.getCorrelationId()
        );
        PublicRegistryResponseDetails details = PublicRegistryResponseDetails.builder()
                .taxId(taxId)
                .digitalAddress(response.getDigitalAddress())
                .physicalAddress(response.getPhysicalAddress())
                .build();

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, correlationId, details);
    }


    public TimelineElement buildPublicRegistryCallTimelineElement(String iun, String taxId, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        log.debug("buildPublicRegistryCallTimelineElement - iun {} and id {}", iun, taxId);

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
                .taxId(taxId)
                .contactPhase(contactPhase)
                .sentAttemptMade(sentAttemptMade)
                .deliveryMode(deliveryMode)
                .sendDate(instantNowSupplier.get())
                .build();

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_CALL, eventId, details);
    }


    public TimelineElement buildAnalogFailureAttemptTimelineElement(ExtChannelResponse response, int sentAttemptMade, SendPaperDetails sendPaperDetails) {
        log.debug("buildAnalogFailureAttemptTimelineElement - iun {} and id {}", response.getIun(), sendPaperDetails.getTaxId());

        String iun = response.getIun();

        String elementId = TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(sendPaperDetails.getTaxId())
                        .index(sentAttemptMade)
                        .build()
        );
        SendPaperFeedbackDetails details = new SendPaperFeedbackDetails(
                SendPaperDetails.builder()
                        .taxId(sendPaperDetails.getTaxId())
                        .address(sendPaperDetails.getAddress())
                        .sentAttemptMade(sentAttemptMade)
                        .serviceLevel(sendPaperDetails.getServiceLevel())
                        .build(),
                response.getAnalogNewAddressFromInvestigation(),
                response.getAttachmentKeys(),
                response.getErrorList()
        );

        return buildTimeline(iun, TimelineElementCategory.SEND_PAPER_FEEDBACK, elementId, details);

    }

    public TimelineElement buildNotificationViewedTimelineElement(String iun, String taxId) {
        log.debug("buildNotificationViewedTimelineElement - iun {} and id {}", iun, taxId);

        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        NotificationViewedDetails details = NotificationViewedDetails.builder()
                .taxId(taxId)
                .build();

        return buildTimeline(iun, TimelineElementCategory.NOTIFICATION_VIEWED, elementId, details);
    }

    public TimelineElement buildCompletelyUnreachableTimelineElement(String iun, String taxId) {
        log.debug("buildCompletelyUnreachableTimelineElement - iun {} and id {}", iun, taxId);

        String elementId = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        CompletlyUnreachableDetails details = CompletlyUnreachableDetails.builder()
                .taxId(taxId)
                .build();

        return buildTimeline(iun, TimelineElementCategory.COMPLETELY_UNREACHABLE, elementId, details);
    }

    public TimelineElement buildScheduleDigitalWorkflowTimeline(String iun, String taxId, DigitalAddressInfo lastAttemptInfo) {
        log.debug("buildScheduledActionTimeline - iun {} and id {}", iun, taxId);
        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        ScheduleDigitalWorkflow details = ScheduleDigitalWorkflow.builder()
                .taxId(taxId)
                .lastAttemptInfo(lastAttemptInfo)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, elementId, details);
    }

    public TimelineElement buildScheduleAnalogWorkflowTimeline(String iun, String taxId) {
        log.debug("buildScheduleAnalogWorkflowTimeline - iun {} and id {}", iun, taxId);
        String elementId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());

        ScheduleAnalogWorkflow details = ScheduleAnalogWorkflow.builder()
                .taxId(taxId)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, elementId, details);
    }

    public TimelineElement buildRefinementTimelineElement(String iun, String taxId) {
        log.debug("buildRefinementTimelineElement - iun {} and id {}", iun, taxId);
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        RefinementDetails details = RefinementDetails.builder()
                .taxId(taxId)
                .build();

        return buildTimeline(iun, TimelineElementCategory.REFINEMENT, elementId, details);
    }
    
    public TimelineElement buildScheduleRefinement(String iun, String taxId) {
        log.debug("buildScheduleRefinement - iun {} and id {}", iun, taxId);
        String elementId = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .build());
        ScheduleRefinement details = ScheduleRefinement.builder()
                .taxId(taxId)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_REFINEMENT, elementId, details);
    }
}
