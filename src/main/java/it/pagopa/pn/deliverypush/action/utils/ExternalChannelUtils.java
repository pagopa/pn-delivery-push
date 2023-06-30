package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.SendInformation;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.AarGenerationDetailsInt;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND;
import static it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;

@Service
@Slf4j
public class ExternalChannelUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;

    public ExternalChannelUtils(TimelineService timelineService,
                                TimelineUtils timelineUtils) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
    }

    public void addSendDigitalNotificationToTimeline(NotificationInt notification,
                                                     Integer recIndex,
                                                     SendInformation sendInformation,
                                                     String eventId) {
        addTimelineElement(
                timelineUtils.buildSendDigitalNotificationTimelineElement(recIndex, notification, sendInformation, eventId),
                notification
        );
    }


    private void addTimelineElement(TimelineElementInternal element, NotificationInt notification) {
        timelineService.addTimelineElement(element, notification);
    }

    public TimelineElementInternal getExternalChannelNotificationTimelineElement(String iun, String eventId) {
        //Viene ottenuto l'oggetto di timeline creato in fase d'invio notifica al public registry
        Optional<TimelineElementInternal> timelineElement = timelineService.getTimelineElement(iun, eventId);

        if (timelineElement.isPresent()) {
            return timelineElement.get();
        } else {
            log.error("There isn't timelineElement - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " eventId " + eventId, ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }

    public String getAarKey(String iun, int recIndex) {
        
        String eventId = TimelineEventId.AAR_GENERATION.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build()
        );
        
        Optional<AarGenerationDetailsInt> aarDetailsOpt = timelineService.getTimelineElementDetails(iun, eventId, AarGenerationDetailsInt.class);

        if (aarDetailsOpt.isPresent()) {
            String fileKey = aarDetailsOpt.get().getGeneratedAarUrl();
            /*
            #ECP
            if(fileKey != null){
                // elimino eventuale prefisso di safestorage
                fileKey = fileKey.replace(SAFE_STORAGE_URL_PREFIX, "");
            }*/
            return fileKey;
        } else {
            log.error("There isn't AAR timeline element - iun {} eventId {}", iun, eventId);
            throw new PnInternalException("There isn't AAR timeline element - iun " + iun + " eventId " + eventId, ERROR_CODE_DELIVERYPUSH_TIMELINENOTFOUND);
        }
    }
}
