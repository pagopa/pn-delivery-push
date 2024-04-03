package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.ResultFilter;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.paperchannel.model.SendResponse;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.AnalogDtoInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND;

@Service
@Slf4j
public class PaperChannelUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final PnDeliveryPushConfigs pnDeliveryPushConfigs;

    public PaperChannelUtils(TimelineService timelineService,
                             TimelineUtils timelineUtils,
                             PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.pnDeliveryPushConfigs = pnDeliveryPushConfigs;
    }

    public PhysicalAddressInt getSenderAddress(){
        return pnDeliveryPushConfigs.getPaperChannel().getSenderPhysicalAddress();
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


    public String buildSendAnalogDomicileEventId(NotificationInt notification, Integer recIndex, int sentAttemptMade){
        return TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
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

    public String addPrepareSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                           String eventId) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildPrepareSimpleRegisteredLetterTimelineElement(recIndex, notification, physicalAddress, eventId);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
    }

    public String addSendSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                          SendResponse sendResponse, String productType, String prepareRequestId,
                                                          List<String> replacedF24AttachmentUrls) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recIndex, notification, physicalAddress, sendResponse, productType, prepareRequestId,replacedF24AttachmentUrls);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
    }


    public String addPrepareAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, String relatedRequestId,
                                                    int sentAttemptMade, String eventId, PhysicalAddressInt discoveredAddress) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildPrepareAnalogNotificationTimelineElement(physicalAddress, recIndex, notification, relatedRequestId, sentAttemptMade, eventId, discoveredAddress);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
    }


    public void addPrepareAnalogFailureTimelineElement(PhysicalAddressInt foundAddress, String prepareRequestId, String failureCause, Integer recIndex, NotificationInt notification) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildPrepareAnalogFailureTimelineElement(foundAddress, prepareRequestId, failureCause, recIndex, notification);
        addTimelineElement(timelineElementInternal,
                notification
        );
    }

    
    public String addSendAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex,
                                                      AnalogDtoInt analogDtoInfo, List<String> replacedF24AttachmentUrls,
                                                      List<ResultFilter> acceptedAttachments, List<ResultFilter> discardedAttachments) {
        TimelineElementInternal timelineElementInternal = timelineUtils.buildSendAnalogNotificationTimelineElement(
                physicalAddress, recIndex, notification, analogDtoInfo, replacedF24AttachmentUrls, acceptedAttachments, discardedAttachments);
        addTimelineElement(timelineElementInternal,
                notification
        );
        return timelineElementInternal.getElementId();
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

    public String getSendRequestIdByPrepareRequestId(String iun, String prepareRequestId) {
        Set<TimelineElementInternal> timeline = timelineService.getTimeline(iun, false);
        Optional<String> sendRequestIdOpt =  timeline.stream()
                .filter(timelineElement -> filterSendByPrepareRequestId(timelineElement, prepareRequestId))
                .map(TimelineElementInternal::getElementId)
                .findFirst();
        
        if(sendRequestIdOpt.isPresent()){
            return sendRequestIdOpt.get();
        }else {
            log.warn("SendRequestId is not present for iun={} prepareRequestId={}", iun, prepareRequestId);
            return null;
        }
    }

    private boolean filterSendByPrepareRequestId(TimelineElementInternal el, String prepareRequestId) {
        switch(el.getCategory()) {
            case SEND_SIMPLE_REGISTERED_LETTER -> {
                SimpleRegisteredLetterDetailsInt details = (SimpleRegisteredLetterDetailsInt) el.getDetails();
                return prepareRequestId.equals(details.getPrepareRequestId());
            }
            case SEND_ANALOG_DOMICILE -> {
                SendAnalogDetailsInt details = (SendAnalogDetailsInt) el.getDetails();
                return prepareRequestId.equals(details.getPrepareRequestId());
            }
            default -> { return false; }

        }
    }

}
