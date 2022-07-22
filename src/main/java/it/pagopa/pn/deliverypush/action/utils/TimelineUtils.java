package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.deliverypush.dto.address.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.DigitalMessageReferenceInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TimelineUtils {

    private final InstantNowSupplier instantNowSupplier;
    private final TimelineService timelineService;

    public TimelineUtils(InstantNowSupplier instantNowSupplier,
                         TimelineService timelineService) {
        this.instantNowSupplier = instantNowSupplier;
        this.timelineService = timelineService;
    }

    public TimelineElementInternal buildTimeline(NotificationInt notification, TimelineElementCategoryInt category, String elementId, TimelineElementDetailsInt details) {
        
        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds(Collections.emptyList());
                
        return buildTimeline( notification, category, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildTimeline(NotificationInt notification, TimelineElementCategoryInt category, String elementId,
                                                 TimelineElementDetailsInt details, TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder) {
        return timelineBuilder
                .iun(notification.getIun())
                .category(category)
                .timestamp(Instant.now())
                .elementId(elementId)
                .details(details)
                .paId(notification.getSender().getPaId())
                .build();
    }
    
    public TimelineElementInternal buildAcceptedRequestTimelineElement(NotificationInt notification, String legalFactId) {
        log.debug("buildAcceptedRequestTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());
        NotificationRequestAcceptedDetailsInt details = NotificationRequestAcceptedDetailsInt.builder().build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( singleLegalFactId( legalFactId, LegalFactCategoryInt.SENDER_ACK ) );

        return timelineBuilder
                .iun(notification.getIun())
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .timestamp(notification.getSentAt())
                .elementId(elementId)
                .details(details)
                .paId(notification.getSender().getPaId())
                .build();
    }

    public TimelineElementInternal buildAvailabilitySourceTimelineElement(Integer recIndex, NotificationInt notification, DigitalAddressSourceInt source, boolean isAvailable,
                                                                          int sentAttemptMade) {
        log.debug("buildAvailabilitySourceTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .source(source)
                        .index(sentAttemptMade)
                        .build()
        );

        GetAddressInfoDetailsInt details = GetAddressInfoDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddressSource(source)
                .isAvailable(isAvailable)
                .attemptDate(instantNowSupplier.get())
                .build();
        
        return buildTimeline(notification, TimelineElementCategoryInt.GET_ADDRESS, elementId, details);
    }


    public TimelineElementInternal buildDigitalFeedbackTimelineElement(NotificationInt notification, ResponseStatusInt status, List<String> errors,
                                                                       SendDigitalDetailsInt sendDigitalDetails, DigitalMessageReferenceInt digitalMessageReference) {
        log.debug("buildDigitaFeedbackTimelineElement - IUN={} and id={}", notification.getIun(), sendDigitalDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendDigitalDetails.getRecIndex())
                        .index(sendDigitalDetails.getRetryNumber())
                        .source(sendDigitalDetails.getDigitalAddressSource())
                        .build()
        );

        SendDigitalFeedbackDetailsInt details = SendDigitalFeedbackDetailsInt.builder()
                .errors(errors)
                .digitalAddress(sendDigitalDetails.getDigitalAddress())
                .responseStatus(status)
                .recIndex(sendDigitalDetails.getRecIndex())
                .notificationDate(instantNowSupplier.get())
                .sendingReceipts(
                        Collections.singletonList(SendingReceipt.builder()
                                .id(digitalMessageReference.getId())
                                .system(digitalMessageReference.getSystem())
                                .location(digitalMessageReference.getLocation())
                        .build())
                )
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_DIGITAL_FEEDBACK, elementId, details);
    }

    public TimelineElementInternal buildDigitalDeliveringProgressTimelineElement(NotificationInt notification, 
                                                                                 SendDigitalDetailsInt sendDigitalDetails,
                                                                                 List<DigitalMessageReferenceInt> digitalMessageReferences,
                                                                                 int index) {
        log.debug("buildDigitalDeliveringProgressTimelineElement - IUN={} and id={}", notification.getIun(), sendDigitalDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendDigitalDetails.getRecIndex())
                        .sentAttemptMade(sendDigitalDetails.getRetryNumber())
                        .source(sendDigitalDetails.getDigitalAddressSource())
                        .index(index)
                        .build()
        );

        SendDigitalProgressDetailsInt details = SendDigitalProgressDetailsInt.builder()
                .digitalAddress(sendDigitalDetails.getDigitalAddress())
                .recIndex(sendDigitalDetails.getRecIndex())
                .notificationDate(instantNowSupplier.get())
                .sendingReceipts(
                        digitalMessageReferences.stream().map(
                                digitalMessageReference ->
                                        SendingReceipt.builder()
                                        .id(digitalMessageReference.getId())
                                        .system(digitalMessageReference.getSystem())
                                        .location(digitalMessageReference.getLocation())
                                        .build()
                        ).collect(Collectors.toList())
                )
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_DIGITAL_PROGRESS, elementId, details);
    }
    
    public TimelineElementInternal buildSendCourtesyMessageTimelineElement(Integer recIndex, NotificationInt notification, CourtesyDigitalAddressInt address, Instant sendDate, String eventId) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SendCourtesyMessageDetailsInt details = SendCourtesyMessageDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddress(address)
                .sendDate(sendDate)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_COURTESY_MESSAGE, eventId, details);
    }


    public TimelineElementInternal buildSendSimpleRegisteredLetterTimelineElement(Integer recIndex, NotificationInt notification, PhysicalAddressInt address,
                                                                                  String eventId, Integer numberOfPages) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SimpleRegisteredLetterDetailsInt details = SimpleRegisteredLetterDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .foreignState(address.getForeignState())
                .numberOfPages(numberOfPages)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( Collections.emptyList() );
        
        return buildTimeline(notification, TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER, eventId, details , timelineBuilder);
    }


    public TimelineElementInternal buildSendDigitalNotificationTimelineElement(LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, Integer recIndex,
                                                                               NotificationInt notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .recIndex(recIndex)
                .retryNumber(sentAttemptMade)
                .digitalAddress(digitalAddress)
                .digitalAddressSource(addressSource)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE, eventId, details);
    }


    public TimelineElementInternal buildSendAnalogNotificationTimelineElement(PhysicalAddressInt address, Integer recIndex, NotificationInt notification,
                                                                              boolean investigation, int sentAttemptMade, String eventId, Integer numberOfPages) {
        log.debug("buildSendAnalogNotificationTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);
        ServiceLevelInt serviceLevel = notification.getPhysicalCommunicationType() != null ? ServiceLevelInt.valueOf(notification.getPhysicalCommunicationType().name()) : null;
        
        SendAnalogDetailsInt details = SendAnalogDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .serviceLevel(serviceLevel)
                .sentAttemptMade(sentAttemptMade)
                .investigation(investigation)
                .numberOfPages( numberOfPages )
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( Collections.emptyList() );

        return buildTimeline(notification, TimelineElementCategoryInt.SEND_ANALOG_DOMICILE, eventId, details, timelineBuilder);
    }
    
    public TimelineElementInternal buildSuccessDigitalWorkflowTimelineElement(NotificationInt notification, Integer recIndex, LegalDigitalAddressInt address,
                                                                              String legalFactId) {
        log.debug("buildSuccessDigitalWorkflowTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        DigitalSuccessWorkflowDetailsInt details = DigitalSuccessWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddress(address)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( singleLegalFactId(legalFactId, LegalFactCategoryInt.DIGITAL_DELIVERY) );

        return buildTimeline(notification, TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW, elementId,
                details, timelineBuilder);
    }
    
    public TimelineElementInternal buildFailureDigitalWorkflowTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId) {
        log.debug("buildFailureDigitalWorkflowTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        DigitalFailureWorkflowDetailsInt details = DigitalFailureWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .build();
        
        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( singleLegalFactId(legalFactId, LegalFactCategoryInt.DIGITAL_DELIVERY) );
        
        return buildTimeline(notification, TimelineElementCategoryInt.DIGITAL_FAILURE_WORKFLOW, elementId,
                details, timelineBuilder);
    }


    public TimelineElementInternal buildSuccessAnalogWorkflowTimelineElement(NotificationInt notification, Integer recIndex, PhysicalAddressInt address, List<LegalFactsIdInt> attachments) {
        log.debug("buildSuccessAnalogWorkflowTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        AnalogSuccessWorkflowDetailsInt details = AnalogSuccessWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( attachments );

        return buildTimeline(notification, TimelineElementCategoryInt.ANALOG_SUCCESS_WORKFLOW, elementId,
                details, timelineBuilder);
    }


    public TimelineElementInternal buildFailureAnalogWorkflowTimelineElement(NotificationInt notification, Integer recIndex, List<LegalFactsIdInt> attachments) {
        log.debug("buildFailureAnalogWorkflowTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        AnalogFailureWorkflowDetailsInt details = AnalogFailureWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( attachments );

        return buildTimeline(notification, TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW, elementId,
                details, timelineBuilder);
    }


    public TimelineElementInternal buildPublicRegistryResponseCallTimelineElement(NotificationInt notification, Integer recIndex, PublicRegistryResponse response) {
        log.debug("buildPublicRegistryResponseCallTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String eventId = TimelineEventId.PUBLIC_REGISTRY_RESPONSE.buildEventId(response.getCorrelationId());
                
        PublicRegistryResponseDetailsInt details = PublicRegistryResponseDetailsInt.builder()
                .recIndex(recIndex)
                .digitalAddress(response.getDigitalAddress())
                .physicalAddress(response.getPhysicalAddress())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PUBLIC_REGISTRY_RESPONSE, eventId, details);
    }


    public TimelineElementInternal  buildPublicRegistryCallTimelineElement(NotificationInt notification, Integer recIndex, String eventId, DeliveryModeInt deliveryMode, 
                                                                           ContactPhaseInt contactPhase, int sentAttemptMade) {
        log.debug("buildPublicRegistryCallTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        PublicRegistryCallDetailsInt details = PublicRegistryCallDetailsInt.builder()
                .recIndex(recIndex)
                .contactPhase(contactPhase)
                .sentAttemptMade(sentAttemptMade)
                .deliveryMode(deliveryMode)
                .sendDate(instantNowSupplier.get())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.PUBLIC_REGISTRY_CALL, eventId, details);
    }


    public TimelineElementInternal buildAnalogFailureAttemptTimelineElement(NotificationInt notification, int sentAttemptMade, List<LegalFactsIdInt> legalFactsListEntryIds,
                                                                            PhysicalAddressInt newAddress, List<String> errors, SendAnalogDetailsInt sendPaperDetails) {
        log.debug("buildAnalogFailureAttemptTimelineElement - iun={} and id={}", notification.getIun(), sendPaperDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendPaperDetails.getRecIndex())
                        .index(sentAttemptMade)
                        .build()
        );

        SendAnalogFeedbackDetailsInt details = SendAnalogFeedbackDetailsInt.builder()
                .recIndex(sendPaperDetails.getRecIndex())
                .physicalAddress(sendPaperDetails.getPhysicalAddress())
                .sentAttemptMade(sentAttemptMade)
                .serviceLevel(sendPaperDetails.getServiceLevel())
                .newAddress(newAddress)
                .errors(errors)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( legalFactsListEntryIds );

        return buildTimeline( notification, TimelineElementCategoryInt.SEND_PAPER_FEEDBACK, elementId,
                details, timelineBuilder );
    }

    public TimelineElementInternal  buildNotificationViewedTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId) {
        log.debug("buildNotificationViewedTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        NotificationViewedDetailsInt details = NotificationViewedDetailsInt.builder()
                .recIndex(recIndex)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.builder()
                .legalFactsIds( singleLegalFactId( legalFactId, LegalFactCategoryInt.RECIPIENT_ACCESS ) );

        return buildTimeline(notification, TimelineElementCategoryInt.NOTIFICATION_VIEWED, elementId,
                details, timelineBuilder);
    }

    public TimelineElementInternal  buildCompletelyUnreachableTimelineElement(NotificationInt notification, Integer recIndex) {
        log.debug("buildCompletelyUnreachableTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        CompletelyUnreachableDetailsInt details = CompletelyUnreachableDetailsInt.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.COMPLETELY_UNREACHABLE, elementId, details);
    }

    public TimelineElementInternal buildScheduleDigitalWorkflowTimeline(NotificationInt notification, Integer recIndex, DigitalAddressInfo lastAttemptInfo) {
        log.debug("buildScheduledActionTimeline - iun={} and id={}", notification.getIun(), recIndex);
        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        ScheduleDigitalWorkflowDetailsInt details = ScheduleDigitalWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .lastAttemptDate(lastAttemptInfo.getLastAttemptDate())
                .digitalAddress(lastAttemptInfo.getDigitalAddress())
                .digitalAddressSource(lastAttemptInfo.getDigitalAddressSource())
                .sentAttemptMade(lastAttemptInfo.getSentAttemptMade())
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SCHEDULE_DIGITAL_WORKFLOW, elementId, details);
    }

    public TimelineElementInternal buildScheduleAnalogWorkflowTimeline(NotificationInt notification, Integer recIndex) {
        log.debug("buildScheduleAnalogWorkflowTimeline - iun={} and id={}", notification.getIun(), recIndex);
        String elementId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        ScheduleAnalogWorkflowDetailsInt details = ScheduleAnalogWorkflowDetailsInt.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SCHEDULE_ANALOG_WORKFLOW, elementId, details);
    }

    public TimelineElementInternal  buildRefinementTimelineElement(NotificationInt notification, Integer recIndex) {
        log.debug("buildRefinementTimelineElement - iun={} and id={}", notification.getIun(), recIndex);
        
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        RefinementDetailsInt details = RefinementDetailsInt.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.REFINEMENT, elementId, details);
    }
    
    public TimelineElementInternal buildScheduleRefinement(NotificationInt notification, Integer recIndex) {
        log.debug("buildScheduleRefinement - iun={} and id={}", notification.getIun(), recIndex);
        
        String elementId = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        ScheduleRefinementDetailsInt details = ScheduleRefinementDetailsInt.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.SCHEDULE_REFINEMENT, elementId, details);
    }

    public TimelineElementInternal buildRefusedRequestTimelineElement(NotificationInt notification, List<String> errors) {
        log.debug("buildRefusedRequestTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_REFUSED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        RequestRefusedDetailsInt details = RequestRefusedDetailsInt.builder()
                .errors(errors)
                .build();

        return buildTimeline(notification, TimelineElementCategoryInt.REQUEST_REFUSED, elementId, details);
    }
    
    public TimelineElementInternal buildAarGenerationTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId, Integer numberOfPages) {
        log.debug("buildAarGenerationTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        AarGenerationDetailsInt details = AarGenerationDetailsInt.builder()
                .recIndex(recIndex)
                .generatedAarUrl(legalFactId)
                .numberOfPages(numberOfPages)
                .build();

        return buildTimeline(
                notification,
                TimelineElementCategoryInt.AAR_GENERATION,
                elementId,
                details
        );
    }

    public TimelineElementInternal buildNotHandledTimelineElement(NotificationInt notification, Integer recIndex,
                                                                  String reasonCode, String reason) {
        log.debug("buildNotHandledTimelineElement - iun={} id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.NOT_HANDLED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        NotHandledDetailsInt details = NotHandledDetailsInt.builder()
                .recIndex(recIndex)
                .reasonCode(reasonCode)
                .reason(reason)
                .build();

        return buildTimeline(
                notification,
                TimelineElementCategoryInt.NOT_HANDLED,
                elementId,
                details
        );
    }

    public List<LegalFactsIdInt> singleLegalFactId(String legalFactKey, LegalFactCategoryInt type) {
        return Collections.singletonList( LegalFactsIdInt.builder()
                .key( legalFactKey )
                .category( type )
                .build() );
    }

    public boolean checkNotificationIsAlreadyViewed(String iun, Integer recIndex){

        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<TimelineElementInternal> timelineOpt = timelineService.getTimelineElement(iun, elementId);
        return timelineOpt.isPresent();
    }
    
    
    

    public String getIunFromTimelineId(String timelineId)
    {
        return timelineId.split("_")[0];
    }

}
