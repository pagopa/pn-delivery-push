package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.api.dto.notification.NotificationAttachment;
import it.pagopa.pn.api.dto.notification.NotificationRecipient;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.api.dto.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.commons_delivery.middleware.TimelineDao;
import it.pagopa.pn.deliverypush.actions.PecFailSendPaperActionHandler;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TimeLineServiceImpl implements TimelineService {
    private final TimelineDao timelineDao;

    public TimeLineServiceImpl(TimelineDao timelineDao) {
        this.timelineDao = timelineDao;
    }

    @Override
    public void addTimelineElement(TimelineElement element) {

    }

    @Override
    public Optional<TimelineElement> getTimelineElement(String iun, String timelineId) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getTimelineElement(String iun, String timelineId, Class<T> timelineDetailsClass) {
        log.debug("Start getTimelineElement for iun {} timelineId {}", iun, timelineId);

        Optional<TimelineElement> row = this.timelineDao.getTimelineElement(iun, timelineId);
        return row.map(el -> timelineDetailsClass.cast(el.getDetails()));
    }

    @Override
    public Set<TimelineElement> getTimeline(String iun) {
        log.debug("Start getTimeline for iun {} ", iun);
        return this.timelineDao.getTimeline(iun);
    }

    /**
     * Insert availability information in timeline for user
     *
     * @param taxId       User identifier
     * @param iun         iun Notification unique identifier
     * @param source      Source address PLATFORM, SPECIAL, GENERAL
     * @param isAvailable is address available
     */
    @Override
    public void addAvailabilitySourceToTimeline(String taxId, String iun, DigitalAddressSource source, boolean isAvailable, int sentAttemptMade) {

        addTimelineElement(TimelineElement.builder()
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
                                .build())
                .build());
    }

    @Override
    public void addDigitalFailureAttemptToTimeline(ExtChannelResponse response) {

        addTimelineElement(TimelineElement.builder()
                .iun(response.getIun())
                .category(TimelineElementCategory.SEND_DIGITAL_FEEDBACK_FAILURE)
                .details(SendDigitalFeedbackFailure.builder()
                        .errors(response.getErrorList())
                        .address(response.getDigitalUsedAddress())
                        .taxId(response.getTaxId())
                        .build())
                .build());
    }

    @Override
    public void addSendCourtesyMessageToTimeline(String taxId, String iun, DigitalAddress address, Instant sendDate, String eventId) {
        log.debug("Add send courtesy message to timeline");

        //Viene aggiunto l'invio alla timeline con un particolare elementId utile a ottenere tali elementi successivamente nel workflow (Start analog workflow)
        addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SEND_COURTESY_MESSAGE)
                .elementId(eventId)
                .iun(iun)
                .timestamp(Instant.now())
                .details(SendCourtesyMessageDetails.builder()
                        .taxId(taxId)
                        .address(address)
                        .sendDate(sendDate)
                        .build())
                .build());
    }

    @Override
    public void addSendSimpleRegisteredLetterToTimeline(String taxId, String iun, PhysicalAddress address, String eventId) {
        log.debug("Add send simple registered letter to timeline");

        //Viene aggiunto l'invio alla timeline con un particolare elementId utile a ottenere tali elementi successivamente nel workflow (Start analog workflow)
        addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SEND_SIMPLE_REGISTERED_LETTER)
                .elementId(eventId)
                .iun(iun)
                .timestamp(Instant.now())
                .details(SimpleRegisteredLetterDetails.builder()
                        .taxId(taxId)
                        .address(address)
                        .build())
                .build());
    }

    @Override
    public void addSendDigitalNotificationToTimeline(DigitalAddress digitalAddress, NotificationRecipient recipient, Notification notification, int sentAttemptMade, String eventId) {
        addTimelineElement(
                TimelineElement.builder()
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
                        .build());
    }

    @Override
    public void addSendAnalogNotificationToTimeline(PhysicalAddress address, NotificationRecipient recipient, Notification notification, boolean investigation,
                                                    int sentAttemptMade, String eventId) {
        addTimelineElement(
                TimelineElement.builder()
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
                        ).build());
    }

    @Override
    public void addSuccessDigitalWorkflowToTimeline(String taxId, String iun, DigitalAddress address) {
        log.debug("AddSuccessWorkflowToTimeline for iun {} id {}", iun, taxId);

        addTimelineElement(TimelineElement.builder()
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
                .build());
    }

    @Override
    public void addFailureDigitalWorkflowToTimeline(String taxId, String iun) {
        log.debug("Add Failure Workflow To Timeline for iun {} id {}", iun, taxId);

        addTimelineElement(TimelineElement.builder()
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
                .build());
    }

    @Override
    public void addSuccessAnalogWorkflowToTimeline(String taxId, String iun, PhysicalAddress address) {
        addTimelineElement(TimelineElement.builder()
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
                .build());
    }

    @Override
    public void addFailureAnalogWorkflowToTimeline(String taxId, String iun) {
        addTimelineElement(TimelineElement.builder()
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
                .build());
    }

    @Override
    public void addPublicRegistryResponseCallToTimeline(String iun, String taxId, PublicRegistryResponse response) {
        addTimelineElement(TimelineElement.builder()
                .iun(iun)
                .elementId(response.getCorrelationId())
                .category(TimelineElementCategory.PUBLIC_REGISTRY_RESPONSE)
                .timestamp(Instant.now())
                .details(PublicRegistryResponseDetails.builder()
                        .taxId(taxId)
                        .digitalAddress(response.getDigitalAddress())
                        .physicalAddress(response.getPhysicalAddress())
                        .build())
                .build());
    }

    @Override
    public void addPublicRegistryCallToTimeline(String iun, String taxId, String eventId, DeliveryMode deliveryMode, ContactPhase contactPhase, int sentAttemptMade) {
        addTimelineElement(TimelineElement.builder()
                .iun(iun)
                .elementId(eventId)
                .category(TimelineElementCategory.PUBLIC_REGISTRY_CALL)
                .details(PublicRegistryCallDetails.builder()
                        .taxId(taxId)
                        .contactPhase(contactPhase)
                        .sentAttemptMade(sentAttemptMade)
                        .deliveryMode(deliveryMode)
                        .build())
                .build());
    }

    @Override
    public void addAnalogFailureAttemptToTimeline(ExtChannelResponse response, int sentAttemptMade) {
        String iun = response.getIun();
        String taxId = response.getTaxId();

        addTimelineElement(TimelineElement.builder()
                .category(TimelineElementCategory.SEND_PAPER_FEEDBACK)
                .elementId(TimelineEventId.SEND_PAPER_FEEDBACK.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recipientId(taxId)
                                .index(sentAttemptMade)
                                .build()
                ))
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
                .build());
    }

    public void addAcceptedRequestToTimeline(Notification notification, String taxId) {

        addTimelineElement(TimelineElement.builder()
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
                .build());
    }


}
