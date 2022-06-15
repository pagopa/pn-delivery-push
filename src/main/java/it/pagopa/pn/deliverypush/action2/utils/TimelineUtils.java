package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.mapper.CourtesyDigitalAddressMapper;
import it.pagopa.pn.deliverypush.service.mapper.LegalDigitalAddressMapper;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    public TimelineElementInternal buildTimeline(NotificationInt notification, TimelineElementCategory category, String elementId, TimelineElementDetails details) {
        
        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds(Collections.emptyList());
                
        return buildTimeline( notification, category, elementId, details, timelineBuilder);
    }

    public TimelineElementInternal buildTimeline(NotificationInt notification, TimelineElementCategory category, String elementId,
                                         TimelineElementDetails details,  TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder) {
        
        return timelineBuilder
                .iun(notification.getIun())
                .category(category)
                .timestamp(Instant.now())
                .elementId(elementId)
                .details(details)
                .paId(notification.getSender().getPaId())
                .build();
    }
    
    public TimelineElementInternal  buildAcceptedRequestTimelineElement(NotificationInt notification, String legalFactId) {
        log.debug("buildAcceptedRequestTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());
        NotificationRequestAccepted details = NotificationRequestAccepted.builder().build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( singleLegalFactId( legalFactId, LegalFactCategory.SENDER_ACK ) );

        return buildTimeline(
                notification,
                TimelineElementCategory.REQUEST_ACCEPTED,
                elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class),
                timelineBuilder
            );
    }

    public TimelineElementInternal buildAvailabilitySourceTimelineElement(Integer recIndex, NotificationInt notification, DigitalAddressSource source, boolean isAvailable,
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

        GetAddressInfo details = GetAddressInfo.builder()
                .recIndex(recIndex)
                .digitalAddressSource(source)
                .isAvailable(isAvailable)
                .attemptDate(instantNowSupplier.get())
                .build();
        
        return buildTimeline(notification, TimelineElementCategory.GET_ADDRESS, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildDigitalFeedbackTimelineElement(NotificationInt notification, ResponseStatus status, List<String> errors, SendDigitalDetails sendDigitalDetails) {
        log.debug("buildDigitaFeedbackTimelineElement - IUN={} and id={}", notification.getIun(), sendDigitalDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendDigitalDetails.getRecIndex())
                        .index(sendDigitalDetails.getRetryNumber())
                        .source(sendDigitalDetails.getDigitalAddressSource())
                        .build()
        );

        SendDigitalFeedback details = SendDigitalFeedback.builder()
                .errors(errors)
                .digitalAddress(sendDigitalDetails.getDigitalAddress())
                .responseStatus(status)
                .recIndex(sendDigitalDetails.getRecIndex())
                .notificationDate(instantNowSupplier.get())
                .build();

        return buildTimeline(notification, TimelineElementCategory.SEND_DIGITAL_FEEDBACK, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal buildSendCourtesyMessageTimelineElement(Integer recIndex, NotificationInt notification, CourtesyDigitalAddressInt address, Instant sendDate, String eventId) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SendCourtesyMessageDetails details = SendCourtesyMessageDetails.builder()
                .recIndex(recIndex)
                .digitalAddress(CourtesyDigitalAddressMapper.courtesyToDigital(address))
                .sendDate(sendDate)
                .build();

        return buildTimeline(notification, TimelineElementCategory.SEND_COURTESY_MESSAGE, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildSendSimpleRegisteredLetterTimelineElement(Integer recIndex, NotificationInt notification, PhysicalAddress address,
                                                                                  String eventId, Integer numberOfPages) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SimpleRegisteredLetterDetails details = SimpleRegisteredLetterDetails.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .numberOfPages(numberOfPages)
                .foreignState(address.getForeignState())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( Collections.emptyList() );
        
        return buildTimeline(notification, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class) , timelineBuilder);
    }


    public TimelineElementInternal buildSendDigitalNotificationTimelineElement(LegalDigitalAddressInt digitalAddress, DigitalAddressSource addressSource, Integer recIndex, NotificationInt notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        SendDigitalDetails details = SendDigitalDetails.builder()
                .recIndex(recIndex)
                .retryNumber(sentAttemptMade)
                .digitalAddress(LegalDigitalAddressMapper.legalToDigital(digitalAddress))
                .digitalAddressSource(addressSource)
                .build();

        return buildTimeline(notification, TimelineElementCategory.SEND_DIGITAL_DOMICILE, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildSendAnalogNotificationTimelineElement(PhysicalAddress address, Integer recIndex, NotificationInt notification, boolean investigation,
                                                                              int sentAttemptMade, String eventId, Integer numberOfPages) {
        log.debug("buildSendAnalogNotificationTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);
        ServiceLevel serviceLevel = notification.getPhysicalCommunicationType() != null ? ServiceLevel.valueOf(notification.getPhysicalCommunicationType().name()) : null;
        
        SendPaperDetails details = SendPaperDetails.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .serviceLevel(serviceLevel)
                .sentAttemptMade(sentAttemptMade)
                .investigation(investigation)
                .numberOfPages(numberOfPages)
                .foreignState(address.getForeignState())
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( Collections.emptyList() );

        return buildTimeline(notification, TimelineElementCategory.SEND_ANALOG_DOMICILE, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class), timelineBuilder);
    }
    
    public TimelineElementInternal buildSuccessDigitalWorkflowTimelineElement(NotificationInt notification, Integer recIndex, LegalDigitalAddressInt address, String legalFactId) {
        log.debug("buildSuccessDigitalWorkflowTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        DigitalSuccessWorkflow details = DigitalSuccessWorkflow.builder()
                .recIndex(recIndex)
                .digitalAddress(LegalDigitalAddressMapper.legalToDigital(address))
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( singleLegalFactId(legalFactId, LegalFactCategory.DIGITAL_DELIVERY) );

        return buildTimeline(notification, TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class), timelineBuilder);
    }
    
    public TimelineElementInternal buildFailureDigitalWorkflowTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId) {
        log.debug("buildFailureDigitalWorkflowTimelineElement - IUN={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        DigitalFailureWorkflow details = DigitalFailureWorkflow.builder()
                .recIndex(recIndex)
                .build();


        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( singleLegalFactId(legalFactId, LegalFactCategory.DIGITAL_DELIVERY) );
        
        return buildTimeline(notification, TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class), timelineBuilder);
    }


    public TimelineElementInternal buildSuccessAnalogWorkflowTimelineElement(NotificationInt notification, Integer recIndex, PhysicalAddress address, List<LegalFactsId> attachments) {
        log.debug("buildSuccessAnalogWorkflowTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        AnalogSuccessWorkflow details = AnalogSuccessWorkflow.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( attachments );

        return buildTimeline(notification, TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW, elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class), timelineBuilder);
    }


    public TimelineElementInternal buildFailureAnalogWorkflowTimelineElement(NotificationInt notification, Integer recIndex, List<LegalFactsId> attachments) {
        log.debug("buildFailureAnalogWorkflowTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        AnalogFailureWorkflow details = AnalogFailureWorkflow.builder()
                .recIndex(recIndex)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( attachments );

        return buildTimeline(notification, TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class), timelineBuilder);
    }


    public TimelineElementInternal buildPublicRegistryResponseCallTimelineElement(NotificationInt notification, Integer recIndex, PublicRegistryResponse response) {
        log.debug("buildPublicRegistryResponseCallTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String eventId = TimelineEventId.PUBLIC_REGISTRY_RESPONSE.buildEventId(response.getCorrelationId());
                
        PublicRegistryResponseDetails details = PublicRegistryResponseDetails.builder()
                .recIndex(recIndex)
                .digitalAddress(LegalDigitalAddressMapper.legalToDigital(response.getDigitalAddress()))
                .physicalAddress(response.getPhysicalAddress())
                .build();

        return buildTimeline(notification, TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal  buildPublicRegistryCallTimelineElement(NotificationInt notification, Integer recIndex, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        log.debug("buildPublicRegistryCallTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
                .recIndex(recIndex)
                .contactPhase(contactPhase)
                .sentAttemptMade(sentAttemptMade)
                .deliveryMode(deliveryMode)
                .sendDate(instantNowSupplier.get())
                .build();

        return buildTimeline(notification, TimelineElementCategory.PUBLIC_REGISTRY_CALL, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildAnalogFailureAttemptTimelineElement(NotificationInt notification, int sentAttemptMade, List<LegalFactsId> legalFactsListEntryIds, PhysicalAddress newAddress, List<String> errors, SendPaperDetails sendPaperDetails) {
        log.debug("buildAnalogFailureAttemptTimelineElement - iun={} and id={}", notification.getIun(), sendPaperDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(sendPaperDetails.getRecIndex())
                        .index(sentAttemptMade)
                        .build()
        );

        SendPaperFeedbackDetails details = SendPaperFeedbackDetails.builder()
                .recIndex(sendPaperDetails.getRecIndex())
                .physicalAddress(sendPaperDetails.getPhysicalAddress())
                .sentAttemptMade(sentAttemptMade)
                .serviceLevel(sendPaperDetails.getServiceLevel())
                .newAddress(newAddress)
                .errors(errors)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( legalFactsListEntryIds );

        return buildTimeline( notification, TimelineElementCategory.SEND_PAPER_FEEDBACK, elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class), timelineBuilder );
    }

    public TimelineElementInternal  buildNotificationViewedTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId) {
        log.debug("buildNotificationViewedTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        NotificationViewedDetails details = NotificationViewedDetails.builder()
                .recIndex(recIndex)
                .build();

        TimelineElementInternal.TimelineElementInternalBuilder timelineBuilder = TimelineElementInternal.timelineInternalBuilder()
                .legalFactsIds( singleLegalFactId( legalFactId, LegalFactCategory.RECIPIENT_ACCESS ) );

        return buildTimeline(notification, TimelineElementCategory.NOTIFICATION_VIEWED, elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class), timelineBuilder);
    }

    public TimelineElementInternal  buildCompletelyUnreachableTimelineElement(NotificationInt notification, Integer recIndex) {
        log.debug("buildCompletelyUnreachableTimelineElement - iun={} and id={}", notification.getIun(), recIndex);

        String elementId = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        CompletelyUnreachableDetails details = CompletelyUnreachableDetails.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategory.COMPLETELY_UNREACHABLE, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal  buildScheduleDigitalWorkflowTimeline(NotificationInt notification, Integer recIndex, DigitalAddressInfo lastAttemptInfo) {
        log.debug("buildScheduledActionTimeline - iun={} and id={}", notification.getIun(), recIndex);
        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        ScheduleDigitalWorkflow details = ScheduleDigitalWorkflow.builder()
                .recIndex(recIndex)
                .lastAttemptDate(lastAttemptInfo.getLastAttemptDate())
                .digitalAddress(LegalDigitalAddressMapper.legalToDigital(lastAttemptInfo.getDigitalAddress()))
                .digitalAddressSource(lastAttemptInfo.getDigitalAddressSource())
                .sentAttemptMade(lastAttemptInfo.getSentAttemptMade())
                .build();

        return buildTimeline(notification, TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal buildScheduleAnalogWorkflowTimeline(NotificationInt notification, Integer recIndex) {
        log.debug("buildScheduleAnalogWorkflowTimeline - iun={} and id={}", notification.getIun(), recIndex);
        String elementId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());

        ScheduleAnalogWorkflow details = ScheduleAnalogWorkflow.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal  buildRefinementTimelineElement(NotificationInt notification, Integer recIndex) {
        log.debug("buildRefinementTimelineElement - iun={} and id={}", notification.getIun(), recIndex);
        
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        RefinementDetails details = RefinementDetails.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategory.REFINEMENT, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }
    
    public TimelineElementInternal buildScheduleRefinement(NotificationInt notification, Integer recIndex) {
        log.debug("buildScheduleRefinement - iun={} and id={}", notification.getIun(), recIndex);
        
        String elementId = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        
        ScheduleRefinement details = ScheduleRefinement.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(notification, TimelineElementCategory.SCHEDULE_REFINEMENT, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal buildRefusedRequestTimelineElement(NotificationInt notification, List<String> errors) {
        log.debug("buildRefusedRequestTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_REFUSED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        RequestRefusedDetails details = RequestRefusedDetails.builder()
                .errors(errors)
                .build();

        return buildTimeline(notification, TimelineElementCategory.REQUEST_REFUSED, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildAarGenerationTimelineElement(NotificationInt notification, Integer recIndex, String legalFactId, Integer numberOfPages) {
        log.debug("buildAarGenerationTimelineElement - iun={}", notification.getIun());

        String elementId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build());
        AarGenerationDetails details = AarGenerationDetails.builder()
                .recIndex(recIndex)
                .generatedAarUrl(legalFactId)
                .numberOfPages(numberOfPages)
                .build();

        return buildTimeline(
                notification,
                TimelineElementCategory.AAR_GENERATION,
                elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class)
        );
    }

    public List<LegalFactsId> singleLegalFactId(String legalFactKey, LegalFactCategory type) {
        return Collections.singletonList( LegalFactsId.builder()
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
