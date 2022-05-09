package it.pagopa.pn.deliverypush.action2.utils;

import it.pagopa.pn.commons.utils.DateUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.Notification;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationAttachment;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelResponse;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TimelineUtils {

    private final InstantNowSupplier instantNowSupplier;

    public TimelineUtils(InstantNowSupplier instantNowSupplier) {
        this.instantNowSupplier = instantNowSupplier;
    }

    public TimelineElementInternal buildTimeline(String iun, TimelineElementCategory category, String elementId, TimelineElementDetails details) {
        return buildTimeline( iun, category, elementId, details, Collections.emptyList() );
    }

    public TimelineElementInternal buildTimeline(String iun, TimelineElementCategory category, String elementId,
                                         TimelineElementDetails details,  List<LegalFactsId> legalFactsListEntryIds) {

        return new TimelineElementInternal(
                TimelineElement.builder()
                        .category(category)
                        .timestamp(DateUtils.convertInstantToDate(instantNowSupplier.get()))
                        .elementId(elementId)
                        .details(details)
                        .legalFactsIds( legalFactsListEntryIds )
                        .build(), 
                iun
        );
    }
    
    public TimelineElementDetails getGenericDetails(Object specificDetails){
        TimelineElementDetails timelineElementDetails = new TimelineElementDetails();
        BeanUtils.copyProperties(specificDetails, timelineElementDetails);
        return timelineElementDetails;
    }

    public void getSpecificDetails(TimelineElementDetails genericDetails, Object specificDetails){
        //TODO Verificare se funziona
        BeanUtils.copyProperties(genericDetails, specificDetails,
                getNullPropertyNames(genericDetails));
    }
    
    private <T> String[] getNullPropertyNames (T source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for(java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
    
    public TimelineElementInternal  buildAcceptedRequestTimelineElement(Notification notification, String legalFactId) {
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
                .source(source)
                .isAvailable(isAvailable)
                .attemptDate(DateUtils.convertInstantToDate(instantNowSupplier.get()))
                .build();
        
        return buildTimeline(iun, TimelineElementCategory.GET_ADDRESS, elementId, getGenericDetails(details));
    }


    public TimelineElementInternal buildDigitaFeedbackTimelineElement(ExtChannelResponse response, SendDigitalDetails sendDigitalDetails) {
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
                .notificationDate(DateUtils.convertInstantToDate(instantNowSupplier.get()))
                .build();

        return buildTimeline(response.getIun(), TimelineElementCategory.SEND_DIGITAL_FEEDBACK, elementId, getGenericDetails(details));
    }

    public TimelineElementInternal buildSendCourtesyMessageTimelineElement(Integer recIndex, String iun, DigitalAddress address, Instant sendDate, String eventId) {
        log.debug("buildSendCourtesyMessageTimelineElement - IUN {} and id {}", iun, recIndex);

        SendCourtesyMessageDetails details = SendCourtesyMessageDetails.builder()
                .recIndex(recIndex)
                .address(address)
                .sendDate(DateUtils.convertInstantToDate(sendDate))
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_COURTESY_MESSAGE, eventId, getGenericDetails(details));
    }


    public TimelineElementInternal buildSendSimpleRegisteredLetterTimelineElement(Integer recIndex, String iun, PhysicalAddress address, String eventId) {
        log.debug("buildSendSimpleRegisteredLetterTimelineElement - IUN {} and id {}", iun, recIndex);

        SimpleRegisteredLetterDetails details = SimpleRegisteredLetterDetails.builder()
                .recIndex(recIndex)
                .address(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER, eventId, getGenericDetails(details));
    }


    public TimelineElementInternal buildSendDigitalNotificationTimelineElement(DigitalAddress digitalAddress, DigitalAddressSource addressSource, Integer recIndex, Notification notification, int sentAttemptMade, String eventId) {
        log.debug("buildSendDigitalNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recIndex);

        SendDigitalDetails details = SendDigitalDetails.builder()
                .recIndex(recIndex)
                .retryNumber(sentAttemptMade)
                .address(digitalAddress)
                .addressSource(addressSource)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_DIGITAL_DOMICILE, eventId, getGenericDetails(details));
    }


    public TimelineElementInternal buildSendAnalogNotificationTimelineElement(PhysicalAddress address, Integer recIndex, Notification notification, boolean investigation,
                                                                      int sentAttemptMade, String eventId) {
        log.debug("buildSendAnalogNotificationTimelineElement - IUN {} and id {}", notification.getIun(), recIndex);

        SendPaperDetails details = SendPaperDetails.builder()
                .recIndex(recIndex)
                .address(address)
                .serviceLevel(notification.getPhysicalCommunicationType())
                .sentAttemptMade(sentAttemptMade)
                .investigation(investigation)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.SEND_ANALOG_DOMICILE, eventId, getGenericDetails(details));
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
                .address(address)
                .build();

        List<LegalFactsId> legalFactIds = singleLegalFactId(legalFactId, LegalFactCategory.DIGITAL_DELIVERY);
        return buildTimeline(iun, TimelineElementCategory.DIGITAL_SUCCESS_WORKFLOW, elementId, getGenericDetails(details), legalFactIds);
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
        return buildTimeline(iun, TimelineElementCategory.DIGITAL_FAILURE_WORKFLOW, elementId, getGenericDetails(details), legalFactIds);
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
                .address(address)
                .build();

        return buildTimeline(iun, TimelineElementCategory.ANALOG_SUCCESS_WORKFLOW, elementId, getGenericDetails(details));
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

        return buildTimeline(iun, TimelineElementCategory.ANALOG_FAILURE_WORKFLOW, elementId, getGenericDetails(details));
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

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE, correlationId, getGenericDetails(details));
    }


    public TimelineElementInternal  buildPublicRegistryCallTimelineElement(String iun, Integer recIndex, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        log.debug("buildPublicRegistryCallTimelineElement - iun {} and id {}", iun, recIndex);

        PublicRegistryCallDetails details = PublicRegistryCallDetails.builder()
                .recIndex(recIndex)
                .contactPhase(contactPhase)
                .sentAttemptMade(sentAttemptMade)
                .deliveryMode(deliveryMode)
                .sendDate(DateUtils.convertInstantToDate(instantNowSupplier.get()))
                .build();

        return buildTimeline(iun, TimelineElementCategory.PUBLIC_REGISTRY_CALL, eventId, getGenericDetails(details));
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
                .address(sendPaperDetails.getAddress())
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
                                                                            elementId, getGenericDetails(details), legalFactsListEntryIds );
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
        return buildTimeline(iun, TimelineElementCategory.NOTIFICATION_VIEWED, elementId, getGenericDetails(details), legalFactIds);
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

        return buildTimeline(iun, TimelineElementCategory.COMPLETELY_UNREACHABLE, elementId, getGenericDetails(details));
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
                .lastAttemptInfo(lastAttemptInfo)
                .build();

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_DIGITAL_WORKFLOW, elementId, getGenericDetails(details));
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

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_ANALOG_WORKFLOW, elementId, getGenericDetails(details));
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

        return buildTimeline(iun, TimelineElementCategory.REFINEMENT, elementId, getGenericDetails(details));
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

        return buildTimeline(iun, TimelineElementCategory.SCHEDULE_REFINEMENT, elementId, getGenericDetails(details));
    }

    public TimelineElementInternal  buildRefusedRequestTimelineElement(Notification notification, List<String> errors) {
        log.debug("buildRefusedRequestTimelineElement - iun {}", notification.getIun());

        String elementId = TimelineEventId.REQUEST_REFUSED.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .build());

        RequestRefusedDetails details = RequestRefusedDetails.builder()
                .errors(errors)
                .build();

        return buildTimeline(notification.getIun(), TimelineElementCategory.REQUEST_REFUSED, elementId, getGenericDetails(details));
    }

    public List<LegalFactsId> singleLegalFactId(String legalFactKey, LegalFactCategory type) {
        return Collections.singletonList( LegalFactsId.builder()
                .key( legalFactKey )
                .category( type )
                .build() );
    }

}
