package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotHandledDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND;

@Service
@Slf4j
public class PaperChannelUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public PaperChannelUtils(TimelineService timelineService,
                             TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }


    public String buildPrepareSimpleRegisteredLetterEventId(NotificationInt notification, Integer recIndex){
        return TimelineEventId.PREPARE_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );
    }



    public String buildPrepareAnalogDomicileEventId(NotificationInt notification, Integer recIndex, int sentAttemptMade){
        return TimelineEventId.PREPARE_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
    }


    public String buildSendAnalogFeedbackEventId(NotificationInt notification, Integer recIndex, int sentAttemptMade){
        return TimelineEventId.SEND_ANALOG_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .sentAttemptMade(sentAttemptMade)
                        .build()
        );
    }

    public void addPrepareSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                           String eventId, Integer numberOfPages) {
        addTimelineElement(
                timelineUtils.buildPrepareSimpleRegisteredLetterTimelineElement(recIndex, notification, physicalAddress, eventId, numberOfPages),
                notification
        );
    }

    public void addSendSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                        String eventId, Integer analogCost) {
        addTimelineElement(
                timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recIndex, notification, physicalAddress, eventId, analogCost),
                notification
        );
    }


    public void addPrepareAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, String relatedRequestId,
                                                    int sentAttemptMade, String eventId) {
        addTimelineElement(
                timelineUtils.buildPrepareAnalogNotificationTimelineElement(physicalAddress, recIndex, notification, relatedRequestId, sentAttemptMade, eventId),
                notification
        );
    }
    public void addSendAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, String eventId,
                                                    int sentAttemptMade, Integer analogCost, String relatedRequestId) {
        addTimelineElement(
                timelineUtils.buildSendAnalogNotificationTimelineElement(physicalAddress, recIndex, notification, relatedRequestId, sentAttemptMade, eventId, analogCost),
                notification
        );
    }


    public void addPaperNotificationNotHandledToTimeline(NotificationInt notification, Integer recIndex) {
        addTimelineElement(
                timelineUtils.buildNotHandledTimelineElement(
                        notification,
                        recIndex,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_CODE,
                        NotHandledDetailsInt.PAPER_MESSAGE_NOT_HANDLED_REASON
                ),
                notification
        );
    }

    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    public TimelineElementInternal getPaperChannelNotificationTimelineElement(String iun, String eventId) {
        //Viene ottenuto l'oggetto di timeline
        Optional<TimelineElementInternal> timelineElement = timelineService.getTimelineElement(iun, eventId);

        if (timelineElement.isPresent()) {
            return timelineElement.get();
        } else {
            log.error("There isn't timelineElement - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " eventId " + eventId, ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }
}
