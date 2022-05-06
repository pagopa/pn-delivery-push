package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.legalfacts.LegalFactType;
import it.pagopa.pn.api.dto.legalfacts.LegalFactsListEntryId;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressInfo;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TimelineUtils {

    private final InstantNowSupplier instantNowSupplier;

    public TimelineUtils(InstantNowSupplier instantNowSupplier) {
        this.instantNowSupplier = instantNowSupplier;
    }

    public TimelineElement buildTimeline(String iun, TimelineElementCategory category, String elementId, TimelineElementDetails details) {
        return buildTimeline( iun, category, elementId, details, Collections.emptyList() );
    }

    public TimelineElement buildTimeline(String iun, TimelineElementCategory category, String elementId,
                                         TimelineElementDetails details, List<LegalFactsListEntryId> legalFactsListEntryIds) {
        return TimelineElement.builder()
                .category(category)
                .timestamp(instantNowSupplier.get())
                .iun(iun)
                .elementId(elementId)
                .details(details)
                .legalFactsIds( legalFactsListEntryIds )
                .build();
    }
    
    public TimelineElement buildAcceptedRequestTimelineElement(Notification notification, String legalFactId) {
        log.debug("buildAcceptedRequestTimelineElement - iun {}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        return buildTimeline(
                notification.getIun(),
                TimelineElementCategory.REQUEST_ACCEPTED,
                elementId,
                ReceivedDetails.builder()
                        .recipients(notification.getRecipients())
                        .documentsDigests(notification.getDocuments()
                                .stream()
                                .map(NotificationAttachment::getDigests)
                                .collect(Collectors.toList())
                        )
                        .build(),
                singleLegalFactId( legalFactId, LegalFactType.SENDER_ACK )
            );
    }

    public TimelineElement buildAvailabilitySourceTimelineElement(int recIndex, String iun, DigitalAddressSource source, boolean isAvailable, 
                                                                  int sentAttemptMade) {
        log.debug("buildAvailabilitySourceTimelineElement - IUN {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .source(source)
                        .index(sentAttemptMade)
                        .build()
        );

        GetAddressInfo details = GetAddressInfo.builder()
                .recIndex(recIndex)
                .source(source)
                .isAvailable(isAvailable)
                .attemptDate(instantNowSupplier.get())
                .build();

        return buildTimeline(iun, TimelineElementCategory.GET_ADDRESS, elementId, details);
    }


    public TimelineElement buildDigitaFeedbackTimelineElement(ExtChannelResponse response, SendDigitalDetails sendDigitalDetails) {
        log.debug("buildDigitaFeedbackTimelineElement - IUN {} and id {}", response.getIun(), sendDigitalDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(response.getIun())
                        .recIndex(sendDigitalDetails.getRecIndex())
                        .index(sendDigitalDetails.getRetryNumber())
                        .build()
        );

        SendDigitalFeedback details = SendDigitalFeedback.builder()
                .errors(response.getErrorList())
                .address(sendDigitalDetails.getAddress())
                .responseStatus(response.getResponseStatus())
                .recIndex(sendDigitalDetails.getRecIndex())
                .notificationDate(response.getNotificationDate())
                .build();

        return buildTimeline(response.getIun(), TimelineElementCategory.SEND_DIGITAL_FEEDBACK, elementId, details);
    }

    public TimelineElement buildSendCourtesyMessageTimelineElement(int recIndex, String iun, DigitalAddress address, Instant sendDate, String eventId) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN {} and id {}", iun, recIndex);

        SendCourtesyMessageDetails details = SendCourtesyMessageDetails.builder()
                .recIndex(recIndex)
                .address(address)
                .sendDate(sendDate)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_COURTESY_MESSAGE, eventId, details);
    }


    public TimelineElement buildSendSimpleRegisteredLetterTimelineElement(int recIndex, String iun, PhysicalAddress address, String eventId) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN {} and id {}", iun, recIndex);

        SimpleRegisteredLetterDetails details = SimpleRegisteredLetterDetails.builder()
                .recIndex(recIndex)
                .address(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, eventId, details);
    }


    public TimelineElement buildSendDigitalNotificationTimelineElement(DigitalAddress digitalAddress, DigitalAddressSource addressSource, int recIndex, Notification notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recIndex);

        SendDigitalDetails details = SendDigitalDetails.sendBuilder()
                .recIndex(recIndex)
                .retryNumber(sentAttemptMade)
                .address(digitalAddress)
                .addressSource(addressSource)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_DIGITAL_DOMICILE, eventId, details);
    }


    public TimelineElement buildSendAnalogNotificationTimelineElement(PhysicalAddress address, int recIndex, Notification notification, boolean investigation,
                                                                      int sentAttemptMade, String eventId) {
        log.debug("buildSendAnalogNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recIndex);

        SendPaperDetails details = SendPaperDetails.builder()
                .recIndex(recIndex)
                .address(address)
                .serviceLevel(notification.getPhysicalCommunicationType())
                .sentAttemptMade(sentAttemptMade)
                .investigation(investigation)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_ANALOG_DOMICILE, eventId, details);
    }


    public TimelineElement buildSuccessDigitalWorkflowTimelineElement(String iun, int recIndex, DigitalAddress address, String legalFactId) {
        log.debug("buildSuccessDigitalWorkflowTimelineElement - IUN {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        
        DigitalSuccessWorkflow details = DigitalSuccessWorkflow.builder()
                .recIndex(recIndex)
                .address(address)
                .build();

        List<LegalFactsListEntryId> legalFactIds = singleLegalFactId(legalFactId, LegalFactType.DIGITAL_DELIVERY);
        return buildTimeline(iun, TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, elementId, details, legalFactIds);
    }


    public TimelineElement buildFailureDigitalWorkflowTimelineElement(String iun, int recIndex, String legalFactId) {
        log.debug("buildFailureDigitalWorkflowTimelineElement - IUN {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        
        DigitalFailureWorkflow details = DigitalFailureWorkflow.builder()
                .recIndex(recIndex)
                .build();

        List<LegalFactsListEntryId> legalFactIds = singleLegalFactId(legalFactId, LegalFactType.DIGITAL_DELIVERY);
        return buildTimeline(iun, TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, elementId, details, legalFactIds);
    }


    public TimelineElement buildSuccessAnalogWorkflowTimelineElement(String iun, int recIndex, PhysicalAddress address) {
        log.debug("buildSuccessAnalogWorkflowTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        AnalogSuccessWorkflow details = AnalogSuccessWorkflow.builder()
                .recIndex(recIndex)
                .address(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW, elementId, details);
    }


    public TimelineElement buildFailureAnalogWorkflowTimelineElement(String iun, int recIndex) {
        log.debug("buildFailureAnalogWorkflowTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        AnalogFailureWorkflow details = AnalogFailureWorkflow.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, elementId, details);
    }


    public TimelineElement buildPublicRegistryResponseCallTimelineElement(String iun, int recIndex, PublicRegistryResponse response) {
        log.debug("buildPublicRegistryResponseCallTimelineElement - iun {} and id {}", iun, recIndex);
        String correlationId = String.format(
                "response_%s",
                response.getCorrelationId()
        );
        PublicRegistryResponseDetails details = PublicRegistryResponseDetails.builder()
                .recIndex(recIndex)
                .digitalAddress(response.getDigitalAddress())
                .physicalAddress(response.getPhysicalAddress())
                .build();

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, correlationId, details);
    }


    public TimelineElement buildPublicRegistryCallTimelineElement(String iun, int recIndex, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        log.debug("buildPublicRegistryCallTimelineElement - iun {} and id {}", iun, recIndex);

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
                .recIndex(recIndex)
                .contactPhase(contactPhase)
                .sentAttemptMade(sentAttemptMade)
                .deliveryMode(deliveryMode)
                .sendDate(instantNowSupplier.get())
                .build();

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_CALL, eventId, details);
    }


    public TimelineElement buildAnalogFailureAttemptTimelineElement(ExtChannelResponse response, int sentAttemptMade, SendPaperDetails sendPaperDetails) {
        log.debug("buildAnalogFailureAttemptTimelineElement - iun {} and id {}", response.getIun(), sendPaperDetails.getRecIndex());

        String iun = response.getIun();

        String elementId = TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(sendPaperDetails.getRecIndex())
                        .index(sentAttemptMade)
                        .build()
        );
        SendPaperFeedbackDetails details = new SendPaperFeedbackDetails(
                SendPaperDetails.builder()
                        .recIndex(sendPaperDetails.getRecIndex())
                        .address(sendPaperDetails.getAddress())
                        .sentAttemptMade(sentAttemptMade)
                        .serviceLevel(sendPaperDetails.getServiceLevel())
                        .build(),
                response.getAnalogNewAddressFromInvestigation(),
                response.getErrorList()
        );

        final List<String> attachmentKeys = response.getAttachmentKeys();
        List<LegalFactsListEntryId> legalFactsListEntryIds;
        if ( attachmentKeys != null ) {
            legalFactsListEntryIds = attachmentKeys.stream()
                    .map( k -> LegalFactsListEntryId.builder()
                            .key( k )
                            .type( LegalFactType.ANALOG_DELIVERY )
                            .build()
                    ).collect(Collectors.toList());
        } else {
            legalFactsListEntryIds = Collections.emptyList();
        }
        return buildTimeline( iun, TimelineElementCategory.SEND_PAPER_FEEDBACK,
                                                                            elementId, details, legalFactsListEntryIds );
    }

    public TimelineElement buildNotificationViewedTimelineElement(String iun, int recIndex, String legalFactId) {
        log.debug("buildNotificationViewedTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        NotificationViewedDetails details = NotificationViewedDetails.builder()
                .recIndex(recIndex)
                .build();

        List<LegalFactsListEntryId> legalFactIds = singleLegalFactId( legalFactId, LegalFactType.RECIPIENT_ACCESS );
        return buildTimeline(iun, TimelineElementCategory.NOTIFICATION_VIEWED, elementId, details, legalFactIds);
    }

    public TimelineElement buildCompletelyUnreachableTimelineElement(String iun, int recIndex) {
        log.debug("buildCompletelyUnreachableTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        CompletelyUnreachableDetails details = CompletelyUnreachableDetails.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.COMPLETELY_UNREACHABLE, elementId, details);
    }

    public TimelineElement buildScheduleDigitalWorkflowTimeline(String iun, int recIndex, DigitalAddressInfo lastAttemptInfo) {
        log.debug("buildScheduledActionTimeline - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        ScheduleDigitalWorkflow details = ScheduleDigitalWorkflow.builder()
                .recIndex(recIndex)
                .lastAttemptInfo(lastAttemptInfo)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, elementId, details);
    }

    public TimelineElement buildScheduleAnalogWorkflowTimeline(String iun, int recIndex) {
        log.debug("buildScheduleAnalogWorkflowTimeline - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        ScheduleAnalogWorkflow details = ScheduleAnalogWorkflow.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, elementId, details);
    }

    public TimelineElement buildRefinementTimelineElement(String iun, int recIndex) {
        log.debug("buildRefinementTimelineElement - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        RefinementDetails details = RefinementDetails.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.REFINEMENT, elementId, details);
    }
    
    public TimelineElement buildScheduleRefinement(String iun, int recIndex) {
        log.debug("buildScheduleRefinement - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        ScheduleRefinement details = ScheduleRefinement.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_REFINEMENT, elementId, details);
    }

    public TimelineElement buildRefusedRequestTimelineElement(Notification notification, List<String> errors) {
        log.debug("buildRefusedRequestTimelineElement - iun {}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_REFUSED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        RequestRefusedDetails details = RequestRefusedDetails.builder()
                .errors(errors)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.REQUEST_REFUSED, elementId, details);
    }

    public List<LegalFactsListEntryId> singleLegalFactId(String legalFactKey, LegalFactType type) {
        return Collections.singletonList( LegalFactsListEntryId.builder()
                .key( legalFactKey )
                .type( type )
                .build() );
    }
}
