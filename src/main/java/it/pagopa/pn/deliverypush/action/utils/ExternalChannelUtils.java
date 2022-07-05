package it.pagopa.pn.deliverypush.action.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.service.TimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ExternalChannelUtils {
    private final TimelineService timelineService;
    private final TimelineUtils timelineUtils;
    private final InstantNowSupplier instantNowSupplier;
    
    public ExternalChannelUtils(TimelineService timelineService,
                                TimelineUtils timelineUtils, 
                                InstantNowSupplier instantNowSupplier) {
        this.timelineService = timelineService;
        this.timelineUtils = timelineUtils;
        this.instantNowSupplier = instantNowSupplier;
    }

    public void addSendDigitalNotificationToTimeline(NotificationInt notification, LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, Integer recIndex, int sentAttemptMade, String eventId) {
        addTimelineElement(
                timelineUtils.buildSendDigitalNotificationTimelineElement(digitalAddress, addressSource, recIndex, notification, sentAttemptMade, eventId),
                notification
        );
    }

    public void addSendSimpleRegisteredLetterToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, 
                                                        String eventId, Integer numberOfPages) {
        addTimelineElement(
                timelineUtils.buildSendSimpleRegisteredLetterTimelineElement(recIndex, notification, physicalAddress, eventId, numberOfPages),
                notification
        );
    }
    
    public void addSendAnalogNotificationToTimeline(NotificationInt notification, PhysicalAddressInt physicalAddress, Integer recIndex, boolean investigation,
                                                    int sentAttemptMade, String eventId, Integer numberOfPages) {
        addTimelineElement(
                timelineUtils.buildSendAnalogNotificationTimelineElement(physicalAddress, recIndex, notification, investigation, sentAttemptMade, eventId, numberOfPages),
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
            throw new PnInternalException("There isn't timelineElement - iun " + iun + " eventId " + eventId);
        }
    }
}
