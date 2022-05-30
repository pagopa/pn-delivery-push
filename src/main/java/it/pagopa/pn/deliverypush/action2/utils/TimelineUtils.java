package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfo;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelResponse;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
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

    public TimelineElementInternal buildTimeline(String iun, TimelineElementCategory category, String elementId, TimelineElementDetails details) {
        return buildTimeline( iun, category, elementId, details, Collections.emptyList() );
    }

    public TimelineElementInternal buildTimeline(String iun, TimelineElementCategory category, String elementId,
                                         TimelineElementDetails details,  List<LegalFactsId> legalFactsListEntryIds) {

        return TimelineElementInternal.timelineInternalBuilder()
                .iun(iun)
                .category(category)
                .timestamp(Instant.now())
                .elementId(elementId)
                .details(details)
                .legalFactsIds( legalFactsListEntryIds )
                .build();
    }
    
    public TimelineElementInternal  buildAcceptedRequestTimelineElement(NotificationInt notification, String legalFactId) {
        log.debug("buildAcceptedRequestTimelineElement - iun {}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_ACCEPTED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());
        NotificationRequestAccepted details = NotificationRequestAccepted.builder().build();

        return buildTimeline(
                notification.getIun(),
                TimelineElementCategory.REQUEST_ACCEPTED,
                elementId,
                SmartMapper.mapToClass(details, TimelineElementDetails.class),
                singleLegalFactId( legalFactId, LegalFactCategory.SENDER_ACK )
            );
    }

    public TimelineElementInternal buildAvailabilitySourceTimelineElement(Integer recIndex, String iun, DigitalAddressSource source, boolean isAvailable,
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
                .digitalAddressSource(source)
                .isAvailable(isAvailable)
                .attemptDate(instantNowSupplier.get())
                .build();
        
        return buildTimeline(iun, TimelineElementCategory.GET_ADDRESS, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildDigitaFeedbackTimelineElement(ExtChannelResponse response, SendDigitalDetails sendDigitalDetails) {
        log.debug("buildDigitaFeedbackTimelineElement - IUN {} and id {}", response.getIun(), sendDigitalDetails.getRecIndex());

        String elementId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(response.getIun())
                        .recIndex(sendDigitalDetails.getRecIndex())
                        .index(sendDigitalDetails.getRetryNumber())
                        .source(sendDigitalDetails.getDigitalAddressSource())
                        .build()
        );

        SendDigitalFeedback details = SendDigitalFeedback.builder()
                .errors(response.getErrorList())
                .digitalAddress(sendDigitalDetails.getDigitalAddress())
                .responseStatus(response.getResponseStatus())
                .recIndex(sendDigitalDetails.getRecIndex())
                .notificationDate(instantNowSupplier.get())
                .build();

        return buildTimeline(response.getIun(), TimelineElementCategory.SEND_DIGITAL_FEEDBACK, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal buildSendCourtesyMessageTimelineElement(Integer recIndex, String iun, DigitalAddress address, Instant sendDate, String eventId) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN {} and id {}", iun, recIndex);

        SendCourtesyMessageDetails details = SendCourtesyMessageDetails.builder()
                .recIndex(recIndex)
                .digitalAddress(address)
                .sendDate(sendDate)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_COURTESY_MESSAGE, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildSendSimpleRegisteredLetterTimelineElement(Integer recIndex, String iun, PhysicalAddress address, String eventId) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN {} and id {}", iun, recIndex);

        SimpleRegisteredLetterDetails details = SimpleRegisteredLetterDetails.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildSendDigitalNotificationTimelineElement(DigitalAddress digitalAddress, DigitalAddressSource addressSource, Integer recIndex, NotificationInt notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recIndex);

        SendDigitalDetails details = SendDigitalDetails.builder()
                .recIndex(recIndex)
                .retryNumber(sentAttemptMade)
                .digitalAddress(digitalAddress)
                .digitalAddressSource(addressSource)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_DIGITAL_DOMICILE, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildSendAnalogNotificationTimelineElement(PhysicalAddress address, Integer recIndex, NotificationInt notification, boolean investigation,
                                                                              int sentAttemptMade, String eventId) {
        log.debug("buildSendAnalogNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recIndex);
        ServiceLevel serviceLevel = notification.getPhysicalCommunicationType() != null ? ServiceLevel.valueOf(notification.getPhysicalCommunicationType().name()) : null;
        SendPaperDetails details = SendPaperDetails.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .serviceLevel(serviceLevel)
                .sentAttemptMade(sentAttemptMade)
                .investigation(investigation)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_ANALOG_DOMICILE, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildSuccessDigitalWorkflowTimelineElement(String iun, Integer recIndex, DigitalAddress address, String legalFactId) {
        log.debug("buildSuccessDigitalWorkflowTimelineElement - IUN {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        
        DigitalSuccessWorkflow details = DigitalSuccessWorkflow.builder()
                .recIndex(recIndex)
                .digitalAddress(address)
                .build();

        List<LegalFactsId> legalFactIds = singleLegalFactId(legalFactId, LegalFactCategory.DIGITAL_DELIVERY);
        return buildTimeline(iun, TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class), legalFactIds);
    }


    public TimelineElementInternal buildFailureDigitalWorkflowTimelineElement(String iun, Integer recIndex, String legalFactId) {
        log.debug("buildFailureDigitalWorkflowTimelineElement - IUN {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        
        DigitalFailureWorkflow details = DigitalFailureWorkflow.builder()
                .recIndex(recIndex)
                .build();

        List<LegalFactsId> legalFactIds = singleLegalFactId(legalFactId, LegalFactCategory.DIGITAL_DELIVERY);
        return buildTimeline(iun, TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class), legalFactIds);
    }


    public TimelineElementInternal buildSuccessAnalogWorkflowTimelineElement(String iun, Integer recIndex, PhysicalAddress address) {
        log.debug("buildSuccessAnalogWorkflowTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        AnalogSuccessWorkflow details = AnalogSuccessWorkflow.builder()
                .recIndex(recIndex)
                .physicalAddress(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildFailureAnalogWorkflowTimelineElement(String iun, Integer recIndex) {
        log.debug("buildFailureAnalogWorkflowTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.ANALOG_FAILURE_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        AnalogFailureWorkflow details = AnalogFailureWorkflow.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildPublicRegistryResponseCallTimelineElement(String iun, Integer recIndex, PublicRegistryResponse response) {
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

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, correlationId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal  buildPublicRegistryCallTimelineElement(String iun, Integer recIndex, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        log.debug("buildPublicRegistryCallTimelineElement - iun {} and id {}", iun, recIndex);

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
                .recIndex(recIndex)
                .contactPhase(contactPhase)
                .sentAttemptMade(sentAttemptMade)
                .deliveryMode(deliveryMode)
                .sendDate(instantNowSupplier.get())
                .build();

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_CALL, eventId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }


    public TimelineElementInternal buildAnalogFailureAttemptTimelineElement(ExtChannelResponse response, int sentAttemptMade, SendPaperDetails sendPaperDetails) {
        log.debug("buildAnalogFailureAttemptTimelineElement - iun {} and id {}", response.getIun(), sendPaperDetails.getRecIndex());

        String iun = response.getIun();

        String elementId = TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(sendPaperDetails.getRecIndex())
                        .index(sentAttemptMade)
                        .build()
        );

        SendPaperFeedbackDetails details = SendPaperFeedbackDetails.builder()
                .recIndex(sendPaperDetails.getRecIndex())
                .physicalAddress(sendPaperDetails.getPhysicalAddress())
                .sentAttemptMade(sentAttemptMade)
                .serviceLevel(sendPaperDetails.getServiceLevel())
                .newAddress(response.getAnalogNewAddressFromInvestigation())
                .errors(response.getErrorList())
                .build();

        final List<String> attachmentKeys = response.getAttachmentKeys();
        List<LegalFactsId> legalFactsListEntryIds;
        if ( attachmentKeys != null ) {
            legalFactsListEntryIds = attachmentKeys.stream()
                    .map( k -> LegalFactsId.builder()
                            .key( k )
                            .category( LegalFactCategory.ANALOG_DELIVERY )
                            .build()
                    ).collect(Collectors.toList());
        } else {
            legalFactsListEntryIds = Collections.emptyList();
        }
        return buildTimeline( iun, TimelineElementCategory.SEND_PAPER_FEEDBACK,
                                                                            elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class), legalFactsListEntryIds );
    }

    public TimelineElementInternal  buildNotificationViewedTimelineElement(String iun, Integer recIndex, String legalFactId) {
        log.debug("buildNotificationViewedTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        NotificationViewedDetails details = NotificationViewedDetails.builder()
                .recIndex(recIndex)
                .build();

        List<LegalFactsId> legalFactIds = singleLegalFactId( legalFactId, LegalFactCategory.RECIPIENT_ACCESS );
        return buildTimeline(iun, TimelineElementCategory.NOTIFICATION_VIEWED, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class), legalFactIds);
    }

    public TimelineElementInternal  buildCompletelyUnreachableTimelineElement(String iun, Integer recIndex) {
        log.debug("buildCompletelyUnreachableTimelineElement - iun {} and id {}", iun, recIndex);

        String elementId = TimelineEventId.COMPLETELY_UNREACHABLE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        CompletelyUnreachableDetails details = CompletelyUnreachableDetails.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.COMPLETELY_UNREACHABLE, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal  buildScheduleDigitalWorkflowTimeline(String iun, Integer recIndex, DigitalAddressInfo lastAttemptInfo) {
        log.debug("buildScheduledActionTimeline - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.SCHEDULE_DIGITAL_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        
        ScheduleDigitalWorkflow details = ScheduleDigitalWorkflow.builder()
                .recIndex(recIndex)
                .lastAttemptDate(lastAttemptInfo.getLastAttemptDate())
                .digitalAddress(lastAttemptInfo.getDigitalAddress())
                .digitalAddressSource(lastAttemptInfo.getDigitalAddressSource())
                .sentAttemptMade(lastAttemptInfo.getSentAttemptMade())
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal  buildScheduleAnalogWorkflowTimeline(String iun, Integer recIndex) {
        log.debug("buildScheduleAnalogWorkflowTimeline - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.SCHEDULE_ANALOG_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        ScheduleAnalogWorkflow details = ScheduleAnalogWorkflow.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal  buildRefinementTimelineElement(String iun, Integer recIndex) {
        log.debug("buildRefinementTimelineElement - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.REFINEMENT.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        RefinementDetails details = RefinementDetails.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.REFINEMENT, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }
    
    public TimelineElementInternal  buildScheduleRefinement(String iun, Integer recIndex) {
        log.debug("buildScheduleRefinement - iun {} and id {}", iun, recIndex);
        String elementId = TimelineEventId.SCHEDULE_REFINEMENT_WORKFLOW.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());
        ScheduleRefinement details = ScheduleRefinement.builder()
                .recIndex(recIndex)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_REFINEMENT, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
    }

    public TimelineElementInternal  buildRefusedRequestTimelineElement(NotificationInt notification, List<String> errors) {
        log.debug("buildRefusedRequestTimelineElement - iun {}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_REFUSED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        RequestRefusedDetails details = RequestRefusedDetails.builder()
                .errors(errors)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.REQUEST_REFUSED, elementId, SmartMapper.mapToClass(details, TimelineElementDetails.class));
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

}
